# Context and performance — 0.46.0

October 2, 2026. This update changes the phone's direction classifier and runtime.
It does **not** establish live emotional accuracy or retrain the large emotion-bar
model. All development conversations were fictional; no private chat was read,
uploaded or used for training.

## What the app now uses

A small trained logistic-regression head considers the current incoming turn,
the previous incoming turn, intervening outgoing replies, earlier visible incoming
language and changes between turns. It estimates increasing conflict, softening,
possible withdrawal, unresolved concerns or no clear interpersonal escalation.
It can distinguish an ambiguous identical reply in different contexts. Its
uncertainty threshold remains separate from the availability of the eight bars.

The bars retain the pinned 28-label INT8 GoEmotions model's existing eight-label
mapping for the latest incoming message. Local-speaker text cannot change those
bars. The new head changes the short explanation/direction and character, not
the underlying emotion scores. A single verified incoming message can already
produce bars; contextual direction needs at least two incoming turns and preceding
outgoing context. This corrects older documentation that described three messages
as an unconditional phone-inference gate.

## Training and evaluation limits

The pilot uses 38 training conversations and 14 validation conversations, each
five turns, in English and Roman-script Hinglish. New domains and four paired
counterfactual scenarios form a separate 28-conversation holdout (14 per language).
Scenarios cover blame, sarcasm, repeated conflict, rejected apologies, residual
hurt after repair, external stress, boundaries, pauses and disengagement. No
ratings were treated as emotion labels.

The first candidate failed: settings were selected before applying the deployed
abstention rule, causing excessive uncertainty. Its rejected report is preserved.
Settings selection was corrected using validation predictions with abstention.
The original test set was excluded and a fresh holdout was authored before the
second run. The resulting head agreed with 28/28 authored holdout labels; the old
two-message tension proxy agreed with 6/28. Macro-F1 across the five named classes
was 1.0 versus 0.075 on that small set. Both languages improved with no class
recall regression in this pilot.

**These are synthetic regression results, not “100% model accuracy.”** One author
created the examples, features and labels; vocabulary overlaps across scenarios.
The second holdout was authored after seeing the first aggregate failure, so it
is not an independent external benchmark. Novel phrasing, subtle sarcasm,
Hindi-heavy text, quoted speech and hidden history remain important weaknesses.
Independent human-reviewed conversation evaluation is still required before
claiming general accuracy or fine-tuning/replacing the large model. The separate
customer-feedback promotion gates were not relaxed.

Training is reproducible with `scripts/context-training-requirements.txt` and
`scripts/train-context-model.py --model <pinned-int8.onnx> --output <directory>`.
Only `--install` with passing pilot gates writes the Android context asset.
`models/context` contains frozen source data, feature definitions, the rejected
report and the current report. Java feature vectors/probabilities are checked
against Python on all 80 current examples. The context head is bound to the base
model hash; a future approved base replacement falls back to the existing summary
until the context head has been evaluated against it.

## Runtime changes

- Pending Accessibility work has a leading deadline: repeated events can bring
  work forward, but cannot keep postponing it. Ordinary content waits at most
  150 ms and scroll events initially at most 300 ms before the scheduled probe,
  excluding Android main-thread scheduling and layout-read retries.
- Outgoing text after the target incoming turn no longer cancels its inference.
  Earlier context changes and a new incoming turn still invalidate stale results.
- Up to eight hashed text keys and eight-float score vectors are cached in memory
  within one conversation. No raw-text cache or disk cache is added. Changing
  identity/departure clears derived context; old in-flight results cannot publish.
- Public weights can warm when ON is ready. A brief departure retains only model
  assets for up to 60 seconds; repeated departure events do not extend that
  deadline. OFF/revocation/service destruction release the model. No warm-up reads
  chat text. Input snapshots and queued work remain bounded.
- Adapter child lookup is one pass with sibling indexes rather than scanning the
  entire node list for every row. Planning costs O(N + R log R), down from O(N²),
  using O(N) temporary indexes; N remains capped at 256 and retained turns at 8.
- Byte-pair encoding uses linked token indexes and a priority queue rather than
  rebuilding/concatenating string lists for each merge. For a word of B bytes and
  V merge entries, lookup/merging is O(B(log B + log V)), with O(B) temporary space.
  The model still caps input at 128 tokens. Transformer attention complexity is
  unchanged; this is not a claim that the entire application is linear-time.
- An 800,924-byte compiled tokenizer table replaces runtime JSON/string-map
  construction. The original public tables remain available for reproducibility.
  All 113 independent token-ID references pass, including long and Unicode input.

## Controlled phone measurements

OnePlus 8T, Android 14/API 34, same generated five-turn conversation and two CPU
inference threads. The frozen Phase 45 engine and final engine run in separate
instrumentation processes. Cold time includes hash verification, tokenizer/model
loading and both initial remote predictions. Repeated timing is the median of
seven identical-window calls; new-message timing is the median of eight different
incoming updates sharing their preceding context. Memory is measured after those
updates and the same explicit GC/finalization/settling procedure in both runs.

| Measurement | Frozen Phase 45 | Phase 46 |
| --- | ---: | ---: |
| Cold analysis | 896.1 ms | 661.5 ms |
| Unchanged-window median | 50.9 ms | 2.25 ms |
| New incoming message median | 50.8 ms | 25.6 ms |
| Retained Java heap | 11,243,264 bytes | 3,352,992 bytes |
| Settled process PSS | 209,311 KB | 202,065 KB |
| Native allocated memory | 159,494,384 bytes | 159,388,128 bytes |

These are single-device observations, not an end-to-end UI latency guarantee.
The model still dominates native memory. The first optimization attempt increased
memory/startup cost and was replaced with compact tables before delivery. Earlier
uncontrolled samples differed, so only the controlled paired comparison supports
the memory claim. No benchmark measures unseen private chats.

The actual phone additionally checks all 28 held-out conversations, four appended
repair transitions, first-message bars and explicit-stop rejection. ARM INT8
scores differ from desktop ONNX scores (up to about 0.09 in this set); deployment
direction checks therefore run on the phone too. The optimization must preserve
the old engine's spectrum on the same phone, independently of that platform drift.

Universal chat-app support, live accuracy, purchase deployment and a public
feedback service are not established by this work. Production reading remains
restricted to verified WhatsApp layouts and the user's explicit consent.
