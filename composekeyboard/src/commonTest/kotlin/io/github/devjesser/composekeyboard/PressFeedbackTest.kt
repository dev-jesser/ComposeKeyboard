package io.github.devjesser.composekeyboard

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.ExperimentalTime
import kotlin.time.TestTimeSource

@OptIn(ExperimentalTime::class)
class PressFeedbackTest {
    private val time = TestTimeSource()
    private val feedback = PressFeedback(minVisible = 120.milliseconds, timeSource = time)

    @Test
    fun aTapReleasedImmediatelyStillHasToStayVisible() {
        val press = feedback.pressStarted()
        // Touch screens: down and up arrive together, so no time has passed.
        assertEquals(120.milliseconds, feedback.remainingVisibleTime(press))
    }

    @Test
    fun onlyTheMissingTimeIsWaited() {
        val press = feedback.pressStarted()
        time += 50.milliseconds
        assertEquals(70.milliseconds, feedback.remainingVisibleTime(press))
    }

    @Test
    fun aLongPressClearsImmediately() {
        val press = feedback.pressStarted()
        time += 500.milliseconds
        assertEquals(0.milliseconds, feedback.remainingVisibleTime(press))
        assertTrue(feedback.mayClear(press))
    }

    @Test
    fun anOlderPressNeverClearsANewerOne() {
        val first = feedback.pressStarted()
        time += 30.milliseconds
        val second = feedback.pressStarted()
        assertFalse(feedback.mayClear(first))
        assertEquals(0.milliseconds, feedback.remainingVisibleTime(first))
        assertTrue(feedback.mayClear(second))
        assertEquals(120.milliseconds, feedback.remainingVisibleTime(second))
    }
}
