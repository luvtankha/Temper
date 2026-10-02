# Phase 46 — Context, complex-chat evaluation and latency

1. Objective: improve recognition of changing emotional situations in complex English/Hinglish conversations, train and evaluate a candidate on conversation data, and reduce startup/update latency and bounded runtime costs.
2. Repository: clean Phase 45 at 531f583, Android 0.45.0, one-button automatic supported-chat mode installed.
3. Previous phases: preserve Android/privacy/overlay/consent/optional feedback and working foundation backend; retain one-button control and movement.
4. Baseline: 29 JVM tests, native foundation/consumer/dummy overlay checks, zero lint errors; current emotion weights unchanged from upstream INT8 RoBERTa.
5. Architecture: the phone currently classifies two remote utterances independently, compares their tension scores, reloads the model after departures and uses trailing event debounce. Analysis is bounded to eight visible turns.
6. Risks: synthetic author labels cannot establish human accuracy; public data requires clear provenance and appropriate terms; whole-conversation train/test separation matters. Context must not confuse the local speaker's feelings with the remote speaker. Speed changes must preserve stop/identity/privacy checks.
7. Scope: complex evaluation with held-out conversation groups; trained candidate with reproducible metrics and promotion decision; contextual inference and direction; measured scheduling/model/cache/parser optimizations; Android regression/build/device validation and documentation.
8. Non-goals: claiming perfect emotional understanding or that all bugs are eliminated, reading private chats for training, automatic uploads, store publishing or unsupported adapters. No account/server setup is needed for local dummy/public-data work.
9. Acceptance: report before/after complex-chat and latency results honestly; prevent debounce starvation; preserve bounded memory and stop cancellation; start useful spectrum estimates with minimal verified evidence while treating direction separately; only ship a trained replacement if evaluation supports it, and document remaining limits.
