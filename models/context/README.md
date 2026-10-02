# Fictional conversation-context experiment

All conversations in this directory are newly authored, fictional English or
Roman-script Hinglish examples. No user conversation or rating is used. Labels
are engineering hypotheses, not independent human annotations. This is a small
regression/training pilot, not a measurement of real-world emotional accuracy.

`scenarios.txt` records the original scenario-level train/validation/test assignments before
training. Original test rows are retained for transparency and excluded from v2;
`holdout-v2.txt` supplies its new test set. The failed v1 report is preserved.
This is scenario-separated synthetic data, not independent annotation. Each line is `id|split|language|direction|REMOTE|LOCAL|REMOTE|LOCAL|REMOTE`.
Paired scenario variants stay in the same split; scenario domains differ across splits. Do not move examples between splits
or tune on test failures. The training script records hashes, per-language and
per-class results, baselines, and individual test errors. Validation selects
regularization; test is reporting/promotion only. A failed gate preserves the
previous asset. Five direction labels describe observable language, never a
person's true feelings or intent.

The trained linear context head uses bounded, role-separated language cues and
the existing frozen RoBERTa emotion scores. It does not replace or fine-tune
RoBERTa. Eight emotion bars remain that model's independent scores for the latest
incoming message. In particular, improved direction classification does not
establish improved Hinglish emotion-bar accuracy. Quoted text and negated cues
need counterexamples; missing or weak evidence must remain uncertain.
