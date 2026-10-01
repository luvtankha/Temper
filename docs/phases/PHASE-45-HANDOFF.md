# Phase 45 — One-button ON/OFF experience

## 1. What changed

TEMPER opens to one large ON/OFF button with two secondary actions: Choose companion and Settings. ON starts the draggable foreground companion and automatic on-phone processing of currently visible supported chats. OFF removes both companions, pauses capture, invalidates the current generation and rejects pending results. Detailed permissions, model information, optional ratings and developer tools live in Settings.

First use guides the user through the new automatic-visible-chat disclosure, the existing offline model download, display-over-apps permission and Android Accessibility. Existing selected-chat consent does not authorize automatic chat switching. Saving the new consent leaves TEMPER OFF. Once setup is ready, activation requires one tap and no per-chat selection, one-minute timer or manual resume.

Automatic mode selects the verified header before reading message fields and requires a matching second observation. A chat switch invalidates the previous generation and resets the analysis before the new text is processed. Leaving a supported chat or locking the phone clears capture while ON remains available for supported re-entry. Temporary keyboard/layout changes recover automatically. Automatic mode never creates optional rated-feedback sessions; explicitly selected developer/feedback sessions retain their separate boundaries.

Home/notification OFF, permission revocation, foreground companion loss and task removal stop processing. Cold process startup resets stale ON without erasing consent, model or Android permissions. Rapid OFF→ON waits asynchronously, at most five seconds, for the old service to stop. A distinct explicit-stop sequence cancels pending ON even if power was already OFF; normal old-service destruction does not cancel a later activation. Stale buttons labeled OFF remain stop actions. Rated-session start rechecks ON and all required permissions at click time.

## 2. Files changed or added

- MainActivity, new TemperApplication, manifest and version 0.45.0/code 45.
- New PowerStore and AutomaticCapturePolicy; AnalysisConsent/ConsentStore, FloatingOverlayService and Accessibility state/service/pipeline changes.
- New automatic-gate JVM test, PowerUiChecks and PowerOverlayChecks; extended live-state checks and updated native runner/navigation.
- README, release/privacy/store drafts, architecture notes, Phase 45 context/handoff and consumer packaging.

## 3. Verification and build results

Final debug APK, instrumentation APK and R8 unsigned review AAB build successfully. **29 Android JVM tests passed, zero failures/errors/skips**, including actual desktop INT8 inference. Debug/release lint have zero errors and 33/32 warnings. Eight native libraries pass 16 KB ELF alignment; APK ZIP alignment passes. The final default 0.45.0 APK is installed on the OnePlus 8T, Android 14/API 34.

The model is unchanged: SHA256 `0c1981c5b479674747911c8e2228f0c4ec90bf47bf66e830f7d4fc62be082958`. Default purchase and feedback origins remain empty. Backend/model/purchase implementations were unchanged; Phase 43's 78 backend tests remain the baseline and were not rerun for these Android changes.

## 4. Tests run

- JVM automatic eligibility checks every combination of power, explicit automatic consent, installed model and live foreground owner.
- Native foundation checks pass, including automatic header generations, selected-session priority, departure/re-entry, consent migration, compact home, persisted OFF and existing structural/privacy/Keystore fixtures.
- Native power UI checks pass for unchecked disclosure, backing out, consent saving without activation, stale OFF, disabled pending-start UI, timeout, explicit stop and destruction cancellation. The final stop-sequence regression verifies that an explicit STOP while already OFF cancels pending ON before activation.
- Native consumer checks pass: 25 generated English/Hinglish conversations with the actual phone model, independent tokenizer references, remote-speaker invariance, four avatars × eight expressions, purchase gates and existing suspended-worker recovery.
- Native overlay checks pass: real foreground companion over TEMPER's own dummy screen; actual neutral→angry model bars across automatic conversation generations; no feedback session; actual home OFF; foreground notification STOP route and cancellation sequence; suppression of late callbacks. Existing drag/tap/cancel/popup/keyboard-position/recreation checks also pass.

The first new overlay fixture waited for a new MainActivity when Android reused the existing root. Own-process thread stacks identified the fixture wait, with main/model worker idle. The fixture now finishes its dummy activity and waits for the existing home to regain focus. It passed unaided after correction and again with the final stop-sequence code. All text was generated; no host root, private chat screenshot or Settings automation was used. Temporary fixture preferences were restored.

## 5. Known issues and limits

Instrumentation disconnects Accessibility, so ON consent/power in the actual-overlay fixture is controlled test setup. Real foreground service, JNI model work, home OFF and notification STOP are exercised; a fresh user-driven automatic WhatsApp consent→ON→chat-switch check remains a release validation step. Drag verification uses application-overlay windows over TEMPER's dummy screen rather than production Accessibility-overlay windows.

Automatic estimates remain limited to verified WhatsApp 2.26.37.73 portrait layouts, at least three complete text turns with both roles visible. Other apps can show the companion without estimates. Unsupported content fails closed. Existing English/Hinglish/sarcasm/direction limitations remain; generated regression agreement is not measured live-human accuracy. The public model's first-install network download remains a release check. No data collection or model upgrade is claimed.

Real Play products, signing, purchase license tests, privacy hosting and an actual feedback server/domain remain pending. The testing APK and unsigned review AAB are review artifacts, not a published production release.

## 6. Next prerequisites and artifacts

Open TEMPER and tap ON. The guided page shows the next missing setup step; existing selected consent needs the new affirmative automatic-analysis opt-in. If Android disconnected its Accessibility service during update/testing, the guide explains how to enable or rebind Use TEMPER once. Complete setup, turn ON and use a fictional supported chat. Switching supported chats should refresh without rearming; OFF should remove the companion immediately.

Candidate outputs: `dist/TEMPER-0.45.0-testing.apk`, `dist/TEMPER-0.45.0-UNSIGNED-review.aab`, `dist/TEMPER-consumer-0.45.0.zip` and distribution/archive checksums. The package includes committed source, Phase 45 documentation and a screenshot captured only after the installed TEMPER home was confirmed focused. Historical Phase 41 visual/model evidence remains labeled and the Phase 42 synthetic training smoke remains non-promotable. Weights, private runtime data and keys are excluded.
