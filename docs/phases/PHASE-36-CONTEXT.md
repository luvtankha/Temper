# Phase36 — Privacy and cleanup
1. Objective: make inspection cleanup explicit and audit request/session lifetime.
2. Repository: separate consent, pause, transient8-turn window, private token, disabled backups and no raw logging already exist.
3. Previous:20–35 implemented/verified;35 native rendering regression planned in38.
4. Checks: Android build/lint passes; real pause saved true with both overlay windows absent; model/transport/privacy tests previously pass.
5. Architecture: on-device structural gate before text; USB loopback stateless inference without repositories; bounded stale-result filtering.
6. Risks: already-sent requests may finish in RAM; immutable Java strings cannot be securely zeroed; persistent optional metadata/token need explicit cleanup.
7. Scope: clear inspection data action, remove USB config action, interrupt cleanup, zero backend request bytes, documented lifetime/privacy boundaries.
8. Non-goals: OS data deletion, disabling user system settings by command, cloud processing, chat storage, account removal.
9. Acceptance: pause/clear removes probes/results/files, token removal revokes live consent; no stale result after interruption; no raw-text normal logs.
