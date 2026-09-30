# TEMPER

Conversation intelligence for CEREBRO. Follow [AGENT.md](AGENT.md) and the sequential evidence in [docs/phases](docs/phases).

## Development

Prerequisites: Node.js 22+ (tested with 24), Java **21** JDK, network access for the first dependency install. Maven wrapper is included; PostgreSQL and AI models are not required for the current foundation.

```powershell
# Terminal 1: set JAVA_HOME to your Java 21 JDK
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
cd ../backend
.\mvnw.cmd verify
```

Frontend uses same-origin `/api/v1` by default. `VITE_API_BASE_URL` can override it, but a cross-origin server would also need an explicit CORS policy. Root `.env.example` is a configuration reference; no secrets or private conversations belong in Git.

## Current limits

Phase 00 foundation: no real-time chat, database, model inference, authentication, Rive rig or deployment yet. Health reports `analysisMode: NONE` explicitly.
