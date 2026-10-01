# Updated Phase23 — Compact overlay mapping
1. Objective: isolated unit-testable transform to small character state, current/direction strings and eight spectrum bars.
2. Repo: historical00–19 plus updated20–22 verified; native Java42/42 and OpenAPI0.11.0 valid; legacy frontend unchanged22/build.
3. Completed: overlay-only pivot, configurable conflict, seven-state fail-closed trajectory; guide and handoffs reviewed.
4. Builds: Spring packaged, no Android project yet; web remains debug-only.
5. Architecture: mapper consumes analyzed turns/trajectory and remote speaker role; no UI or host-app assumptions; remote emotion separated from whole-conversation direction.
6. Risks: avoid reflecting local speaker's emotions as remote; fixtures/unknown role/unavailable estimates return no invented spectrum.
7. Scope: eight-state character enum, bounded spectrum order, two concise strings, availability and source reference; pure transform consumed by later live pipeline; no render/dashboard additions.
8. Non-goals: Android24, account25, capture/adapters/WhatsApp/device acceptance; no new chat features.
9. Acceptance: mapper pure/unit-tested for remote selection, safe unavailable and all trajectory priorities; stable API contract, previous checks pass.
