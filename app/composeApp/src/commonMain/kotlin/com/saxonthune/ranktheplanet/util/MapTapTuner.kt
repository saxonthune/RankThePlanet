package com.saxonthune.ranktheplanet.util

/**
 * Strip MLNMapView of every gesture recognizer that forces the maplibre-compose single-tap
 * to wait — multi-tap and multi-finger UITapGestureRecognizers, MLN's annotation-tap, and
 * MLN's quick-zoom long-press. Without this, every pin tap on iOS waits ~300–400ms for the
 * iOS double-tap arbitration window to expire before our tap handler runs. Pinch, pan, and
 * rotation are unaffected; users lose double-tap-to-zoom and two-finger-tap-to-zoom-out.
 *
 * Returns true once at least one map view has been tuned. Safe to call repeatedly.
 */
expect fun tuneMapForFastTaps(): Boolean

/**
 * Diagnostic probe: attach a passive touch-down listener to the MLNMapView that logs
 * the gap from finger-down to .Ended, and dumps the recognizer list at tap-time so
 * post-tune mutations are visible. Returns true once a probe has been installed.
 */
expect fun installMapTapProbe(): Boolean
