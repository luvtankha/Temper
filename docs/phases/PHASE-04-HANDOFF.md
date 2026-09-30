# Phase 04 — BLOCKED / INCOMPLETE checkpoint

**This is a resumable checkpoint, not a completion handoff. Do not advance to Phase 05 yet.**

## Implemented independently of editor access

- Typed ten-channel semantic control contract; safe finite normalization/clamping including signed valence; eight manual authoring targets; neutral reset.
- All-or-nothing runtime validation: missing, duplicate or wrong-type inputs reject before any mutation.
- Optional Rive React 4.36.0 / Canvas 2.44.0 renderer behind the existing swapped RemoteAvatar component. Local bundled main/fallback WASM; lazy-loaded renderer; explicit load/incompatibility errors. State machine `TemperEmotion`; artboards `TemperMale` / `TemperFemale`.
- Development-only `/avatar-lab`: inspect manual expression/intensity controls and participant swap; explicitly warns when no rig exists. Production route is excluded.
- `check:rigs` checks actual exports and reports SHA256; documents that header validation cannot prove animation.
- Original vector source/provenance and complete rig authoring/acceptance specification.
- Visual compatibility fix: increased avatar layer height so original hair/head is not cropped at desktop or phone sizes. Existing composer containment checks pass.
- Compatibility fixes: sidebar identity follows participant preview; media-query changes update open insights panel dialog semantics and focus when the window resizes.

## Added / changed

Added avatar `controls.ts`, tests, rigConfig, RiveCharacter, AvatarLab; `frontend/scripts/check-rigs.mjs`; browser authoring-harness test; rig specification; phase context/checkpoint. Changed RemoteAvatar, App routing, Shell, styles, browser chat/layout tests, package/lock, root environment example, README and architecture overview. No backend/API/schema/migration changes.

## Evidence

- `npm run build`: passes with dedicated lazy Rive chunk and locally emitted WASM files.
- `npm test`: 12/12 pass. Numeric rig input tests are mocks; they prove validation/mapping only.
- `npm run test:e2e -- --workers=2`: 27/27 pass; ten responsive viewport sizes, live resize-to-dialog focus/semantics, sidebar participant identity and all previous chat/placement/proxy integration regressions included.
- Java 21 `mvnw.cmd verify`: previously passed 2/2; backend unchanged. Running Java service returns UP directly and through Vite.
- `npm audit`: zero vulnerabilities after Rive dependency installation.
- `npm run check:rigs`: **exit 1**, accurately reports missing male and female `.riv` exports. This is an expected blocked phase acceptance gate, not a hidden passing result.
- Desktop 1440×900 and mobile 390×844 screenshots visually inspected; hair/head now complete, composer visible. Captures are ignored local test outputs.

## Blocker

- **Root cause:** Neither original `.riv` character export was supplied; available source attachments are flat PNG sheets. Rive editor navigates to a login form in the available in-app browser, so character authoring is unavailable without the user’s sign-in. Runtime export requires suitable existing plan access per official export docs.
- **Attempted fixes:** Created original importable vector sources; checked tools for a Rive authoring capability (none available); attempted browser editor, enabled accessibility, verified login redirect; read official runtime and export docs; completed code/spec/test work independent of authoring.
- **Safe state:** Phases 00–03 remain runnable; current main chat uses clearly labeled neutral vector previews. Optional renderer will only load when configured. No PNG emotion swapping, CSS animation substitute, licensed-unclear downloaded character, account creation, purchase or public deployment performed.
- **Required:** User sign-in to an accessible Rive editor with runtime export capability, or existing original exported male/female rigs/project. Ask the user to sign in, never request passwords in chat. Pending request was presented while independent work continued.

## Run / configuration / next steps

README development and test commands remain valid. Set `VITE_MALE_RIG_URL=/avatars/male.riv` and `VITE_FEMALE_RIG_URL=/avatars/female.riv` in `frontend/.env.local` after original exports exist. Restart Vite. Open `/avatar-lab`; check every target and both participant views, then return to neutral.

Continue **Phase 04** after access arrives: author both rigs against `docs/architecture/rive-rig-spec.md`, record ownership/source/license and checksums, verify actual canvas playback and visible reversible face/head/body transitions, run regressions, update this handoff to COMPLETE only after evidence. Phase 05 and all later phases remain unstarted.

Rollback can restore the Phase 03 commit; there is no database migration or private conversation data to recover.
