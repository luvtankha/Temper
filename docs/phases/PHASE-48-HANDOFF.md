# Phase 48 — Final verification and release review

## 1. What changed

Android 0.48.0/code 48 preserves the approved carousel, single power switch, movable character and compact analysis panel. This pass fixes center rounding/interrupted swipes, capability/service-based power state, service/overlay failure cleanup, window metadata/insets, bounded/finite model input validation, summary/graph clipping, selected-avatar fictional previews and corrupt-model retry. Public size/hash-verified weights are bundled after a real phone CDN DNS failure; first-use preparation now works offline. Classifier weights and product scope are unchanged.

## 2. Files changed or added

- `android/app/build.gradle`: version, isolated QA flag and typed AGP generated model assets.
- `MainActivity`, `home/AvatarCarousel`, `home/AvatarHome`: capability gating, pending-start lifecycle, exact center/cancellation, inset handling and accurate model disclosure.
- `overlay/FloatingOverlayService`, `FloatingPlacement`, `OverlayManager`, `accessibility/ForegroundSessionPolicy`, `TemperAccessibilityService` and service XML: foreground readiness, permission/error cleanup, usable geometry and system-window observation.
- `inference/ModelFiles`, `OnDeviceAnalysis`, `ConversationContextModel`: bundled preparation/integrity recovery, capped mention expansion and context validation.
- `analytics/AnalyticsPanel`, `SpectrumView`, `character/CharacterView`: readable measured layouts and resource fallback.
- JVM carousel/placement/session tests and new `inference/ModelBoundaryTests`; updated Android checks; new `FreshInstallChecks`, `ModelRecoveryChecks`, `PermissionRecoveryChecks` and `overlay/FloatingInteractionChecks`.
- Root/Android READMEs, release setup/report/privacy/feedback guides, model notice, packaging script, phase context/handoff and text-free evidence JSON.

## 3. Verification and build results

Clean debug, Android-test, R8/resource-shrunk release APK and unsigned review AAB pass. JVM: 44 tests, no failures/errors/skips. Lint: zero errors, 34 debug/33 release warnings. Model bytes/hash match inside all APK/AAB variants. ZIP and eight native ELF libraries pass 16 KB alignment. The minified, non-debuggable, development-signed review release is installed on the OnePlus 8T API 34.

Manual home/settings/setup, privacy/model, unconfigured feedback, legacy companion/expressions, selected fictional preview and labelled panel inspection showed centered artwork, readable copy/bars, connected Accessibility, prepared model and actual ON. The final live extracted-text graph/update and repeated full demo are not yet confirmed. The [final report](../release/FINAL-VERIFICATION.md) uses the required **FAIL — RELEASE BLOCKERS REMAIN** result while critical evidence is outstanding. A packaged artifact is a review candidate, not a claim of accepted release readiness.

## 4. Tests run

Passing generated device lanes: carouselOnly, powerOnly, consumerOnly, complexOnly, overlayOnly, performanceOnly and foundation. They cover real model/tokenizer execution on fictional inputs, 28 complex English/Hinglish held-out cases, repair transitions, first incoming bars, stopped/stale results, app-generated overlay drag/tap/keyboard/stop and large catalogs. Isolated freshOnly and modelRecoveryOnly pass OFF/default unchecked consent, offline bundled preparation, corrupt same-size weights, retry/restoration and real inference. The owner's primary data was not cleared; disposable QA packages were removed after testing.

Latest real-model timing: cold 692.57 ms; new incoming median 25.70 ms; repeated unchanged about 2.22 ms. Peak/settled PSS about 230.5/199 MiB. These exclude full host extraction/scheduling/UI latency. Raw main-source logging search and tracked secrets/runtime-artifact audit found no leak path. Independent read-only review found no additional confirmed major source defect.

## 5. Known issues

Permission-only AppOps automation was blocked by this phone's OS shell permissions and is not a passed lane. Actual overlay/Accessibility revoke/re-grant, configured purchase/feedback branches, final live update and repeated evaluator flow remain open. Fresh default/model preparation was tested, not a complete fresh grant-to-host sequence. General emotion/Hinglish accuracy, hardware/API/16 KB runtime matrix, reboot, full airplane-mode demo, packet capture and long battery/heap soak are unverified.

Automatic analysis is restricted to WhatsApp 2.26.37.73, portrait one-to-one plain text. Unsupported content fails closed; fallback characters do not imply another app is analyzed. Model bars are independent latest-incoming estimates; pilot direction is not validated ground truth. Bundling adds substantial installation/storage cost. Publishing still needs owner signing, Play account/declarations, final policy hosting and real service/store tests. Lower-priority lint/Gradle migration warnings remain.

## 6. Next phase prerequisites

Use [SETUP-AND-DEMO.md](../release/SETUP-AND-DEMO.md) for exact build/install/first-use and evaluator steps. Complete the pending fictional live graph/change, keyboard/drag/OFF/ON repeat and actual permission loss/recovery checks, then update the final report/evidence with observed results. Do not use UIAutomator on an active Accessibility session. If Android disconnects the service after a test/update, the human can rebind Use TEMPER OFF/ON; do not clear the primary installation. The permissionOnly helper needs isolated QA, prepared weights and a permitted external AppOps revocation driver and covers only revocation. Never count ADB exit zero without the lane's explicit PASS.

Source ZIPs exclude weights and keys; source builds obtain the pinned public weights or use TEMPER_MODEL_FILE. Installable review artifacts contain the verified weights. No backend is needed for normal on-phone inference. Review artifacts remain development-signed/unsigned as labelled; publish only after separate owner release prerequisites and actual acceptance gates are complete.
