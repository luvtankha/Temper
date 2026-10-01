# Phase34 — Compact live graph
1. Objective: verify the existing single eight-row live spectrum.
2. Repository: SpectrumView/AnalyticsPanel already render eight bounded independent scores and explicit unavailable dashes.
3. Previous:20–33 complete for constrained demo.
4. Checks: latest Android build/lint/adapter tests pass; actual graph, scroll refresh and keyboard behavior confirmed by user.
5. Architecture: backend remote scores → validated eight-element response → immutable OverlaySummary → one SpectrumView.
6. Risks: font scaling and unavailable states; unsupported layouts stop the session.
7. Scope: audit chart order/data bounds/accessibility and record real-device acceptance.
8. Non-goals: additional graphs, probabilities summing to one, analytics dashboard.
9. Acceptance: one readable graph, eight named rows, real updates, no invented values when unavailable.
