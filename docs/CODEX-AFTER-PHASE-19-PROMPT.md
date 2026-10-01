# CODEX CONTINUATION PROMPT — TEMPER AFTER PHASE 19 (OVERLAY-ONLY)

You are continuing the EXISTING TEMPER project.

The project has already reached **Phase 19** of the earlier roadmap.

The roadmap has now changed.

Your new source of truth is:
- `TEMPER-AGENT-UPDATED.md`

Read it fully before writing or modifying code.

## CRITICAL PRODUCT RULE

TEMPER is **NOT** a chat app.
TEMPER is **NOT** a WhatsApp clone.
TEMPER is **NOT** a messaging platform.

TEMPER is now an **Android overlay app** for existing chat applications.

The main goal is:
- observe ongoing visible chat from supported apps
- detect current conversational emotion/tension and direction
- show a **small 2D grounded character** near the bottom-left chatbox/composer
- make that character change facial expression according to how the conversation is going
- when the character is clicked, open a **compact analytics box**
- the analytics box should show:
  1. short current-state text
  2. short direction text
  3. **only one bar graph** containing the emotional spectrum

Do not build a large analytics dashboard into the overlay.
Do not continue building a standalone chat product.

The provided example image is the intended placement/style reference:
- character near the bottom-left message bar
- character small and unobtrusive
- character looks attached to / grounded on the chatbox edge
- clicking it reveals the compact analytics box

## BEFORE CODING

1. Read `TEMPER-AGENT-UPDATED.md` completely.
2. Inspect the repository.
3. Inspect Git status.
4. Read Phase 00–19 context/handoff docs if present.
5. Verify whether Phase 19 is actually complete.
6. Build/test the current backend.
7. Build/test any current Android/frontend project if present.
8. Identify what old standalone-chat code should become legacy/test-only.

Then report only this short checkpoint:

```text
WORKSPACE:
CURRENT PHASE:
PHASE 19 STATUS:
ANDROID:
BACKEND:
LEGACY UI:
TESTS:
BLOCKERS:
NEXT:
```

If Phase 19 is incomplete, finish Phase 19 first and write/update its handoff.
Then continue from **Phase 20** of the updated roadmap.

## ABSOLUTE EXECUTION RULES

- Do not restart the project.
- Do not rebuild Phases 00–19 from scratch.
- Do not remove working backend/model code.
- Do not keep building TEMPER as a chat app.
- Do not wait for permission between normal phases.
- After each phase:
  - create/update the phase context file
  - implement the phase
  - run relevant builds/tests
  - verify acceptance criteria
  - write the phase handoff
  - automatically continue

## WHAT TO BUILD AFTER PHASE 19

Follow the updated roadmap exactly.

### Priority order

1. **Phase 20** — preserve core analysis and remove product drift
2. **Phase 21** — conflict engine finalization
3. **Phase 22** — trajectory engine
4. **Phase 23** — compact overlay mapping layer
5. **Phase 24** — Android app foundation
6. **Phase 25** — auth/account basics (minimal)
7. **Phase 26** — AccessibilityService + consent
8. **Phase 27** — platform adapter contract
9. **Phase 28** — WhatsApp adapter
10. **Phase 29** — character overlay shell
11. **Phase 30** — 2D character emotional state system
12. **Phase 31** — character click popup / analytics box
13. **Phase 32** — live capture pipeline
14. **Phase 33** — direction-aware character reactions
15. **Phase 34** — compact emotional spectrum bar graph
16. **Phase 35** — short analytics messaging
17. **Phase 36** — privacy + safe data handling
18. **Phase 37** — additional adapter framework (only if time allows)
19. **Phase 38** — testing and demo hardening
20. **Phase 39** — final packaging

## CHARACTER / OVERLAY SPECIFICATION

The final character must:
- be **2D**
- stay small
- be near the bottom-left of the message composer
- appear as if standing/sitting/leaning on the composer edge
- feel grounded using the edge/shadow/base
- use mainly face + small posture changes
- not cover too much chat space

This is the correct visual goal.

### Character states
- Neutral
- Happy / Positive
- Concerned
- Confused
- Sad
- Frustrated
- Angry
- Surprised

### Character behavior
Use the conversation state + trajectory to drive expression.
Examples:
- stable → neutral
- tension rising → concerned / slightly tense
- escalation risk → frustrated / angry
- de-escalating → relax toward neutral
- withdrawal risk → low-energy / withdrawn

## ANALYTICS BOX SPECIFICATION

When the character is tapped, open a compact popup/box.

The popup must show **only**:
1. `Current state:` short text
2. `Direction:` short text
3. one compact **emotional spectrum bar graph**

Do not add:
- large dashboards
- multiple charts
- giant panels
- cluttered information

Example:
- `Current state: Frustration rising`
- `Direction: Escalation risk`
- bar graph with the emotional spectrum

## CHAT APP SUPPORT STRATEGY

Primary demo target:
- WhatsApp on Android

Only after WhatsApp works should you think about:
- Instagram
- Telegram
- Messenger
- Discord

Do not support many apps badly.
Support WhatsApp well first.

## ACCESSIBILITY / PRIVACY RULES

TEMPER must:
- ask for explicit user consent
- explain what Accessibility access is for
- read only the minimum visible chat content needed
- not click/send/automate host-app behavior
- not log raw chat messages in normal logs
- allow pause/disable
- fail closed when parsing is uncertain

If the adapter cannot confidently parse the supported screen, show:
- analysis unavailable

Do not guess.

## KEEP / REMOVE SCOPE CORRECTLY

### Keep / reuse
- existing backend analysis logic
- existing model integrations
- existing conflict-related work
- existing auth/account code if useful
- any tests/docs that still apply

### De-prioritize or remove from active roadmap
- standalone chat UI as product goal
- large analytics dashboards for the overlay
- multi-screen messaging app ambitions
- unnecessary social-app functionality

A legacy web UI may remain as a debug/test surface only.

## DEFINITION OF SUCCESS

A successful TEMPER demo now means:

```text
User opens TEMPER
↓
User signs in if needed
↓
User enables Accessibility with informed consent
↓
User opens WhatsApp
↓
User opens a conversation
↓
Small 2D character appears near the bottom-left chatbox/composer
↓
Conversation is analyzed in the background
↓
Character facial expression changes based on the conversation
↓
User taps the character
↓
Compact analytics box appears
  - Current state text
  - Direction text
  - Emotional spectrum bar graph
```

That is the goal.

## WHEN SOMETHING FAILS

Do not hide failures.
For any blocker:
1. reproduce it
2. isolate it
3. identify root cause
4. attempt safe fix
5. rerun tests/builds
6. record it in the phase handoff

If something genuinely cannot be completed, document:
- BLOCKER
- ROOT CAUSE
- ATTEMPTED FIXES
- CURRENT SAFE STATE
- WHAT IS REQUIRED NEXT

Then continue any other independent work that remains possible.

## START NOW

Read `TEMPER-AGENT-UPDATED.md`.
Inspect the current repository.
Verify/finish Phase 19.
Then continue automatically from **Phase 20** of the updated overlay-only roadmap.
