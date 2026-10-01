# TEMPER — AGENT.md

## 0. Mission — FINAL PIVOT AFTER PHASE 19

Build **TEMPER** as an **Android-first overlay app** for existing chat applications.

TEMPER is **not** a standalone chat app.
TEMPER must **not** become a WhatsApp clone, Instagram clone, or messaging platform of its own.

TEMPER's purpose is:
- detect the direction of an ongoing conversation
- estimate conversational emotion and tension from recent visible turns
- show a **small 2D grounded character overlay** near the chat composer
- update the character's face/posture according to the conversation
- open a **small analytics box when the character is tapped**
- inside that analytics box show **only a bar graph of the emotional spectrum** and a short text explanation of what is happening and where the conversation appears to be heading

TEMPER must work like an **overlay companion** on top of existing chat apps.

Primary target:
- **Android**
- **WhatsApp first**
- then other chat apps through adapters only after WhatsApp works

Important:
- The product must remain small, focused, and demoable.
- Remove or ignore unnecessary old product ambitions that turned TEMPER into a full messaging app.
- From Phase 20 onward, the goal is **overlay only**.

---

# 1. Hard Rules

## 1.1 Current State Rule

Phases **00–19** belong to the earlier foundation/history of the project.
Do **not** restart the project.
Do **not** rebuild Phases 00–19 from scratch.
Do **not** destroy working backend/model code already completed.

Treat the current repository as an existing project whose direction changes **after Phase 19**.

## 1.2 Product Rule

TEMPER is an **overlay app**, not a chat app.

No primary work after Phase 19 should be spent building:
- conversation list screens
- in-app messaging UI
- new social features
- stories/statuses
- group chat clone features
- voice/video chat features
- sticker marketplaces
- a standalone chat product

If any old standalone chat code exists, it may remain as a test harness only. It is no longer the product goal.

## 1.3 Overlay Rule

The final user-facing surface must be:
- a small **2D character** near the message bar
- the character should look like it is **standing, sitting, or leaning on the chatbox/composer edge**
- the character must take **minimal space**
- tapping the character opens a **compact analytics text box / panel**
- the analytics view should contain:
  1. a short status summary
  2. a short trajectory/direction summary
  3. **only one bar graph** showing the emotional spectrum

Do **not** build a complex large analytics dashboard into the overlay.

## 1.4 Character Rule

The character must:
- be **2D**
- be compact like a UI companion
- stay close to the bottom-left composer area
- feel visually grounded on the chatbox edge
- use smooth expression transitions
- change mostly through face + slight posture changes
- avoid taking over the screen

## 1.5 Privacy Rule

Because this is an overlay over chat apps, TEMPER must:
- require explicit user consent
- clearly explain Accessibility access
- read only the minimum visible conversation content needed
- not auto-send messages
- not auto-click host app controls
- not scrape unrelated content
- not log raw private chats in normal logs
- allow pause/disable

## 1.6 Build Rule

Every phase after Phase 19 must leave the project runnable.

If something new is not ready, use safe stubs/adapters/mocks behind interfaces.
Do not leave the repo broken.

---

# 2. Final Product Definition

## 2.1 What TEMPER Does

TEMPER overlays supported chat apps and analyzes the ongoing visible conversation.

Flow:

```text
Supported Chat App (WhatsApp first)
          ↓
Android AccessibilityService
          ↓
Platform Adapter
          ↓
Recent visible conversation turns
          ↓
TEMPER analysis pipeline
          ↓
Conflict / emotion / trajectory
          ↓
Small 2D character overlay
          ↓
Tap character → compact analytics box
```

## 2.2 What the User Sees

Default state:
- chat app remains usable
- a small 2D character appears near the composer
- character reacts based on the direction of the conversation

On tap:
- a compact analytics box opens
- it shows:
  - short current-state summary
  - short where-the-conversation-is-heading summary
  - one emotional-spectrum bar graph

No giant dashboard in the overlay.

## 2.3 What the Character Represents

The character represents the **remote participant / other side of the conversation**.

