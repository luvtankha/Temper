# Rated analysis and model improvement — 0.42.0

TEMPER produces the emotional spectrum and direction itself. Users are asked only for a **1–5 quality rating**, not to supply emotion labels or perform analysis. Core features work when optional feedback is declined.

## User flow and exact data

Home → Rate analysis presents a separate prominent disclosure and unchecked feedback opt-in. A per-session confirmation covers age 18+ and permission from everyone whose messages are included. This is necessary because a user's chat contains other people's messages. No learning terminology or emotion-labeling exercise is required from the user.

After permission, Start a conversation to rate selects the next supported WhatsApp conversation using the existing fail-closed adapter. Normal model inference stays on-device. The separate feedback buffer accumulates **visible conversation segments**, not unseen full history: at most 64 unique turns (1000 characters each) and 32 actual estimates within ten minutes. Each estimate records the actual ordered visible window, remote target, eight scores, state, direction and trajectory. A phone-generated conversation identity remains local and is never submitted. No drafts, contact names, timestamps, composer positions, contact identifiers or unrelated app content are sent.

Leaving the chat or pausing stops additions. An explicitly opted-in feedback buffer remains in RAM for review until the ten-minute deadline, discard or revoke; it is not a persistent queue. Revoke analysis/clear inspection also discards it. Default sessions do not collect feedback. Only returning to Rate analysis, reviewing the redacted text, selecting a rating and tapping **Send rating and reviewed text** makes an HTTPS request. Automatic redaction covers common emails, phone numbers, URLs and handles; it is incomplete, so the user must discard sessions containing remaining identifying/sensitive details. Nothing is silently uploaded.

The payload includes redacted turns/roles, ordered analysis windows and actual outputs, overall quality rating, app/model version, random session ID and permission assertions. A random app-private deletion token is sent in the authorization header; the server stores its SHA256 rather than device/account/contact identity. This is pseudonymous, not guaranteed anonymous. Language is initially OTHER and can be categorized privately during publisher review; the user is not asked to classify it. The user does not need a TEMPER account or a purchase.

Stop sharing disables future feedback and discards pending text. Delete my submitted feedback works after withdrawal, removes active encrypted records, and revokes the old contribution key against late/replayed requests. A request already sent can finish; a confirmed deletion blocks its later reuse. The token is kept on a failed deletion for manual retry and removed only after a confirmed server response. Delete before uninstall: uninstall removes the private deletion key. Active records expire after 90 days; the running server purges hourly and on startup/access. Previously trained model influence cannot be promised to disappear. Temporary exports and backups require the operational controls below.

## Server configuration and release gates

Collection is **disabled in this build** because the owner has not supplied/deployed an HTTPS feedback service. There is no fabricated collection endpoint or background telemetry SDK. Build with `-PtemperLearningUrl=https://YOUR_FEEDBACK_HOST` only after all gates below are complete. Permission is tied to the service origin and version; changing the origin requires deletion of prior contributions before sharing with the new origin.

- Deploy `deployment/feedback/Dockerfile` from the repository root, separately from the purchase server. Run **one instance** with a private persistent filesystem volume, not an ephemeral autoscaled container filesystem. The in-process lock does not coordinate multiple replicas.
- Mount a secret containing Base64 of 32 cryptographically random bytes, set `TEMPER_LEARNING_KEY_FILE`, and retain access for restore/deletion. AES-256-GCM protects every stored session; owner/session identity is authenticated as associated data. File permissions restrict POSIX access to the service user; Windows/private-volume ACLs must be configured by the operator.
- The boundary exposes only health, POST sessions and DELETE contributions; there is no public data read/export route. It limits request bodies to 384 KB and simultaneous requests to four. Each random contributor key is limited to 20 retained sessions and the initial installation to 5000 session records. These are protective pilot limits, not a claim of internet-scale infrastructure.
- Configure TLS termination, edge rate limiting/app-abuse controls, connection/read timeouts, private administrative access and volume monitoring before public use. Anonymous clients can otherwise mint new deletion keys; the local quotas are not sufficient abuse prevention. Disable request-body, Authorization and private-export logging. No models or chat-analysis routes are exposed on this deployment.
- For the pilot, do not retain independent automatic data-volume snapshots/backups. If backups are introduced, implement verified deletion and expiry propagation before enabling them. Operators must delete temporary plaintext review/training exports promptly (at most seven days and immediately on relevant deletion requests), keep them on encrypted private storage and never commit/publicly upload them. New training starts from a fresh active export, not previous deleted/expired copies.
- Finalize the privacy policy with actual hosting providers, operational metadata retention and public URL; complete Messages/App activity/other-ID Data safety classifications and the changed Accessibility demonstration. Verify affirmative/declined permission, native capture/review, HTTPS submission, revoke during request, offline deletion/retry, reinstall behavior, scheduled expiry and infrastructure lifecycle in an internal track. No real user data was collected during development.

## Upgrade workflow

