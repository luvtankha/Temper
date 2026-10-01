# On-device emotional-spectrum check — 0.41.0

Date: October 1, 2026. Device: OnePlus 8T, Android 14/API 34. All text was generated fiction. These checks obtained no Accessibility root, private chat or contact data.

The Android CPU runtime executed the pinned INT8 RoBERTa GoEmotions model on 25 dummy conversations. All eight clear English examples and eight mixed English/Hinglish examples produced the intended dominant spectrum label. These examples explicitly name emotions and are small regression fixtures, not a held-out accuracy benchmark or a measurement of live-chat accuracy. The same remote text produced bit-identical bars when local-speaker text changed between neutral, angry and happy.

| Clear style | English top bar | Mixed Hinglish top bar |
| --- | --- | --- |
| Happy | 51.3% | 65.9% |
| Angry | 84.4% | 83.0% |
| Sad | 93.0% | 91.1% |
| Concerned | 78.1% | 34.2% |
| Confused | 94.6% | 93.8% |
| Frustrated | 68.4% | 66.6% |
| Surprised | 88.7% | 86.3% |
| Neutral | 96.1% | 58.0% |

Bar values are independent model scores after label mapping. They need not sum to 100% and are not calibrated probabilities of a person's actual emotion. A dominant label can still have weak evidence; for example the mixed-Hinglish concerned fixture has only a 34.2% top bar. Caring alone is excluded from the concerned signal so reassurance is not treated as distress.

Review cases reveal real limitations. The sarcastic example's top mapped label was happiness at only 14.6%; the app's weak-evidence threshold does not present this as strong happiness. Four Hindi-heavy romanized fixtures all had neutral as their top mapped label, including angry (91.9%) and happy (94.2%) text. This is an English model and must not be advertised as reliable Hindi/Hinglish inference. Negated anger produced weak neutral evidence (16.7%).

Direction compares the latest two remote turns using a conservative tension proxy. It can return uncertain while bars remain available. It does not reproduce all legacy backend trajectory, sarcasm, sentiment and toxicity models. The mixed-Hinglish concern fixture was interpreted as softening because its latest modeled distress score fell; this illustrates why direction remains an estimate rather than verified intent.

Additional checks passed: 105 tokenization cases matched independent Hugging Face reference IDs on Android; all four avatars rendered eight distinct expressions; unowned premium selection was rejected. The complete structural adapter/consent/pause/keyboard test suite also passed. The previously verified live WhatsApp flow used the older USB engine; this phase tested the new phone engine with dummy normalized conversations. A full consumer consent → verified WhatsApp → on-device refresh check remains a release gate.

Model SHA256: `0c1981c5b479674747911c8e2228f0c4ec90bf47bf66e830f7d4fc62be082958`. The phone test model was seeded over USB after the same hash was verified; a first-install network model download remains to be exercised. No weights or raw phone data are included in the release archive.

## 0.44.0 continuity and movement check

October 2, 2026, same OnePlus 8T. Twenty-eight JVM tests and native foundation/consumer checks passed. The actual phone worker rejected results after suspension/explicit stop and resumed numeric estimates for identical generated text without rearming. The 25 generated model cases and tokenizer/speaker controls also passed with unchanged weights; these do not measure live-human accuracy.

The production analysis-overlay manager was exercised over TEMPER's own fictional screen using the user-granted application-overlay permission. Generated touch events moved the companion, distinguished tap/drag/cancel, preserved position through simulated keyboard changes/analysis updates, fitted the popup below a top-positioned companion and restored placement after recreation. This test used application-overlay windows rather than production Accessibility-overlay windows, and did not obtain a host root. A fresh user-driven WhatsApp keyboard/drag check remains a release validation step. See [Phase 44 handoff](../phases/PHASE-44-HANDOFF.md).
