# Emotional spectrum check — 1 October 2026

Used 25 generated one-to-one conversations through the running authenticated `/api/overlay/analyze` endpoint, with the real local sentiment, GoEmotions, sarcasm and toxicity models. These are the same endpoint and response fields used by the Android overlay. No phone chat content was inspected for this pass. This is a regression/smoke check, not a human-labelled accuracy benchmark.

## Fixed speaker attribution

The old overlay classifier input concatenated the current message and preceding messages from both participants. Consequently, the same neutral REMOTE message, "The meeting starts at ten.", changed from neutral to angry or happy solely because of LOCAL text.

| LOCAL history | Old remote anger | Old remote happiness | Fixed remote neutral | Fixed remote anger | Fixed remote happiness |
|---|---:|---:|---:|---:|---:|
| Neutral | 0.1% | 0.1% | 95.6% | 0.1% | 0.1% |
| Angry | 44.1% | 0.2% | 95.6% | 0.1% | 0.1% |
| Happy | 0.2% | 32.2% | 95.6% | 0.1% | 0.1% |

The overlay now classifies each speaker's current text separately. Conversation indicators/conflict/direction still retain causal history, and the spectrum still selects the latest REMOTE turn. The legacy web analysis entry point retains its documented history-based behavior. Backend regression tests using native ONNX inference compare every returned spectrum value with the directly classified REMOTE text and check exact equality across the three LOCAL-tone controls.

## Clear dummy styles after the fix

Each row's intended dominant emotion agreed with the returned dominant bar in both generated examples. Percentages below are model outputs, not measured accuracy or probabilities of a person's feelings. The eight independent bars need not sum to 100%.

| Dummy style | English dominant bar | Mixed Hinglish dominant bar |
|---|---:|---:|
| Happy | Happy 52.2% | Happy 62.4% |
| Angry | Angry 84.2% | Angry 82.7% |
| Sad | Sad 92.6% | Sad 88.7% |
| Concerned | Concerned 78.7% | Concerned 28.5% |
| Confused | Confused 93.5% | Confused 93.2% |
| Frustrated | Frustrated 69.5% | Frustrated 68.5% |
| Surprised | Surprised 88.6% | Surprised 86.3% |
| Neutral | Neutral 96.0% | Neutral 63.1% |

The mixed Hinglish examples contain explicit English emotion cues alongside Roman Hindi. Eight such examples passing does not establish general Hinglish support.

## Model limitations reproduced

- Sarcastic "Wonderful. More pointless work. Lucky me." gave Happy 13.2%; sarcasm is a separate classifier signal and is not one of the eight bars. The emotion model does not reliably resolve sarcastic polarity.
- "No worries. I am not upset or angry at all." kept Anger low at 2.3% but Concern reached 40.6%. Concern is the documented max(caring, fear, nervousness) proxy, so reassuring/caring language can also raise it.
- Hindi-heavy Roman Hindi failed to express the intended emotional category reliably. Angry wording produced Neutral 76.4%, happy wording Neutral 93.6%, sad wording had only Sad 0.2%, and confused wording had Confused 5.8%. These cases remain outside validated support; no score correction or fake translated confidence was inserted.

The [model author identifies the model as English and trained on Reddit GoEmotions](https://huggingface.co/SamLowe/roberta-base-go_emotions). The [dataset authors describe English Reddit comments](https://research.google/blog/goemotions-a-dataset-for-fine-grained-emotion-classification/). These sources provide no validation for this app's live Hinglish conversations. A labelled English/Hinglish evaluation set and a suitable multilingual model or separately validated translation approach would be needed to establish broader accuracy.

## Verification and rerun

- Full backend `mvnw.cmd verify`: 50 tests, zero failures/errors/skips with all five model directories configured.
- New actual-model regressions cover all 25 spectra, eight values in protocol order, finite 0–1 bounds, direct REMOTE-model fidelity and LOCAL-tone independence.
- Actual HTTP dummy run: all 25 available; all 16 clear English/mixed-Hinglish dominant-label checks passed; all three control spectra identical.
- Android chart/protocol are unchanged. This pass did not repeat phone layout, keyboard or accessibility tests; their earlier evidence is in Phase38/39.

With the configured local backend running, execute:

```powershell
./scripts/evaluate-overlay-spectrum.ps1
```

The script reads only the checked-in generated fixture and saves aggregate results under ignored `temp/overlay-spectrum-results.json`. It never reads a phone, accepts private chat input or prints the connection token. Fixture: `backend/src/test/resources/overlay-chat-styles.json`. Actual-model test: `backend/src/test/java/dev/temper/cerebro/LiveSpectrumIntegrationTest.java`.
