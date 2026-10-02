# Phase 47 — Avatar carousel and focused Android home

1. Objective: implement the attached UI specification in the current Android project and create an editable Figma library and prototype using the reference for interaction.
2. Repository: Phase 46 commit f717994; Android 0.46.0; existing automatic on-phone inference, draggable companion, explicit setup and optional reviewed feedback.
3. Previous phases: retain model, analysis scheduling, Android service, privacy, purchase entitlement verification and overlay movement.
4. Baseline: Phase 46 JVM/native/build checks passed; no new verification has run yet.
5. Architecture: native Java views with a cosmetic Avatar enum, shared CharacterView, persistent AvatarSelection and one PowerStore control. New carousel geometry will be independent of catalog size and render a bounded neighborhood.
6. Risks: custom gesture arbitration, exact snapping, lifecycle cancellation, font scaling, selected-art consistency, and not confusing cosmetic selection with activation or bypassing consent. Existing paid entitlements remain valid; the requested prototype avatars are separate free examples.
7. Scope: five original 2D prototype characters; dark responsive home; carousel, hero/platform, status pill, single switch and preview surface; accessible settings route; Figma pages, tokens, reusable components and forward/reverse prototype; regression tests and review APK.
8. Non-goals: retraining the classifier, new host-chat adapters, storefront UI, publishing, collecting chats, or granting paid legacy avatars without ownership. Figma Smart Animate illustrates state transitions; continuous gesture geometry runs in Android.
9. Acceptance: centered continuous carousel with 300 ms snapping; bounded rendering for larger catalogs; persisted cosmetic selection shared with overlays; explicit setup before ON and immediate OFF; no duplicate home actions or monetization panels; native and Figma visual checks, existing analysis regression checks and documented limits.
