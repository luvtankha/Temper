# Phase 44 — Continuous analysis and movable companion

## 1. What changed

Keyboard/window events no longer end the selected conversation. Missing composer/root and temporarily unreadable text invalidate old estimates and queued work, retain the selected opaque identity and retry automatically: 400 ms initially, then every 1500 ms after the first 1500 ms. A readable snapshot resumes analysis without rearming, including an unchanged snapshot after suspension. The loaded on-device model stays available during that wait; obsolete callbacks cannot publish or enter optional feedback.

Same verified conversation identity can rebind a replacement application window. The existing header gate still runs before reading body text. A different verified conversation, another foreground application, lock, explicit pause/stop, revocation or service shutdown ends capture. An unsupported/non-chat screen inside WhatsApp can wait without reading messages and resume when the same selected chat returns.

The analysis companion now supports dragging across the visible chat area. Until moved, it follows its original composer anchor. After moving, its preferred normalized position is saved separately from the general floating companion, clamped above the composer/keyboard and restored when space returns. Drag, cancellation and multi-touch do not open analytics. Taps toggle the panel; panel placement supports top/bottom corners and sides. Generic floating gestures also receive cancellation handling.

## 2. Files changed or added

- Accessibility service, `LiveCaptureState`, `LayoutRecovery`, `LivePipeline`, new `ForegroundSessionPolicy`, native session/pipeline checks and three JVM continuity tests.
- `OverlayManager`, `FloatingOverlayService`, `PopupPlacement`, new `DragGesture`/`DraggablePlacement`, six JVM movement tests, native placement/popup checks and `OverlayInteractionChecks`.
- Native runner and debug-only fictional overlay screen; the latter now keeps its own visible screen awake during testing.
- Version 0.44.0, in-app instructions, README, privacy/release/architecture documentation and candidate packaging with Phase 44 documents.

## 3. Verification and build results

Debug APK, instrumentation APK and R8 unsigned review AAB build successfully. Android JVM tests: **28 passed, zero failures/errors/skips**, including actual desktop INT8 inference. Debug/release lint: zero errors, 32/31 existing warnings. The final default 0.44.0 APK is installed on the OnePlus 8T, Android 14/API 34. Eight native libraries pass 16 KB ELF alignment; APK ZIP alignment passes.

The default store and feedback origins remain empty. The model hash remains `0c1981c5b479674747911c8e2228f0c4ec90bf47bf66e830f7d4fc62be082958`. Backend/model/purchase/feedback implementations are unchanged; Phase 43's 78 backend tests remain the baseline and were not rerun for these Android-only changes.

## 4. Tests run

- Three new JVM checks: keyboard/overlay windows preserve the host, unknown roots wait while confirmed other foreground application/lock stops, and long unreadable gaps remain eligible for bounded-frequency recovery.
- Six new movement checks: saved preference and keyboard restoration, clamping across densities/origins, invalid geometry, tap/drag/cancel separation, popup corners, side placement and bounded fallback.
- Native foundation checks passed: selected identity/window rebinding, prolonged layout wait, identity mismatch, local/USB consent, pause/revocation, observed synthetic adapter/privacy/placement fixtures, app UI and Keystore. An old fixture initially assumed the user's local-analysis consent was absent; it now uses isolated preferences and explicitly rejects base-only consent for both modes.
- Native consumer checks passed: actual phone model on 25 generated English/Hinglish conversations, independent tokenizer references, speaker independence, four avatars × eight expressions and app flows. New actual worker check proves suspended callbacks are discarded, identical generated text resumes with numeric estimates without rearming, and explicit stop discards a later result. Fixture permissions are isolated.
- Native overlay check passed using TEMPER's own dummy screen and the production `OverlayManager` touch handlers/window updates: drag movement, no accidental popup, updates preserve position, simulated keyboard clamping/restoration, canceled touches, top-position popup fit and recreation from saved coordinates. Test coordinates are restored afterward. The first attempt was blocked by the sleeping phone; after the user unlocked it, the check passed. No host chat was inspected or private screenshot captured.

## 5. Known issues and limits

New continuity/drag behavior has synthetic native/device verification; a fresh user-driven end-to-end WhatsApp keyboard/drag check remains useful before broad release. The dummy overlay uses the same manager with `TYPE_APPLICATION_OVERLAY`; production uses the Accessibility overlay type. Android may hide overlays on secure screens. Automatic text analysis remains limited to the verified WhatsApp portrait layout and complete supported text turns. Unsupported text still has no numeric estimates; waiting does not bypass parsing or consent.

The model's English/Hinglish/sarcasm/direction limitations remain. No accuracy improvement, real feedback collection or model upgrade is claimed. Play products/signing/purchase tests and a real feedback server/domain remain pending. A testing APK and unsigned review AAB are not a signed production release.

## 6. Next prerequisites and artifacts

If Android disconnected or disabled TEMPER during update/consent tests, enable Use TEMPER again in Android Accessibility settings, open TEMPER and choose Analyze my next WhatsApp chat. Open a fictional supported chat. Close/open the keyboard and analytics panel, drag to either side, and confirm updates without restarting. Explicit pause/stop remains available in TEMPER and its companion notification.

Candidate output: `dist/TEMPER-0.44.0-testing.apk`, `dist/TEMPER-0.44.0-UNSIGNED-review.aab`, `dist/TEMPER-consumer-0.44.0.zip` and archive/distribution checksums. Historical Phase 41 visual/model evidence remains labeled, and the Phase 42 training smoke remains non-promotable. Current source and Phase 44 verification are included; weights, private runtime data and keys remain excluded.
