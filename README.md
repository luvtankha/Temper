# TEMPER

Conversation intelligence for CEREBRO. Follow [AGENT.md](AGENT.md) and the sequential evidence in [docs/phases](docs/phases). **Phases 00–03 verified; Phase 04 in progress, blocked on Rive authoring/export access.** This is a working local chat foundation, not the final AI product.

## Development

Prerequisites: Node.js 22+ (tested with 24), Java **21** JDK, network access for the first dependency install. Maven wrapper is included; PostgreSQL and AI models are not required for the current foundation.

```powershell
# Terminal 1: set JAVA_HOME to your Java 21 JDK
# On the original development machine, the provisioned JDK is:
$env:JAVA_HOME = 'C:\Users\Admin\.codex\cache\temper-tools\jdk-21.0.12.1+1'
cd backend
.\mvnw.cmd spring-boot:run

# Terminal 2
cd frontend
npm ci
npm run dev
```

Open http://127.0.0.1:5173. Vite proxies `/api` and `/actuator` to port 8080. Backend health: http://127.0.0.1:8080/api/v1/health.

```powershell
cd frontend
npm test
npm run build
npm run test:e2e -- --workers=2 # Start the backend first for health integration
cd ../backend
.\mvnw.cmd verify
```

Frontend uses same-origin `/api/v1` by default. `VITE_API_BASE_URL` can override it, but a cross-origin server would also need an explicit CORS policy. Root `.env.example` is a configuration reference; no secrets or private conversations belong in Git.

## Current limits

The responsive dark UI includes local two-participant chat, fictional sample messages, multiline input, emoji, message selection, loading/empty/paused states and simulated typing. Conversation options let you preview either participant: Alex sees Nova’s avatar; Nova sees Alex’s avatar. Neutral vector previews are anchored above the composer. Insights are collapsible, but genuine analytics have not been introduced yet.

There is no two-client real-time transport, database, model inference, authentication or deployment yet. History, analysis and settings are route placeholders. Messages stay in memory; reload restores the fictional sample. Health reports `analysisMode: NONE` explicitly.

## Phase 04 Rive checkpoint

Typed emotion controls, runtime validation, bundled local WASM and a lazy-loaded Rive renderer are prepared. **Both authored character exports are missing**, so smooth Rive emotional animation has not passed acceptance. The editor currently requires sign-in; runtime export also requires suitable Rive plan access. No account or paid plan was created.

See [rig authoring contract](docs/architecture/rive-rig-spec.md) and [Phase 04 checkpoint](docs/phases/PHASE-04-HANDOFF.md). `npm run check:rigs` deliberately fails until original male/female runtime exports exist. This failure is separate from the passing frontend build and regression tests.

After supplying `frontend/public/avatars/male.riv` and `female.riv`, set the optional rig keys from root `.env.example` in **frontend/.env.local**, restart Vite, and open http://127.0.0.1:5173/avatar-lab (development only). Drive all eight states for both participant views and inspect smooth reversal. A mapping unit test or valid file header does not prove animated rig acceptance.

## Verification checkpoint

- React/TypeScript production build: passes.
- Frontend unit tests: 12 pass.
- Browser checks: 27 pass, including ten viewport sizes, swapped-avatar placement, chat interaction, resize-to-dialog accessibility, authoring-harness honesty and actual Java health through Vite.
- Java 21 Maven verify: 2 tests pass; executable Spring Boot JAR builds and serves health.
- Dependency audit: zero reported vulnerabilities.
- Phase 04 genuine Rive playback gate: **not passed**; no runtime character exports.
