# Android primary client

Native Java, minAPI26, target/compileAPI36. Pinned AGP8.13.2/Gradle8.13/Java17+. Onboarding, settings, fictional preview, encrypted local account and consent-gated AccessibilityService are implemented. Default pause=true, backup disabled. Current service observes WhatsApp package metadata only; no chat text or network transmission. Actual overlay arrives later.

SDK use requires the user's acceptance of https://developer.android.com/studio/terms . Install platform36, build-tools35.0.0 and platform-tools after acceptance; set ANDROID_HOME or ignored local.properties sdk.dir. Set JAVA_HOME, run gradlew.bat assembleDebug lint. Install via adb install -r app/build/outputs/apk/debug/app-debug.apk, launch adb shell am start -n dev.temper.android/.MainActivity. Record device/API; verify onboarding, pause persistence after restart and clear settings.

SDK installation authorized by user on2026-10-01. Local SDK: C:/Users/Admin/.codex/cache/temper-tools/android-sdk. Gradle assembleDebug/lint pass; signed debug APK produced. Install/run verified on OnePlus8T KB2001 Android14/API34. Phase24 acceptance complete. This is not yet a functioning overlay.

Device smoke: build assembleDebugAndroidTest, install app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk, run adb shell am instrument -w dev.temper.android.test/dev.temper.android.FoundationSmokeInstrumentation. PASS must verify onboarding/preview, paused default, persistence and clear. Instrumentation executed on the authorized USB phone: PASS onboarding/preview, paused default, persistence and clear. Emulator driver was not installed.
