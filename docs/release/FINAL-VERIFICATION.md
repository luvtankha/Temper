## TEMPER FINAL VERIFICATION REPORT

Candidate **0.48.0 / code 48**, reviewed October 2, 2026. Baseline: Phase 47 commit `69c6b2eead5c8651272f55769df8689e027265e9`. Device: OnePlus 8T KB2001, Android 14 / API 34. All generated chat checks use fictional English and Roman-script Hinglish. No private chats were used, exported or trained on during this pass.

### 1. Build Status

The Android application is `android/app`. Native Java views/services are the primary product. Backend and React modules remain explicitly described historical/developer harnesses; they were not changed or reverified in this pass.

| Check | Result |
| --- | --- |
| Clean debug APK and instrumentation APK | PASS |
| Clean R8/resource-shrunk release APK and unsigned release AAB | PASS |
| JVM tests with the pinned real model | 44 tests; 0 failures, errors or skips |
| Debug / release lint | 0 errors; 34 / 33 warnings |
| Model inside debug APK, release APK and AAB | Size and SHA-256 match the pinned public weights |
| APK ZIP alignment / native ELF alignment | 16 KB ZIP alignment; all eight native libraries have 16 KB load-segment alignment |
| Installed minified release | Version 0.48.0, primary package, non-debuggable; locally development-signed for device review |
| Isolated fresh installation | PASS for OFF defaults, unchecked consent and missing-model setup; primary installation/data preserved |

The complete clean command passed in 1 minute 11 seconds. Later privacy-copy/selected-preview corrections also passed debug/release compilation, tests, lint and bundle generation; the final affected build passed in 1 minute 1 second. Exact commands and toolchain requirements are in [SETUP-AND-DEMO.md](SETUP-AND-DEMO.md).

Warnings are localization debt (`SetTextI18n`), touch-listener accessibility notices, intentional physical overlay coordinates, programmatic view constructors, conservative storage checks and compatibility fallbacks. Release also lacks a monochrome themed launcher icon. The stage glow no longer allocates a shader on each draw. Gradle reports deprecated Groovy assignment syntax against future Gradle 9; the verified wrapper remains pinned to 8.13. Vendor native binaries are retained when symbol stripping is unavailable. No warning is being represented as a runtime crash, and none substitutes for the outstanding live acceptance checks.

### 2. Core Product Flow

Generated device checks pass avatar selection, persistent center snapping, bounded catalogs, setup gating, ON/OFF, selected-character overlay and real on-device inference/graph rendering. ON now requires explicit consent, prepared weights, overlay permission, connected Accessibility and a successful foreground-service start. A preference alone cannot advertise active analysis.

The minified app's home, settings, setup, privacy/model information, unconfigured feedback/unchecked opt-in, companion list, expression preview, selected fictional preview and labelled fictional panel were manually viewed. Setup showed the prepared model and connected Accessibility; the actual ON control changed the home to active. The latest complete supported-host extraction → model → numeric graph → changed incoming-message result has **not yet been confirmed for this candidate**. Earlier user-observed live behavior is historical evidence, not a new final-release pass.

### 3. Permissions

Accessibility and overlay permission are granted in the primary test installation. The setup screen explicitly shows service connection separately from Android's enabled switch. Notifications are denied on the test phone; the foreground service and home OFF remain usable. Notification permission is optional, and the drawer OFF control requires it. Generated tests exercise the notification STOP action, rather than proving a physical drawer tap while permission is denied.

The foreground service starts its notification before allocating heavy model/overlay state. Startup failure, task removal, service destruction, lost model availability and overlay attach failure stop power and clear analysis. Permission/session gates and metadata transitions have fixture/source coverage.

Actual overlay and Accessibility revocation followed by restoration is **not verified on this final candidate**. Android rejected the disposable QA AppOps driver because shell UID 2000 lacks `MANAGE_APP_OPS_MODES`. This was an OS permission restriction. The attempted `permissionOnly` lane is not counted as a pass. Its helper requires the disposable QA package, prepared weights, initial overlay permission and a permitted external revocation driver; it checks overlay revocation only, not Accessibility re-grant. Manual recovery remains required.

### 4. Emotion Model

The pinned public RoBERTa GoEmotions INT8 model is **125,397,543 bytes**, SHA-256 `0c1981c5b479674747911c8e2228f0c4ec90bf47bf66e830f7d4fc62be082958`. The build checks size/hash and bundles it through AGP's generated-assets API. A first-use user action prepares a verified private copy offline. A genuine phone CDN/DNS failure discovered during fresh setup no longer blocks this primary path.

