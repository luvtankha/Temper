"""Synthetic-only checks for curation, grouped splits and release gates."""
import copy
import importlib.util
import json
import tempfile
import time
import unittest
from pathlib import Path


def load(name, file):
    spec = importlib.util.spec_from_file_location(name, Path(__file__).with_name(file))
    module = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(module)
    return module


training = load("feedback_training_test", "train-feedback-model.py")
promotion = load("feedback_promotion_test", "approve-feedback-model.py")


class FeedbackTests(unittest.TestCase):
    def test_rating_is_never_used_as_an_emotion_label(self):
        with tempfile.TemporaryDirectory() as directory:
            export = Path(directory) / "export.jsonl"
            annotations = Path(directory) / "labels.jsonl"
            export.write_text(json.dumps({"contributor": "a" * 64,"exportedAt":int(time.time()*1000),"expiresAt":int(time.time()*1000)+86400000, "submission": {"sessionId": "fictional",
                              "qualityRating": 5, "turns": [{"role": "REMOTE", "text": "Fictional anger example"}],
                              "analyses": [{"target": 0, "scores": [0, 1, 0, 0, 0, 0, 0, 0]}]}}))
            annotation = {"contributor": "a" * 64, "sessionId": "fictional", "target": 0,
                          "labels": ["angry"], "language": "ENGLISH", "humanReviewed": True}
            annotations.write_text(json.dumps(annotation))
            result = training.prepare(export, annotations)
            self.assertEqual(result[0]["labels"], [0, 0, 0, 0, 0, 0, 1, 0])
            annotation["humanReviewed"] = False
            annotations.write_text(json.dumps(annotation))
            with self.assertRaises(ValueError):
                training.prepare(export, annotations)

    def test_contributor_groups_never_overlap(self):
        examples = [{"group": str(i), "text": "fictional " + str(i)} for i in range(100) for _ in range(3)]
        split = training.split_examples(examples)
        groups = [set(row["group"] for row in values) for values in split.values()]
        self.assertEqual(sum(map(len, groups)), 100)
        for i, group in enumerate(groups):
            for other in groups[:i]:
                self.assertFalse(group & other)
        self.assertEqual(split, training.split_examples(examples))

    def test_synthetic_or_insufficient_models_cannot_be_promoted(self):
        with self.assertRaises(ValueError):
            promotion.approve({"synthetic": True, "promotable": True}, Path("unused"), "https://example.invalid/model.onnx")
        examples = [{"group": "only-one", "labels": [1, 0, 0, 0, 0, 0, 0, 0]}]
        scores = [[.9, .1, .1, .1, .1, .1, .1, .1]]
        baseline = {"all": training.metrics([examples[0]["labels"]], scores)}
        reasons = training.eligible(examples, {"test": examples}, baseline, copy.deepcopy(baseline))
        self.assertTrue(any("1000" in reason for reason in reasons))
        self.assertTrue(any("improvement" in reason for reason in reasons))

    def test_nonfinite_evaluation_cannot_be_promoted(self):
        metrics = {"examples": 100, "macroF1": float("nan"), "perLabelF1": {label: .8 for label in training.LABELS},
                   "positiveCounts": {label: 10 for label in training.LABELS}}
        report = {"synthetic": False, "promotable": True, "reasons": [], "outputLabels": 28, "maxTokens": 128,
                  "reviewedExamples": 1000, "contributors": 50, "splitGroups": {"train": ["a"], "validation": ["b"], "test": ["c"]},
                  "baseline": {"all": metrics}, "candidate": {"all": metrics}}
        with self.assertRaisesRegex(ValueError, "Invalid evaluation scores"):
            promotion.approve(report, Path("unused"), "https://example.invalid/model.onnx")


if __name__ == "__main__":
    unittest.main()
