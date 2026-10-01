# TEMPER — Android overlay companion

TEMPER is an Android-first overlay for existing chat apps, with WhatsApp as the first supported target. The product is a small grounded 2D character near the composer; tapping it shows current state, direction and one emotional-spectrum bar graph. Source of truth: docs/TEMPER-AGENT-UPDATED.md (also root AGENT.md). The revised roadmap replaces all earlier plans after Phase19.

The Android foundation is in progress under android/: native onboarding/settings/fictional preview, debug APK builds and lint passes; actual overlay/capture not implemented yet. The existing React chat/dashboard is a **legacy development and test harness**, retained to verify backend/model behavior; it is not the product or an Android overlay.
Conversation intelligence for CEREBRO. Historical foundation evidence: docs/phases. **Phases00–17 verified.**

## Development
Requires Node22+ (tested24), Java21 JDK; Maven wrapper included. No DB or models required for the default fictional frontend demo; backend language rules are enabled by default.
PowerShell terminal1: set JAVA_HOME to a Java21 JDK, enter backend, run .\mvnw.cmd spring-boot:run.
This machine's JDK: C:\Users\Admin\.codex\cache\temper-tools\jdk-21.0.12.1+1.
Terminal2: enter frontend, npm ci, npm run dev. Open http://127.0.0.1:5173 . Vite proxies /api, /actuator and native /ws to Java8080.
On Windows run spring-boot:run or a JAR copied outside backend/target so packaging can replace build output.

## Verification
Frontend: npm test; npm run build; npm run test:e2e -- --workers=2 (backend running); npm run check:rigs.
Backend: Java21 .\mvnw.cmd verify. Base integration tests explicitly use model-free configuration; actual model tests are artifact-gated. With all five verified model directories configured, all37 Java tests pass with zero skips, including actual native STOMP and ONNX. Without artifacts27 pass/ten explicit skips. Frontend22 units/build pass;71 browser behaviors defined. Phase15 UI regressions19/19; configured sentiment phone inspector/dashboard1/1; real two-client typing/presence/reconnect1/1. OpenAPI0.9.0 validates. Detailed phase evidence includes failures and fixes.

## Behavior and configuration
Responsive dark UI, local/REST/live conversation, multiline composer, emoji, selection, accessible inspector, collapsible analytics, timeline and speaker dashboard. Original Rive male/female characters stay above the composer: Alex sees Nova; Nova sees Alex. Runtime controls provide eight smooth reversible poses, breathing/blinking/gaze/head drift, real typing attention, pause and reduced-motion support.
Default local adapter and analysis are hand-authored fictional fixtures in tab memory. Backend storage is synchronized in-memory: survives browser reload, clears on Java restart. No PostgreSQL, auth, imports or public deployment yet. TEMPER_DEMO_ENABLED=false disables backend seed. New conversation creates a room without deleting others.
Open /chat?transport=backend (optional &conversation=UUID) for native STOMP; transport=rest for HTTP-only. Share a room UUID between two browser sessions, select Alex/Nova respectively. Delivery broadcasts before analysis; typing/presence and reconnect catch-up are real. No automatic resend after uncertain delivery. Optional VITE_WS_URL uses ws:// or wss://; TEMPER_WS_ALLOWED_ORIGINS restricts browser origins. Room identity claims are development-only until authentication phase29. Protocol: contracts/websocket.md.
Context uses current turn/speaker plus3–5 prior turns ordered by server sequence. TEMPER_CONTEXT_PREVIOUS_TURNS defaults5. GET /api/v1/messages/{id}/context exposes causal history; future turns are excluded.

## Local model inference
Optional foundation: scripts/download-foundation.ps1 -Destination CACHE_DIRECTORY; set TEMPER_FOUNDATION_MODEL_DIR. POST /api/v1/ai/foundation/classify runs the verified Apache2 DistilBERT SST-2 classifier independently of conversation analysis. Missing configuration returns503.
Optional RoBERTa sentiment: create isolated export Python environment using scripts/model-export-requirements.txt, then scripts/download-sentiment.ps1 -Destination CACHE_DIRECTORY -Python EXPORT_PYTHON. Set TEMPER_SENTIMENT_MODEL_DIR before Java starts. CC BY4.0 attribution, source, hashes and local conversion/parity evidence: docs/model-cards/sentiment.md and models/sentiment/ATTRIBUTION.md. Weights/caches stay outside Git. Production inference runs Java CPU ONNX with a reusable session and matching local tokenizer, no hosted inference.
When sentiment is configured, existing analyze calls replace signed and negative sentiment with real RoBERTa estimates; positive/neutral/negative probabilities appear in model evidence. Current text and causal speaker-tagged history are supplied. This tweet-trained classifier is not validated for dialogue interpretation. Unconfigured model fields and other signals/conflict/turning points remain explicit fixtures; modeHYBRID and source labels identify the partial integration. With no sentiment model, modeMOCK and healthNONE remain available. Configured healthHYBRID; invalid configured hashes fail startup.
Artifact tests: .\mvnw.cmd -Dtest=FoundationInferenceTest,SentimentIntegrationTest test with applicable env directories. Configured browser gate: set TEMPER_REAL_SENTIMENT_TEST=1 then npm run test:e2e -- sentiment.spec.ts.

