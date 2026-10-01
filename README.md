# TEMPER
Conversation intelligence for CEREBRO. Source of truth: AGENT.md; sequential evidence: docs/phases. **Phases00–15 verified.**

## Development
Requires Node22+ (tested24), Java21 JDK; Maven wrapper included. No DB or models required for the default fictional demo.
PowerShell terminal1: set JAVA_HOME to a Java21 JDK, enter backend, run .\mvnw.cmd spring-boot:run.
This machine's JDK: C:\Users\Admin\.codex\cache\temper-tools\jdk-21.0.12.1+1.
Terminal2: enter frontend, npm ci, npm run dev. Open http://127.0.0.1:5173 . Vite proxies /api, /actuator and native /ws to Java8080.
On Windows run spring-boot:run or a JAR copied outside backend/target so packaging can replace build output.

## Verification
Frontend: npm test; npm run build; npm run test:e2e -- --workers=2 (backend running); npm run check:rigs.
Backend: Java21 .\mvnw.cmd verify. Base integration tests explicitly use model-free configuration; actual model tests are artifact-gated. With both verified model directories configured, all25 Java tests pass with zero skips, including actual native STOMP and ONNX. Without artifacts21 pass/four explicit skips. Frontend22 units/build pass;67 browser behaviors defined. Phase15 UI regressions19/19; configured sentiment phone inspector/dashboard1/1; real two-client typing/presence/reconnect1/1. OpenAPI0.5.0 validates. Detailed phase evidence includes failures and fixes.

## Behavior and configuration
Responsive dark UI, local/REST/live conversation, multiline composer, emoji, selection, accessible inspector, collapsible analytics, timeline and speaker dashboard. Original Rive male/female characters stay above the composer: Alex sees Nova; Nova sees Alex. Runtime controls provide eight smooth reversible poses, breathing/blinking/gaze/head drift, real typing attention, pause and reduced-motion support.
Default local adapter and analysis are hand-authored fictional fixtures in tab memory. Backend storage is synchronized in-memory: survives browser reload, clears on Java restart. No PostgreSQL, auth, imports or public deployment yet. TEMPER_DEMO_ENABLED=false disables backend seed. New conversation creates a room without deleting others.
Open /chat?transport=backend (optional &conversation=UUID) for native STOMP; transport=rest for HTTP-only. Share a room UUID between two browser sessions, select Alex/Nova respectively. Delivery broadcasts before analysis; typing/presence and reconnect catch-up are real. No automatic resend after uncertain delivery. Optional VITE_WS_URL uses ws:// or wss://; TEMPER_WS_ALLOWED_ORIGINS restricts browser origins. Room identity claims are development-only until authentication phase29. Protocol: contracts/websocket.md.
Context uses current turn/speaker plus3–5 prior turns ordered by server sequence. TEMPER_CONTEXT_PREVIOUS_TURNS defaults5. GET /api/v1/messages/{id}/context exposes causal history; future turns are excluded.

## Local model inference
Optional foundation: scripts/download-foundation.ps1 -Destination CACHE_DIRECTORY; set TEMPER_FOUNDATION_MODEL_DIR. POST /api/v1/ai/foundation/classify runs the verified Apache2 DistilBERT SST-2 classifier independently of conversation analysis. Missing configuration returns503.
Optional RoBERTa sentiment: create isolated export Python environment using scripts/model-export-requirements.txt, then scripts/download-sentiment.ps1 -Destination CACHE_DIRECTORY -Python EXPORT_PYTHON. Set TEMPER_SENTIMENT_MODEL_DIR before Java starts. CC BY4.0 attribution, source, hashes and local conversion/parity evidence: docs/model-cards/sentiment.md and models/sentiment/ATTRIBUTION.md. Weights/caches stay outside Git. Production inference runs Java CPU ONNX with a reusable session and matching local tokenizer, no hosted inference.
When sentiment is configured, existing analyze calls replace signed and negative sentiment with real RoBERTa estimates; positive/neutral/negative probabilities appear in model evidence. Current text and causal speaker-tagged history are supplied. This tweet-trained classifier is not validated for dialogue interpretation. Other emotions/signals/conflict/turning points remain explicit fixtures; modeHYBRID and source labels identify the partial integration. With no sentiment model, modeMOCK and healthNONE remain available. Configured healthHYBRID; invalid configured hashes fail startup.
Artifact tests: .\mvnw.cmd -Dtest=FoundationInferenceTest,SentimentIntegrationTest test with applicable env directories. Configured browser gate: set TEMPER_REAL_SENTIMENT_TEST=1 then npm run test:e2e -- sentiment.spec.ts.

## Rive source
Original editable source: assets/avatars/temper and docs/architecture/rive-rig-spec.md. Native unscripted rigs compile with official CLI1.2.0 without sign-in; no copied proprietary character assets. Development-only /avatar-lab drives manual values. Rebuild with node frontend/scripts/build-rig-source.mjs, verify/inspect/--once in Rive CLI, copy build/temper.riv to both public filenames. Both contain artboards selected per participant. Runtime/browser tests prove actual canvas transitions. Optional VITE_MALE_RIG_URL/VITE_FEMALE_RIG_URL override public files.
