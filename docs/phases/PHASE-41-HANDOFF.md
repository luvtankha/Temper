# Phase 41 — Consumer candidate 0.41.0

## 1. What changed

TEMPER opens to a native companion picker and activation flow. Alex is free; Nova, Orbit and Luma are optional one-time cosmetic products with eight expressions each. The general foreground companion uses Android display-over-other-apps permission, supports dragging/tapping/stopping, and reads no host content. The existing verified WhatsApp Accessibility overlay retains composer/keyboard anchoring and suppresses the general companion to avoid duplication.

Automatic analysis now has a separate versioned consent and an on-device INT8 English emotion model. A bounded public model download is verified by SHA256; chat text is held only in memory. Latest remote-speaker text supplies the spectrum, and the prior remote turn supplies a cautious direction proxy. Developer USB tools are available only in debug builds. Existing foundation/backend work remains intact.

Google Play Billing queries real localized products and restores ownership. Premium selection requires a cryptographically verified, expiring server claim. The purchase service checks Google's signed receipt and current purchase state, acknowledges non-consumables, and returns a signed claim. A dedicated deployment filter exposes only verification and health. Real prices and purchase actions stay disabled until the actual Play products and HTTPS verification configuration exist.

The owner chose to create a Play account and supplied public publisher details: Luv Tankha, third-year BTech student, luvtankha06@gmail.com, +91-8178185449. These are included in the policy/listing drafts. Free core features plus paid cosmetics and private phone inference are the initial revenue/cost strategy, not an optimized-revenue claim.

## 2. Files changed or added

- Android: consumer MainActivity, avatar catalog/ownership selection, general foreground overlay, on-device model/tokenizer, local analysis consent/pipeline, Google Play purchase client and signed entitlement verification, permissions/resources/build signing preflight.
- Backend: `dev.temper.cerebro.store` receipt/Google Publisher verification, acknowledgement, signed entitlements and isolated deployment boundary; purchase/boundary tests and store configuration.
- Release: `docs/release`, `deployment/store`, Docker ignore rules, model attribution, build/preflight, alignment/tokenizer and packaging scripts, README and this context/handoff.
- Dummy tests: 25 fictional chat styles, 105 independently generated tokenizer cases, native model/ownership/avatar/overlay checks. No private chat fixtures were created.

## 3. Verification and build results

- Android debug APK, debug instrumentation APK, unsigned release AAB and R8 release build completed. Debug and release lint had zero errors. Remaining warnings include localization/version/RTL items; the initial UI is English.
- Release signing/preflight was exercised with an isolated temporary test keystore and dummy HTTPS URL/public key. Signature and 16 KB zip alignment verification passed. That test-signed APK is deliberately excluded from the candidate package. Owner production signing is still required.
- Eight debug APK JNI libraries passed 16 KB ELF alignment checks; APK zip alignment passed.
- Latest 0.41.0 debug build was installed on the connected OnePlus 8T. The owner manually granted display-over-other-apps permission. Foreground overlay appearance was verified on TEMPER's own empty fictional screen, with an own-screen screenshot. No screenshot of another app was taken in this phase.
- The full backend Maven verification completed with 60 tests, zero failures/errors/skips, including the existing actual-model integration suite.

## 4. Tests run

- Seven Android unit tests: entitlement tampering/product/time guards, clamp behavior, model label mapping/direction, avatar products, 105 reference tokenizer cases, and actual desktop INT8 inference.
- Native full FoundationSmokeInstrumentation: placement/density/keyboard, verified adapter/roles/parser guards, privacy/consent/pause/revoke, graph and Keystore behavior.
- Native consumer checks: four avatars × eight distinct expressions, premium lock, 105 tokenizer cases, actual phone model, remote-speaker independence and 25 dummy conversations. Sixteen clear style fixtures agreed with expected labels; see `docs/release/ON-DEVICE-CHECK.md` for limitations.
- Native overlay-only check: user-granted permission, foreground service and visible companion on the own fictional test screen. Manual own-screen visual review and gallery review.
- Backend store tests: invalid/tampered/foreign receipts, pending/cancelled/refunded/consumed state, current-state restore, acknowledgement failure/retry, RSA claims, rejected legacy endpoints and bounded request bodies. Google authority was mocked for commerce tests; no real purchase was made. The filter's four-request concurrency cap is implemented but was not load-tested.

## 5. Known issues and release limits

- No Play developer/payments account, actual catalog, production verification deployment or owner signing key exists yet. Real purchases, restore/reinstall/second-device/refund flows remain untested.
- Automatic analysis supports the exact observed WhatsApp 2.26.37.73 portrait adapter. Other apps can display the general companion but have no automatic chat analysis. Sensitive screens can hide overlays. General placement is draggable; it does not have a verified universal keyboard anchor.
- Hindi-heavy, sarcasm and subtle/contextual emotion cases remain unreliable. No live-chat accuracy percentage is claimed. On-phone direction is smaller than the legacy backend engine.
- New phone inference was tested with generated normalized conversations; final end-to-end consumer consent/WhatsApp refresh remains pending. Initial network model download and broader Android/device testing also remain pending.
- Automatic approval review rejected the local HTTP purchase-verification smoke command without a stated reason. It was not retried or bypassed. Passing service/filter tests do not replace that smoke check or real Play license tests.
- The privacy draft needs final hosting providers, retention and public URL. Data safety, Accessibility/special-use service declarations and store review are pending. The candidate debug APK includes developer tools and is for testing only.

## 6. Next prerequisites and artifacts

Follow `docs/release/PLAY-CONSOLE-SETUP.md`, then configure products, service account, HTTPS deployment, entitlement keys and owner upload signing per `CONSUMER-RELEASE.md`. Complete all live purchase, first-download, consumer WhatsApp and required closed-testing gates before submission. Google approval is external.

`scripts/package-consumer-candidate.ps1` packages the tested debug APK, unsigned review AAB, backend JAR, committed source ZIP, release instructions, own-screen/gallery evidence and checksums. Models, private runtime files, test signing keys and credentials are excluded. Output: `dist/TEMPER-0.41.0-testing.apk`, `dist/TEMPER-0.41.0-UNSIGNED-review.aab`, and `dist/TEMPER-consumer-0.41.0.zip`. This is a tested candidate, not a published or customer-sale-ready deployment.
