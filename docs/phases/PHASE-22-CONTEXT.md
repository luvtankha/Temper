# Updated Phase22 — Trajectory engine
1. Objective: seven deterministic estimated direction states, strength/explanation/contributors and fail-closed uncertainty.
2. Repo: updated20/21 verified and committed;40 Java tests including native models, legacy web22/build, Android pending24.
3. Completed: historical00–19; updated overlay scope20; additive causal conflict21. Updated guide and prior handoffs reviewed.
4. Build status: complete configured Maven40/40 and OpenAPI0.10.0 passed; no frontend changes planned.
5. Architecture: pure trajectory engine reads bounded recent analyzed turns and new source-aware conflict results; no capture or rendering dependencies.
6. Risks: fixture channels, insufficient turns, gaps and uncertain model estimates must not become claims about future behavior.
7. Scope: STABLE/TENSION_RISING/ESCALATION_RISK/DEESCALATING/WITHDRAWAL_RISK/REPAIR_OPPORTUNITY/UNCERTAIN, inspectable rule priorities/thresholds and evidence strength; additive REST/snapshot output, fixtures for each state and causal compatibility regression.
8. Non-goals: Android24, visual mapping23, new classifiers, messaging or dashboards.
9. Acceptance: deterministic for fixed windows, insufficient/fixture/gapped context UNCERTAIN, bounded strength and honest short explanations, all prior tests/contract pass.
