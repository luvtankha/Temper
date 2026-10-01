# Phase17 — Dedicated sarcasm classifier
1. Objective: genuine separate model estimates per-message sarcasm using available causal context; never derive sarcasm from sentiment.
2. State:00–16verified; working responsiveUI/Rive/two-client chat/context; local sentiment+dedicated mapped emotions actual, remaining scoresfixtures.
3. Completed:00–16 with contexts/handoffs and sequential gates. Sourceguide/repository/contracts/modelcards/tests reviewed; latest16handoff describes fullstate.
4. Architecture: Reactproviders/adapters and modular Spring domain/ports/memory; interchangeable hybridanalysis calls optional pinned singleton classifiers, deliversmessagesbeforeanalysis.
5. Contracts: REST0.6.0/STOMP stable; signal.sarcasm existing0..1, evidence/source/contextIDs; modeHYBRID reflectsremainingfixtures.
6. DB: memory only, no migrations.
7. Frontend: chat/originalswappedRive/livepresence, panel/timeline/inspector/dashboard;6mainemotions+optional2inspectormeters; mixedprovenance.
8. Backend: causal3–5turncontext, domain/store/REST/STOMP, optionalfoundation/sentiment/emotion classifiers and analysis composition.
9. Models: Apache2DistilBERT, ownCCBY4RoBERTaexport, authorMITGoEmotions eachnativeverified/hashpinned; sarcasmsource/license/tokenizer/labelmapping toverify beforeuse.
10. Tests: Java29/29native/artifacts, frontend22/build,20browserregressionsincluding2modelgates+STOMP;68defined;OpenAPIvalid.
11. Limits: sarcasm/toxicity/additional/conflict/arc remainfixtures;semanticemotionproxiesdocumented, modelsnotdialoguevalidated;noDB/auth/import/deploy, Dockerabsent.
12. Preserve: swappedrigs/layout, realtimewithoutblockingAI, currentmodels/contextcausality, honestfixtures/source, APIbounds and model-freefallbackonlywhenunconfigured.
13. Scope: verifiedlicensedseparate sarcasmmodel, localexport/cache ifneeded, explicitlabels/tokenizer/hash, genuineindependent score+evidence/currentcontext, accurateUIchartsource, native/API/browser tests/regressions.
14. Out of scope: toxicity18/additional19/conflict20/arc21/avatar22/async23/persistence24/security/deploy. No earlyoptimization/infrastructure.
15. Acceptance: input-dependent realdedicatedsarcasminference replacesexistingfixturescore, inspector/timeline exposeMODELsource, sentimentnotusedasproxy; source/license documented andbuilds/previousfeaturespass.
