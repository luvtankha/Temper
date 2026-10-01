# Dedicated sarcasm classifier

Publisher: dima806/sarcasm-detection-distilbert (Apache-2.0 declared in model card), revision8f0e2429ce5ba0cc976ded7dd2b359ca9d1a66ab. Despite repository name, config and weights are BertForSequenceClassification,12layers,768hidden.
Base tokenizer: google-bert/bert-base-uncased Apache2 revision86b5e0934494bd15c9632b12f734a8a67f723594; same30522vocabulary/uncasedWordPiece and special IDs. Native Java matches original reference output.
Canonical hash/labels: backend/src/main/resources/models/sarcasm.json. Original weights and own exported ONNX remain outside Git.
See docs/model-cards/sarcasm.md; scripts/download-sarcasm.ps1 -Destination CACHE_DIRECTORY -Python EXPORT_PYTHON. Retain upstream attribution/license/card and indicate local ONNX conversion when redistributing.