If the user is chatting with another person, the overlay character reflects the model-estimated response state of the other person based on the visible conversation.

This is a UX representation, not proof of true internal emotion.

## 2.4 Emotional Spectrum

The bar graph may contain these emotions/signals as the primary display spectrum:
- neutral
- positive / happy
- concerned
- confused
- sad
- frustrated
- angry
- surprised

Additional internal signals may still exist in backend logic:
- sentiment
- sarcasm
- toxicity
- passive aggression
- blame
- defensiveness
- disagreement
- withdrawal

But the compact overlay graph should stay visually simple.

## 2.5 Conversation Direction / Trajectory

TEMPER must estimate where the conversation appears to be heading.

Primary trajectory states:
- `STABLE`
- `TENSION_RISING`
- `ESCALATION_RISK`
- `DEESCALATING`
- `WITHDRAWAL_RISK`
- `REPAIR_OPPORTUNITY`
- `UNCERTAIN`

TEMPER must never claim certainty.
Use wording like:
- "Tension is rising"
- "Escalation risk is increasing"
- "Conversation appears to be de-escalating"
- "Current direction is uncertain"

Avoid deterministic wording.

---

# 3. Final Architecture

## 3.1 Required Components

### A. Android overlay client (primary product)
- Android app
- AccessibilityService
- platform adapters
- overlay manager
- compact 2D character UI
- compact analytics popup/box
- local rolling context buffer
- auth/settings/privacy controls

### B. Spring Boot analysis backend (reuse existing work)
- Java 21
- Spring Boot
- existing model integration from earlier phases
- conversation context processing
- emotion/sentiment/sarcasm/toxicity/conflict logic
- trajectory engine
- optional account/session/history

### C. Optional existing UI code
If an old React UI already exists, preserve it only if useful for:
- debugging
- testing
- demo support
- viewing stored results

It is no longer the main product and should not drive the roadmap.

## 3.2 Architecture Flow

```text
WhatsApp / Other supported chat app
               ↓
Android AccessibilityService
               ↓
ChatPlatformAdapter
               ↓
Normalized visible conversation window
               ↓
TEMPER analysis service
   ├── sentiment
   ├── emotions
   ├── sarcasm
   ├── toxicity
   ├── passive aggression
   ├── blame
   ├── defensiveness
   ├── disagreement
   ├── withdrawal
   ├── conflict engine
   └── trajectory engine
               ↓
Overlay mapping layer
   ├── character state
   ├── short status text
   ├── direction text
   └── emotional spectrum bars
               ↓
2D character overlay + compact analytics box
```

## 3.3 Local-first Preference

Where possible:
- keep only a rolling window of recent visible messages
- avoid uploading unnecessary full chat history
- avoid storing raw chat text unless explicitly needed and enabled

---

# 4. Repository Structure

```text
temper/
│
├── AGENT.md
├── README.md
├── .gitignore
├── .env.example
│
├── android/
│   ├── app/
│   │   ├── src/main/java/.../temper/
│   │   │   ├── accessibility/
│   │   │   ├── adapters/
│   │   │   ├── overlay/
│   │   │   ├── character/
│   │   │   ├── analytics/
│   │   │   ├── api/
│   │   │   ├── auth/
│   │   │   ├── privacy/
│   │   │   └── storage/
│   │   ├── src/main/res/
│   │   └── AndroidManifest.xml
│   └── build files
│
├── backend/
│   └── existing Spring Boot project
│
├── frontend/
│   └── optional legacy/debug UI only
│
├── models/
├── contracts/
└── docs/
    └── phases/
```

---

# 5. Required UI/UX Specification for the Overlay

## 5.1 Placement

The example image is the visual intention.

TEMPER's character should appear:
- near the **bottom-left** of the active chat composer
- slightly above or resting on the composer edge
- small enough not to cover the conversation or input

Think of it as:
- sitting on the composer border
- standing on the composer edge
- leaning from the corner of the composer

## 5.2 Grounded Look

