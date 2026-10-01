# Overlay conflict aggregation v1
New additive conflictAnalysis accompanies every analyzed REST turn/snapshot turn. Existing conflict and summary fields remain unchanged for legacy clients; the overlay consumes conflictAnalysis, not historical fixture conflict.

Raw score = .25 negativeSentiment + .20 anger + .20 toxicity + .15 sarcasm + .10 blame + .10 defensiveness. TEMPER_CONFLICT_WEIGHTS may replace all six weights; nonnegative, finite and sum1 required. topContributors contains all six, descending contribution with deterministic signal-name ties, signal value, configured weight, contribution and MODEL/HEURISTIC/MOCK source. It is an engineering index, not calibrated risk or intent.

Smoothed score = (.6 current raw + .3 sequence-1 raw + .1 sequence-2 raw) / sum of weights whose positions have analyzed data. Missing positions are not substituted by older messages or zero. TEMPER_CONFLICT_SMOOTHING must supply three nonnegative finite weights summing1, first positive. Trend compares current smoothing to immediately preceding analyzed position's smoothing; absent predecessor → FLAT. Default threshold .08, configurable TEMPER_CONFLICT_TREND_THRESHOLD: delta≥threshold UP; delta≤-threshold DOWN; otherwise FLAT. Only earlier sequence positions enter calculation; future turns excluded.

fixtureInputs flags any weighted current/smoothing input that is still MOCK. Results using incomplete/unconfigured model inputs are unsuitable for live overlay claims; later mapping must fail closed. Previous fixtures remain testable. No new training/calibration claim.
