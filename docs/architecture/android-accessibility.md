# Accessibility consent — detection-only build
Open TEMPER → Accessibility and consent. Read the disclosure, check the opt-in and press Save consent. Acceptance leaves TEMPER paused. Open Android Accessibility settings, select TEMPER under downloaded/installed services and enable it yourself. Return to TEMPER and press Resume TEMPER. Open WhatsApp, return to TEMPER and press Refresh status to see whether a supported package event was recently received. This status does not mean a conversation was parsed.

Pause stops detection immediately. Revoke consent and disable removes consent, restores pause and requests Android to disable the connected service. The system settings toggle is independent: if it remains enabled, turn it off in Android Accessibility settings. A service connected without current consent disables itself. Sign out also pauses TEMPER.

Version1 consent permits package detection only. The service is protected by BIND_ACCESSIBILITY_SERVICE, filtered to com.whatsapp window/content events and does not traverse nodes, read event text, log chats, perform gestures, click or send. Its window-content capability is declared for future adapters but unused here. It records only an in-memory recent supported-event timestamp. No Internet permission or overlay drawing exists in this build. Future live text extraction/server transmission requires a new disclosure and consent version.

Tests exercise default/stale consent, pause, exact supported-package boundaries, UI opt-in and revoke, plus existing account/foundation checks. Real system enable/disable and actual WhatsApp detection require manual user activation on the test phone; simulated gate tests are not evidence of real service delivery.

Platform references: [AccessibilityService](https://developer.android.com/reference/android/accessibilityservice/AccessibilityService), [AccessibilityServiceInfo](https://developer.android.com/reference/android/accessibilityservice/AccessibilityServiceInfo).
