# Phase 12 — Real-time WebSocket chat
1. Objective: two browser sessions exchange messages over Spring WebSocket/STOMP, with real remote typing/presence.
2. State: responsive local and Java REST chat, original swapped Rive animation, full MOCK analytics/inspector/dashboard, error handling; all verified.
3. Completed phases: 00–11, prior contexts/handoffs and contracts reviewed.
4. Architecture: React adapter/provider interfaces; modular Spring monolith, in-memory repository ports and application services; add broker transport around existing message service.
5. Contracts: versioned REST in OpenAPI 0.2.0. STOMP /ws, /app/conversations/{id}/send and /typing; room topics messages/typing/presence; separate analysis topic reserved for Phase 23.
6. DB schema: none, in-memory unchanged.
7. Frontend: shell/chat/provider, local and backend adapters, Rive, analytics/dashboard/inspector; history/settings placeholders.
8. Backend: domain/ports/store, REST controllers/services, explicit ordinal mock analysis and stable errors; add websocket package.
9. Models: no ONNX; MOCK never reads message text. No real analysis engine yet.
10. Tests: Java build14 tests; frontend build20 units; full64 browser tests pass; OpenAPI valid.
11. Limits: no DB/auth/model/deployment. Identities are development claims until security Phase29. Docker unavailable.
12. Preserve: REST compatibility, ordering and membership validation; no duplicate sender message; responsive composer/avatars, local fixtures, stable health NONE.
13. Scope: STOMP broker/session/member checks, send save/broadcast, typing/presence, reconnect/resync and clear connection states; backend selectable transport; real two-context browser verification including disconnect/reconnect and avatar swap.
14. Out of scope: contextual inference13, ONNX14+, asynchronous analysis23, persistence24, authentication29, deployment.
15. Acceptance: two independent browser sessions send/receive immediately without waiting for analysis; no duplicates/order loss, real typing affects opposite avatar, disconnect recovers with catch-up; touched builds/tests and previous REST/local UI pass.
