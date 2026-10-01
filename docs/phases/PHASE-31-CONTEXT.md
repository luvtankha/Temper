# Phase31 — Compact character popup
1. Objective: tapping the compact character opens a small dismissible analytics box.
2. Repository: Phase30 implemented, device tests and visual gallery verified; Phase29 real overlay/keyboard/input gate complete.
3. Completed: historical00–19 preserved; updated20–30 complete.
4. Builds/tests: Android APK/test/lint and physical-device suite PASS. Backend/debug harness unchanged.
5. Architecture: native accessibility overlay, shared eight-state CharacterView; no network/live analysis yet. Conservative composer anchor independent of message availability.
6. Risks: touch interception, popup covering composer, stale popup on departure, font scaling and insufficient space.
7. Scope: character touch target, compact current-state/direction text and exactly one eight-emotion graph, safe placement/dismissal/lifecycle, own-app preview and device checks.
8. Non-goals: real analysis/server connection32, trajectory33, additional graphs, dashboards or host automation.
9. Acceptance: character tap opens popup, graph/text readable, outside/second tap dismisses, host input usable, popup follows/hides with overlay; build/tests plus phone gate verified.
