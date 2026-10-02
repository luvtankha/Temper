# Phase 47 — Avatar carousel and Android home

## 1. What changed

Android 0.47.0/code 47 implements the supplied home interaction specification:
five original 2D prototype avatars, continuous horizontal movement with distance
scaling/opacity and exact 300 ms center snapping, selected hero/platform, one
112 × 60 dp master switch, ON/OFF status, and one preview surface. Settings is
secondary. OFF dims/desaturates the hero; the existing consent and permission
flow still gates ON. Power preference updates bind the existing home instead of
rebuilding it mid-gesture.

Selection persists and both overlay paths reuse the artwork with eight expressions.
Already-owned legacy companions remain selectable without shop or locked-card
panels; unowned paid products remain gated. The default legacy free starter moves
to Astra. Rendering reuses at most seven slots and supports larger data catalogs.
Gestures, cancellation, second-pointer interruption, detachment, TalkBack and
keyboard navigation are handled. The analytics panel adopts the home surface and
accent while retaining its one graph, percentages and two short summaries.

The [editable Figma file](https://www.figma.com/design/YyV1MhtSXYbI6u6Qjzyyd7?node-id=7-4)
contains four main home states, all five avatars × two power states in a linked
prototype, the requested reusable components/properties/variants, original editable
vector artwork, 23 variables, four Roboto type styles, a tokens board and a clearly
illustrative fictional overlay. Android implements continuous dragging; Figma uses
Smart Animate between states. Model weights and analysis capture are unchanged.

## 2. Files changed or added

- `android/app/src/main/java/dev/temper/android/home/`: home, switch, bounded
  carousel, geometry and semantic tokens.
- `MainActivity`, `character/Avatar`, `character/CharacterView`, analytics views,
  theme and Gradle version.
- Five `res/drawable/avatar_*.xml` vector bodies and matching editable SVG sources.
- `design/phase47/`: catalog, tokens, node ledgers, authoring scripts and generated
  own-app previews; `scripts/generate-studio-avatars.py` and UTF-8 payload composer.
- Native carousel checks, updated power/consumer tests and three JVM carousel tests.
- `docs/release/AVATAR-CAROUSEL.md`, updated consumer guide and package script,
  phase context and this handoff.

## 3. Verification and build results

Final debug APK, instrumentation APK and unsigned release AAB built successfully.
Debug/release lint: zero errors, 35/34 warnings respectively, including existing
platform deprecations, synchronous preference persistence and overlay touch-listener
accessibility notices. APK ZIP and eight native ELF libraries pass 16 KB alignment.
The final tested 0.47.0 APK is installed on the OnePlus 8T.

Own-app renders were inspected at 390 dp and 320 dp/font scale 1.3. The latter
scrolls vertically for the preview copy. No horizontal scrolling is required.
The Figma home structure/font/composition inspection found zero raster fills and
zero non-Roboto text nodes; the UI is editable rather than a flattened mockup.

## 4. Tests run

- 36 JVM tests: zero failures, errors or skipped tests, with the real INT8 model
  supplied through `TEMPER_TEST_MODEL`.
- `carouselOnly`: continuous drag, exact snap, forward/reverse selection and
  persistence, cancelled second-pointer handling, 51-item/seven-view bounds,
  320/390/430 dp at font scales 1.0/1.3 and OFF/ON render fixtures.
- `powerOnly`: one master control, unchecked consent and setup gating, stale OFF
  safety, pending-start disable/cancellation, timeout, preference refresh and OFF.
- `consumerOnly`: nine avatars × eight distinct expressions, legacy paid gate,
  independent tokenizer fixtures, actual phone model, pipeline recovery/stale
  suppression, remote-speaker invariance and the new home/setup route.
- `overlayOnly`: automatic generated-chat switching, home/notification OFF,
  floating and analysis drag/tap/cancel/popup/keyboard/restore on our dummy screen.
- `complexOnly`: 28 held-out fictional English/Hinglish chats, identical replies
  in different contexts, four repair transitions, first-message spectrum and stop.
- Foundation instrumentation: observed layout fixtures, roles/clipping/redaction,
  consent opt-in/resume/revoke, version/package gates and encrypted account checks.

These checks read only app-generated fixtures and existing text-free adapter
fixtures. No real host chat was opened, read, trained on or uploaded for this phase.

## 5. Known issues

Figma Starter rejected a fourth page: `04 – Design Tokens` is a named section on
the Components page. The tool quota then rejected the final additional prototype
inspection after its successful creation. Home composition, typography and editable
structure were inspected before the quota was exhausted. Figma does not reproduce
native velocity/pointer tracking; prototype arrows support both directions and
drag illustrates the next state.

This testing APK and unsigned AAB are review artifacts. Publishing, owner signing,
real Play and feedback-service configuration remain prerequisites. Automatic chat
analysis still supports the verified WhatsApp adapter, not every chat application.
Underlying real-world emotion-accuracy limitations from Phase 46 remain. Android
may require manual Accessibility reconnection after instrumentation restarts.

## 6. Next phase prerequisites

Use the installed app or `dist/TEMPER-0.47.0-testing.apk` for review. Turn ON after
the existing one-time setup; use Settings to reconnect Accessibility if Android
disconnected it during testing. Larger catalogs use the same carousel; replacement
artwork must share the face anchors or provide a corresponding expression renderer.
Further Figma work needs available MCP quota; a fourth physical page needs a plan
that permits it. Read `AVATAR-CAROUSEL.md` for design/implementation details and
the existing release gates before distribution.
