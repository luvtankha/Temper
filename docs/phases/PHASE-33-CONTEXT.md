# Phase33 — Direction-aware reactions
1. Objective: verify that conversation trajectory selects the compact character expression.
2. Repository: existing CompactOverlayMapper already implements the trajectory mapping; Android renders its result through live callbacks.
3. Previous: phases20–32 complete for the constrained WhatsApp plain-text demo; historical00–19 preserved.
4. Checks:48 backend tests previously passed with models; Android build/lint and targeted parser checks passed; user confirmed actual scores, scroll refresh and keyboard behavior.
5. Architecture: stateless remote-spectrum mapping plus latest trajectory; fixed64×88dp renderer with260ms transition.
6. Risks: limited WhatsApp build/layout support; phone temporarily disconnected; no inferred emotion is proof of feelings.
7. Scope: audit existing direction mapping and rerun its deterministic regressions.
8. Non-goals: new model, larger body motion, extra graphs or host automation.
9. Acceptance: rising tension, escalation, withdrawal and de-escalation select different expressions while preserving remote spectrum.
