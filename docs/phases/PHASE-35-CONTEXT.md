# Phase35 — Short analytics text
1. Objective: keep two short summaries useful during inference and failure as well as success.
2. Repository: actual estimates are already concise; pipeline currently clears to generic unavailable during requests or transport failures.
3. Previous:20–34 complete.
4. Checks: backend mapper3/3; Android build/lint/parser tests and real graph/scroll/keyboard gate pass.
5. Architecture: two labels plus one graph; summary data max64 characters, scores hidden when unavailable.
6. Risks: generic uncertainty can be mistaken for a model result; staged native tests run in38 to avoid repeated service restarts.
7. Scope: distinct analyzing/connection/unavailable wording, current onboarding/disclosure wording; preserve compact content.
8. Non-goals: detailed evidence dashboard, raw message quotations, recommendations or extra graphs.
9. Acceptance: exactly two bounded summaries; estimates remain tentative; transport/layout absence distinguished from model evidence.
