# Phase41 — Consumer app and avatar catalog
1. Objective: turn TEMPER into an Android app with a free starter, paid cosmetic avatars, activation and a deployable release path.
2. Repository: constrained USB WhatsApp demo through40; spectrum speaker-attribution fix;50 backend tests pass; clean commit08c700f.
3. Previous: existing analysis/models, native character, consent, parser and test history preserved.
4. Checks: Android0.39.0 build/native suite previously passed; phone disconnected; local backend healthy.
5. Architecture: native Java Android app, original2D vector character, exact-build chat adapter, local Java model backend. New consumer work will separate a general floating companion from validated automatic chat analysis.
6. Risks: no Play Console products/account, production purchase verification or signed release configured; universal message extraction is not guaranteed by Android; current emotion model has Hinglish/sarcasm limits.
7. Scope: original avatar catalog/selection, consumer home/shop, floating overlay controls, on-device inference, real Play Billing with server verification, versioned consent, release build/deployment configuration and dummy-only verification. These are implemented together in this phase because the owner delegated the revenue and runtime choices. Production sales still require owner configuration and store testing.
8. Non-goals: fake purchases/prices/entitlements, automatic host actions, arbitrary chat scraping, claiming unverified app/model compatibility or public store approval.
9. Acceptance: free avatar/activation flow works, premium previews remain locked without verified purchases, chosen avatar renders in the companion, app/build stays runnable; release blockers stated precisely.