The character must appear as if it is on **solid ground**.

Acceptable grounding methods:
- feet/body resting visually on the composer edge
- a tiny base/shadow line
- a subtle ledge effect

The character must not feel like a random floating sticker.

## 5.3 Size

Keep it compact.
Suggested mobile footprint:
- width around 56–84 px
- height around 72–110 px

The exact values can adapt by screen density and app layout.

## 5.4 Character Style

- 2D
- clear face
- readable expression at small size
- minimal body motion
- high facial readability
- subtle idle animation

## 5.5 Animation Principles

Most emotion change should come from:
- eyebrows
- eyes
- eyelids
- mouth
- slight head tilt
- slight shoulder/posture change

Avoid large exaggerated full-body movement.

## 5.6 Click Behavior

Tapping the character should open a compact analytics box.
The analytics box should:
- not be full screen
- not cover the whole chat
- be dismissible
- be easy to read quickly

## 5.7 Overlay Analytics Content

Inside the analytics box show only:

1. **Current state text**
   - e.g. `Current state: Frustration rising`

2. **Direction text**
   - e.g. `Direction: Escalation risk`

3. **Single bar graph: Emotional Spectrum**
   - one compact chart only
   - no multiple graphs
   - no oversized dashboards

Example emotional spectrum bars:
- Happy
- Concerned
- Confused
- Sad
- Frustrated
- Angry
- Surprised
- Neutral

No additional charts unless explicitly required later.

---

# 6. Completed History / Preserved Scope

Treat phases **00–19** as prior foundation.
The project may already contain:
- model work
- backend APIs
- existing analysis services
- earlier UI experiments
- tests
- docs

Preserve and reuse what helps.
Do not keep old messaging-app ambitions alive.

---

# 7. Phase Documentation Standard

Before every phase, create:
- `docs/phases/PHASE-XX-CONTEXT.md`

After every phase, create:
- `docs/phases/PHASE-XX-HANDOFF.md`

Each context file must contain:
1. current objective
2. current repo state
3. completed previous phases
4. current builds/tests status
5. architecture state
6. blockers/risks
7. exact phase scope
8. explicit non-goals
9. acceptance criteria

Each handoff must contain:
1. what changed
2. files changed/added
3. verification/build results
4. tests run
5. known issues
6. next phase prerequisites

---

# 8. POST-PHASE-19 ROADMAP

Everything below replaces the old future roadmap.
Continue from Phase 19.

---

# 9. PHASE 20 — Preserve Core Analysis and Remove Product Drift

## Objective
Freeze the old messaging-app direction and pivot the product to overlay-only.

## Tasks
- inspect the repo and identify standalone chat-app/product code
- decide what stays as reusable infrastructure vs what becomes legacy/test-only
- preserve backend/model services that are useful
- preserve any auth/account code if useful
- mark old chat-app UI as non-primary
- update docs to reflect overlay-only goal
- ensure existing backend and tests still run

## Acceptance
- repo is not restarted
- Phase 19 outputs remain intact
- project direction is clearly changed to overlay-only
- working backend/model code preserved

---

# 10. PHASE 21 — Conflict Engine Finalization

## Objective
Use the outputs from earlier phases to produce a reliable conversation conflict signal.

## Output
Return:
- raw conflict score
- smoothed conflict score
- trend (`UP`, `FLAT`, `DOWN`)
- top contributors

## Acceptance
- deterministic for fixed inputs
- tests added/updated
- backward-compatible API contract retained

---

# 11. PHASE 22 — Trajectory Engine

## Objective
Estimate where the conversation is heading.

## Output trajectory states
- `STABLE`
- `TENSION_RISING`
- `ESCALATION_RISK`
- `DEESCALATING`
- `WITHDRAWAL_RISK`
- `REPAIR_OPPORTUNITY`
- `UNCERTAIN`

## Output format
Should include:
- state
- confidence/evidence strength
- short explanation
- contributing signals

## Acceptance
- deterministic fixtures
- stable output on sample conversations
- insufficient context handled as `UNCERTAIN`

