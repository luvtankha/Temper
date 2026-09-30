# Original avatar assets
Male Alex and female Nova are original repository-authored upper-body vectors, inspired by the user-supplied expression references. The PNG sheets are reference only; no proprietary avatar art is used.
Phase 03 created SVG placement art. Phase 04 converts it to native cubic Rive geometry using frontend/scripts/build-rig-source.mjs, with gradients, separate articulated facial/head/body groups and continuous reversible state-machine poses.
Source: assets/avatars/temper/scene.rml. Runtime: frontend/public/avatars/male.riv and female.riv. Both contain TemperMale and TemperFemale artboards; the renderer always selects the other participant. Each has one TemperEmotion machine and ten numeric inputs. See rive-rig-spec.md and Phase 04 handoff for reproducible build and genuine runtime evidence.

