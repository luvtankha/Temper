# Updated Phase21 — Conflict engine finalization
1. Objective: deterministic raw/smoothed conflict, UP/FLAT/DOWN trend and ranked contributors.
2. Repo: updated20 verified; legacy web harness,37 passing Java tests,22 frontend tests/build; Android pending24.
3. Completed:00–19 preserved foundations,20 overlay-only pivot; updated source read and prior handoffs reviewed.
4. Tests: complete native configured suite passed at20; model/tokenizer hash checks remain intact.
5. Architecture: pure configurable engine consumes bounded stored signal maps, no Android/render dependencies; additive conflictAnalysis field preserves existing legacy fields.
6. Risks: scores engineering indicators rather than calibrated probabilities; fixture inputs must be explicitly identified. Smoothing must exclude future turns and handle gaps.
7. Scope: validated six weights, three prior-position smoothing weights and trend threshold; ranked source-aware contributions; stable additive REST output and meaningful math/context tests.
8. Non-goals: trajectory22, mapping23, Android24, changes to models, legacy messaging features.
9. Acceptance: deterministic fixed inputs, bounded outputs, causal smoothing, config failures rejected, old API/tests pass and new contract validated.
