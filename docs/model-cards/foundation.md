# Phase 14 local inference foundation

Source: https://huggingface.co/distilbert/distilbert-base-uncased-finetuned-sst-2-english
Pinned upstream revision: 714eb0fa89d2f80546fda750413ed43d93601a13. All artifacts come from that repository's onnx directory, including the matching tokenizer. Apache-2.0 license per upstream model card; retain license/attribution and notices when redistributing. This repository distributes metadata and download instructions, not model weights. Review upstream terms before a later release redistributes weights.

DistilBertForSequenceClassification, six layers, uncased English WordPiece vocabulary 30522. SST-2 movie-review sentiment fine-tuning. config id2label 0 NEGATIVE / 1 POSITIVE; no neutral or emotion label. Model SHA256 is the publisher's LFS digest, verified against downloaded bytes. All six artifact hashes are in models/foundation/manifest.json; startup verifies the two execution artifacts. Output softmax probabilities are estimates, not psychological facts or calibrated conversation confidence. Truncation retains at most 128 tokens including special tokens; long text loses later material. Request input limit 12000 characters. Model has dataset/domain/bias limits described upstream and is not a validated multi-turn chat classifier.

Java CPU ONNX Runtime 1.30.0 (MIT) and DJL tokenizers 0.38.0 (Apache-2.0) run locally. Native DLLs are bundled in the Maven dependencies; no hosted inference is called. OPT_OUT_TRACKING=true disables DJL's optional tracking before initialization. Download/setup requires network; classification reads only local artifacts. One Spring classifier/session/tokenizer per application; synchronized tokenization/inference and resource closure on shutdown. Each request closes its tensors and output. CPU threads limited to two. This foundation diagnostic is separate from the main MOCK conversation engine; RoBERTa sentiment integration is Phase15.

Run scripts/download-foundation.ps1 -Destination CACHE_DIRECTORY. Set TEMPER_FOUNDATION_MODEL_DIR, start backend. POST /api/v1/ai/foundation/classify with JSON {"text":"I loved this movie"}. Returns modelId, source MODEL, negative/positive probabilities, tokenCount and inferenceMillis; submitted text is not returned or logged. Unconfigured endpoint returns503 while existing chat remains available. Invalid configured artifacts fail startup, never silently produce mock output.

Genuine native test: set TEMPER_FOUNDATION_MODEL_DIR then backend/mvnw.cmd -Dtest=FoundationInferenceTest test. Ordinary verify skips this explicitly opt-in test when artifacts are absent; it still tests unconfigured503 and input validation. See phase14 handoff for actual executed inference and HTTP evidence.

Runtime documentation: https://onnxruntime.ai/docs/get-started/with-java.html
Tokenizer documentation: https://docs.djl.ai/master/extensions/tokenizers/index.html
