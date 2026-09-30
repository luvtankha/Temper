# Phase 08 — Message-level explainability UI
1. Objective: inspect every analyzed message's scores/evidence without treating estimates as feelings.
2. Working state: local chat, original Rive presence, labeled mock analytics, responsive Recharts timeline and real point-to-message navigation; Java health.
3. Completed phases: 00–07.
4. Architecture: ChatApi/ChatProvider, AnalysisApi/AnalysisProvider with per-turn signals/events, lazy Rive and timeline modules, modular Java health.
5. Contracts: health REST unchanged; frontend analysis message identity, six emotion/six linguistic scores, signed sentiment/conflict and conversation events.
6. DB: no schema.
7. Frontend: chat/avatar/panel/timeline; history/settings placeholders.
8. Backend: application/common health.
9. AI/model: NONE; synthetic UI fixtures never inspect message text.
10. Tests: 16 units, 48 browser tests defined; 32 phase-specific browser checks pass latest, earlier unaffected Rive/placement checks pass; Java 2 tests. Builds pass.
11. Limits: in-memory messages, simulated typing, no live transport/model/persistence.
12. Preserve: graph focus navigation, participant swap, composer availability, drawer focus and explicit mock labels.
13. Scope: reusable responsive message inspector, normalized signal meters, why-this-matters explanation, source/provenance and previous-turn context; pending/unavailable/error states; accessible dialog/focus/keyboard behavior.
14. Out of scope: Phase 09 complete dashboard, actual models/context engine/backend.
15. Acceptance: any analyzed message exposes scores and source, labels uncertainty, preserves chat on close, supports desktop/mobile and actual keyboard interaction; builds/regressions pass.

