# Phase 46 — Complex context and runtime costs

## 1. What changed

Android 0.46.0/code 46 adds a compact trained conversation-direction head. It uses
bounded role-separated contextual features and frozen emotion scores to recognise
increasing conflict, repair/softening, possible disengagement, unresolved concerns
and stable exchanges. Identical final replies can receive different directions
because preceding turns differ. The eight bars still describe the latest incoming
message using the original pinned INT8 emotion model. New outgoing replies do not
rewrite that person's emotion estimate or cancel pending incoming inference.

Accessibility scheduling now keeps a leading deadline instead of indefinitely
postponing analysis during repeated events. Model warm-up uses public assets only
when ON is ready. The worker caches at most eight hashed score entries without raw
text; identity/departure clears them immediately. Public model assets can survive
a short departure, with a 60-second idle release deadline that repeated departure
events cannot postpone. Pending release tasks survive snapshot cancellation;
OFF/revocation/destruction release resources and reject stale results. Closing a
worker twice is safe.

The adapter indexes direct children in one pass; hashing avoids per-byte format
allocations. Byte-pair tokenization uses a priority queue and compact primitive
tables, preserving the pinned tokenizer's IDs. Runtime no longer constructs the
large vocabulary/merge string maps. Transformer weights and attention complexity
are unchanged. First-message bars remain available; direction needs more context.

## 2. Files changed or added

- Android inference: OnDeviceAnalysis, RobertaTokenizer, ConversationContextModel,
  compiled tokenizer/context assets and model attribution.
- Android capture: ProbeDeadline, LivePipeline, TemperAccessibilityService and
  WhatsAppAdapter; application version and model-status guidance updated.
- Training/evidence: models/context, train-context-model.py,
  compile-mobile-tokenizer.py and pinned context-training requirements.
- Tests: JVM context/tokenizer/scheduler checks; native complex conversation and
  performance checks with frozen Phase 45 baseline engines; runner and consumer
  references. Frozen engines are test-only and absent from the consumer APK.
- Feedback parser accepts the two new local direction labels; backend regression
  test added. Existing consent, review, deletion and human-reviewed training gates
  remain unchanged.
- README, release/architecture documentation, phase context/handoff, .gitignore
  exception for the public compiled tokenizer and consumer packaging evidence.

## 3. Verification and build results

Debug/instrumentation APKs and the R8 unsigned release-review AAB build. Android
JVM suite: **33 tests, zero failures/errors/skips**, including real desktop INT8
inference. Debug/release lint: zero errors, 33/32 warnings. Backend affected
learning suites: **19 tests, zero failures/errors/skips**, and executable JAR
rebuilt. JNI ELF and APK ZIP alignment are checked separately before packaging.

The phone's frozen Phase 45 versus Phase 46 comparison measured cold inference
896.1 → 661.5 ms, repeated-window median 50.9 → 2.25 ms, new incoming-message median
50.8 → 25.6 ms, retained Java heap 11.24 → 3.35 MB and settled process PSS
209,311 → 202,065 KB. Native allocated memory is essentially unchanged (~159 MB).
These are one-device measurements, not a UI latency promise. Model loading and
Android scheduling can still take longer on other devices. Detailed method and
complexity bounds are in CONTEXT-AND-PERFORMANCE.md.

The emotion base remains SHA256
`0c1981c5b479674747911c8e2228f0c4ec90bf47bf66e830f7d4fc62be082958`.
The direction asset is about 14 KB and binds to that hash. Future approved base
model replacements use the original summary until a matching head is evaluated.
Default store and feedback origins remain empty.

## 4. Tests run

The newly authored pilot uses 38 train/14 validation conversations and 28 fresh
held-out conversations, balanced across English and Roman-script Hinglish. Each
has five turns. Settings are selected on deployed validation predictions with the
same abstention threshold. Direction agreement on the fresh holdout improved from
6/28 for the old proxy to 28/28 for the candidate, with no per-class or language
regression in this small set. Java matches all 80 Python feature/probability
references. Counterfactual pairs retain identical final replies while changing
the preceding situation. The first candidate's failed gate/report is preserved;
the original test set was excluded after correcting settings selection.

Actual phone inference passes all 28 held-out chats, four appended repair
transitions, first-incoming-message spectrum and stopped-analysis rejection.
Optimized and frozen engines produce matching bars on the same phone. ARM INT8
outputs differ from desktop outputs by up to ~0.09 in this set, so desktop score
equality is not used as a phone assertion. Direction still passes on the phone.

All 113 independent tokenizer references pass, including long repeated words,
emoji, Devanagari, accented characters, whitespace, special tokens and truncation.
Existing native consumer checks retain the 25 historical simple-style fixtures as
regressions, not as the new complex evaluation. They also check remote-speaker
invariance, suspended-worker recovery, stale-result suppression, avatars/purchase
gates and private-model setup. Native foundation and dummy overlay tests cover
observed adapter layouts/roles/clipping/dedup/privacy, consent controls, automatic
chat switching, home/notification OFF, movement/keyboard/popup/restore and no
automatic feedback collection. No host root, private chat or private screenshot
was read. Fixture preferences were restored.

## 5. Known issues and limits

This is a **synthetic direction pilot**, not independently measured live accuracy.
The same author designed labels/features/examples; wording overlaps across domains.
The fresh holdout was written after the first aggregate failure. Perfect agreement
on it must not be marketed as perfect emotional understanding. Novel language,
subtle sarcasm, Hindi-heavy chats and missing history remain weak areas. The large
emotion-bar model was not fine-tuned; its Hinglish accuracy remains unvalidated.
The app uses only up to eight visible turns, not a complete hidden conversation.

The exact WhatsApp layout restriction remains. Instrumentation can disconnect
Accessibility, so a user-driven ON → actual supported-chat change check remains a
release gate. First-install model download and real Play purchase/signing/privacy
hosting/feedback-server setup remain pending. No app-store publication, real
feedback collection or automatic model upgrade is claimed. This phase fixes
identified issues; it does not establish that all app bugs have been eliminated.

## 6. Artifacts and next prerequisites

The tested 0.46.0 APK is installed on the connected OnePlus 8T. Normal operation
remains one home ON/OFF button after informed one-time setup. If Android disconnected
Accessibility during instrumentation, the setup guide shows the required service
rebind. No user consent is enabled by these tests.

Consumer artifacts: `dist/TEMPER-0.46.0-testing.apk`,
`dist/TEMPER-0.46.0-UNSIGNED-review.aab`, `dist/TEMPER-consumer-0.46.0.zip` and SHA256
manifests. The package includes the committed source, current/failure evaluation,
controlled phone performance, generated phone context results and release limits.
Historical visual evidence remains labelled by phase. Weights, keys and private
runtime content remain excluded from the source/archive.
