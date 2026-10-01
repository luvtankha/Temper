# Phase 15 — RoBERTa sentiment integration
1. Objective: replace mock signed/negative sentiment through existing conversation API using genuine licensed three-label RoBERTa inference and causal context.
2. Working state:00–14 verified; responsive chat, two-client STOMP, original swapped Rive, context3–5, MOCK dashboard/inspector and optional genuine DistilBERT diagnostic.
3. Completed phases:00 foundation,01 responsive shell,02 chat,03 placement,04 rigs,05 presence,06 analytics,07 timeline,08 inspector,09 dashboard,10 domain,11 REST,12 STOMP,13 context,14 Java ONNX. Evidence in preceding context/handoff files.
4. Architecture: React providers/adapters, modular Spring domain/ports/memory, delivery separate from analysis, reusable local TextClassifier.
5. Contracts: REST0.4.0; stable message analysis emotions/signals/signed sentiment/conflict/evidence/context IDs and STOMP messages. Foundation diagnostic isolated; new sentiment provenance must identify partial hybrid output.
6. Schema: in-memory only, no DB/migrations.
7. Frontend: chat/remote Rive/analytics/timeline/inspector/dashboard and local/rest/live adapters. Backend mode labels require truthful HYBRID support already in models.
8. Backend: chat/conversation/context/analysis/store/websocket and ai singleton/native resources, configured foundation diagnostic.
9. Models: pinned Apache2 DistilBERT verified genuine local CPU; RoBERTa source/license/tokenizer/labels/export to verify before use. Binaries outside Git.
10. Tests: Java21 default pass+1 explicit artifact skip; genuine native1/1 plus packaged API; frontend22/build unchanged; live regression7/7,66 browser checks defined; contracts valid.
11. Limits: main analysis MOCK, no PostgreSQL/auth/import/deployment; Docker absent. Other signal classifiers belong16–19.
12. Preserve: delivery without inference delay, causal sequence context, stable schema, bounded probabilities/provenance, default model-free app, original rigs/responsive UX.
13. Scope: licensed three-class RoBERTa/tokenizer/hash manifest, local caching/session singleton, deterministic context input, model sentiment→signed positive-minus-negative and negativeSentiment; other fixtures explicitly remain MOCK; genuine inference and API/browser evidence.
14. Out of scope: dedicated emotions/sarcasm/toxicity/conflict/arc, async analysis stream23, persistence/security/deploy. Do not imply unvalidated context concatenation creates conversation-trained model.
15. Acceptance: actual local RoBERTa sentiment replaces those two mock fields without API break; input-dependent positive/neutral/negative, causal history used and recorded; license/source/hash documented; model unavailable default remains honest MOCK; tests/build/real chat pass.
