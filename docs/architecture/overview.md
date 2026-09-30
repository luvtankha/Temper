# Architecture through Phase 03, Phase 04 preparation

React/Vite → typed fetch adapter → same-origin dev proxy → Java 21 Spring Boot modular monolith.

The backend common module provides health only. No database or AI is required at startup. Later domain, inference and persistence implementations must sit behind stable contracts; message delivery must remain independent from inference.

React’s Shell owns responsive navigation and collapsible insights. An app-level ChatProvider holds the current participant and messages across routes; a ChatApi interface isolates local mock storage. ChatPage renders bubbles, sender metadata, composer, simulated typing and selection. The other feature routes are placeholders, and analytical values are not fabricated.

RemoteAvatar resolves the other participant from local identity. Original neutral SVG previews are contained in the composer wrapper. Phase 04 preparation adds normalized semantic inputs and an optional lazy-loaded Rive renderer; configured files must expose all required numeric inputs. Runtime WASM is bundled locally. An invalid configured rig shows an error; an unconfigured rig keeps an explicitly labeled neutral preview.

The development-only `/avatar-lab` inspects manual authoring targets, not NLP outputs. See `rive-rig-spec.md`. Genuine character playback cannot be verified until original `.riv` files are exported. No model is integrated and no database schema exists.

The permanent UI must show the remote participant above its composer. Rive character rigs and continuous semantic emotion inputs are required at Phase 04; supplied PNG sheets are art references, not animation rigs.
