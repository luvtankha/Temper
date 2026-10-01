"""Pinned local export. No conversation data is needed or uploaded. Binaries stay outside Git."""
import argparse, hashlib, json, os
from pathlib import Path
os.environ["HF_HUB_DISABLE_TELEMETRY"] = "1"
import numpy as np
import torch
import onnx
import onnxruntime as ort
from transformers import AutoModelForSequenceClassification, RobertaTokenizerFast

parser = argparse.ArgumentParser()
parser.add_argument("directory", type=Path)
args = parser.parse_args()
directory = args.directory.resolve()
expected = "4d24a3e32a88ed1c4e5b789fc6644e2e767500554e954b27dccf52a8e762cbae"
assert hashlib.sha256((directory / "pytorch_model.bin").read_bytes()).hexdigest() == expected
torch.set_num_threads(2)
tokenizer = RobertaTokenizerFast(vocab_file=str(directory / "vocab.json"), merges_file=str(directory / "merges.txt"))
tokenizer.save_pretrained(directory)
model = AutoModelForSequenceClassification.from_pretrained(directory, local_files_only=True, attn_implementation="eager").eval()
assert model.config.id2label == {0: "negative", 1: "neutral", 2: "positive"}, model.config.id2label

class Export(torch.nn.Module):
    def __init__(self, model):
        super().__init__()
        self.model = model
    def forward(self, input_ids, attention_mask):
        return self.model(input_ids=input_ids, attention_mask=attention_mask).logits

sample = tokenizer("A wonderful movie", return_tensors="pt")
with torch.no_grad():
    torch.onnx.export(Export(model).eval(), (sample["input_ids"], sample["attention_mask"]), str(directory / "model.onnx"),
        input_names=["input_ids", "attention_mask"], output_names=["logits"], opset_version=17, dynamo=False,
        dynamic_axes={"input_ids": {0: "batch", 1: "sequence"}, "attention_mask": {0: "batch", 1: "sequence"}, "logits": {0: "batch"}})
onnx.checker.check_model(str(directory / "model.onnx"))
session = ort.InferenceSession(str(directory / "model.onnx"), providers=["CPUExecutionProvider"])
for text in ["I love this wonderful day!", "I hate this awful day!", "The meeting is at three o'clock."]:
    encoding = tokenizer(text, return_tensors="pt")
    with torch.no_grad(): expected_logits = model(**encoding).logits.numpy()
    actual = session.run(None, {key: encoding[key].numpy() for key in ["input_ids", "attention_mask"]})[0]
    np.testing.assert_allclose(actual, expected_logits, atol=1e-4, rtol=1e-4)
    print("Export parity passed:", actual.tolist(), flush=True)
spec = {"modelId": "cardiffnlp/twitter-roberta-base-sentiment-latest",
        "modelSha256": hashlib.sha256((directory / "model.onnx").read_bytes()).hexdigest(),
        "tokenizerSha256": hashlib.sha256((directory / "tokenizer.json").read_bytes()).hexdigest(),
        "labels": ["negative", "neutral", "positive"], "maxTokens": 512}
(directory / "temper-spec.json").write_text(json.dumps(spec, indent=2), encoding="utf-8")
print(json.dumps(spec), flush=True)
