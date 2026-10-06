# TEMPER

An Android app that places a movable 2D avatar over chat apps and estimates the emotional tone and direction of supported conversations. Select an avatar, turn TEMPER **ON**, and tap the avatar to see a compact emotional-spectrum panel. Turn it **OFF** to stop analysis and remove the overlay.

**Current candidate:** 0.48.0 · **Package:** `dev.temper.android` · **Minimum Android:** 8.0 / API 26

Normal analysis runs on the phone using ONNX Runtime. A computer, backend, and TEMPER account are not required.

## Features

- Swipeable avatar carousel with saved selection and five free home avatars.
- One ON/OFF control after first-use consent and Android permission setup.
- Draggable avatar, keyboard-aware placement, and a dismissible analysis panel.
- Eight emotion estimates: Neutral, Happy, Concerned, Confused, Sad, Frustrated, Angry, and Surprised.
- Short current-state and conversation-direction estimates using recent visible turns.
- Bundled, checksum-verified model with offline first-use preparation.

## Supported conversations

Automatic analysis currently supports the verified **WhatsApp 2.26.37.73** layout, version code `263707322`, in portrait one-to-one chats with fully visible plain-text messages. The adapter reads up to eight visible turns, with a maximum of 1,000 UTF-16 characters per turn. Drafts and unseen history are excluded.

Groups, quoted replies, reactions, media/document rows, and unknown app versions or layouts are unsupported. Other apps can display the movable avatar, but their messages are not analyzed. Unsupported content shows an unavailable or waiting state.

Emotion bars describe the latest fully visible incoming message. They are independent model probabilities and do not need to sum to 100%. Direction additionally needs two incoming turns with preceding outgoing context and can remain uncertain. English and Roman-script Hinglish have fictional regression fixtures; real-world accuracy has not been established. Estimates cannot establish a person's feelings or intent.

## Build and install

Use JDK 21, Android SDK platform 36, Build Tools 35.0.0, and Android platform tools. The project pins Gradle 8.13 and Android Gradle Plugin 8.13.2. Configure `JAVA_HOME` and `ANDROID_HOME` for your own installed paths, or set `sdk.dir` in an untracked `android/local.properties` file.

From the repository root on Windows:

```powershell
.\android\gradlew.bat -p android :app:assembleDebug
adb install -r android\app\build\outputs\apk\debug\app-debug.apk
adb shell am start -n dev.temper.android/dev.temper.android.MainActivity
```

On Linux/macOS, use `bash android/gradlew -p android :app:assembleDebug`. The first source build downloads dependencies and the pinned public model. To use an existing verified model instead of downloading its weights, set `TEMPER_MODEL_FILE` to its absolute path before building. The task validates its size and SHA-256; the pin is in [android/app/build.gradle](android/app/build.gradle).

Weights are bundled into APK/AAB builds and excluded from Git. Debug APKs use a development signature. Updating an existing installation requires a compatible signature; use its matching key rather than uninstalling to resolve a mismatch.

## Start analysis

1. Open TEMPER and choose an avatar.
2. Tap ON and complete the first-use setup: read the visible-chat disclosure, opt in, and prepare the offline model. Preparation copies about 126 MB into private app storage; allow about 150 MB of free space.
3. Grant **Display over other apps** and enable **Use TEMPER** in Android Accessibility settings. Return to TEMPER and confirm the service is connected. Notifications are optional and enable the notification's OFF action.
4. Tap ON, open a supported chat, and tap the avatar for analysis. Drag the avatar to reposition it. Tap outside the panel to dismiss it.
5. Tap OFF in TEMPER or its visible notification to stop and remove the avatar.

Closing TEMPER's running task, force-stopping it, losing required permissions, or starting a new process leaves it OFF. Reopen and tap ON to resume. Temporary unsupported screens clear old conversation context; analysis can resume when a supported chat returns while TEMPER remains ON.

If analysis is unavailable, check the supported WhatsApp version/layout, visible incoming text, prepared model, consent, permissions, and ON state. After installation or instrumentation, Android may show Accessibility enabled while disconnected; switch Use TEMPER OFF and ON once, then return to the app. Failed offline preparation offers an explicit retry.

## Repository layout

| Directory | Purpose |
| --- | --- |
| `android/` | Primary Android app, Accessibility adapter, overlay, local inference, and tests |
| `backend/` | Optional Java 21 / Spring Boot analysis, feedback, and purchase services |
| `frontend/` | Legacy React/Vite development and demonstration harness |
| `models/` | Model metadata, fictional context datasets, and evaluation results |
| `scripts/` | Model tooling, verification, release builds, and packaging |
| `deployment/` | Optional feedback and purchase-service deployment configuration |
| `contracts/` | OpenAPI service contract |
| `design/` | Avatar artwork, design tokens, and carousel previews |

