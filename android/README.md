# TEMPER Android0.39.0

Native Java primary client: small grounded2D character, eight expressions/260ms transitions, one compact analytics graph, exact-build WhatsApp adapter, separate inspection/USB consent, pause/cleanup and encrypted optional device account. Debug transport is fixed127.0.0.1:8080 over ADB USB forwarding to local Java/model backend; release cleartext disabled.

Verified OnePlus8T KB2001 Android14/API34 with WhatsApp2.26.37.73/version263707322, portrait supported plain text. MinSDK26/compile-target36; other device/API/host versions unverified. [Demo guide](../docs/demo/ANDROID-USB-DEMO.md).

With accepted SDK terms, Java17+, platform36/build-tools35.0.0/platform-tools and JAVA_HOME/ANDROID_HOME, run `./gradlew.bat assembleDebug assembleDebugAndroidTest lint --console=plain`. APK: app/build/outputs/apk/debug/app-debug.apk. Install via adb install -r; launch dev.temper.android/.MainActivity.

Instrumentation runner dev.temper.android.test/dev.temper.android.FoundationSmokeInstrumentation covers consent/account, observed structural fixtures/roles/clipping, bounds/grounding/expressions, popup/chart/status replacement, registry/session identity/privacy cleanup. `-e transportOnly true` tests configured real USB backend with generated text; `-e adapterOnly true` runs parser regressions. Tests preserve original configuration but may require Android service off/on rebind afterward.

No host control clicking/sending, raw chat storage, content-description/draft reads or additional supported apps. No continuous idle animation; small expression transitions provide motion. Scores are independent estimates, not required to sum to one.
