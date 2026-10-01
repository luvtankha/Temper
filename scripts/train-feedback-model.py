"""Offline, reviewed feedback training. User stars prioritize review; they are not gold labels.

No network calls, automatic publication, or implicit use of prediction scores as truth.
The trained ONNX keeps the original 28-output schema used by Android.
"""
import argparse
import hashlib
import json
import random
import time
from pathlib import Path

LABELS = ["neutral", "happy", "concerned", "confused", "sad", "frustrated", "angry", "surprised"]
VOCAB_HASH = "ed19656ea1707df69134c4af35c8ceda2cc9860bf2c3495026153a133670ab5e"
MERGES_HASH = "1ce1664773c50f3e0cc8842619a93edc4624525b728b188a9e0be33b7726adc5"


def digest(path):
    result = hashlib.sha256()
    with open(path, "rb") as stream:
        for block in iter(lambda: stream.read(65536), b""):
            result.update(block)
    return result.hexdigest()


def rows(path):
    with open(path, encoding="utf-8") as stream:
        for line in stream:
            if line.strip():
                yield json.loads(line)


def prepare(export, annotations):
    """Annotations are made by the publisher's reviewers, never requested from users."""
    sessions = {}
    for record in rows(export):
        now = int(time.time() * 1000)
        if not now - 3_600_000 <= record.get("exportedAt", 0) <= now + 60_000:
            raise ValueError("Create a fresh active export immediately before training; do not reuse old copies")
        if record.get("expiresAt", 0) <= now:
            continue
        owner = record["contributor"]
        if len(owner) != 64 or any(c not in "0123456789abcdef" for c in owner):
            raise ValueError("Invalid contributor group")
        submission = record["submission"]
        if not 1 <= submission["qualityRating"] <= 5:
            raise ValueError("Invalid rating")
        key = (owner, submission["sessionId"])
        if key in sessions:
            raise ValueError("Duplicate exported session")
        sessions[key] = submission
    examples, seen, text_owners = [], set(), {}
    for annotation in rows(annotations):
        if annotation.get("humanReviewed") is not True:
            raise ValueError("An independent human-reviewed label is required")
        key = (annotation["contributor"], annotation["sessionId"])
        if key not in sessions:
            continue  # Deleted or expired contributions are absent from the current active snapshot.
        submission = sessions[key]
        target = annotation["target"]
        identity = (*key, target)
        if identity in seen:
            raise ValueError("Duplicate annotation")
        seen.add(identity)
        if target not in {item["target"] for item in submission["analyses"]}:
            raise ValueError("Annotation does not match a recorded estimate")
        turn = submission["turns"][target]
        if turn["role"] != "REMOTE":
            raise ValueError("Remote target required")
        labels = annotation["labels"]
        if not labels or len(set(labels)) != len(labels) or any(label not in LABELS for label in labels):
            raise ValueError("Invalid reviewed labels")
        if "neutral" in labels and len(labels) != 1:
            raise ValueError("Neutral is exclusive")
        language = annotation["language"]
        if language not in ("ENGLISH", "HINGLISH", "OTHER"):
            raise ValueError("Reviewer must classify language")
        text = turn["text"]
        if not text.strip() or len(text) > 1000:
            raise ValueError("Invalid training text")
        normalized = " ".join(text.casefold().split())
        text_hash = hashlib.sha256(normalized.encode()).hexdigest()
        # Drop shared/repeated text across contributors so copied messages do not leak across splits.
        text_owners.setdefault(text_hash, set()).add(key[0])
        examples.append({"group": key[0], "textHash": text_hash, "text": text,
                         "labels": [int(label in labels) for label in LABELS],
                         "language": language, "rating": submission["qualityRating"]})
    unique = {}
    for example in examples:
        if len(text_owners[example["textHash"]]) != 1:
            continue
        identity = (example["group"], example["textHash"])
        if identity in unique and unique[identity]["labels"] != example["labels"]:
            raise ValueError("Conflicting labels for identical text require review")
        unique[identity] = example
    return list(unique.values())


