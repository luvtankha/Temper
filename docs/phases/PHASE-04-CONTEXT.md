# Phase 04 — Avatar rig and emotional state machine

1. **Objective:** Replace placement placeholders with original male/female Rive rigs supporting smooth reversible eight-state transitions.
2. **Current working state:** React local chat and Java 21 health service run. Composer holds swapped neutral SVG previews; previous phases verified and committed.
3. **Completed phases:** 00–03. All previous contexts/handoffs, architecture, avatar provenance and API contract reviewed.
4. **Architecture:** ChatApi abstraction and app-level provider; RemoteAvatar chooses other participant; introduce typed semantic controls, Rive renderer and explicit rig configuration behind that same component.
5. **Contracts:** REST health unchanged. Avatar continuous controls: anger, sadness, happiness, frustration, confusion, concern, surprise, valence, arousal, sarcasm; no server-generated frames. Rive state machine planned as `TemperEmotion`.
6. **Database schema:** None.
7. **Frontend:** App/Shell, ChatProvider/ChatPage, RemoteAvatar, vector sources, mock and health adapters; add rig control mapper, Rive integration and validation.
8. **Backend:** TemperApplication and HealthController; unchanged in this phase.
9. **AI/model/rig status:** No inference. No `.riv` assets supplied. Original SVG sources exist. Checking Rive editor authoring/export access; animation gate requires real rig assets and runtime evidence.
10. **Passing tests:** Frontend 8/8 unit and 25/25 browser; Java 2/2. Both builds pass.
11. **Known limitations:** Flat PNG sheets cannot directly become rig/state machines. Final Rive character export requires editor access; none yet established. No Docker or deployed service.
12. **Must preserve:** Swapped participant identity, composer containment, responsive sizing, all existing chat behaviors, health contract and ignored local outputs.
13. **Exact scope:** Continuous control contract and safe normalization, genuine Rive runtime adapter, eight-state authoring specification, original character rig authoring/export if access permits; validate required runtime inputs and missing assets honestly.
14. **Out of scope:** Phase 05 idle/typing micro-interactions, Phase 06+ analytics, backend chat domain, AI/persistence/security and deployment. Cannot advance sequential phase order until rig gate passes.
15. **Acceptance:** Original male and female `.riv` files load, state machine numeric inputs drive visible facial/head/body changes smoothly through all eight states and back to neutral; frontend build/tests and previous layout/chat/integration pass. Code or mocks alone are insufficient.
