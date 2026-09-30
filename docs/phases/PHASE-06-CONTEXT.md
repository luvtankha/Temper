# Phase 06 — Live analytics UI
1. Objective: collapsible analytics using clearly labeled mock data.
2. Working state: local chat, responsive shell, swapped genuine Rive expressions/presence and Java health.
3. Completed phases: 00–05; all gates recorded.
4. Architecture: React Shell/ChatProvider/ChatApi; RemoteAvatar lazy Rive runtime; local generator/rig source; modular Spring Boot health service.
5. API contracts: health REST only; ten normalized semantic avatar controls; twelve internal rig inputs. Introduce frontend AnalysisApi without claiming backend analysis.
6. DB schema: none.
7. Frontend: chat, avatar, health, shell; analytics/history/settings placeholders.
8. Backend: application and health module; no changes planned.
9. AI/model status: NONE. Analytics must explicitly identify synthetic fixtures, independent of message text.
10. Passing tests: 12 frontend units, 32 browser, 2 Java; builds and rig verify pass.
11. Limits: in-memory sample messages, simulated typing, no transport/inference/persistence/auth.
12. Preserve: composer, participant swap, presence pause/typing, responsive drawers/focus and health.
13. Scope: typed analysis API/mock adapter/provider; emotion and linguistic signal meters; conflict/sentiment/intensity/direction/escalation/peak metrics; loading/empty/error handling and live refresh after sends; clear uncertainty/fixture labels.
14. Out of scope: Phase 07 charts, Phase 08 detailed message explainability, Phase 09 dashboard, backend analysis/inference.
15. Acceptance: metrics visible responsively on desktop/tablet/mobile; panel hides without harming chat; mock nature explicit; new turns update snapshot through adapter; prior tests/build pass.

