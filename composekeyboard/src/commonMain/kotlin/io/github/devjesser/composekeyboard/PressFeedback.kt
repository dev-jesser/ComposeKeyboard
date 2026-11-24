package io.github.devjesser.composekeyboard

import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.TimeMark
import kotlin.time.TimeSource

/**
 * Keeps a key's "pressed" look on screen for a minimum time.
 *
 * On a touch screen the OS can deliver a tap as mouse-down and mouse-up back to back, so a
 * highlight that lasts exactly as long as the press may be set and cleared within one frame and
 * never get drawn. This tracks the latest press of one key so the highlight can be held a little
 * longer, and so an older press never clears the highlight of a newer one (fast double taps).
 */
internal class PressFeedback(
    private val minVisible: Duration = DefaultMinVisible,
    private val timeSource: TimeSource = TimeSource.Monotonic,
) {
    private var latest = 0
    private var latestMark: TimeMark = timeSource.markNow()

    /** Call when a press starts. Returns a token to pass to the other functions. */
    fun pressStarted(): Int {
        latest++
        latestMark = timeSource.markNow()
        return latest
    }

    /** How much longer the pressed look must stay up for [press]; zero if it can clear now. */
    fun remainingVisibleTime(press: Int): Duration =
        if (press == latest) (minVisible - latestMark.elapsedNow()).coerceAtLeast(Duration.ZERO) else Duration.ZERO

    /** False when a newer press has started, which then owns the highlight. */
    fun mayClear(press: Int): Boolean = press == latest

    companion object {
        val DefaultMinVisible: Duration = 120.milliseconds
    }
}
