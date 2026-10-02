# TEMPER — Android companion app 0.46.0 (release candidate)

Consumer flow: complete the guided one-time consent/model/Android permissions, then tap **ON** and use supported chats normally. Tap **OFF** to remove the companion and stop analysis. There is no per-chat selection or timer in this mode. The home screen has one power button plus small companion/settings actions. Alex is free; Nova, Orbit and Luma are one-time Play purchases with eight expressions each. Normal analysis stays on-device and never uploads chat text. Optional [rated conversation feedback](docs/release/ANALYSIS-FEEDBACK.md) still has separate permission, selected-session boundaries and a manual reviewed-text submission. Collection remains disabled until an HTTPS service is configured. The emotion-bar weights are unchanged; 0.46.0 adds a trained fictional-data context head for changing conflict, repair and withdrawal. [Context evaluation and measured runtime improvements](docs/release/CONTEXT-AND-PERFORMANCE.md) document its limits.

The app, Billing integration, purchase-verification backend and release tooling are implemented. Real sales/public publishing still require owner Play products, production HTTPS verification, signing credentials, publisher details and successful store/device tests. Premium purchases fail closed until configured. [Consumer setup and release gates](docs/release/CONSUMER-RELEASE.md), [privacy draft](docs/release/PRIVACY-POLICY.md), [store listing](docs/release/STORE-LISTING.md), [phase 41 evidence](docs/phases/PHASE-41-HANDOFF.md).

The floating character works across ordinary apps; automatic analysis remains restricted to verified adapters. Universal automatic chat reading, Hinglish accuracy and store approval are not claimed. The former USB demo below is retained as developer history and an optional debug tool.

TEMPER places a small original2D character above the WhatsApp composer initially. Drag it to either side or another comfortable place; its chosen position is remembered. Tap it for Current state, Direction and one eight-emotion bar graph. Closing the keyboard or analytics panel keeps analysis running while ON. Switching chats clears old results before analyzing the new verified visible chat. Temporarily unreadable layouts show no scores and recover automatically. Other apps show the companion without automatic estimates. OFF, notification OFF or closing TEMPER's task removes the companion and stops capture; cold process starts default OFF. Estimates cannot establish someone's feelings.

The Android app above is the primary product. Phases00–19 and working backend/models are preserved; React messaging and the USB demo below are developer test harnesses. Roadmap: [AGENT.md](AGENT.md). USB setup, fictional script and troubleshooting: [Android USB demo guide](docs/demo/ANDROID-USB-DEMO.md).

## Supported USB demo (developer history)

- Verified OnePlus8T KB2001, Android14/API34, portrait.
- Exact personal WhatsApp2.26.37.73/versionCode263707322/packagecom.whatsapp.
- Selected fictional one-to-one chat; at least3 complete visible text turns and both roles; latest8 supported turns, max1000 characters each.
- Fully visible media/documents, quotes, reactions, groups, landscape and unrecognized layouts/builds are unavailable. Edge-clipped rows excluded; date/call metadata are not message text.
- Windows computer with Java21, five prepared local model directories and authorized USB/ADB. No public deployment or standalone phone inference.

The real model graph, scrolling refresh, keyboard movement and popup input usability were confirmed on the test phone in the earlier demo. Version 0.44.0 keeps the selection during temporary keyboard/layout gaps and waits for supported text instead of requiring a restart. Leaving WhatsApp, switching to another supported chat, explicit pause/stop, revocation or service shutdown still ends capture. Android service rebind can be needed after APK updates/tests.

Phase40 fixes cross-speaker contamination in the live spectrum. [Dummy English/Hinglish results and model limits](docs/demo/EMOTIONAL-SPECTRUM-CHECK.md) cover25 generated endpoint checks and the reproducible evaluation script. Hindi-heavy Roman Hindi and sarcasm remain unreliable; dummy agreement is not measured live-human accuracy.

## Run

Install dist/TEMPER-0.39.0.apk after packaging, or android/app/build/outputs/apk/debug/app-debug.apk after a source build. Connect/authorize USB debugging. PowerShell7:

```powershell
./scripts/start-overlay-backend.ps1 -JavaHome 'PATH_TO_JAVA21' -ModelsRoot 'PATH_TO_PREPARED_MODELS'
./scripts/configure-overlay-usb.ps1 -Serial 'USB_DEVICE_SERIAL' -AdbPath 'PATH_TO_ADB_EXE'
```

Phone: Accessibility and consent → read/save inspection opt-in → enable TEMPER in Android Accessibility settings → Resume TEMPER. Live analysis → read/save separate processing opt-in → Start selected fictional chat → return to that chat within one minute. Device account is optional/local only. Configuration does not consent or capture.

Tap the character and allow inference to finish. Leaving/opening TEMPER ends the selected session. Clear inspection data and pause removes probes/files/context. Remove USB connection and pause also removes the phone token/revokes live processing.

## Build and verify

External prerequisites: accepted Android SDK terms, platform36/build-tools35.0.0/platform-tools, Java17+ for Android and Java21 backend.

```powershell
$env:JAVA_HOME = 'PATH_TO_JDK'
$env:ANDROID_HOME = 'PATH_TO_ANDROID_SDK'
./android/gradlew.bat -p android assembleDebug assembleDebugAndroidTest lint --console=plain
```

From backend, run `./mvnw.cmd verify`. All48 tests pass with five TEMPER_*_MODEL_DIR variables set to prepared foundation/sentiment/emotion/sarcasm/toxicity artifacts. Model scripts/cards: scripts/ and docs/model-cards/. Missing model artifacts cause explicit test skips and do not prove real inference.

Install app and test APKs, run `adb -s SERIAL shell am instrument -w dev.temper.android.test/dev.temper.android.FoundationSmokeInstrumentation`. Add `-e transportOnly true` before the runner for actual USB inference using generated text. Tests preserve original private configuration/consent but can disconnect Android's service.

After checks pass, `./scripts/package-overlay-demo.ps1` creates separate APK/ZIP and SHA256 sums. Models, tokens, logs, metadata and local settings are excluded. Model licenses/limits accompany the bundle.

Architecture: exact-build structural plan → selected identity check → bounded stable visible turns → stateless local models/conflict/trajectory → character plus compact panel. Backups disabled; no raw chat persistence/normal logs; pause invalidates pending results. Already-sent requests can finish in bounded memory. See [privacy](docs/architecture/android-privacy.md), [pipeline](docs/architecture/live-overlay-pipeline.md), [adapter extension](docs/architecture/adapter-extension.md) and phase20–39 handoffs.

Historical web development instructions: [legacy-web-development.md](docs/architecture/legacy-web-development.md). They record earlier foundation stages, not current Android product status.