def split_examples(examples):
    groups = sorted({row["group"] for row in examples})
    random.Random(42).shuffle(groups)
    n = len(groups)
    if n < 5:
        raise ValueError("At least five independent contributor groups are required even for a trial")
    assigned = {group: "train" if i < int(n * .7) else "validation" if i < int(n * .85) else "test"
                for i, group in enumerate(groups)}
    return {name: [row for row in examples if assigned[row["group"]] == name]
            for name in ("train", "validation", "test")}


def mapped_scores(logits):
    import torch
    p = torch.sigmoid(logits)
    return torch.stack([p[:, 27], p[:, 17], torch.maximum(p[:, 14], p[:, 19]),
                        p[:, 6], p[:, 25], p[:, 3], p[:, 2], p[:, 26]], dim=1)


def metrics(gold, scores):
    import numpy as np
    gold = np.asarray(gold, dtype=bool)
    predicted = np.asarray(scores) >= .5
    tp = (gold & predicted).sum(axis=0)
    fp = (~gold & predicted).sum(axis=0)
    fn = (gold & ~predicted).sum(axis=0)
    f1 = 2 * tp / np.maximum(2 * tp + fp + fn, 1)
    return {"macroF1": float(f1.mean()), "perLabelF1": dict(zip(LABELS, f1.tolist())),
            "positiveCounts": dict(zip(LABELS, gold.sum(axis=0).tolist())), "examples": len(gold)}


def evaluate(model, tokenizer, examples):
    import torch
    scores = []
    model.eval()
    with torch.no_grad():
        for start in range(0, len(examples), 8):
            encoded = tokenizer([row["text"] for row in examples[start:start + 8]],
                                padding=True, truncation=True, max_length=128, return_tensors="pt")
            scores.extend(mapped_scores(model(**encoded).logits).tolist())
    return metric_groups(examples, scores)


def metric_groups(examples, scores):
    result = {"all": metrics([row["labels"] for row in examples], scores)}
    for language in ("ENGLISH", "HINGLISH", "OTHER"):
        indexes = [i for i, row in enumerate(examples) if row["language"] == language]
        if indexes:
            result[language] = metrics([examples[i]["labels"] for i in indexes], [scores[i] for i in indexes])
    return result


def export_quantized(model, destination, tokens=None):
    import torch
    from onnxruntime.quantization import quantize_dynamic, QuantType
    class Outputs(torch.nn.Module):
        def __init__(self, model):
            super().__init__()
            self.model = model
        def forward(self, input_ids, attention_mask):
            return self.model(input_ids=input_ids, attention_mask=attention_mask).logits
    model.eval()
    inputs = tokens if tokens is not None else (torch.tensor([[0, 10, 20, 2]]), torch.ones(1, 4, dtype=torch.int64))
    full = destination / "model-fp32.onnx"
    torch.onnx.export(Outputs(model), inputs, str(full), opset_version=17, dynamo=False,
                      input_names=["input_ids", "attention_mask"], output_names=["logits"],
                      dynamic_axes={"input_ids": {0: "batch", 1: "sequence"},
                                    "attention_mask": {0: "batch", 1: "sequence"}, "logits": {0: "batch"}})
    quantized = destination / "model-quantized.onnx"
    quantize_dynamic(str(full), str(quantized), weight_type=QuantType.QInt8)
    return quantized


def evaluate_onnx(path, tokenizer, examples):
    import onnxruntime as ort
    import numpy as np
    session = ort.InferenceSession(str(path), providers=["CPUExecutionProvider"])
    scores = []
    for row in examples:
        encoded = tokenizer(row["text"], truncation=True, max_length=128, return_tensors="np")
        logits = session.run(None, {name: encoded[name].astype(np.int64) for name in ("input_ids", "attention_mask")})[0][0]
        if len(logits) != 28 or not np.isfinite(logits).all():
            raise ValueError("Incompatible ONNX output")
        p = 1 / (1 + np.exp(-np.clip(logits, -80, 80)))
        scores.append([p[27], p[17], max(p[14], p[19]), p[6], p[25], p[3], p[2], p[26]])
    return metric_groups(examples, scores)


