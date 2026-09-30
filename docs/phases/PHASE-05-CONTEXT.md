# Phase 05 — Avatar presence and micro-interactions
1. Objective: add genuine Rive breathing, blinking, eye/head drift, typing attention and staggered message reaction; intentional pause freezes motion.
2. Working state: local chat, swapped animated remote characters, eight reversible continuous expressions and Java health work.
3. Completed phases: 00–04, all documented with evidence.
4. Architecture: React Shell → ChatProvider/ChatApi; RemoteAvatar → lazy RiveCharacter; original editable RML source generated from authored SVG vectors. Java modular monolith health only.
5. API contracts: REST GET /api/v1/health unchanged; frontend ten normalized semantic controls, percentage conversion internal to Rive. No server frames.
6. Database: no schema or persistence.
7. Frontend modules: chat, avatar, health, shell, placeholder analysis/history/settings; development avatar authoring lab.
8. Backend modules: TemperApplication, health controller; unchanged.
9. AI/model status: NONE; manual targets and simulated typing, no inference. Real unscripted .riv rigs supplied locally using official CLI.
10. Passing tests: 12 frontend units, 28 browser checks, 2 backend tests; React/Java builds pass; CLI verify/inspect clean.
11. Limitations: local in-memory conversation, no live two-client transport or analysis; presence reactions must explicitly use preview fixtures until later integration.
12. Preserve: swapped identity, all emotion transitions, numeric validation, responsive composer placement, chat semantics, pause/resume, health endpoint.
13. Scope: add independent idle layers and presence controls; consume existing remoteTyping/paused; lab message-reaction preview behind reusable avatar props; reduced-motion preference; test actual canvas changes and pause.
14. Out of scope: analytics, backend messaging/model/DB, external publishing.
15. Acceptance: non-frozen idle unless paused, breathing/blinking/gaze/head motion; typing rises and looks toward chat; analysis-shaped manual target triggers face/head/body timing; motion reversible; all previous behavior/build/tests pass.

