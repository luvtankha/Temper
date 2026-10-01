# Updated Phase26 — Accessibility and consent
1. Objective: informed opt-in, Android AccessibilityService skeleton, WhatsApp package detection and pause/disable controls.
2. Repo state: phases20–25 complete, account commit8dfc7d7; Android foundation and encrypted session tested on OnePlus8T Android14/API34.
3. Completed previous phases: historical00–19 preserved; pivot20, conflict21, trajectory22, compact mapping23, Android24 and local identity25.
4. Builds/tests: Android app/test/lint pass; actual-device foundation/account suite PASS; Java45/45 and legacy frontend22/build/browser11 results preserved.
5. Architecture: consent policy gates a native service limited to com.whatsapp events. This phase detects package only; adapters, node traversal, network and overlays remain later phases.
6. Risks: Android accessibility activation requires the user's manual system permission. Consent must be independent of that permission and default paused. Revocation/pause must immediately remove detection state. Never read event text or traverse nodes here.
7. Exact scope: disclosure, unchecked opt-in, persistent versioned consent, system settings shortcut, supported-package gate, detection metadata only, pause/revoke/disable, own-app/device tests.
8. Non-goals: host clicks/sends, raw-chat extraction/logging, background scraping, adapter parsing, network transfer, actual overlay drawing.
9. Acceptance: build/lint and device consent-gate tests pass; user manually enables/disables service; observed WhatsApp package detection only after consent and resume. If permission is pending, record pending gate rather than claiming completion.
