# Phase 07 — Emotional arc and conflict timeline UI
1. Objective: responsive Recharts plots and chart-to-message navigation.
2. Working state: chat, original Rive presence, collapsible labeled mock analytics and Java health.
3. Completed phases: 00–06.
4. Architecture: React Shell, ChatProvider/ChatApi, AnalysisProvider/AnalysisApi mock adapter, lazy Rive renderer, Java modular monolith.
5. Contracts: GET /api/v1/health; frontend ConversationAnalysis and per-turn normalized emotions/signals, signed sentiment and conflict; internal twelve rig inputs.
6. DB: no schema.
7. Frontend: shell/chat/avatar/analysis panel; analysis/history/settings routes currently placeholders.
8. Backend: health application only.
9. AI: NONE; ordinal fixtures explicitly labeled, no text inference.
10. Tests: 16 frontend units, 37 browser tests defined; 31 relevant browser checks rerun in Phase 06, six unchanged Rive checks passed Phase 05; Java 2 tests. Builds pass.
11. Limits: local memory, simulated typing, mock signals, no live transport/persistence.
12. Preserve: all chat/input/composer behavior, swapped avatar, drawer accessibility, fixture labels and contract identity.
13. Scope: Recharts timeline plotting sentiment/anger/frustration/sarcasm/conflict over sequence; escalation/peak/major shift/recovery markers; clicking or keyboard activating points selects and scrolls to corresponding chat message; responsive compact/full chart.
14. Out of scope: Phase 08 detailed explanation, Phase 09 full dashboard, backend/domain/inference.
15. Acceptance: chart responsive across all normal tested ratios, signals and markers visible, point navigation actually focuses correct message; build/relevant regression tests pass.