def eligible(examples, split, baseline, candidate):
    reasons = []
    if len(examples) < 1000 or len({row["group"] for row in examples}) < 50:
        reasons.append("Need at least 1000 reviewed examples from 50 independent contributors")
    if len(split["test"]) < 100:
        reasons.append("Need at least 100 held-out examples")
    if candidate["all"]["macroF1"] < baseline["all"]["macroF1"] + .02:
        reasons.append("Held-out macro F1 improvement is below 0.02")
    for language in ("ENGLISH", "HINGLISH"):
        if language not in candidate or candidate[language]["examples"] < 50:
            reasons.append("Need at least 50 held-out " + language + " examples")
        elif candidate[language]["macroF1"] < baseline[language]["macroF1"] - .01:
            reasons.append(language + " performance regressed")
    for label in LABELS:
        if candidate["all"]["positiveCounts"][label] < 5:
            reasons.append("Too few held-out positives: " + label)
        if candidate["all"]["perLabelF1"][label] < baseline["all"]["perLabelF1"][label] - .03:
            reasons.append("Label performance regressed: " + label)
    return reasons


def smoke(destination):
    """Real training/export/inference on a tiny synthetic model; no accuracy or release claim."""
    import torch
    import onnxruntime as ort
    import numpy as np
    from transformers import RobertaConfig, RobertaForSequenceClassification
    torch.manual_seed(42)
    config = RobertaConfig(vocab_size=32, hidden_size=16, num_hidden_layers=1,
                           num_attention_heads=2, intermediate_size=32, max_position_embeddings=132,
                           num_labels=28, attention_probs_dropout_prob=0, hidden_dropout_prob=0)
    config._attn_implementation = "eager"
    model = RobertaForSequenceClassification(config)
    input_ids, mask = torch.tensor([[0, 10, 20, 2]]), torch.ones(1, 4, dtype=torch.int64)
    optimizer = torch.optim.AdamW(model.parameters(), lr=.01)
    target = torch.tensor([[0., 1., 0., 0., 0., 0., 0., 0.]])
    before = float(torch.nn.functional.binary_cross_entropy(mapped_scores(model(input_ids, attention_mask=mask).logits), target).detach())
    for _ in range(8):
        optimizer.zero_grad()
        loss = torch.nn.functional.binary_cross_entropy(mapped_scores(model(input_ids, attention_mask=mask).logits), target)
        loss.backward()
        optimizer.step()
    after = float(torch.nn.functional.binary_cross_entropy(mapped_scores(model(input_ids, attention_mask=mask).logits), target).detach())
    assert after < before
    path = export_quantized(model, destination, (input_ids, mask))
    output = ort.InferenceSession(str(path), providers=["CPUExecutionProvider"]).run(None, {"input_ids": input_ids.numpy(), "attention_mask": mask.numpy()})[0]
    assert output.shape == (1, 28) and np.isfinite(output).all()
    (destination / "smoke-report.json").write_text(json.dumps({"synthetic": True, "promotable": False,
            "lossBefore": before, "lossAfter": after, "onnxSha256": digest(path)}, indent=2))
    print("PASS: synthetic gradient training, original 28-label export, INT8 CPU inference. No production upgrade.")


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--export", type=Path)
    parser.add_argument("--annotations", type=Path)
    parser.add_argument("--base", type=Path, help="Local original 28-label PyTorch checkpoint; no automatic download")
    parser.add_argument("--baseline-onnx", type=Path, help="The actual deployed model for honest INT8 comparison")
    parser.add_argument("--output", required=True, type=Path)
    parser.add_argument("--epochs", type=int, default=2)
    parser.add_argument("--smoke-test", action="store_true")
    args = parser.parse_args()
    if args.output.exists():
        raise ValueError("Use a new private output directory")
    args.output.mkdir(parents=True, mode=0o700)
    if args.smoke_test:
        smoke(args.output)
        return
    if not all((args.export, args.annotations, args.base, args.baseline_onnx)) or not 1 <= args.epochs <= 5:
        parser.error("Provide export, independent annotations, local base, actual baseline ONNX and 1–5 epochs")
    import torch
    from transformers import AutoTokenizer, AutoModelForSequenceClassification
    if digest(args.base / "vocab.json") != VOCAB_HASH or digest(args.base / "merges.txt") != MERGES_HASH:
        raise ValueError("Tokenizer must match the Android vocabulary and merges")
    examples = prepare(args.export, args.annotations)
    split = split_examples(examples)
    if any(not values for values in split.values()):
        raise ValueError("Empty split")
    tokenizer = AutoTokenizer.from_pretrained(args.base, local_files_only=True)
    model = AutoModelForSequenceClassification.from_pretrained(args.base, local_files_only=True, attn_implementation="eager")
    expected = {2: "anger", 3: "annoyance", 6: "confusion", 14: "fear", 17: "joy", 19: "nervousness", 25: "sadness", 26: "surprise", 27: "neutral"}
    if model.config.num_labels != 28 or any(model.config.id2label[i].lower() != label for i, label in expected.items()):
        raise ValueError("Checkpoint must preserve GoEmotions label order")
    baseline = evaluate_onnx(args.baseline_onnx, tokenizer, split["test"])
    torch.manual_seed(42)
    optimizer = torch.optim.AdamW(model.parameters(), lr=1e-5)
    training = list(split["train"])
    for epoch in range(args.epochs):
        random.Random(42 + epoch).shuffle(training)
        model.train()
        for start in range(0, len(training), 4):
            batch = training[start:start + 4]
            inputs = tokenizer([row["text"] for row in batch], padding=True, truncation=True, max_length=128, return_tensors="pt")
            labels = torch.tensor([row["labels"] for row in batch], dtype=torch.float32)
            optimizer.zero_grad()
            loss = torch.nn.functional.binary_cross_entropy(mapped_scores(model(**inputs).logits), labels)
            loss.backward()
            torch.nn.utils.clip_grad_norm_(model.parameters(), 1)
            optimizer.step()
        # Validation is separate; the held-out test is evaluated only after the fixed training schedule.
        print(json.dumps({"epoch": epoch + 1, "validation": evaluate(model, tokenizer, split["validation"])}))
    model.save_pretrained(args.output / "checkpoint")
    tokenizer.save_pretrained(args.output / "checkpoint")
    path = export_quantized(model, args.output)
    candidate = evaluate_onnx(path, tokenizer, split["test"])
    reasons = eligible(examples, split, baseline, candidate)
    groups = {name: sorted({row["group"] for row in values}) for name, values in split.items()}
    report = {"schemaVersion": 1, "synthetic": False, "promotable": not reasons, "reasons": reasons,
              "reviewedExamples": len(examples), "contributors": len({row["group"] for row in examples}),
              "splitGroups": groups, "baselineHash": digest(args.baseline_onnx), "baseline": baseline,
              "candidate": candidate, "modelSha256": digest(path), "modelSize": path.stat().st_size,
              "vocabSha256": VOCAB_HASH, "mergesSha256": MERGES_HASH, "outputLabels": 28, "maxTokens": 128}
    (args.output / "evaluation.json").write_text(json.dumps(report, indent=2), encoding="utf-8")
    print("Candidate evaluated. " + ("Eligible for publisher review." if not reasons else "Promotion blocked by evaluation gates."))


if __name__ == "__main__":
    main()
