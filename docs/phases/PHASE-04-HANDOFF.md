# Phase 04 — COMPLETE
Original male/female Rive character rigs now load and animate in TEMPER.

## Implementation
Original SVG vectors were converted to editable RML cubic paths and gradients, with articulated brow, eye, mouth, head, upper-body and arm/hand groups. Each artboard has one TemperEmotion state machine, ten numeric inputs, eight poses and three independent layers. Continuous intensity blends between neutral and expression poses. Face/head/posture transitions take 300/400/700ms. Priority (anger, frustration, surprise, sadness, concern, confusion, happiness) makes overlapping channels deterministic. Every state has reversible transitions.

Semantic controls remain 0–1, signed valence −1–1. The adapter multiplies by 100 for Rive's percentage axes. Valence/arousal/sarcasm are exposed for later presence/context mapping; emotional pose channels drive the current layers. No inference is claimed.

## Files / architecture
Added assets/avatars/temper source project, frontend/scripts/build-rig-source.mjs and two public .riv files. Both files contain the same two-artboard resource; each avatar selects its own artboard. Changed rig defaults, validated input mapping, authoring lab, tests, README and architecture documents. Existing lazy runtime, locally bundled WASM, swapped participant resolution and explicit load errors are preserved. No API, backend, database or migrations changed.

## Genuine evidence
- Rive CLI 1.2.0 verify: zero errors/warnings; inspect: no problems, two artboards, three layers, eight poses per layer and ten numeric inputs per rig.
- Each export: 37,307 bytes; SHA256 00cd823c7f9003b9eec20f3cbed427bb22b6f62862b70b7d36bec5709d452fb4.
- React production build passes; 12 unit tests pass.
- Full browser regression: 28/28 pass. Strengthened two real canvas tests then pass again (32.4s): visible character pixels, eight distinct poses, intermediate intensity, exact neutral reversal, in-flight transition frame, overlapping input arbitration, and concern → frustration → anger → neutral for both rigs.
- Male/female captures visually inspected. Capture issue isolated: runtime suspends offscreen rendering; tests now resume before capture and reject blank images.
- Previous Java build/test: 2/2 pass; backend unchanged; live proxy health regression passes.
- Dependency audit after generator/test packages: zero vulnerabilities.

## Cleared blocker / provenance
The earlier editor-login blocker was premature. Official Rive CLI getting-started documentation permits local builds without an account; unsigned files containing no scripts are unaffected by browser signing restrictions. This project contains no scripts. CLI release manifest hash was verified before extracting to a user-local cache. No account, purchase, publication or upload was required.
Sources: https://rive.app/docs/cli/getting-started and https://rive.app/docs/cli/overview.
All character geometry is original repository-authored vector art, inspired by the supplied references; no proprietary avatar asset or raster expression swap.

## Run / rebuild / limits
Normal README run commands work with default /avatars/male.riv and /avatars/female.riv. Optional VITE_MALE_RIG_URL / VITE_FEMALE_RIG_URL override these paths. Development /avatar-lab supplies explicit engineering targets; it does not infer emotions.
Rebuild: npm --prefix frontend ci; node frontend/scripts/build-rig-source.mjs; rive assets/avatars/temper --verify; rive inspect assets/avatars/temper --summary; rive assets/avatars/temper --once; copy build/temper.riv to the two public filenames. CLI is not committed. Root build ignore excludes generated source build outputs; public runtime assets are intentional deliverables.
The source-project AGENTS.md caution against web preview concerns unsigned scripts. The user's explicit React runtime acceptance requirement applies here, and these rigs contain no scripts.
Idle/typing motion is Phase 05; live analysis and transport remain later phases.

