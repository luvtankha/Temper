# TEMPER0.39.0 — Windows/Android USB demo

## Files and prerequisites

ZIP: TEMPER.apk, Spring Boot backend under backend/target/, setup/model preparation scripts, model cards and privacy/support docs. Java21, Android platform-tools and five verified model artifact sets are external. No tokens/logs/chat metadata/weights bundled. Use PowerShell7. Accept applicable Android SDK terms before installation; source Android builds additionally need platform36/build-tools35.0.0.

Prepare folders foundation-distilbert, sentiment-roberta, emotion-goemotions, sarcasm-bert and toxicity-bert below one ModelsRoot. Read docs/model-cards/ for pinned licenses, limits and complete artifact requirements. The included download/export scripts create verified artifacts. Sentiment has CC BY4.0 attribution included; other checkpoints' licenses are in their cards. Sentiment/sarcasm/toxicity export needs an isolated Python environment using scripts/model-export-requirements.txt. Runtime inference is local Java. Existing prepared cache on this machine: C:/Users/Admin/.codex/cache/temper-models.

If artifacts are not already prepared, create an isolated Python environment, install the pinned requirements, then run these commands from the extracted bundle. Downloads/export require network access and disk space for the original checkpoints and ONNX outputs. Historical model cards describe earlier test-harness stages; the overlay pipeline uses the integrated models described in the pipeline document.

```powershell
$models = 'PATH_TO_MODELS_ROOT'
$python = 'PATH_TO_EXPORT_ENVIRONMENT/python.exe'
& $python -m pip install -r ./scripts/model-export-requirements.txt
./scripts/download-foundation.ps1 -Destination "$models/foundation-distilbert"
./scripts/download-emotion.ps1 -Destination "$models/emotion-goemotions"
./scripts/download-sentiment.ps1 -Destination "$models/sentiment-roberta" -Python $python
./scripts/download-sarcasm.ps1 -Destination "$models/sarcasm-bert" -Python $python
./scripts/download-toxicity.ps1 -Destination "$models/toxicity-bert" -Python $python
```

Supported: personal WhatsApp2.26.37.73/code263707322 on OnePlus8T KB2001 Android14/API34, portrait. Other builds/Business/groups/apps/landscape are unavailable. Require3+ complete visible text turns with both roles, use latest8/max1000 characters each. Edge-clipped rows excluded; date/call summaries not text turns. Fully visible media, documents, quotes/reactions and unrecognized layouts stop analysis. This is a constrained developer demo.

## Computer setup

Extract ZIP and enter its folder. Connect USB, enable developer USB debugging and approve that computer's prompt. Use the exact USB serial from adb devices:

```powershell
$adb = 'PATH_TO_PLATFORM_TOOLS/adb.exe'
& $adb devices
& $adb -s 'SERIAL' install -r ./TEMPER.apk
./scripts/start-overlay-backend.ps1 -JavaHome 'PATH_TO_JAVA21' -ModelsRoot 'PATH_TO_MODELS_ROOT'
Invoke-RestMethod http://127.0.0.1:8080/actuator/health
./scripts/configure-overlay-usb.ps1 -Serial 'SERIAL' -AdbPath $adb
```

Wait for health UP. Startup loads models; check temp/overlay-backend.log and temp/overlay-backend-error.log if needed. Launcher defaults to this machine's .codex/cache tool/model paths, binds127.0.0.1:8080, generates/reuses private random token under temp/ and runs a copy of the JAR to avoid locking source build output. It refuses to replace another port8080 listener. Stop only your own displayed PID before restarting/when finished.

Configuration sends token through stdin straight into TEMPER's private file and restores adb reverse tcp:8080. It does not consent, resume or capture. Rerun after USB/ADB restart. A different package folder/token needs that folder's configuration copied to the phone. Keep temp/ private.

For source checkout, APK path is android/app/build/outputs/apk/debug/app-debug.apk. Package verified builds with scripts/package-overlay-demo.ps1.

## Phone setup and use

1. TEMPER → Accessibility and consent: read disclosure, tick initially unchecked inspection opt-in, Save consent.
2. Android Accessibility → Downloaded/Installed apps → TEMPER → Use TEMPER. Approve Android prompt, return and Resume TEMPER.
3. If enabled but disconnected after install/tests, turn Use TEMPER off and on through normal Android UI. Live start requires a connected service.
4. Live analysis (USB demo): read/save separate processing opt-in, Start selected fictional chat, return to that fictional WhatsApp chat within one minute. Show supported plain text with both roles.
5. Tap grounded character above bottom-left composer. Panel has Current state, Direction and one graph. Analyzing shows no scores until inference finishes. Scores reflect remote language; direction uses recent conversation evidence.
6. Continue/scroll manually. Keyboard moves the anchor; outside tap dismisses panel. TEMPER never clicks host controls/sends messages.
7. Leaving/opening TEMPER ends session; explicitly start for the next selected chat. Pause/revoke stop new reads/requests; already-sent requests may finish in memory.

Optional device account is local only; it neither creates a cloud account nor secures backend endpoints.

## Fictional one-to-one script

Use a dedicated willing test participant. Create exchanges manually in a fictional test chat. Nothing is sent automatically. Keep3–8 complete plain-text turns and both roles visible.

| Role | Positive generated example |
|---|---|
| LOCAL | How did your fictional project go? |
| REMOTE | I am happy and excited. It was a wonderful success! |
| LOCAL | Congratulations, I am glad it went well. |
| REMOTE | Thank you. I feel proud of the result and grateful for your help. |

| Role | Tense generated example |
|---|---|
| LOCAL | Can we discuss our fictional plan? |
| REMOTE | I am angry and frustrated. You keep ignoring what I say. |
| LOCAL | I disagree with that. I think we should review what happened. |
| REMOTE | This is annoying. You never listen to me. |

No exact label/percentage is promised. Remote spectrum and direction may differ because they summarize different evidence. Scores do not prove feelings; models/lexical indicators are not validated for every language/dialogue style.

Dummy spectrum checks and the Phase40 speaker-attribution fix are documented in docs/demo/EMOTIONAL-SPECTRUM-CHECK.md. English/mixed-Hinglish smoke examples passed; Hindi-heavy Roman Hindi and sarcasm remain unreliable. Run scripts/evaluate-overlay-spectrum.ps1 against the configured local backend to reproduce the generated examples. The Android APK remains0.39.0; this update changes its local backend.

## Troubleshooting and cleanup

- No character: check inspection consent, Resume, connected service, exact build and Start selected fictional chat. Selection arm expires after one minute.
- Computer connection unavailable: check USB debugging/authorization, health UP and rerun configure-overlay-usb.ps1.
- Analyzing: allow up to45-second network timeout on slow computers. Pending work is bounded; obsolete results ignored.
- Insufficient supported evidence: show3+ complete turns/both roles and a recent remote turn. Missing models/fixture-derived input return unavailable, never fake bars.
- Chat layout changing: short grace period retries after motion, with no scores during invalid layout.
- Unsupported chat layout: choose supported text area and explicitly restart session in TEMPER. Unknown media/layout/build cannot be guessed.
- Keyboard/scroll changes visible context; missing enough turns or unsupported content can stop the session.
- Settings → Clear inspection data and pause removes probes/context/layout/parser files. Remove USB connection and pause additionally removes phone token/revokes live consent. Account remains; sign out separately. Android app-data deletion/uninstall removes all app-local data.

See privacy/pipeline docs and phase38/39 evidence. This is a signed debug USB demo, not a production store release; release cleartext remains disabled.
