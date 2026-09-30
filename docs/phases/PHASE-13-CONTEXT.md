# Phase 13 — Conversation context engine
1. Objective: every analysis request receives current turn plus previous 3–5 ordered turns and speaker identity.
2. Working state: genuine two-session STOMP chat/typing/presence/recovery, REST adapter, original swapped Rive, responsive mock analytics/dashboard/inspector.
3. Completed: 00–12. Prior context/handoffs, contracts, architecture, source guide, modules/tests reviewed.
4. Architecture: Java domain ports and in-memory storage; application services separate delivery from explicit analysis. React stable ChatApi/AnalysisApi and streaming provider.
5. Contracts: versioned REST0.2.0 and native STOMP documented; analysis context IDs currently empty in backend fixture. Extend context read while preserving existing response fields.
6. Schema: none; process memory, no migrations.
7. Frontend: shell/chat/avatar/provider, REST/live adapters, analysis/dashboard/timeline/inspector. No frontend restructure needed.
8. Backend: domain/store/REST, mock EmotionAnalysisService and analysis orchestration; websocket broker/guard/presence. Add conversation context service/window.
9. Models: no ONNX. Ordinal MOCK data remains mock even after it carries actual history; no contextual interpretation claimed.
10. Tests: Java16/16/build; frontend22 units/build; 65 browser behaviors verified (64 broad + repaired domain check). Actual network clients and original Rive verified.
11. Limits: no database/auth/model; dev identity claims only. Docker absent.
12. Preserve: immediate message delivery, REST/live compatibility, score provenance, causal message IDs, avatars/responsive interactions.
13. Scope: immutable typed context window/current/speaker, repository-driven service bounded to 3–5 preceding turns, context-aware analysis interface, stored references, diagnostic REST context read and tests.
14. Out of scope: ONNX14+, actual contextual sentiment/emotion15+, conflict/arc20–21, async events23, DB24/security29.
15. Acceptance: actual analyze path uses context service; ordering follows server sequence regardless of timestamps; no current/future/foreign turns leak into preceding window; immutable outputs, validated configurable bounds, relevant builds/network regressions pass.
