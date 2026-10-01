# Compact character rendering

`Emotion` defines the eight presentation states. `CharacterView` renders an original 2D vector character inside a fixed 64×88dp frame, shared by the overlay and fictional preview. Hair, clothing, feet and grounding shadow remain consistent; eyebrows, eye aperture, mouth, head tilt and shoulders distinguish the states. The character is a visual estimate, never evidence of another person's feelings.

State changes interpolate from the currently displayed pose over 260ms, including when an earlier transition is interrupted. Android's disabled-animation preference skips interpolation. Detaching the view cancels animation and settles the final pose. No continuous background animation or layout growth occurs. Feet/shadow remain fixed across all expressions.

The own-app preview has a selector for actual transitions and a gallery using the production renderer at its normal dp size. Device checks compare rendered pixels for distinct states, verify transparent margins and fixed grounding, and exercise interrupted transitions plus detach cleanup. A device-rendered contact sheet is exported only by instrumentation; it contains generated character art and labels, no chat data.

Until Phase32 connects analysis, supported host overlays remain neutral. Preview selection does not modify a host chat or imply live analysis.
