# Updated Phase24 — Android foundation
1. Objective: primary Android project with runnable onboarding/settings and synthetic preview shell, preserving backend.
2. Repo: updated20–23 committed/verified, Java45/45; web legacy22/build; Android SDK/adb/project absent at entry.
3. Completed: foundational00–19, overlay pivot20, conflict21, trajectory22, pure compact mapping23; updated guide/handoffs reviewed.
4. Build status: Java native suite passed; Android tooling/build/device must be established and actually verified.
5. Architecture: native Java Android Activity shell, modular folders reserved for later capture/adapters/overlay/privacy; AGP8.13.2/Gradle8.13 per official compatibility, API36/min26, no unnecessary UI framework dependency.
6. Risks/blockers: User explicitly accepted SDK license and installation on2026-10-01; SDK platform36/build-tools35/platform-tools installed; no connected device/emulator confirmed. Cannot claim install/run gate until real verification.
7. Scope: pinned build/wrapper, minimal onboarding/settings/fictional preview, dark theme, backup disabled, paused by default, run/setup docs; no host-app access.
8. Non-goals: authentication25, consent/service26, adapters27/28, actual overlay29 or popup31, no standalone messaging client.
9. Acceptance: assemble/unit/lint pass, installs/runs on documented Android device/emulator, backend stays passing; record external blocker honestly if SDK/device unavailable.
