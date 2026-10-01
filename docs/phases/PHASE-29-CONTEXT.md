# Updated Phase29 — Grounded overlay shell
1. Objective: compact native overlay grounded at the lower-left composer edge, stable in portrait with keyboard changes.
2. Repo: phases20–28 complete; fd8f11f conservative adapter; actual report AVAILABLE/eight turns/one local/seven remote/repeat suppressed/composer verified.
3. Completed previous phases: preserved00–19; updated core20–23; Android/account/consent24–26; adapter contract27 and actual WhatsApp28.
4. Builds/tests: app/test/lint and complete actual-device fixture/consent/account suite pass; real host parse passes. Backend45/45 and legacy web22/build/browser11 preserved.
5. Architecture: native TYPE_ACCESSIBILITY_OVERLAY owned by the consent-gated service. A pure geometry layer determines compact bounds; only structural observations update the composer, no fresh message reads after the explicitly armed capture.
6. Risks: hide immediately on app/screen change, ambiguity, pause/revoke or service destruction; avoid stale participant state. Receiving unrelated package metadata solely to hide must never traverse unrelated nodes. Android activation after test/update remains manual.
7. Exact scope:64×88dp placeholder character, tiny ground shadow, composer-relative portrait geometry, nonfocusable/nonmodal window, keyboard anchor refresh and lifecycle cleanup; geometry/device tests and real visual usability gate.
8. Non-goals: eight expressions30, popup31, live text capture/backend32, new host actions, full-screen panels or drawing over unsupported screens.
9. Acceptance: meaningful bounds/keyboard/lifecycle checks pass; actual overlay visible near selected test composer, host input remains usable with keyboard open/closed, hide/pause verified. Do not count fixtures as real visual acceptance.
