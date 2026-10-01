# Adapter build profiles

AdapterRegistry is an immutable pure registry of exact packageName/versionName/versionCode and ChatPlatformAdapter. Android service build verification now uses its production WhatsApp profile. A profile is rejected if its adapter does not support the package or an identical build key already exists. Unknown/mismatched builds resolve empty; no version guessing. The normalized VisibleConversation/ScreenObservation boundary stays platform independent.

Adding a future app needs a separately validated observation reader and composer/role/read plan, synthetic-text structural fixtures from an authorized test device, failure/clipping/draft/group tests, bounded dedup/session handling and an exact build profile. It also needs an explicit consent/package allowlist change, appropriate manifest query and a reviewed service routing path. Registry selection alone cannot broaden ConsentStore's WhatsApp-only permission or live processing policy. The current service still routes only WhatsApp and uses its specific approved text reader. No other app is shipped as supported.

RegistryChecks demonstrates an isolated example.test.chat adapter can be registered without changing WhatsApp selection; it is test-only and never observed on a device. Production contains one WhatsApp2.26.37.73 profile, tested on Android14/API34 OnePlus8T.
