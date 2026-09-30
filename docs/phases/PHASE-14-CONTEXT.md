# Phase 14 — Java ONNX inference foundation
1. Objective: one genuine local text classifier executes through Java ONNX Runtime with compatible tokenizer and singleton model lifecycle.
2. State: real STOMP/REST chat, causal speaker-aware history, original swapped Rive, responsive analytics/inspector/dashboard currently explicit MOCK.
3. Completed: 00–13. Prior phase evidence, contracts, repository modules/tests, model/schema status and source guide reviewed.
4. Architecture: modular Spring monolith with repository ports, context engine and interchangeable analysis interface; React adapters/providers. Introduce isolated ai TextClassifier boundary.
5. Contracts: REST0.3.0 plus native STOMP; health NONE still describes main analysis engine. Diagnostic classification endpoint may expose foundation independently; no premature production signal substitution.
6. DB: none, memory; no migrations.
7. Frontend: existing local/REST/live chat, original avatars, mock analytics/dashboard/inspector; unchanged planned.
8. Backend: domain/ports/store, delivery/analysis/context services, WebSocket broker/session/member checks. Add local tokenizer/session/label result/configuration.
9. Model status: no model binaries present. Select primary-source licensed pretrained classifier and matching tokenizer; pin revision/checksum, cache outside Git. Full RoBERTa signal integration is Phase15.
10. Tests: Java20/build; frontend22/build; 66 browser checks defined, latest context/domain/health/REST/realtime7/7. OpenAPI valid.
11. Limits: no genuine inference yet, PostgreSQL/auth/deployment absent; Docker not installed. Native tokenizer/runtime compatibility on Windows must be proven.
12. Preserve: delivery independence, causal context, stable REST, explicit mock provenance, original rigs/responsive behavior, secret/model-cache ignore rules.
13. Scope: TextClassifier/ClassificationResult, actual CPU ONNX/session/tokenization, bounded inputs and resource closure, optional local model configuration, license/hash/source docs and download reproducibility; genuine Java/API inference tests and previous regressions.
14. Out of scope: replacing main sentiment with RoBERTa15, dedicated emotion16/sarcasm17/toxicity18, real conflict20/arc21/avatar22, async23, PostgreSQL24/security29/deployment.
15. Acceptance: actual pretrained model classifies texts locally from Spring Boot; output depends on tokens; tokenizer compatibility verified; session loaded once and reused; missing configuration leaves working mock app with honest unavailable diagnostic; builds/tests and live transport continue.