---

# 12. PHASE 23 — Compact Overlay Mapping Layer

## Objective
Translate analysis outputs into:
- character emotional state
- short status text
- short direction text
- emotional spectrum bars

## Acceptance
- mapping logic separated from Android UI rendering
- unit-testable transformation layer exists

---

# 13. PHASE 24 — Android App Foundation

## Objective
Create or solidify the Android project as the primary client.

## Required
- Android app builds successfully
- onboarding/settings shell exists
- auth/session shell if needed
- placeholder screen or demo harness exists

## Acceptance
- app installs and runs
- base structure created without breaking backend

---

# 14. PHASE 25 — Authentication / Account Basics

## Objective
Make TEMPER a proper app with user access, but keep it minimal.

## Required
- sign in
- create account
- sign out
- current session
- optional profile settings

This is support infrastructure, not the main demo.

## Acceptance
- user can create account and sign in
- local session persists correctly

---

# 15. PHASE 26 — AccessibilityService + Consent

## Objective
Create the Android capability that makes TEMPER behave like an overlay extension.

## Required
- onboarding explaining accessibility access
- explicit user opt-in
- AccessibilityService skeleton
- supported package detection
- pause/disable controls

## Acceptance
- service can be enabled and disabled
- supported app detection works
- no unrelated automation is performed

---

# 16. PHASE 27 — Platform Adapter Contract

## Objective
Create a clean interface for reading supported chat apps.

## Interface responsibility
A platform adapter should:
- declare support for a package/app
- read a visible active conversation snapshot
- identify message composer anchor/bounds when possible

## Acceptance
- adapter abstraction exists
- fake/test adapter exists
- analysis core remains platform-agnostic

---

# 17. PHASE 28 — WhatsApp Adapter

## Objective
Make WhatsApp the first real supported host app.

## Required
- detect active conversation screen
- extract visible messages when possible
- distinguish local vs remote turns when possible
- identify composer region/bounds when possible
- deduplicate repeated UI reads/events

## Failure rule
If parsing is uncertain:
- fail closed
- show analysis unavailable
- do not guess

## Acceptance
- tested on documented device/app build
- normalized messages produced correctly for supported test conversation

---

# 18. PHASE 29 — Character Overlay Shell

## Objective
Show the small 2D character near the chatbox like the provided example.

## Required
- overlay anchored near bottom-left composer area
- compact size
- appears grounded on the composer edge
- subtle shadow/base if needed
- does not block main input usage
- survives keyboard open/close
- works in portrait layout

## Acceptance
- overlay appears consistently on supported screen
- position is stable and usable

---

# 19. PHASE 30 — 2D Character Emotional State System

## Objective
Implement the compact 2D character behavior.

## Required visible states
- Neutral
- Happy / Positive
- Concerned
- Confused
- Sad
- Frustrated
- Angry
- Surprised

## Animation style
- face-first changes
- slight head/posture changes
- minimal motion footprint
- no giant body animation

## Acceptance
- state transitions are visible and readable at small size
- no oversized overlay footprint

---

# 20. PHASE 31 — Character Click Popup / Analytics Box

## Objective
When the character is tapped, open a compact analytics box.

## Box content
Only show:
1. short current-state text
2. short direction text
3. one bar graph: emotional spectrum

Do not add:
- multiple graphs
- giant dashboards
- large side panels
- unnecessary details

## Acceptance
- tapping the character opens the compact box
- graph renders correctly
- the chat app remains usable

---

# 21. PHASE 32 — Live Capture Pipeline

## Objective
Connect WhatsApp adapter output to TEMPER analysis.

## Flow
```text
Accessibility event
↓
WhatsApp adapter
↓
normalized visible turns
↓
rolling context window
↓
analysis service
↓
conflict + direction + emotional spectrum
↓
character state update
↓
analytics box content update
```

## Acceptance
- new visible chat events trigger updated overlay analysis
- overlay updates without needing manual text entry

---

# 22. PHASE 33 — Direction-aware Character Reactions

