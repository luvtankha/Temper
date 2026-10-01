"""Create a pinned Android build spec only for an independently evaluated candidate."""
import argparse
import json
import math
from pathlib import Path
from urllib.parse import urlsplit
from importlib.util import spec_from_file_location, module_from_spec

spec = spec_from_file_location("feedback_training", Path(__file__).with_name("train-feedback-model.py"))
training = module_from_spec(spec)
spec.loader.exec_module(training)


def approve(report, model, url):
    parsed = urlsplit(url)
    if parsed.scheme != "https" or not parsed.hostname or parsed.username or parsed.password or parsed.fragment:
        raise ValueError("An HTTPS public model artifact URL is required")
    if report.get("synthetic") is not False or report.get("promotable") is not True or report.get("reasons"):
        raise ValueError("Evaluation did not approve this candidate")
    if report.get("outputLabels") != 28 or report.get("maxTokens") != 128:
        raise ValueError("Incompatible model schema")
    if report.get("reviewedExamples", 0) < 1000 or report.get("contributors", 0) < 50:
        raise ValueError("Insufficient reviewed contributors/examples")
    groups = [set(report["splitGroups"][name]) for name in ("train", "validation", "test")]
    if any(not group for group in groups) or any(groups[i] & groups[j] for i in range(3) for j in range(i)):
        raise ValueError("Leaking contributor split")
    before, after = report["baseline"], report["candidate"]
    for metrics in (before, after):
        for values in metrics.values():
            scores = [values["macroF1"], *values["perLabelF1"].values()]
            if any(not isinstance(value, (int, float)) or not math.isfinite(value) or not 0 <= value <= 1 for value in scores):
                raise ValueError("Invalid evaluation scores")
    if any(name not in before or before[name]["examples"] != values["examples"] for name, values in after.items()):
        raise ValueError("Baseline and candidate must use the same held-out examples")
    if after["all"]["examples"] < 100 or after["all"]["macroF1"] < before["all"]["macroF1"] + .02:
        raise ValueError("Held-out improvement not established")
    for language in ("ENGLISH", "HINGLISH"):
        if language not in after or after[language]["examples"] < 50 or after[language]["macroF1"] < before[language]["macroF1"] - .01:
            raise ValueError("Language gate failed")
    for label in training.LABELS:
        if after["all"]["positiveCounts"][label] < 5 or after["all"]["perLabelF1"][label] < before["all"]["perLabelF1"][label] - .03:
            raise ValueError("Emotion gate failed")
    if report.get("vocabSha256") != training.VOCAB_HASH or report.get("mergesSha256") != training.MERGES_HASH:
        raise ValueError("Tokenizer differs from Android")
    if training.digest(model) != report["modelSha256"] or model.stat().st_size != report["modelSize"] or not 10_000_000 <= model.stat().st_size <= 200_000_000:
        raise ValueError("Model artifact mismatch or outside mobile size bounds")
    import onnxruntime as ort
    session = ort.InferenceSession(str(model), providers=["CPUExecutionProvider"])
    if session.get_outputs()[0].shape[-1] != 28 or {item.name for item in session.get_inputs()} != {"input_ids", "attention_mask"}:
        raise ValueError("Model runtime schema mismatch")
    return {"approved": True, "url": url, "sha256": report["modelSha256"], "size": report["modelSize"],
            "outputLabels": 28, "maxTokens": 128, "vocabSha256": training.VOCAB_HASH, "mergesSha256": training.MERGES_HASH}


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--report", required=True, type=Path)
    parser.add_argument("--model", required=True, type=Path)
    parser.add_argument("--url", required=True)
    parser.add_argument("--output", required=True, type=Path)
    args = parser.parse_args()
    approved = approve(json.loads(args.report.read_text(encoding="utf-8")), args.model, args.url)
    with args.output.open("x", encoding="utf-8") as output:
        json.dump(approved, output, indent=2)
    print("Pinned model spec prepared for publisher/device review. Build a new app release; nothing was uploaded.")


if __name__ == "__main__":
    main()