The compiled tokenizer is 800,924 bytes, SHA-256 `75cd4a321b71e40d862559cc715075e6a34ce59c290d99966ff52bd76844cdf1`; vocabulary and merges remain pinned. Independent token fixtures and real phone/JVM inference pass. Eight displayed labels map to the existing 28 model logits using independent sigmoid probabilities: **Neutral, Happy, Concerned, Confused, Sad, Frustrated, Angry, Surprised**. They are not a distribution required to sum to 100%.

The graph describes the latest fully visible incoming text. One incoming message can produce bars. Current state/direction additionally uses recent turns; direction needs two incoming turns and earlier outgoing context, and can correctly remain uncertain. The small context head uses 38 training, 14 validation and 28 held-out fictional examples. No classifier weights were retrained in this final QA pass. Synthetic agreement is not a real-world accuracy percentage. English, Hinglish, sarcasm, subtle conflict and intent remain fallible.

Bundled preparation, corrupt same-size model rejection/removal, real retry, valid restoration and real neutral inference pass in the isolated QA installation. Wrong context labels, non-finite parameters/features and expanded mention input boundaries are rejected. Ordinary file I/O failure preserves weights; only identifiable corrupt public weights are removed.

### 5. UI/UX

Carousel checks cover 1, 2, 5, 10, 20 and 50 items across densities 1, 1.5, 2, 2.625 and 3 and even/odd physical widths. A 51-item catalog retains at most seven rendered slots. Selection remains at the physical center, including half-pixel cases. Interruption, detach, cancelled/multiple-pointer gestures and boundary taps preserve the committed selection and hero artwork.

Own-app fixtures cover 320/390/430 dp and font scale 1.3. Home can scroll vertically when necessary. System bar, cutout and IME insets are applied to activity roots. Target API 36 hardware was not available; correct inset code and generated layouts are not a hardware substitute. The existing 2D artwork and eight expressions remain; missing drawable resources use a visible code-rendered fallback.

The analytics panel retains two short estimates and one graph. It scrolls rather than truncating essential summaries; the graph measures available width/font size and uses a stacked layout when narrow. The fictional panel was manually readable, including both estimates and all eight labels/percentages. It is explicitly a preview and is not counted as real analysis. Final live panel behavior and configured purchase/feedback branches remain unverified. Manual inspection found that the fictional preview ignored avatar selection; the fix passed a separate QA device regression and now visibly shows selected Astra in the final minified build.

### 6. Overlay

Generated own-screen integration passes actual foreground overlay appearance, tap versus drag, cancellation, popup dismissal/reopening, keyboard movement, saved-position restoration and OFF through home/STOP intent. Placement respects usable bounds, system-bar/cutout origins, IME and invalid saved coordinates. Outside-dismiss touch listeners do not consume message-input interaction.

Configured system/settings/permission/installer metadata hides the fallback companion; ordinary unsupported apps can retain it without analysis. A failed overlay attachment stops capture rather than continuing invisibly. Closing the running task, force-stopping, losing required permission or cold process start leaves OFF. Reboot and a long OEM background/battery soak were not physically exercised. The final minified live keyboard/drag/switching repeat remains a manual check.

### 7. Live Chat Pipeline

The supported adapter is pinned to `com.whatsapp`, **2.26.37.73 / code 263707322**, still installed on the phone. Only portrait, one-to-one, fully visible plain-text layouts are accepted. Quotes, reactions, documents/media, groups, unknown versions and ambiguous rows fail closed. Configured system-window metadata is observed before reading a root; only the selected supported root is traversed. Drafts, contact lists and unseen history are excluded.

At most eight turns of 1,000 UTF-16 characters each reach a 128-token bounded tokenizer. Mention expansion is bounded again without splitting surrogate pairs. Roles, clipping, duplicate fixtures, adapter version/package contracts, immutable windows, foreground interruption, current-conversation replacement and stopped/stale inference suppression pass generated device/JVM checks. Window-change metadata is processed immediately; body reads are debounced. First analysis uses a short 150 ms schedule and scroll changes a 300 ms bound, rather than requiring a long chat.

These tests establish actual model execution on app-generated text and the adapter's observed structural fixtures. They do **not** independently prove that the final installed minified release extracted the user's prepared fictional host messages and updated its bars. That human live check and repeated evaluator flow are pending. Identical repeated text/timestamps without stable host identifiers and same-title conversation collisions remain adapter limits.

### 8. Privacy

Normal conversation text is bounded phone memory only. The Android main-source audit found no raw `Log`, `System.out/err` or `printStackTrace` calls. Normal inference has no upload path. OFF, unsupported foreground, lock, invalid layout and conversation replacement clear relevant capture/results; an in-flight result can finish in memory but is discarded after stopping. Coordinates/preferences contain no messages. Public model preparation is the only new disk copy and contains public weights, not chat text.

