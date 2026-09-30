# TEMPER
Conversation intelligence for CEREBRO. Follow AGENT.md and sequential evidence in docs/phases. **Phases 00–11 verified.**

## Development
Requires Node 22+ (tested 24), Java 21 JDK. Maven wrapper included. No PostgreSQL or AI models required yet.

Terminal 1 (PowerShell): set JAVA_HOME to a Java 21 JDK, enter backend, run .\\mvnw.cmd spring-boot:run.
On this machine the JDK is C:\\Users\\Admin\\.codex\\cache\\temper-tools\\jdk-21.0.12.1+1.
Terminal 2: enter frontend, npm ci, npm run dev.
Open http://127.0.0.1:5173. Vite proxies /api and /actuator to backend port 8080.

## Verification
In frontend: npm test; npm run build; npm run test:e2e -- --workers=2 (backend must run); npm run check:rigs.
In backend with Java 21: .\\mvnw.cmd verify.
Checkpoint: frontend build and 20 units pass; full 64 browser checks pass; Java build and 14 tests pass. OpenAPI validates. Actual Rive canvas tests exercise eight poses, intensity blends, reversal, overlapping signals and staged transitions for both characters. Dependency audit reports zero vulnerabilities.

## Current behavior / limits
Responsive dark UI, fictional local chat, multiline input, emoji, message selection, loading/empty/paused states and simulated typing. Rive presence includes breathing, blinking, gaze/head drift, typing attention, pause and reduced-motion support. Alex sees Nova's animated female character; Nova sees Alex's animated male character. Both use original Rive rigs above the composer. Insights show clearly labeled mock fixtures and refresh after local sends; no analysis is inferred from text.
No two-client real-time transport, database, inference, authentication or deployment. Messages stay in memory; reload restores the fictional sample. Health reports analysisMode NONE.
Backend domain records and repository ports have a thread-safe in-memory adapter. GET /api/v1/conversations and /api/v1/conversations/{id} expose fictional seed metadata. TEMPER_DEMO_ENABLED=false disables backend seed data. No backend analyses are fabricated. On Windows, run spring-boot:run or a JAR copied outside backend/target so packaging can replace its build output.
Phase 11 REST supports conversation creation, ordered messages, explicit ordinal MOCK analysis, analytics, timeline and insights. Open /chat?transport=backend (optionally &conversation=UUID) or set VITE_CHAT_TRANSPORT=backend to use the Java adapter with the existing UI. Real delivery persists across browser reloads within the running backend; restarting Java clears in-memory data. New demo creates a fresh backend room without deleting others. Analysis request is separate from message delivery; no model inference. Default local adapter remains available.

## Rive source
See docs/architecture/rive-rig-spec.md and assets/avatars/temper. Original SVG vectors become editable RML paths; official CLI 1.2.0 builds unscripted .riv files locally without sign-in. Both public files contain two artboards, selected per participant.
Development-only /avatar-lab drives manual targets, not analysis. Default files work without environment configuration. VITE_MALE_RIG_URL and VITE_FEMALE_RIG_URL optionally override them in frontend/.env.local.
Rebuild source with node frontend/scripts/build-rig-source.mjs, then Rive CLI verify/inspect/--once and copy build/temper.riv to both public filenames. Check-rigs records SHA256; file headers alone do not prove animation.






