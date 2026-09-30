# Phase 11 — Stable REST API
1. Objective: versioned creation/message/analysis reads and commands, interchangeable frontend backend adapters.
2. State: original animated swapped Rive avatars, responsive chat/dashboard/inspector, mock analytics; Java domain metadata reads verified live.
3. Completed: 00–10. Previous contexts/handoffs, source guide, contracts, modules and tests reviewed.
4. Architecture: React ChatApi/AnalysisApi providers; Spring modular monolith and immutable domain repository ports, thread-safe in-memory adapter.
5. Contracts: health UP/TEMPER/NONE; conversation metadata list/detail; frontend message and analysis contracts. Extend with versioned messages/analyze/analytics/timeline/insights and explicit provenance.
6. Schema: none, in-memory; no migrations needed.
7. Frontend: shell, chat/provider, Rive renderer/lab, analysis/provider/panel/dashboard/inspector, history/settings placeholders.
8. Backend: auth/conversation/chat/analysis/emotion/conflict records and ports, in-memory store, fictional seed and common health.
9. Models: NONE. New analysis implementation must be explicitly MOCK, assigned by ordinal only; actual context engine/inference scheduled later.
10. Tests: frontend build/18 units; 62 browser tests defined, latest 11 live domain/health/chat/dashboard pass; backend clean verify 11 tests.
11. Limits: no WebSocket, database, authentication/model inference/deployment. Docker absent.
12. Preserve: health NONE, seeded metadata, identity/order integrity, responsive routes/avatars, mock behavior and evidence.
13. Scope: REST conversation creation and messages, explicit mock analysis service behind interface, stable error bodies, read analytics/timeline/insights; switchable backend frontend adapters without UI restructuring, request cancellation/errors and tests.
14. Out of scope: STOMP (12), context engine (13), ONNX (14+), real conflict/arc (20–21), persistence/auth/deployment.
15. Acceptance: create/send/list/analyze/read through actual HTTP and frontend provider; errors validated; unavailable analysis distinct from scores; all touched builds/tests pass; preserve local demo.