Optional feedback is separately disclosed, opted in, bounded to ten minutes/64 visible turns/32 estimates, reviewed by the contributor, rated and sent by an explicit action. No production feedback endpoint is configured; no user feedback was collected. It is not covert learning from normal chats. Legacy purchase verification and missing-asset download have separate network paths. Debug USB inspection is release-disabled. No packet capture was performed, so network conclusions are code/fixture evidence rather than a measured traffic trace.

The tracked-file audit found no APK/AAB, private chats, local SDK paths, weights, signing keys or secrets committed. Historical mock harnesses and fictional preview text are labelled and do not feed production inference. Source archives exclude ignored build/runtime data. See [PRIVACY-POLICY.md](PRIVACY-POLICY.md) and [ANALYSIS-FEEDBACK.md](ANALYSIS-FEEDBACK.md).

### 9. Performance

Measured on the OnePlus 8T with generated conversations and the real model:

| Measurement | Result |
| --- | --- |
| Cold model + generated analysis | 692.57 ms |
| New incoming-text inference, eight samples | 23.36–28.09 ms; median 25.70 ms |
| Repeated unchanged window | About 2.22 ms median |
| Peak / settled process PSS | 236,071 / 203,731 KB (about 230.5 / 199 MiB) |
| Java used / native allocated at sample | 3,354,536 / 159,490,560 bytes |

These are model/test timings, **not measured complete live event-to-panel latency**. One inference worker, at most one pending window, bounded eight-entry score vectors, fixed input limits and 60-second idle model release cap work/memory growth. No whole-program asymptotic reduction or CPU/battery improvement percentage is claimed. Idle minified process PSS before ON was approximately 43 MB. The tiny cold-frame sample cannot establish animation FPS. No prolonged CPU, heap or battery soak was run.

Bundling removes phone network setup dependency but increases artifacts: debug APK 232,105,337 bytes, unsigned R8 APK 226,343,981 bytes, development-signed R8 review APK 226,361,108 bytes and review AAB 143,995,681 bytes. Exact hashes are recorded in the package manifest. The prepared private copy needs about 150 MB free space. Universal review APK size is not a Play Console compressed delivery-size measurement; no Console upload occurred.

### 10. Bugs Found

| BUG | SEVERITY | CAUSE | FIX | VERIFICATION |
| --- | --- | --- | --- | --- |
| Fresh model setup failed on the phone's public CDN DNS | P1 | First use depended on phone network delivery | Bundle size/hash-verified public weights; copy/check offline; keep friendly legacy retry | Fresh isolated install, offline bundled copy, corrupt/retry/real inference; checksum all three artifacts |
| Corrupt same-size weights left setup blocked | P1 | Size-only readiness could precede integrity failure | Identify hash/size corruption, discard only corrupt weights, expose preparation retry | Isolated modelRecoveryOnly and real restored inference |
| Home could show ON without a connected/running pipeline | P1 | Stored power was used as active-state evidence | Gate actual capabilities/service ownership; bounded start poll, failure/timeout OFF | powerOnly, consumerOnly, manual minified setup/ON |
| Service startup/destruction or failed overlay attach could retain active state | P1 | Owner publication and cleanup were incomplete | Foreground first; publish only success; stop capture/power and clean windows/handlers on failures | overlayOnly, lifecycle/foreground fixtures, source review |
| System-window changes could retain a stale fallback overlay | P1 | Metadata events omitted window-change coverage | Observe window-change metadata before selected-root/body reads; hide on configured sensitive/system screens | Foundation/session fixtures and overlay integration |
| Context input could accept invalid/non-finite metadata or grow after mention expansion | P1 | Incomplete validation after transformations | Exact labels/hash/finite thresholds/weights/features; re-cap text at 1,000 UTF-16 chars | ModelBoundaryTests, consumerOnly, complexOnly |
| Odd physical widths produced a half-pixel center error | P2 | Integer layout rounding | Apply fractional center correction | Catalog/density/width-parity carouselOnly fixtures |
| Interrupted/cancelled swipes could desynchronize selected hero and persistence | P2 | Pending animation/fling was not finalized consistently | Restore committed selection on detach/cancel; suppress cancelled momentum and duplicate callbacks | Carousel JVM and physical own-view gesture checks |
| Insets/IME or corrupt saved coordinates could move overlay outside usable space | P2 | Assumed zero origin/legacy geometry | Use window metrics/insets and clamp coordinates/keyboard/landscape fallbacks | OverlayMovementTests and own-window drag/keyboard checks |
| Compact analytics could truncate summaries/labels | P2 | Fixed summary/graph sizing | Scrollable panel and measured graph layout | AnalyticsChecks at constrained widths/font scales |
| Missing avatar resource could render an invisible character | P2 | Drawable failure suppressed fallback | Preserve code-rendered body fallback | Character/consumer fixture checks |
| Stage rendering allocated a new glow shader every draw | P3 | Shader created in onDraw | Cache by measured stage geometry | Compilation, lint and visual/source review |
| Fictional preview ignored the selected avatar | P2 | Secondary preview created default Alex views | Bind the persisted selected avatar to live/sample preview characters | New foundation assertion passes in isolated QA; manual final minified Astra preview |
| Privacy/model information described unconditional network download | P3 | Copy predated bundled assets | Describe bundled offline preparation and legacy missing-asset fallback | Rebuild/lint; manually viewed complete disclosure |

