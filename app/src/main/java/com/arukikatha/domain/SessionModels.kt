package com.arukikatha.domain

enum class ArukikathaPhase {
    BRISK,
    PAUSE_TO_NORMAL,
    NORMAL,
    PAUSE_TO_BRISK,
    COMPLETED
}

data class SessionConfig(
    val briskSeconds: Int = 180,
    val normalSeconds: Int = 180,
    val pauseSeconds: Int = 5,
    val longPauseResetThresholdSeconds: Int = 10,
    val targetSuccessfulMinutes: Int = 30
)

data class ActiveSessionState(
    val phase: ArukikathaPhase = ArukikathaPhase.BRISK,
    val phaseRemainingMs: Int = 180_000,
    val currentPhaseDurationMs: Int = 180_000,
    val phaseRemainingSec: Int = 180,
    val successfulMinutes: Int = 0,
    val completedBriskCount: Int = 0,
    val completedNormalCount: Int = 0,
    val isRunning: Boolean = false,
    val isPaused: Boolean = false,
    val pauseAgeSec: Int = 0,
    val resetsTriggered: Int = 0,
    val totalPauses: Int = 0,
    val lastResetMessage: String? = null,
    val resetMessageRemainingMs: Int = 0,
    val totalElapsedSec: Int = 0
)

object ProgressCalculator {
    fun successfulMinutes(briskCount: Int, normalCount: Int): Int {
        return (briskCount + normalCount) * 3
    }
}

class PausePolicy(private val thresholdSeconds: Int) {
    fun shouldReset(pauseDurationSec: Int): Boolean = pauseDurationSec > thresholdSeconds
}
