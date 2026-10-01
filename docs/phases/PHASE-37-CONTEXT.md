# Phase37 — Adapter extension framework
1. Objective: centralize exact package/build profile selection with an extension boundary.
2. Repository: ChatPlatformAdapter/normalized snapshots exist; build pin is embedded in Android service.
3. Previous:20–36 implemented; consolidated35/36 native regressions planned38.
4. Checks: latest Android build/lint and backend targeted6 tests pass; WhatsApp actual capture/graph/keyboard/scroll verified.
5. Architecture: pure adapter transformation separated from Android observation; consent allowlist is independent.
6. Risks: registry changes must not silently permit reading extra apps.
7. Scope: immutable validated profile registry, production WhatsApp build pin, exact resolution tests, extension documentation.
8. Non-goals: shipping Instagram/Telegram/Messenger/Discord capture; broadening current consent or host observation.
9. Acceptance: new profiles can be represented; mismatched builds/unknown apps/duplicate keys fail; existing WhatsApp behavior unchanged.
