# Phase 03 — Remote avatar placement

1. **Objective:** Anchor the other participant’s avatar to the composer at all viewport sizes.
2. **Working state:** Functional local mock chat and responsive shell; Phase 02 committed and verified.
3. **Completed:** 00–02; previous context/handoffs, architecture and contract reviewed.
4. **Architecture:** ChatProvider holds local identity; shared remoteParticipant resolver feeds ChatPage and new RemoteAvatar component. Composer wrapper owns the avatar layer.
5. **API:** Health and frontend ChatApi unchanged.
6. **Schema:** None.
7. **Frontend:** Shell, ChatProvider, ChatPage, mock adapter; add original neutral vector avatar sources and placement component.
8. **Backend:** Health module unchanged.
9. **Models/rigs:** No AI. Neutral vector placeholders only; Rive rig and emotion transitions belong to Phase 04 and are not claimed here.
10. **Tests:** 6 frontend unit, 2 backend, 15 browser passing; both builds verified.
11. **Limitations:** Original PNG references are flat sheets; no exported `.riv` exists. Placeholder artwork is not a substitute for the Phase 04 gate.
12. **Preserve:** Stable ChatApi, current chat behavior, responsive drawers, health integration.
13. **Scope:** Original male/female neutral placeholder, swapped identity resolver, composer-contained placement; document visual references and asset provenance.
14. **Out of scope:** Emotion state machine, blink/typing animations, analytics, real-time transport, models or persistence.
15. **Acceptance:** Viewing as Alex shows Nova/female; viewing as Nova shows Alex/male. Avatar remains inside composer wrapper and on-screen without blocking input at all tested sizes. Build and prior tests pass.
