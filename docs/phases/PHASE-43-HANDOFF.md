# Phase 43 — Feedback reliability and HTTPS deployment kit

## 1. What changed

The rated-analysis workflow now serializes explicit send/delete requests across Activity recreation. A stale completion/expiry cannot discard a newer session; deletion keys are durably written and a stale deletion confirmation cannot erase a replacement key. Review text and callback references are scrubbed when the screen is discarded, expires, navigates away or goes into the background. Paused screens cannot repopulate that text. Response confirmation requires a Boolean true. The app continues generating all analysis; users only select a 1–5 quality rating after separate permission/review.

The feedback backend rejects duplicate/trailing JSON, coerced metadata/summary types, oversized integer wraparound, noncanonical keys and unsafe internal session IDs. It bounds distinct active/revoked contributors to 5000 slots, preserves existing contributors' ability to delete at capacity, cleans empty expired directories and excludes revoked remnants from export. Authenticated expiry is checked before exporting even if a file timestamp changes. Export failure removes the temporary snapshot rather than leaving usable partial training data. Revocation markers expire after 90 days; replay protection is not indefinite.

A separate one-Linux-server deployment kit includes Compose, restricted Caddy HTTPS routes, private persistent mounts, non-root containers, resource limits, a local health helper and missing-configuration-safe initialization/preflight/start commands. It neither purchases infrastructure nor enables Android sharing. The owner clarified that no server/domain is available, so the default candidate still has no feedback endpoint.

## 2. Files changed or added

- Android learning/session/client/consent code, new `LearningOperations` and `LearningDeletionKey`, `MainActivity`, JVM regressions and native discard cleanup assertion; version 0.43.0.
- Backend learning submission/controller/store/export and regressions, plus `LearningTransportTest` for the actual loopback HTTPS servlet stack.
- `deployment/feedback/`: Dockerfile, Compose, Caddyfile, secret/config examples, stdlib operator script/tests, Java health helper and setup README.
- Current README/privacy/release/feedback guides and candidate packaging. Packaging copies only tracked deployment files; private ignored configuration is excluded.

## 3. Verification and build results

Backend Maven verify passed **78 tests, zero failures/errors/skips**, with the five existing actual model directories enabled. The HTTPS test generates an ephemeral localhost certificate/key and fictional session, uses isolated test trust, then checks submission/retry/deletion/replay through the real TLS/Tomcat/filter/controller stack. Production Android TLS trust is unchanged.

Android debug/instrumentation APKs and R8 unsigned review AAB build successfully. **19 JVM tests passed, zero failures/errors/skips**, including actual desktop INT8 inference and independent tokenizer cases. Debug/release lint have zero errors (32/31 existing warnings). The final endpoint-empty testing APK is installed and verified on the OnePlus 8T. JNI/ZIP 16 KB alignment checks pass.

## 4. Tests run

- Seventeen feedback backend regressions cover encryption/idempotency/consent/bounds/deletion/expiry/quota/export plus strict JSON/types/overflow, bounded tombstones, exact expiry/empty-directory cleanup, revoked remnants, failed export and canonical identity. Sixty preserved backend checks and the new real HTTPS transport test also pass.
- Nine additional Android JVM regressions cover stale session completion/expiry, redaction size/diagnostics, request serialization across screens/no implicit queue, failures/listener detachment, exact Boolean confirmation and deletion-key concurrency/replacement/damage. Ten preserved consumer/model/tokenizer/collection checks pass.
- Native learning-only UI check uses `https://feedback.invalid`, generated text and no Send action: separate unchecked opt-in, decline, unselected 1–5 rating, submission gating, discard text/listener cleanup and withdrawal. Preferences restored; the temporary-origin APK is excluded from packaging.
- Native foundation/consumer checks on the final default APK: adapter/privacy/consent/placement/keyboard/Keystore, four avatars × eight expressions, purchase selection gate, 105 independent tokenizer cases, actual phone INT8 model, remote-speaker independence and 25 generated English/Hinglish conversations. No host-chat capture or private screenshot was performed.
- Four deployment synthetic tests and portable containment checks pass; empty configuration refuses initialization. The health helper compiles under Java 21. Official checksum-verified Caddy 2.11.6 adapts/validates the configuration without starting a server. Official checksum-verified Compose 5.5.1 passes configuration parsing without a Docker daemon/containers. Four unchanged model-curation/promotion tests pass. Release-script parsing and whitespace checks pass.

## 5. Known issues and limits

No public server, domain, Docker runtime or feedback endpoint is configured. Container image build, Linux UID/secret mounts, public DNS/ACME issuance, actual phone HTTPS submit/delete, infrastructure failure/reinstall behavior and operational retention still need verification. Parser/loopback tests do not establish a working public deployment. TLS/edge abuse controls and finalized provider/privacy disclosures are required before broad collection; anonymous clients can otherwise exhaust bounded capacity with new keys.

No real user feedback was collected, no genuine corpus trained and no production model upgraded. Existing English/Hinglish/sarcasm/context limitations remain. Ratings express quality satisfaction; independent publisher review supplies training labels, and future releases require held-out improvement. Automatic analysis is still restricted to the verified WhatsApp layout; general overlay support does not imply universal analysis.

Play account/products, purchase server/keys, real purchase tests, owner signing and declarations/store review remain pending. The earlier rejected purchase HTTP smoke was not retried or bypassed. The testing APK contains developer tools; the unsigned AAB is for review. No independent feedback backups are enabled; deletion/expiry propagation is required before introducing them.

## 6. Next prerequisites and artifacts

Obtain/configure a Linux server and real domain, follow `deployment/feedback/README.md`, verify fictional public HTTPS/phone submission and deletion, finalize the privacy policy, then build with the real feedback origin. Do not collect real chats while these gates remain open. Follow the Play setup/release guide separately for monetization. Model curation/training follows `ANALYSIS-FEEDBACK.md`; users are never assigned emotion-labeling work.

Candidate output: `dist/TEMPER-0.43.0-testing.apk`, `dist/TEMPER-0.43.0-UNSIGNED-review.aab`, `dist/TEMPER-consumer-0.43.0.zip` and distribution/archive checksums. Source/backend/deployment instructions are included; weights, keys and private data are excluded. Historical Phase 41 avatar/spectrum/own-screen evidence remains labeled; the Phase 42 synthetic training report is non-promotable. The default model bytes are unchanged.