## Rive source
Original editable source: assets/avatars/temper and docs/architecture/rive-rig-spec.md. Native unscripted rigs compile with official CLI1.2.0 without sign-in; no copied proprietary character assets. Development-only /avatar-lab drives manual values. Rebuild with node frontend/scripts/build-rig-source.mjs, verify/inspect/--once in Rive CLI, copy build/temper.riv to both public filenames. Both contain artboards selected per participant. Runtime/browser tests prove actual canvas transitions. Optional VITE_MALE_RIG_URL/VITE_FEMALE_RIG_URL override public files.

Phase16 adds the dedicated MIT GoEmotions classifier. Run scripts/download-emotion.ps1 -Destination CACHE_DIRECTORY and set TEMPER_EMOTION_MODEL_DIR. Model card: docs/model-cards/emotion.md. All six emotion channels now use independent model signals when configured; frustration/concern are documented semantic proxies, surprise/neutral are additive inspector fields. Raw28labels are inspectable; neutral does not contribute to emotional-intensity maximum. Unconfigured sarcasm, toxicity/additional signals and conflict remain fixtures. Phase16 configured frontend regression20/20 and complete native/backend29/29 pass. TEMPER_REAL_EMOTION_TEST=1 enables its genuine browser gate.

Phase17 adds genuine dedicated sarcasm independent of sentiment. Run scripts/download-sarcasm.ps1 -Destination CACHE_DIRECTORY -Python EXPORT_PYTHON; set TEMPER_SARCASM_MODEL_DIR. Apache2 checkpoint/base tokenizer, exact BERT architecture/label mapping, own ONNX export/hashes/parity and limits: docs/model-cards/sarcasm.md. Inspector and dedicated sarcasm timeline show actual MODEL evidence; toxicity/additional signals/conflict remain fixtures. Native/backend31/31 and live model/dashboard/STOMP regression9/9 pass. TEMPER_REAL_SARCASM_TEST=1 enables this browser gate.

Phase18 adds independent toxicity, insult and threat signals, with separately inspectable hostility=max(insult,threat) explicitly marked as a proxy. Run scripts/download-toxicity.ps1 -Destination CACHE_DIRECTORY -Python EXPORT_PYTHON; set TEMPER_TOXICITY_MODEL_DIR. Pinned Apache2 checkpoint, tokenizer, export/parity and limits: docs/model-cards/toxicity.md. TEMPER_REAL_TOXICITY_TEST=1 enables its genuine browser gate. Additional linguistic indicators/conflict/arc remain fixtures until their phases.

Phase19 adds six separate estimated English lexical/context indicators. Default TEMPER_INDICATORS_ENABLED=true uses inspectable HEURISTIC cues; set false for the backend fixture demonstration. Rules, scores, supported quote/negation handling and limits: docs/analysis/estimated-indicators.md. Repeated disagreement uses current and prior same-speaker cues only; all six are separately inspectable. Unconfigured model channels/conflict/arc remain fixtures.

## Overlay-only roadmap

Phase20 preserves analysis and retires standalone messaging ambitions. Next:21 conflict finalization,22 trajectory,23 compact mapping,24 Android foundation,25 minimal account support,26 consent/accessibility,27 adapters,28 WhatsApp validation,29–35 compact character/popup/live behavior,36 privacy,37 optional extension,38 hardening,39 packaging. Earlier roadmap references in phase00–19 handoffs are historical. No additional legacy messaging features, giant overlay dashboards, imports or social functionality are planned.

Updated Phase21: additive conflictAnalysis provides configurable raw/smoothed scores, UP/FLAT/DOWN and source-aware ranked contributors. Existing conflict fields remain legacy-compatible. Formula/configuration: docs/analysis/conflict-engine.md. Configured Java40/40 pass; OpenAPI0.10.0 valid.

Updated Phase22 trajectory and23 compact remote mapping are verified. Complete Java45/45 and subsequent live browser11/11 pass; legacy web22/build preserved. Android Phase24 install/run and foundation instrumentation passed on OnePlus8T Android14/API34.

Updated Phase25 device-only account/session support builds, passes lint and passes actual-device encrypted storage/password/session/tamper checks. Account scope and setup: docs/architecture/android-account.md. This local identity does not secure backend endpoints.

Updated Phase26 consent/accessibility skeleton builds, passes lint and passes actual-device consent/pause/package-policy tests. User confirmed actual system activation and real WhatsApp package detection. Current build detects package metadata only, with no chat extraction or transmission. Setup: docs/architecture/android-accessibility.md.

Updated Phase27 bounded pure adapter contract and isolated fake adapter pass actual-device role/composer/immutability/redaction/failure checks. No real WhatsApp parsing is claimed yet.

Phase28 is in progress: a user-armed text-free WhatsApp layout probe is installed and its one-shot/composer/export/cleanup device tests pass. Actual layout metadata from a fictional test chat is required before defining message/role parsing; no WhatsApp text extraction is implemented yet. Version2 consent requires a fresh opt-in. Calibration instructions: docs/architecture/android-accessibility.md.

Phase28 update: actual59-node layout retrieved; conservative bounded text-row adapter and one-shot parser implemented. Build/lint and actual-device tests with the observed layout plus fictional text pass. Actual host text parsing remains pending manual activation/test. Exact supported build and parser limits: docs/architecture/whatsapp-adapter.md. Diagnostic report exports no messages or identities; no server transmission exists.

Phase28 actual gate passed: AVAILABLE/eight complete visible turns (one local/seven remote), composer bounds and repeated-read suppression verified from the selected fictional WhatsApp chat's text-free diagnostic report. Phase28 complete.
