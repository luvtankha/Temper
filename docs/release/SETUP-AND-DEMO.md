# TEMPER 0.48.0 — build, install and evaluator demo

Normal analysis runs on the phone. No USB backend, account or feedback server is required. The five home prototypes are free; existing owned legacy avatars remain selectable. A first installation starts OFF.

## Source build

Use JDK 21 (verified), Android SDK platform 36 and build tools 35.0.0, with platform tools/ADB on PATH. AGP 8.13.2 requires JDK 17 or newer; only JDK 21 was exercised in this pass. These requirements match the [official AGP compatibility table](https://developer.android.com/build/releases/agp-8-13-0-release-notes). The checked-in wrapper pins Gradle 8.13 and its checksum. The first source build needs network access for dependencies and the pinned public model. Supply TEMPER_MODEL_FILE with an already verified local model to avoid the model network fetch. Set your own installed paths:

```powershell
$env:JAVA_HOME='C:\path\to\jdk-21'
$env:ANDROID_HOME='C:\path\to\Android\Sdk'
.\android\gradlew.bat -p android :app:clean :app:assembleDebug :app:assembleDebugAndroidTest :app:testDebugUnitTest :app:lintDebug :app:lintRelease :app:assembleRelease :app:bundleRelease --console=plain
adb install -r android\app\build\outputs\apk\debug\app-debug.apk
adb shell am start -n dev.temper.android/dev.temper.android.MainActivity
```

Run from the extracted repository root. Use `-r` to update a compatible signed installation; do not uninstall an existing installation just to switch signatures. A signature mismatch requires the matching signing key. Release builds enable R8/resource shrinking and are unsigned unless owner-managed signing environment variables are supplied. Never publish a development-signed APK or the isolated QA package.

For real-model JVM reference tests, place the exact pinned model outside source control and set `TEMPER_TEST_MODEL` to its absolute path. The model URL, 125,397,543-byte size and SHA-256 are pinned in `android/app/build.gradle`; verified weights are bundled in APK/AAB artifacts, but excluded from the source archive. The build task fetches and validates the pinned model or uses TEMPER_MODEL_FILE (TEMPER_TEST_MODEL is also accepted). Model-independent tests do not require that optional JVM model path.

## First use and everyday use

1. Open TEMPER and choose an avatar. Swipe or use the arrows; the selected avatar snaps to the center and is saved.
2. Tap ON. If setup is incomplete, read the automatic visible-chat disclosure, check the unchecked opt-in and tap Allow automatic analysis. Declining or going Back keeps TEMPER OFF.
3. Tap Prepare offline model once (about 126 MB; allow about 150 MB free space). The verified bundled model is copied and checked in private app storage without network or messages. A failed preparation can be retried. Keep setup visible until complete.
4. Allow Display over other apps. Enable Use TEMPER under Android Settings → Accessibility → Downloaded/Installed apps and accept Android's disclosure. Return to TEMPER. If Android shows enabled but not connected after an update/test restart, switch Use TEMPER OFF and ON once.
5. Allow notifications if you want the notification's OFF action. Notification permission does not replace overlay or Accessibility permission. Android can hide the notification from the drawer when permission is denied.
6. Return home and tap ON. Open a supported chat. Drag the avatar to move it; tap it for estimates; tap outside the panel to dismiss. Keyboard changes should keep analysis active. Tap OFF at home or in the visible notification to stop and remove the avatar.

Closing the running TEMPER task, force-stopping it, losing Accessibility or overlay permission, or a new process start leaves power OFF. After reopening, tap ON again. Reboot does not silently re-enable capture. Temporary unsupported screens clear old chat context; return to a supported chat to resume while power remains ON.

## Supported analysis

This candidate's adapter is pinned to **WhatsApp 2.26.37.73 / code 263707322**, package `com.whatsapp`. Use portrait, one-to-one, fully visible plain-text messages and a readable composer. It reads at most eight turns, at most 1,000 UTF-16 characters per turn. It excludes drafts and unseen history. Groups, media/document rows, reactions, quoted replies and unknown builds/layouts fail closed. The character can appear over ordinary unsupported apps, with analysis unavailable; their messages are not read. It hides on the configured system/permission/settings screens and while locked. This does not identify every potentially sensitive third-party screen.

One supported incoming message can produce emotion bars. Direction needs two incoming turns with preceding outgoing context and can remain uncertain. The graph order is Neutral, Happy, Concerned, Confused, Sad, Frustrated, Angry, Surprised. These are independent model probabilities, not shares that sum to 100%. The bars describe the latest visible incoming text; the short context/direction estimate also uses recent turns. They cannot establish feelings, intent, diagnosis or truthfulness. English and Roman-script Hinglish checks are synthetic; general accuracy is unvalidated.

## Evaluator demo and recovery

Prepare only a fictional chat, with both roles visible and no unsupported rows. Complete setup and offline model preparation ahead of the demo. Cold-open TEMPER, confirm OFF, select another avatar and tap ON. Open the fictional chat and tap the avatar. Confirm numeric bars, then receive a contrasting fictional plain-text message and confirm the bars change. Add a repair exchange and check that the direction remains an estimate. Open/close the keyboard, drag the avatar, dismiss/reopen analytics, and make a small scroll while keeping complete messages visible. Pull down the notification shade and dismiss it; no overlay should remain over the shade. Return home and tap OFF; no avatar or analysis should remain. Repeat ON once.

If estimates are unavailable: check the supported WhatsApp build/layout, incoming text, model preparation, permissions and current power. Return to a supported fictional chat and allow a few seconds. If Accessibility is disconnected after installation or instrumentation, rebind Use TEMPER OFF/ON. If setup/model fails, use its explicit retry; corrupt public weights are discarded to allow preparation again; unavailable states must never show invented scores. OnePlus battery restrictions can stop background services; permit normal foreground-service operation for a demo rather than expecting auto-restart after force-stop.

## Developer device tests

```powershell
adb install -r android\app\build\outputs\apk\androidTest\debug\app-debug-androidTest.apk
adb shell am instrument -w -e carouselOnly true dev.temper.android.test/dev.temper.android.FoundationSmokeInstrumentation
adb shell am instrument -w -e powerOnly true dev.temper.android.test/dev.temper.android.FoundationSmokeInstrumentation
adb shell am instrument -w -e consumerOnly true dev.temper.android.test/dev.temper.android.FoundationSmokeInstrumentation
adb shell am instrument -w -e overlayOnly true dev.temper.android.test/dev.temper.android.FoundationSmokeInstrumentation
adb shell am instrument -w -e complexOnly true dev.temper.android.test/dev.temper.android.FoundationSmokeInstrumentation
adb shell am instrument -w -e performanceOnly true dev.temper.android.test/dev.temper.android.FoundationSmokeInstrumentation
adb shell am instrument -w dev.temper.android.test/dev.temper.android.FoundationSmokeInstrumentation
```

Require an explicit PASS in the output: ADB can return exit code zero after instrumentation failure. These lanes use generated data, not live host messages. Consumer/overlay/performance lanes need the model installed; overlay needs the previously user-granted overlay permission. Tests can disconnect Accessibility, so perform them before the final human live-chat check. UIAutomator must not be used to inspect an active Accessibility session. A separate `-PtemperQa=true` debug/test build and `freshOnly` lane verify fresh defaults in `dev.temper.android.qa` without clearing the owner's primary app.

The QA `modelRecoveryOnly` lane requires the exact public model test asset and exercises offline bundled preparation and corrupt-file restoration. The optional QA `permissionOnly` helper requires prepared weights, initial overlay permission and an external driver permitted to revoke its overlay AppOp. This phone denied that shell operation; the helper is **not passed evidence** and does not test Accessibility re-grant. Use the manual permission-loss/recovery check described in the final report instead. Restore a normal build after QA (`:app:assembleDebug :app:assembleDebugAndroidTest` without `-PtemperQa=true`) before packaging; never install or publish QA as the primary product.

## Publishing and privacy

Review artifacts are for device evaluation. Production signing, Play publisher account/declarations, production endpoints and store purchase tests remain owner release gates. Existing optional reviewed/rated feedback requires separate consent and explicit submission to a configured HTTPS service. There is no background collection or automatic model replacement from normal conversations. Read ANALYSIS-FEEDBACK.md before configuring it, and delete submitted feedback before uninstalling if a deletion key exists. The debug-only legacy USB mode is not part of the release app's normal analysis path.
