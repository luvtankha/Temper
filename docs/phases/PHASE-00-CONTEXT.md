# Phase 00 — Repository and integration foundation

1. **Objective:** Create independently runnable React and Java 21 Spring Boot foundations and verify frontend-to-backend health access.
2. **Working state:** Empty Git repository inspected; no project files or commits existed.
3. **Completed phases:** None.
4. **Architecture:** Planned React/Vite client, modular Spring Boot monolith, stable REST abstraction; no database dependency yet.
5. **API contracts:** Introduce GET `/api/v1/health` and Actuator health. Preserve the analysis schema in AGENT.md for future phases.
6. **Database schema:** None; persistence is out of scope.
7. **Frontend modules:** App shell, router, API health adapter, mock provider, styles.
8. **Backend modules:** Application entry point and common health controller.
9. **AI/model status:** No models, no inference, no generated analysis.
10. **Passing tests:** None at entry.
11. **Limitations:** Host has Node 24 and Java 27; Java 21 and Maven must be provisioned locally. No Docker executable found. Supplied avatar references are PNG sheets, not Rive rigs.
12. **Preserve:** Attached AGENT.md and source images; existing Git metadata.
13. **Scope:** Repository shell, tooling, health API, basic dark app, tests, OpenAPI, README and environment example.
14. **Out of scope:** Chat behavior, avatars, analytics, AI, persistence, authentication, containers and public deployment.
15. **Acceptance:** Frontend builds/starts, backend builds/tests/starts on Java 21, frontend proxy returns backend health, no DB/AI required, committed clean milestone.
