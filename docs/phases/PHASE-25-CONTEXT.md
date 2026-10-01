# Updated Phase25 — Minimal device account/session
1. Objective: create account, sign in/out, current session and persistent local session with minimal support UI.
2. Repo: updated20–24 verified; device OnePlus8T Android14/API34, actual foundation smoke PASS; Java45/45, legacy web22/build and11 browser regressions preserved.
3. Completed: historical00–19, overlay pivot/conflict/trajectory/mapping and real Android foundation install/run; source and handoffs reviewed.
4. Build: app/test APKs compile, lint passes; no capture permissions yet.
5. Architecture: device-only demo identity using Android Keystore encrypted local storage, PBKDF2 salted password verification, minimal native screens; backend account/cloud sync optional and out of scope.
6. Risks: clearly disclose local-only account, not server authorization; never claim backend endpoints secured. Avoid passwords in preferences/logs and perform KDF/storage away from main thread.
7. Scope: bounded account creation/login/logout, encrypted persistent session/profile identifier, meaningful actual-device crypto/session tests with isolated test storage, truthful UI/status, setup docs.
8. Non-goals: social profiles, remote account server/email recovery/cloud sync, Accessibility26, adapter/capture/overlay later.
9. Acceptance: actual create/login/logout/session persistence and wrong-password checks pass; secure encrypted preference inspection on device, prior smoke/build/lint maintained.
