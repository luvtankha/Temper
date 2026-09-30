# Phase 01 — Permanent responsive UI shell

1. **Objective:** Establish final dark responsive layout and four routes before feature work.
2. **Working state:** Verified independently running React and Spring Boot foundation; clean Phase 00 commit.
3. **Prior completed phases:** 00. Its context and handoff, API contract and architecture were reviewed.
4. **Architecture:** React router with permanent navigation, main workspace, collapsible analytics region; typed health adapter to Spring Boot common module.
5. **API:** GET `/api/v1/health`, Actuator health unchanged.
6. **Schema:** None.
7. **Frontend:** App, fetch adapter, mock provider, Tailwind/global styles; add Shell and route placeholders.
8. **Backend:** Application and HealthController unchanged.
9. **Models:** None; no inferred values shown.
10. **Tests:** 2 frontend, 2 backend, 1 browser integration passing; both production builds passed.
11. **Limitations:** No real chat, analytics, persistence or rigs. Docker absent.
12. **Preserve:** Health adapter, proxy, root ignore, Java 21 wrapper, backend tests.
13. **Scope:** `/chat`, `/analysis`, `/history`, `/settings`; responsive desktop/sidebar and analytics drawer; dark palette, keyboard dismissal/focus handling.
14. **Out of scope:** Message behavior, avatar rig, analytical charts, backend domain and inference.
15. **Acceptance:** At ultrawide, laptop, tablet and phone sizes no document horizontal overflow; drawer opens/closes; all routes accessible; frontend builds/tests; existing health integration passes.
