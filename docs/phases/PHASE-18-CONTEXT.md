# Phase18 — Dedicated toxicity and hostility signals
1. Objective: genuine independent toxicity classifier populates existing field; inspect supported insult/threat/hostility proxy without inventing trained classes.
2. State:00–17verified, current native sentiment/emotion/sarcasm, responsive UI/original Rive/livechat/causalcontext; other scoresfixtures.
3. Completed:00–17withcontexts/handoffs/sourceguide/currentcontracts/modelcards/testsreviewed;17gatepassedbeforethisphase.
4. Architecture: stableReactproviders/adapters, modularSpringdomain/ports/memory, causalcontext, independentoptionalsingletonONNXmodels; deliverydoesnotwaitforanalysis.
5. Contracts: REST0.7.0/STOMP; existingtoxicity0..1/sourceevidence, additive supportedsignalsallowed; model-freeMOCK/HYBRIDprovenance.
6. Schema: memoryonly, nomigrations.
7. Frontend: chat/avatar/analytics/timeline/inspector/dashboard; dedicatedMODELlabelsformappedemotions/sarcasm, optionalextraemotionmeters.
8. Backend: domain/context/delivery/WebSocket and hybridclassifiercomposition, reusableverifiedlocaltokenizer/session.
9. Models:4verifiedfoundation/sentiment/emotion/sarcasm; selectedunitary/toxic-bert declaresApache2,6Jigsawlabels. Verifyconfig, matchingtokenizer/checkpoint/exporthash/parity. Upstream warnsHF checkpoint differsfromlatestDetoxifylibrary; documentselectedversion, notlatest-equivalence.
10. Tests: Java31/31completeconfigured, front22/build, latestlive9/9including3modelgates+STOMP+dashboard;69defined;OpenAPIvalid.
11. Limits: toxicity/additional/conflict/arcremainfixtures; classificationnotvalidatedpsychology/chat; noDB/auth/import/deploy, Dockerabsent.
12. Preserve: stableAPIs/models/causalwindow/order, nohostedtext/privateexports, swappedrigs/responsiveflow, honestfixture fallbackonlywhenunconfigured.
13. Scope: licenseddedicatedtoxicityclassifier, source/tokenizer/labels/hashes/export, independentmulti-labelsigmoid, rawsupportedscores, explicitmax(insult,threat)hostilityproxy; API/UIevidenceandtimeline, genuinenative/browserregressions.
14. Out of scope: additional19/conflict20/arc21/avatar22/async23/persistence24/security/deploy; noautomaticmoderationactionorinferredpsychology.
15. Acceptance: genuineinput-dependenttoxicityreplacesfixture, supportedhostilityindicatorseparatelyinspectableandlabeledproxy, validboundedoutput/source/license/hash; tests/builds/previousfeaturespass.
