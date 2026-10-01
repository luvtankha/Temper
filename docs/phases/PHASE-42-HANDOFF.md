# Phase 42 — Rated analysis and controlled model improvement

## 1. What changed

TEMPER still generates the emotional spectrum, current-state estimate and direction itself. End users only give a 1–5 quality rating; no emotion labels, corrected analysis or explanation are requested. The owner requested conversation context alongside those ratings. Separate optional permission, a selected supported session, text review and an explicit Send action control that sharing. Normal private analysis and paid-avatar behavior are preserved.

Opted-in recording is bounded to 64 visible turns, 32 actual estimates and ten minutes in RAM. It retains the actual ordered context/remote target, redacts common contact details and excludes drafts, contact identity and unseen history. Leaving/pausing stops additions; expiry/discard/revoke clears pending feedback. No automatic upload or disk queue exists. Consent is tied to the feedback service origin/version. Users can withdraw and delete submitted records using a random app-private key.

The separate default-disabled backend validates permission/rating/context, bounds request size/concurrency and stores AES-256-GCM encrypted records. Contributor-scoped deletion revokes late/replayed submissions. Retention is 90 days with scheduled/access/startup cleanup; pilot quotas are 20 sessions per key and 5000 records total. No public data read/export route exists. Operator export streams active records offline.

Offline scripts prepare independently reviewed examples, split by contributor, fine-tune the original 28-output model, export INT8 ONNX and compare the deployed baseline against a candidate. Ratings and old predictions are never used as emotion truth. End users do not perform reviewer annotation. Promotion requires adequate English/Hinglish held-out coverage and measured improvement. Approved weights can be pinned in a new app build; upgrades are not uncontrolled online training.

## 2. Files changed or added

- Android: `learning/LearningSession.java`, `LearningConsent.java`, `LearningClient.java`, `MainActivity.java`, capture/pipeline/privacy integration, model configuration/cache/downloader and disclosure strings. Version 0.42.0; feedback endpoint defaults empty.
- Backend: `learning/LearningSubmission.java`, `EncryptedLearningStore.java`, `LearningController.java`, `LearningBoundaryFilter.java`, `LearningScheduling.java`, `LearningExport.java` and default-disabled configuration.
- Verification: Android unit/native learning checks, backend `LearningTests`, `scripts/test-feedback-training.py`.
- Operations: `deployment/feedback/`, training/approval scripts, build/package scripts, privacy/store/release documents and `ANALYSIS-FEEDBACK.md`. Dataset/export/runtime files remain ignored.

## 3. Verification and build results

Android debug APK, instrumentation APK and R8 unsigned release AAB build successfully. Debug/release lint have zero errors; existing warnings include localization/RTL/version items. Eight JNI libraries and APK ZIP entries passed 16 KB alignment checks. The default 0.42.0 testing APK is installed on the connected OnePlus 8T. Feedback and real purchases are unconfigured in this candidate.

Backend Maven verification passed **69 tests with zero failures/errors/skips**, with all existing model directories enabled. Four Python curation/promotion tests passed. A tiny synthetic transformer demonstrated gradient training, ONNX export, quantization and finite 28-output CPU inference: loss 0.692697 → 0.336685. Its report explicitly says synthetic and non-promotable. This verifies the mechanism, not an accuracy improvement.

## 4. Tests run

- Ten Android unit tests: existing consumer/model/tokenizer checks plus collection default-off, deduplication, context ordering, isolation, expiry, bounds, redaction and defensive copies. Actual desktop INT8 inference enabled through `TEMPER_TEST_MODEL`.
- Native learning-only checks on an isolated `https://feedback.invalid` build: separate unchecked consent, decline, review, five rating choices with no selection, Send gating, discard and withdrawal. No Send action or host-chat capture was performed. Original preferences were restored and the default endpoint-empty build reinstalled.
- Native full foundation checks: placement/keyboard/adapter/privacy/consent/Keystore and original preference restoration. Consumer checks: four avatars × eight expressions, premium lock, 105 independent tokenizer cases, actual phone INT8 model, remote-speaker invariance and 25 generated English/Hinglish conversations. The final default APK also passed consumer checks.
- Nine new backend tests cover encryption, redaction/idempotency, contributor deletion/replay, tampering/expiry, strict rating/permission/index validation, endpoint/auth/body boundaries, quota, offline export and the actual enabled Spring deployment context. The 60 preserved tests include actual-model integration.
- Four Python tests cover ratings versus independent labels, contributor split isolation/determinism, synthetic/insufficient promotion rejection and nonfinite evaluation rejection. PowerShell release-script parsing and Git whitespace checks pass.

## 5. Known issues and release limits

No HTTPS feedback service is deployed and **no real user feedback was collected**. The currently distributed model is unchanged. There is no contributed corpus, real-data training run, approved candidate or accuracy-gain claim. Ratings alone cannot identify correct emotion labels. Direction uses the existing smaller on-phone proxy; it is not trained by this workflow. Hindi-heavy, sarcasm and subtle context remain documented weaknesses.

Actual opted-in WhatsApp capture → review → HTTPS submission → deletion, network failure/reinstall behavior, infrastructure retention/deletion and broader-device checks remain required. A general floating character is available across apps; automatic analysis remains limited to the verified WhatsApp layout. Full unseen history and individual per-contact models are not implemented.

The feedback deployment recipe is untested in cloud infrastructure. It requires one persistent-volume instance, private operator storage, secret management, TLS and edge abuse controls. Quotas are pilot protections, not internet-scale abuse prevention. No independent data-volume backups may be enabled until deletion/expiry propagation works. Plaintext review/training exports require timely operator cleanup. Automatic redaction is incomplete; participant permission assertions do not themselves verify everyone's permission.

Play account/products, purchase verification deployment, real purchase tests, owner signing and finalized privacy/Data safety/Accessibility declarations remain pending from Phase 41. Its rejected local HTTP purchase smoke was not retried or bypassed. The testing APK includes developer tools; the unsigned AAB is for review, not a production upload.

## 6. Next prerequisites and artifacts

Follow `docs/release/ANALYSIS-FEEDBACK.md` for service deployment, consent/deletion verification, private reviewer curation and held-out upgrade gates. Obtain the original local trainable PyTorch checkpoint and exact tokenizer before real training. Finalize hosting/provider/privacy information, including the model host and provenance for any future fine-tuned release. Use the Play setup/release guide for monetization gates.

Candidate artifacts: `dist/TEMPER-0.42.0-testing.apk`, `dist/TEMPER-0.42.0-UNSIGNED-review.aab`, `dist/TEMPER-consumer-0.42.0.zip` and checksums. The package includes the committed source ZIP, backend JAR and instructions. Weights, credentials, private exports and signing keys are excluded. Avatar/spectrum/floating evidence is historical Phase 41 output with the unchanged model/assets; the new synthetic training report is explicitly non-promotable. No private host-app screenshots were taken.
