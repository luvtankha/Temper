# Updated Phase28 — WhatsApp adapter
1. Objective: normalize a real visible WhatsApp conversation, roles, composer and repeated-read identity; fail closed on uncertainty.
2. Repo state: phases20–27 complete; adapter contract commit585248d. OnePlus8T KB2001/API34, WhatsApp2.26.37.73/code263707322 installed.
3. Completed: core/models preserved; revised analysis/mapping/native account/consent/package detection and pure adapter contract verified.
4. Builds/tests: Android build/lint and actual-device contract/consent/account/foundation PASS. Real WhatsApp package event confirmed by user.
5. Architecture: first observe a user-armed fictional chat's structural resource IDs/bounds without any text. Use actual build evidence to define the parser. Version2 disclosure covers selected test-chat inspection and future bounded local text parsing, not server transmission; no automatic background capture.
6. Risks: WhatsApp accessibility IDs/layout are not a public stable API; cannot invent a parser from assumptions. User must provide a fictional test chat with both local/remote messages and explicit opt-in. Unknown layouts/roles must be unavailable.
7. Exact scope: bounded one-shot structural calibration, documented observed build, conservative adapter, normalized maximum8 visible turns/1000chars each, composer and dedup, tests and actual-device validation. No raw structural text export.
8. Non-goals: host actions, screenshots/private chat dumps, network transfer/live pipeline, persistent message history, group/multi-participant guesses, other apps.
9. Acceptance: observed actual layout and manually consented fictional test chat parse with verified roles/composer/dedup; uncertain screens return unavailable. Structural collection alone is not completion.
