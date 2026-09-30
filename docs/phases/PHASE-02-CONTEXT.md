# Phase 02 — Chat UI and message composer

1. **Objective:** Functional two-participant local chat with stable adapter; no real-time backend dependency.
2. **State:** Verified responsive shell, four routes, empty insights panel; Phase 01 committed.
3. **Completed:** 00, 01. All previous contexts/handoffs and contract reviewed.
4. **Architecture:** Permanent Shell with route content; add app-level ChatProvider and ChatApi in-memory implementation so navigation preserves chat.
5. **API:** Health REST unchanged. Frontend ChatApi lists, sends and resets messages using stable participant/message types; backend domain deferred.
6. **Schema:** None.
7. **Frontend:** Shell, router, styles, fetch adapter; add chat models, context, mock adapter, ChatPage.
8. **Backend:** Health foundation unchanged.
9. **AI:** None. Message selection shows metadata and unavailable analysis explicitly.
10. **Tests:** 2 frontend, 2 backend; 12 browser checks; both builds pass.
11. **Limitations:** Local memory only; reload clears messages; no real two-client transport or inference.
12. **Preserve:** Responsive panel behavior, routes, proxy integration, root ignore and Java foundation.
13. **Scope:** Bubbles, identities, timestamps, composer/send, auto-scroll, selection, initial loading/empty states, online/pause demo status and simulated typing; preview either participant.
14. **Out of scope:** Avatar placement/rigs, analytical message explainability, WebSocket, persistence and AI.
15. **Acceptance:** Mock messages send immediately; empty input blocked; multiline/long content safely displayed; both identities usable; loading/empty/paused/typing/selection states exercised; responsive and integration regressions pass.
