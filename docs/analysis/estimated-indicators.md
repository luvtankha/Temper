# Estimated English language indicators v1

These deterministic heuristics are inspectable engineering indicators, not trained probabilities or conclusions about intention. Enable TEMPER_INDICATORS_ENABLED (default true). False retains fixture values for these channels, useful for the explicitly fictional model-free demo and regression tests. Other configured models operate independently.

| Indicator | Recognized literal phrases |
|---|---|
| Passive aggression | whatever you say; if you say so; must be nice; fine, do whatever; thanks for nothing |
| Blame | your fault; you caused; you always; you never; because of you; you are to blame |
| Defensiveness | not my fault; i did nothing wrong; don't blame me; do not blame me; i was only trying; i'm just saying |
| Disagreement | i disagree; i don't agree; i do not agree; that's not true; that is not true; you are wrong; you're wrong; no, that's wrong |
| Withdrawal | i'm done talking; i am done talking; leave me alone; i won't discuss; i will not discuss; end of discussion; i don't want to talk; i do not want to talk |
| Repeated disagreement | Current disagreement cue and at least one earlier disagreement cue from the same speaker in the bounded causal window |

Case, curly apostrophes and whitespace normalize before literal word-boundary matching. Double-quoted spans are excluded; `not CUE` and `isn't CUE` suppress that cue. This limited negation handling cannot parse all English grammar. Unquoted reported speech, irony, literal requests for space and benign generalizations can still match; other languages and implicit statements can be missed. No match means no recognized cue, not absence of the behavior.

For the first five indicators, no distinct cue yields0; otherwise min(0.85,0.55+0.15*(distinct cue count−1)). Repetition requires both current and prior same-speaker matches; otherwise0. When present its score is min(0.85,0.55+0.15*(matching prior turns−1)). Constants are deliberately bounded heuristic strengths, not calibrated confidence. Each result exposes matched cues or earlier sequences and the formula in HEURISTIC evidence. Future turns cannot participate; only previous3–5 turns supplied by the validated context service count.

Passive aggression, blame and defensiveness replace their existing fixture fields. Disagreement, withdrawal and repeatedDisagreement are additive bounded fields visible separately in the inspector and unchanged in conversation analytics. Conflict remains a fixture until Phase20; these channels are not hidden in it.