The backend and web harness are optional development tools. They are not needed for normal on-phone inference.

```powershell
# Optional backend, from backend/
.\mvnw.cmd spring-boot:run

# Optional web harness, from frontend/; Node.js 22.12+ required
npm ci
npm run dev
```

The backend defaults to port 8080; the web harness defaults to port 5173. Environment configuration is in [.env.example](.env.example) and [application.yml](backend/src/main/resources/application.yml). Model download/export tools are in `scripts/`; optional Python dependencies are specified by the tracked requirements files.

## Verification

From the repository root:

```powershell
.\android\gradlew.bat -p android :app:testDebugUnitTest :app:lintDebug :app:lintRelease :app:assembleDebugAndroidTest
```

For JVM tests that run the real classifier, set `TEMPER_TEST_MODEL` to the verified model's absolute path. Device instrumentation uses generated fictional fixtures; its optional lanes include `carouselOnly`, `powerOnly`, `overlayOnly`, `complexOnly`, and `performanceOnly`.

```powershell
adb install -r android\app\build\outputs\apk\androidTest\debug\app-debug-androidTest.apk
adb shell am instrument -w -e carouselOnly true dev.temper.android.test/dev.temper.android.FoundationSmokeInstrumentation
```

Require an explicit PASS in the instrumentation output. Some lanes need prepared weights and user-granted overlay permission. Instrumentation may disconnect Accessibility; rebind it before checking a fictional live chat.

Recorded verification for 0.48.0 includes clean debug/release builds, a release AAB, 44 JVM tests with no failures or skips, and lint with zero errors. The retained [verification evidence](docs/release/evidence/phase48.json) records the remaining release blockers. Final live incoming-message updates, manual permission-loss recovery, and the complete repeated demo still require confirmation for this candidate. Earlier fixture results do not establish live-chat accuracy.

## Privacy and optional feedback

Normal analysis requires explicit consent and Accessibility permission. It keeps a bounded visible conversation window in phone memory and does not upload, save, or log that text. TEMPER does not send messages or operate chat controls. OFF stops capture; results from a request already running are discarded after stopping.

Optional feedback is separate and disabled without a configured HTTPS service. Its flow requires separate consent, confirmation of permission from everyone whose messages are included, review of redacted visible text, a 1–5 quality rating, and an explicit submission. Users are not asked to label emotions. A feedback session is limited to 64 visible turns and 32 estimates within ten minutes. Redaction is incomplete; review or discard identifying text before sending. Withdraw and delete controls are provided; delete submissions before uninstalling, which removes the local deletion key.

The optional server encrypts submitted records and applies 90-day expiry. Deploy it with private persistent storage, TLS, abuse controls, and working deletion/expiry checks. Operator exports and backups need deletion propagation. Ratings and model outputs are not ground-truth labels; model improvement requires independent review and evaluation. Normal conversations do not silently retrain or replace the model.

## Release status

This repository contains a development/review candidate. Production publishing still needs an owner-managed upload key, a Google Play developer account and declarations, a public privacy policy, final device acceptance, and any configured production services.

Release builds enable R8 and resource shrinking. Without signing configuration, release APK/AAB artifacts are unsigned. Premium avatar purchase infrastructure exists, but sales remain disabled without real Play products and HTTPS receipt verification. The five home avatars are free.

Review packages label the debuggable testing APK, the minified development-signed R8 device-review APK, and the unsigned review AAB separately. These are evaluation artifacts. Extract the included source ZIP before using source-relative links or building from a packaged copy.

For configured production builds, use [build-consumer-release.ps1](scripts/build-consumer-release.ps1). It requires `TEMPER_KEYSTORE_FILE`, `TEMPER_KEYSTORE_PASSWORD`, `TEMPER_KEYSTORE_ALIAS`, and `TEMPER_KEYSTORE_KEY_PASSWORD`, plus the production store origin and entitlement public key. Keep private keys, credentials, generated APKs/AABs, model weights, and user data out of Git. Nothing in this repository constitutes a published Play release.

## Model attribution

The Android classifier uses the pinned [SamLowe RoBERTa GoEmotions ONNX model](https://huggingface.co/SamLowe/roberta-base-go_emotions-onnx), with unchanged INT8 emotion weights. Its MIT terms and tokenizer provenance are retained in [NOTICE.txt](android/app/src/main/assets/emotion/NOTICE.txt). The direction head is a small pilot trained on fictional context examples; its evaluation is in [models/context/evaluation.json](models/context/evaluation.json).

The optional sentiment tooling retains CardiffNLP's CC BY 4.0 [attribution](models/sentiment/ATTRIBUTION.md). Download tools preserve the other upstream model notices. These notices describe their respective third-party assets.

Developed by **Luv Tankha**. Support and privacy contact: **luvtankha06@gmail.com**.