## Objective
Make the character react based not only on current emotion but also where the conversation is heading.

## Examples
- stable → calm neutral pose
- tension rising → concerned / slight tension
- escalation risk → frustrated/angry expression
- de-escalating → relaxing back toward neutral
- withdrawal risk → lower-energy withdrawn look

## Acceptance
- trajectory affects expression selection/motion intensity

---

# 23. PHASE 34 — Compact Emotional Spectrum Bar Graph

## Objective
Implement the single bar graph shown in the analytics box.

## Requirements
- only one graph
- compact
- easy to read quickly
- bars correspond to the defined emotion spectrum
- values updated from live analysis

## Acceptance
- graph is readable in the popup
- graph updates with conversation changes

---

# 24. PHASE 35 — Short Analytics Messaging

## Objective
Keep overlay text simple and useful.

## Required text fields
1. `Current state:` short text
2. `Direction:` short text

Examples:
- `Current state: Frustration rising`
- `Direction: Escalation risk`
- `Current state: Conversation softening`
- `Direction: De-escalating`

Keep it short. Do not overload the popup.

## Acceptance
- short text remains clear and concise
- no long verbose analysis blocks in overlay

---

# 25. PHASE 36 — Privacy + Safe Data Handling

## Objective
Reduce risk from reading private chats.

## Required
- explicit consent
- minimum visible-data extraction
- no raw chat logs in normal logs
- local rolling context buffer
- optional clear/delete local data
- pause/disable overlay

## Acceptance
- privacy behavior documented
- service can be paused safely

---

# 26. PHASE 37 — Additional App Adapter Framework

## Objective
Prepare support for more chat apps without destabilizing WhatsApp support.

## Possible future adapters
- Instagram
- Telegram
- Messenger
- Discord

But only after WhatsApp works well.

## Acceptance
- adapter framework supports extension
- no degradation of WhatsApp path

---

# 27. PHASE 38 — Testing and Demo Hardening

## Objective
Make the overlay demo reliable.

## Must verify
- Android build works
- service detection works
- WhatsApp adapter path works on test device/build
- character overlay appears correctly
- clicking character opens analytics box
- bar graph updates
- status + direction text update

## Acceptance
- repeatable demo path exists

---

# 28. PHASE 39 — Final Packaging

## Objective
Prepare a usable demo build.

## Deliverables
- working Android APK/build
- clear onboarding steps
- accessibility permission instructions
- supported-app note
- test conversation / demo script
- backend config if required

## Acceptance
- a tester can install and run the demo with instructions

---

# 29. Final Demo Flow

A successful demo should look like this:

```text
Install TEMPER
↓
Sign in (if enabled)
↓
Enable Accessibility with informed consent
↓
Open WhatsApp
↓
Open a conversation
↓
TEMPER small 2D character appears near the bottom-left chatbox/composer
↓
Conversation continues
↓
Character face subtly changes based on the conversation
↓
Tap character
↓
Compact box opens
  - Current state text
  - Direction text
  - Emotional spectrum bar graph
↓
User understands what is happening and where the conversation seems to be heading
```

---

# 30. Definition of Done

TEMPER is complete for this roadmap when:
- it is clearly an overlay app, not a chat app
- WhatsApp path works as the primary demo
- the character appears compactly near the chatbox
- the character looks grounded on the composer edge
- the character changes expression according to the conversation
- tapping the character opens the compact analytics box
- the popup contains only the short text + one emotional spectrum bar graph
- privacy/consent basics are in place
- the build is demoable and documented

---

# 31. Engineering Principles

1. Reuse existing Phase 00–19 core logic.
2. Do not restart the project.
3. Remove unnecessary messaging-product scope.
4. Keep the overlay small and usable.
5. Keep the character readable at small size.
6. Keep analytics compact.
7. Prefer WhatsApp-first quality over multi-app quantity.
8. Keep the app demoable.
9. Fail safely when parsing is uncertain.
10. Never claim certainty about emotion or future conversation outcome.
