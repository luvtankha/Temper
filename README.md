# TEMPER
Conversation intelligence for CEREBRO. Follow AGENT.md and sequential evidence in docs/phases. **Phases 00–13 verified.**

## Development
Requires Node 22+ (tested 24), Java 21 JDK. Maven wrapper included. No PostgreSQL or AI models required yet.

Terminal 1 (PowerShell): set JAVA_HOME to a Java 21 JDK, enter backend, run .\\mvnw.cmd spring-boot:run.
On this machine the JDK is C:\\Users\\Admin\\.codex\\cache\\temper-tools\\jdk-21.0.12.1+1.
Terminal 2: enter frontend, npm ci, npm run dev.
Open http://127.0.0.1:5173. Vite proxies /api and /actuator to backend port 8080.

## Verification
In frontend: npm test; npm run build; npm run test:e2e -- --workers=2 (backend must run); npm run check:rigs.
In backend with Java 21: .\\mvnw.cmd verify.
Checkpoint: frontend build and 22 units pass; 66 browser behaviors defined (phase results in handoffs); Java build and 20 tests pass, including actual native STOMP clients. Phase 13 live context/REST/domain/health/realtime regression 7/7. OpenAPI validates. Actual Rive canvas tests exercise eight poses, intensity blends, reversal, overlapping signals and staged transitions for both characters. Dependency audit reports zero vulnerabilities.

## Current behavior / limits
Responsive dark UI, fictional local chat, multiline input, emoji, message selection, loading/empty/paused states and simulated typing. Rive presence includes breathing, blinking, gaze/head drift, typing attention, pause and reduced-motion support. Alex sees Nova's animated female character; Nova sees Alex's animated male character. Both use original Rive rigs above the composer. Insights show clearly labeled mock fixtures and refresh after local sends; no analysis is inferred from text.
No database, inference, authentication or deployment yet. Default local demo messages stay in tab memory. Health reports analysisMode NONE.
Backend domain records and repository ports have a thread-safe in-memory adapter. GET /api/v1/conversations and /api/v1/conversations/{id} expose fictional seed metadata. TEMPER_DEMO_ENABLED=false disables backend seed data. Seed creates no analyses; explicit analyze requests produce labeled MOCK fixtures. On Windows, run spring-boot:run or a JAR copied outside backend/target so packaging can replace its build output.
Phase 11 REST supports conversation creation, ordered messages, explicit ordinal MOCK analysis, analytics, timeline and insights. Open /chat?transport=backend (optionally &conversation=UUID) or set VITE_CHAT_TRANSPORT=backend to use the Java adapter with the existing UI. Real delivery persists across browser reloads within the running backend; restarting Java clears in-memory data. New demo creates a fresh backend room without deleting others. Analysis request is separate from message delivery; no model inference. Default local adapter remains available.
Phase 12 adds live Spring WebSocket/STOMP for transport=backend; transport=rest preserves REST-only delivery. Open the same conversation UUID in two browser sessions, select Alex in one and Nova in the other. Each sees the other avatar; typing/presence are real, send broadcasts immediately and reconnect restores missed messages. Status disables sending during disconnect. Contracts/websocket.md documents topics, errors, heartbeats and development identity limits. Vite proxies /ws; optional VITE_WS_URL must use ws:// or wss://. TEMPER_WS_ALLOWED_ORIGINS controls browser origins. No automatic resend after uncertain delivery.
Phase 13 routes each analysis request through an immutable context window: current message/speaker plus up to five prior turns, ordered by server sequence. GET /api/v1/messages/{id}/context exposes it; analysis stores those causal message IDs. TEMPER_CONTEXT_PREVIOUS_TURNS accepts 3–5, default 5. The mock carries history but does not interpret it; genuine inference follows.

## Rive source
See docs/architecture/rive-rig-spec.md and assets/avatars/temper. Original SVG vectors become editable RML paths; official CLI 1.2.0 builds unscripted .riv files locally without sign-in. Both public files contain two artboards, selected per participant.
Development-only /avatar-lab drives manual targets, not analysis. Default files work without environment configuration. VITE_MALE_RIG_URL and VITE_FEMALE_RIG_URL optionally override them in frontend/.env.local.
Rebuild source with node frontend/scripts/build-rig-source.mjs, then Rive CLI verify/inspect/--once and copy build/temper.riv to both public filenames. Check-rigs records SHA256; file headers alone do not prove animation.






