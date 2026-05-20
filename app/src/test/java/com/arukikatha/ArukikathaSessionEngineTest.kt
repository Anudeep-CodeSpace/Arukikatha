package com.arukikatha

import com.arukikatha.domain.ArukikathaPhase
import com.arukikatha.domain.ArukikathaSessionEngine
import com.arukikatha.domain.SessionConfig
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ArukikathaSessionEngineTest {

    @Test
    fun pauseNineSeconds_resumesWithoutReset() {
        val engine = ArukikathaSessionEngine(SessionConfig(briskSeconds = 20, normalSeconds = 20, pauseSeconds = 5, targetSuccessfulMinutes = 30))
        engine.start()
        repeat(5) { engine.tick() }

        val beforePauseRemaining = engine.state().phaseRemainingSec
        val elapsedAtPause = engine.state().totalElapsedSec
        engine.pause(elapsedAtPause)
        engine.resume(elapsedAtPause + 9)

        assertFalse(engine.state().isPaused)
        assertEquals(beforePauseRemaining, engine.state().phaseRemainingSec)
        assertEquals(0, engine.state().resetsTriggered)
    }

    @Test
    fun pauseElevenSeconds_resetsCurrentMode() {
        val engine = ArukikathaSessionEngine(SessionConfig(briskSeconds = 20, normalSeconds = 20, pauseSeconds = 5, targetSuccessfulMinutes = 30))
        engine.start()
        repeat(7) { engine.tick() }

        val elapsedAtPause = engine.state().totalElapsedSec
        engine.pause(elapsedAtPause)
        engine.resume(elapsedAtPause + 11)

        assertEquals(20, engine.state().phaseRemainingSec)
        assertEquals(engine.state().currentPhaseDurationMs, engine.state().phaseRemainingMs)
        assertEquals(1, engine.state().resetsTriggered)
    }

    @Test
    fun phaseDurationMs_updatesAcrossTransitions() {
        val engine = ArukikathaSessionEngine(
            SessionConfig(briskSeconds = 3, normalSeconds = 4, pauseSeconds = 2, targetSuccessfulMinutes = 30)
        )
        engine.start()

        assertEquals(3_000, engine.state().currentPhaseDurationMs)

        repeat(3) { engine.tick() }
        assertEquals(ArukikathaPhase.PAUSE_TO_NORMAL, engine.state().phase)
        assertEquals(2_000, engine.state().currentPhaseDurationMs)

        repeat(2) { engine.tick() }
        assertEquals(ArukikathaPhase.NORMAL, engine.state().phase)
        assertEquals(4_000, engine.state().currentPhaseDurationMs)

        repeat(4) { engine.tick() }
        assertEquals(ArukikathaPhase.PAUSE_TO_BRISK, engine.state().phase)
        assertEquals(2_000, engine.state().currentPhaseDurationMs)
    }

    @Test
    fun successfulMinutes_onlyIncreaseOnCompletedWorkModes() {
        val engine = ArukikathaSessionEngine(SessionConfig(briskSeconds = 2, normalSeconds = 2, pauseSeconds = 1, targetSuccessfulMinutes = 30))
        engine.start()

        engine.tick()
        assertEquals(0, engine.state().successfulMinutes)

        engine.tick()
        assertEquals(3, engine.state().successfulMinutes)

        engine.tick()
        engine.tick()
        engine.tick()
        assertEquals(6, engine.state().successfulMinutes)
    }

    @Test
    fun reachesThirtySuccessfulMinutes_afterTenWorkModes() {
        val engine = ArukikathaSessionEngine(SessionConfig(briskSeconds = 1, normalSeconds = 1, pauseSeconds = 1, targetSuccessfulMinutes = 30))
        engine.start()

        while (engine.state().phase != ArukikathaPhase.COMPLETED) {
            engine.tick()
        }

        assertEquals(30, engine.state().successfulMinutes)
        assertFalse(engine.state().isRunning)
        assertTrue(engine.state().completedBriskCount + engine.state().completedNormalCount >= 10)
    }
}