Test-fixture assumptions that allowed power without a real connected service were also corrected. An Android test helper's unsupported file-write API was replaced with byte writes. Failed attempts are retained as diagnostic history and are not counted as passed acceptance gates.

### 11. Remaining Limitations

- Final release live extraction/model/bar change, repeated full evaluator demo and fresh full grant-to-live workflow are unconfirmed. Real overlay/Accessibility revoke-and-restore and configured purchase/feedback branches remain incomplete.
- Only one pinned WhatsApp build and plain portrait one-to-one layout is supported. Other apps can show a movable character but are not analyzed. Not every sensitive third-party app is identified.
- No validated real-world accuracy metric, sarcasm/Hinglish coverage guarantee or reliable claim about a person's emotions/intent. Numeric probabilities and pilot direction can be uncertain or incorrect.
- No API 26/35/36 hardware matrix, actual 16 KB page-size device run, reboot, full airplane-mode live flow, packet capture, prolonged soak, measured animation FPS or battery profile.
- Bundled weights make APKs large and need private-copy storage. Legacy fallback phone CDN reachability failed and remains a fallback limitation; the primary artifact prepares offline.
- Owner production signing, Play account/declarations, public policy hosting, real store products/receipt verification and optional feedback-service deployment/tests remain publishing prerequisites. Review signing is not production release signing.
- Existing English localization/themed-icon and lower-priority lint/Gradle migration warnings remain. Historical backend/frontend checks were not rerun because those modules were unchanged.

### 12. Final Acceptance Result

**FAIL — RELEASE BLOCKERS REMAIN**

This is a tested review candidate, not an accepted production release. No additional confirmed P0/P1 source defect was found by the independent final source audit, but the required live acceptance evidence cannot be inferred from generated fixtures or compilation.

| Gate | Result and evidence boundary |
| --- | --- |
| 1 — Clean build | PASS; full clean Android command |
| 2 — App launch | PASS; isolated fresh OFF/default setup |
| 3 — Avatar selection | PASS; own-view device gestures |
| 4 — Center lock | PASS; physical-width/density parity checks |
| 5 — Scalability | PASS; up to 51 items/seven retained slots |
| 6 — Master toggle | PASS for generated integration and manual release ON; final live OFF/repeat pending |
| 7 — Overlay | PASS for actual own-screen overlay; final host observation pending |
| 8 — Live chat reading | OPEN; latest minified supported-host observation pending |
| 9 — Emotion analysis | PASS on real model/generated text; final extracted-host text proof OPEN |
| 10 — Label mapping | PASS; pinned assets, independent references and finite/bounds tests |
| 11 — Analytics | PASS on generated real-model graph; final host graph proof OPEN |
| 12 — Live update | OPEN; changed incoming fictional host text not yet confirmed |
| 13 — Permission recovery | PARTIAL; missing/failure fixtures pass, actual revoke/re-grant OPEN |
| 14 — Lifecycle | PASS for generated switching/stop/keyboard cases; full manual/reboot/soak not verified |
| 15 — Privacy | PASS for audited bounded local design/fixtures; packet capture not performed |
| 16 — No fake data | PASS; production uses verified ONNX, unavailable states do not invent scores |
| 17 — Demo | OPEN; repeated complete latest-release evaluator flow required |

Build/run instructions and the recommended fictional evaluator sequence are in [SETUP-AND-DEMO.md](SETUP-AND-DEMO.md). Machine-readable evidence is in [evidence/phase48.json](evidence/phase48.json). The Phase 48 handoff lists changed files and recovery prerequisites. Update this result only after the outstanding checks have actual recorded outcomes.