Ratings are satisfaction feedback. A five-star response does **not** prove every predicted emotion is correct; a low rating does not specify the correct emotion. `scripts/train-feedback-model.py` therefore never treats stars or old predictions as gold labels. Ratings identify cases for the publisher's private evaluation/review. Users only rate quality; reviewers independently annotate language and emotion labels from the allowed excerpts. Reviewers must label visible language, not assert another person's true internal feelings. Ambiguous examples can be excluded.

1. Export active encrypted records offline using the backend JAR's `LearningExport` command. There is no HTTP export endpoint:

   ```text
   java -Dloader.main=dev.temper.cerebro.learning.LearningExport -cp backend/target/cerebro-0.0.1-SNAPSHOT.jar org.springframework.boot.loader.launch.PropertiesLauncher PRIVATE_RECORD_DIRECTORY PRIVATE_KEY_FILE NEW_PRIVATE_EXPORT.jsonl
   ```

2. Create a private reviewer JSONL file, with one object per reviewed remote target:

   ```json
   {"contributor":"64-character exported hash","sessionId":"exported UUID","target":3,"language":"HINGLISH","labels":["frustrated"],"humanReviewed":true}
   ```

   Use the recorded `context` index sequence for the real visible ordering, particularly after back-scrolling. Allowed labels: neutral, happy, concerned, confused, sad, frustrated, angry, surprised. Neutral is exclusive. The annotation file is an operator artifact, not an app form. Use separate trusted reviewers/hold-out adjudication; poor or biased labels can still produce a poor model.

3. Obtain the local original 28-label GoEmotions PyTorch checkpoint and identical tokenizer from the cited upstream source. The existing ONNX cache alone is not a trainable PyTorch checkpoint. Training uses the pinned requirements in `scripts/model-export-requirements.txt` and makes no automatic network calls. Prepare a fresh active export immediately before training; deleted/expired examples are excluded, and exports older than one hour are rejected at preparation.

   ```text
   python scripts/train-feedback-model.py --export NEW_ACTIVE_EXPORT.jsonl --annotations PRIVATE_REVIEW.jsonl --base LOCAL_PYTORCH_CHECKPOINT --baseline-onnx ACTUAL_DEPLOYED_MODEL.onnx --output NEW_PRIVATE_CANDIDATE_DIRECTORY
   ```

4. The workflow fine-tunes the existing 28-output model against the eight mapped display signals, exports/quantizes to INT8 and compares the **actual deployed INT8 baseline** with the candidate. It preserves the Android tokenizer, 128-token remote-text input and label order. Full context supports review/evaluation; the present runtime still estimates the latest remote text and uses the previous remote estimate for its conservative direction proxy. This is a shared-model improvement workflow, not a separate per-contact personality model.

5. Contributor groups are split deterministically into train/validation/test; repeated text across contributors is excluded to avoid leakage. Promotion requires at least 1000 reviewed examples from 50 contributors, 100 held-out examples, at least 50 held-out English and Hinglish examples each, per-emotion coverage, overall macro-F1 improvement of at least 0.02, and bounded language/class regressions. These are initial engineering gates, not proof of universal accuracy. Keep a separate untouched regression/challenge set and physical-device checks before publication. Repeatedly optimizing against the same test set invalidates a held-out claim.

6. Only a qualifying evaluation can prepare a public pinned model build spec:

   ```text
   python scripts/approve-feedback-model.py --report CANDIDATE/evaluation.json --model CANDIDATE/model-quantized.onnx --url https://YOUR_MODEL_HOST/IMMUTABLE_MODEL.onnx --output APPROVED_MODEL.json
   ```

   The gate verifies model bytes, schema, tokenizer, split separation and reported metrics. Publisher review must also consider consent/deletion handling, memorization/privacy and model license obligations. It does not automatically upload or release anything. Host the approved public weights, increment the app version and build with `-PtemperEmotionModelSpec=ABSOLUTE_APPROVED_MODEL.json`, then perform native dummy/adapter tests and Play rollout. The new hash gets its own private cache file, preserving rollback to the preceding signed app release. Model upgrades use app releases; there is no uncontrolled online self-training on a user's phone.

## Development evidence and remaining work

The real collection path was tested with generated fictional sessions only. Backend checks cover encryption, rating/consent guards, contextual bounds, contributor-scoped deletion, late replay rejection, expiry, quota, request boundary and offline export. Android checks cover default-off collection, deduplication, per-conversation isolation, expiry, bounds, redaction and five rating choices with an unselected default. A tiny synthetic transformer passed gradient-training → ONNX export → INT8 inference; it is explicitly non-promotable. No contributed training corpus exists, so **the deployed model has not been upgraded and no accuracy gain is claimed**. HTTPS deployment, actual rated WhatsApp session submission/deletion and independent real-data evaluation remain release gates.

References: [Play User Data](https://support.google.com/googleplay/android-developer/answer/10144311), [Data safety definitions](https://support.google.com/googleplay/android-developer/answer/10787469), [fine-tuning documentation](https://huggingface.co/docs/transformers/tasks/sequence_classification), [ONNX quantization](https://onnxruntime.ai/docs/performance/model-optimizations/quantization.html), [Spring offline launcher](https://docs.spring.io/spring-boot/specification/executable-jar/property-launcher.html).
