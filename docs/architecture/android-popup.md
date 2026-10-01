# Compact analytics popup

The character owns a 64×88dp touch target entirely above the composer. Tapping it toggles a separate native accessibility window containing `AnalyticsPanel`: current-state text, direction text and one `SpectrumView` with eight rows. The panel is 280dp wide (clamped to the host viewport) and measures its content with the user's font scale. Pure `PopupPlacement` keeps it above the character, inside the viewport; insufficient room dismisses it rather than covering input.

Both windows are nonfocusable and nonmodal. Only their small rectangles receive touches. Outside touch dismisses the panel and normal host input remains available. A short dismissal guard prevents the same outside/character tap from immediately reopening it. Composer movement repositions the panel; departure, pause, revoke and service teardown remove both windows and clear presentation state. No host controls are clicked or messages sent.

`OverlaySummary` bounds the two texts to 64 characters and defensively copies exactly eight finite scores in [0,1]. Unavailable data always displays “Analysis unavailable”, “Uncertain”, and missing-value dashes; it never fabricates measured scores. The preview uses explicitly fictional values. Connecting real analysis remains Phase32; live graph updates remain Phase34.

The graph exposes all eight labels/values to accessibility services. Device tests verify the data boundary, panel composition, raised-composer placement and insufficient-room behavior. Own-app preview and a device-rendered panel image support visual verification without capturing host chat content.
