# TEMPER native Android app

Current review version: **0.48.0**, package `dev.temper.android`, minimum Android 8/API 26, target/compile SDK 36. Native Java views/services; local ONNX inference; no backend required for normal use.

Read [SETUP-AND-DEMO.md](../docs/release/SETUP-AND-DEMO.md) for exact source-build commands, installation, first-use consent/model/permissions, testing and recovery. Read [FINAL-VERIFICATION.md](../docs/release/FINAL-VERIFICATION.md) for measured results and remaining limitations.

From the repository root, with JDK 21 and Android SDK configured:

```powershell
.\android\gradlew.bat -p android :app:clean :app:assembleDebug :app:assembleDebugAndroidTest :app:testDebugUnitTest :app:lintDebug :app:lintRelease :app:assembleRelease :app:bundleRelease --console=plain
adb install -r android\app\build\outputs\apk\debug\app-debug.apk
adb shell am start -n dev.temper.android/dev.temper.android.MainActivity
```

Debug is signed for development. Release enables R8 and resource shrinking; without owner-managed signing variables its APK/AAB are unsigned review artifacts. A locally development-signed release APK tests R8 runtime but is not a production upload.

The optional `-PtemperQa=true` build property uses `dev.temper.android.qa` for a fresh-install check without clearing the owner's app. Do not publish that package. The device instrumentation uses only generated fixtures and can disconnect Accessibility during test restarts; afterward switch Use TEMPER OFF and ON in Android Accessibility settings once. Do not use UIAutomator to inspect live Accessibility sessions.
