# Phase32 — Live selected-chat capture and analysis
1. Objective: connect bounded visible WhatsApp turns to actual local backend inference and update character/text/spectrum without manual text entry.
2. Repository: Phase31 installed; actual character tap opens/dismisses/reopens readable compact panel. User asks for actual analysis.
3. Completed: historical00–19 preserved; updated20–31 complete, with popup-plus-keyboard combination retained as a hardening check.
4. Builds/tests: latest Android app/test/lint and physical-device suite PASS; backend prior45 tests and five native model integrations preserved.
5. Architecture: conservative two-party adapter, neutral composer fallback, model/conflict/trajectory/overlay mapper in backend; no stateless live endpoint yet. Existing history APIs are unsuitable for private captured windows.
6. Risks: consent2 explicitly forbids transmission; need separate informed live-processing opt-in before any phone text is sent. Bound work, cancel stale results, avoid raw persistence/logs, reject unsupported layouts and fixture-only inference.
7. Scope: stateless bounded backend endpoint using existing model pipeline without repositories; authenticated local demo transport; explicit client opt-in/configuration; selected-chat rolling memory, debounce/dedup/cancellation, unavailable states; real model/phone integration gate.
8. Non-goals: full history upload, contact names/drafts, host automation, public deployment, additional adapters or pretending unsupported media rows are analyzed.
9. Acceptance: consented supported fictional visible chat changes produce actual model-backed overlay updates; unsupported/stale/error states stay unavailable; pause/departure clears/cancels; tests cover privacy boundaries and failure paths; actual inference and phone demo verified.
