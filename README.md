# TEMPER — Android overlay companion 0.48.0

TEMPER shows a small movable 2D avatar over chat apps. On supported visible WhatsApp chats, it runs local emotion inference; tapping the avatar opens two short estimates and one eight-emotion bar graph. The home screen contains avatar selection and one ON/OFF switch. First use requires explicit consent, one-time offline model preparation and Android permissions.

The primary application is `android/`. The backend, React frontend and earlier messaging code remain development/history harnesses. A computer or backend is **not required** for normal on-phone analysis.

See [build, installation and evaluator demo instructions](docs/release/SETUP-AND-DEMO.md) and the [final verification report](docs/release/FINAL-VERIFICATION.md). Previous implementation evidence is in `docs/phases/`; model evaluation limits are in [CONTEXT-AND-PERFORMANCE.md](docs/release/CONTEXT-AND-PERFORMANCE.md).

Normal analysis keeps a bounded recent visible window in memory. It does not upload, save or log conversations. Verified public weights are bundled; first-use preparation is offline. Source builds may fetch pinned public weights from Hugging Face. A legacy missing-asset fallback can contact that host. Optional reviewed conversation feedback and legacy paid-avatar verification have separate network paths and explicit actions; no production service is configured in the review build. Consult [ANALYSIS-FEEDBACK.md](docs/release/ANALYSIS-FEEDBACK.md) before enabling feedback.

Support is limited to the verified WhatsApp adapter. Other apps can show the movable character, but their content is not analyzed. Unsupported layouts display unavailable/waiting states. Estimates are not evidence of feelings or intent. English/Roman-script Hinglish regression checks are synthetic and do not establish real-world accuracy.

Review APKs use a development signature. Publishing requires the owner's upload key, Play account, applicable declarations and production configuration. No app was published in this verification pass.
