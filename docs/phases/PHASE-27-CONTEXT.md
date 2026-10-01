# Updated Phase27 — Platform adapter contract
1. Objective: separate native screen observations from normalized visible turns and composer bounds.
2. Repo: phases20–26 complete; actual WhatsApp package detection confirmed by user. System service now disabled after instrumentation restart.
3. Completed: preserved00–19; pivot/core analysis20–23; native foundation/account/consent24–26.
4. Builds/tests: Android build/lint and actual consent/account/foundation tests pass; real service revocation remains a manual hardening check; backend45/45 and legacy22/build/browser11 preserved.
5. Architecture: pure Java adapter interface accepts bounded immutable screen observations. It returns a closed failure status or immutable role-tagged turns and a composer anchor. No Android nodes or backend models cross that boundary.
6. Risks: avoid plaintext in object debug strings, mutable snapshots, ambiguous participant roles, fake results reaching real capture.
7. Scope: contract, validated DTOs, isolated fake adapter and meaningful actual-device contract checks.
8. Non-goals: WhatsApp parsing or screen traversal, network, overlay drawing, raw chat persistence; no fake adapter registered in the live service.
9. Acceptance: immutable bounded normalized snapshots, declared package support, fake fixture roles/composer and fail-closed paths tested; core remains platform-agnostic.
