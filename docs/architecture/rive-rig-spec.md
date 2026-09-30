# TEMPER Rive rig authoring contract

Phase 04 is verified. Both original rigs run in the React canvas runtime; the phase handoff records actual evidence.

## Files and artboards

| Variant | Runtime file | Artboard | State machine |
|---|---|---|---|
| Male (Alex) | `frontend/public/avatars/male.riv` | `TemperMale` | `TemperEmotion` |
| Female (Nova) | `frontend/public/avatars/female.riv` | `TemperFemale` | `TemperEmotion` |

Original vector previews next to the runtime files can be imported as an authoring starting point. For higher-detail art matching the supplied stylized references, author original layered paths/meshes while preserving facial and body articulation. PNG sheets are reference only. Do not alternate flat PNG frames.

## Numeric inputs

`anger`, `sadness`, `happiness`, `frustration`, `confusion`, `concern`, `surprise`, `arousal`, `sarcasm` range **0–1**; `valence` ranges **−1–1**. All ten are required exactly once with numeric type. Neutral uses all emotion channels zero, valence zero, arousal 0.15. These are semantic API units; the renderer multiplies by 100 for Rive percentage axes (signed valence −100–100).

Use separate skeleton/path groups for brows, upper/lower eyelids, pupils, mouth shape, head/neck, shoulders/torso and arms/hands. Share one rig per variant across all eight emotions. Keep hands near the lower artboard edge so the character rests above the composer.

## Eight expressions

| Expression | Facial change | Head/posture change |
|---|---|---|
| Neutral | Relaxed brows, open eyes, subtle resting smile | Relaxed shoulders |
| Happy | Smile curve, cheeks raised, relaxed/smiling eyes | Lifted head and open posture |
| Concerned | Inner brows raised, mouth slightly downturned | Small head tilt, forward shoulders |
| Confused | Asymmetric brows, searching gaze | Side tilt and one raised hand |
| Sad | Drooping brows/eyelids, downturned mouth | Lowered head and shoulders |
| Frustrated | Brow tension, narrower eyelids, tightened mouth | Tense shoulders, hands rise |
| Angry | Stronger inward brows, clenched mouth | Forward lean, tighter torso and fists |
| Surprised | Raised brows, wide eyes, open mouth | Recoil, lifted hands |

Author continuous blend layers and reversible transition timing: facial channels about 250–400ms, head 300–500ms, body 500–800ms; combined settling 700–1200ms. A single threshold jump or image replacement is insufficient. Arbitration must keep simultaneous channels visually coherent (e.g. concern → frustration → anger) and allow returning smoothly to neutral. Do not require backend frames.

`expressionTarget()` provides labeled engineering test targets, **not predictions**. Backend integration and semantic state selection are later phases.

## Runtime and export

The official [Rive CLI](https://rive.app/docs/cli/getting-started) builds local unscripted runtime files without sign-in. Browser signing requirements apply to scripts; these rigs contain no scripts. The initial editor plan/access blocker is cleared. Source and generator live in the repository.

The React runtime uses `useRive`, local bundled WASM and numeric input validation. Rive’s [React API](https://rive.app/docs/runtimes/react/parameters-and-return-values) says state-machine inputs still work but are deprecated for future major versions. Numeric inputs are used here to honor the requested contract; pin the lockfile and migrate internally to data binding if a future runtime removes them, without changing semantic controls.

Default runtime paths are `/avatars/male.riv` and `/avatars/female.riv`. Optional `VITE_MALE_RIG_URL` and `VITE_FEMALE_RIG_URL` in `frontend/.env.local` override them. Root `.env.example` documents the keys. Runtime is lazy-loaded. Wrong rigs show explicit errors.

## Required acceptance evidence

1. Load both exported files in the actual React canvas runtime.
2. Verify both artboards/state machines and every numeric input.
3. Drive each of eight test targets and return to neutral.
4. Capture visible face, head and posture changes; check smooth transitions and reversal.
5. Check concern → frustration → anger and back; test concurrent channels and intensity extremes.
6. Re-run swapped participant, composer containment, chat and responsive tests.
7. Record runtime version, export checksum, ownership/license and test outcomes in the phase handoff.

Unit tests against input-shaped objects establish mapping/validation only; they cannot certify genuine Rive playback or smooth visible animation.
