# Phase 10 — Spring Boot domain model
1. Objective: persistent-ready backend domain abstractions, usable without PostgreSQL.
2. State: complete frontend mock dashboard/chat with genuine swapped Rive presence, signal inspector/timeline; Java 21 Spring Boot health.
3. Completed: 00–09, all documented.
4. Architecture: React API/provider boundaries; Spring modular monolith, currently common health only. Introduce auth/conversation/chat/analysis/emotion/conflict domain packages and persistence/memory adapters behind repository ports.
5. Contracts: GET /api/v1/health (UP/TEMPER/NONE), frontend ChatApi and AnalysisApi with normalized scores/evidence/events. Add minimal versioned domain reads, document them in OpenAPI.
6. DB: none; no migrations/JPA until its assigned later phase.
7. Frontend: shell, chat, original Rive/presence, analytics/dashboard/charts/inspector, placeholder history/settings.
8. Backend: application/common health, Maven Java 21.
9. AI: NONE; frontend ordinal fixtures only. Backend must not silently synthesize model analyses.
10. Tests: React build, 18 units; 60 browser behaviors verified (59 broad + corrected clock test 3/3); Java build/2 health tests. Genuine rigs verified.
11. Limits: frontend local memory, no real transport/model/database/auth. No Docker executable.
12. Preserve: health payload, frontend/API abstractions, rig files and all responsive/interaction behavior.
13. Scope: immutable User/Conversation/Participant/Message/MessageAnalysis/ConversationAnalysis/EmotionSnapshot/EscalationEvent, repository interfaces, thread-safe in-memory adapters, fictional seed, minimal domain read APIs and domain/controller tests.
14. Out of scope: full REST mutations/analysis endpoints (Phase 11), STOMP (12), context (13), model inference (14+), PostgreSQL/JPA/security/deployment.
15. Acceptance: real Java domain endpoints return correct seeded data without DB/model; ports usable by later adapters; validation and storage behavior tested; backend package/tests and frontend health/regression remain functional.

