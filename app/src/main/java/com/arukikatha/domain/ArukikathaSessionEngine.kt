package com.arukikatha.domain

class ArukikathaSessionEngine(
    private val config: SessionConfig = SessionConfig()
) {
    private val pausePolicy = PausePolicy(config.longPauseResetThresholdSeconds)

    private var state = ActiveSessionState(
        phaseRemainingMs = config.briskSeconds * 1000,
        currentPhaseDurationMs = config.briskSeconds * 1000,
        phaseRemainingSec = config.briskSeconds
    )
    private var pausedAtSec: Int? = null
    private var elapsedMsAccumulator: Int = 0

    fun state(): ActiveSessionState = state

    fun start() {
        if (state.phase == ArukikathaPhase.COMPLETED) {
            stop()
        }
        state = state.copy(
            isRunning = true,
            isPaused = false,
            pauseAgeSec = 0,
            lastResetMessage = null,
            resetMessageRemainingMs = 0
        )
    }

    fun stop() {
        state = ActiveSessionState(
            phaseRemainingMs = config.briskSeconds * 1000,
            currentPhaseDurationMs = config.briskSeconds * 1000,
            phaseRemainingSec = config.briskSeconds
        )
        pausedAtSec = null
        elapsedMsAccumulator = 0
    }

    fun pause(nowElapsedSec: Int) {
        if (!state.isRunning || state.isPaused || state.phase == ArukikathaPhase.COMPLETED) return
        pausedAtSec = nowElapsedSec
        state = state.copy(
            isPaused = true,
            totalPauses = state.totalPauses + 1
        )
    }

    fun resume(nowElapsedSec: Int) {
        if (!state.isRunning || !state.isPaused || state.phase == ArukikathaPhase.COMPLETED) return
        val pauseStart = pausedAtSec ?: nowElapsedSec
        val pauseDuration = nowElapsedSec - pauseStart
        val shouldReset = pausePolicy.shouldReset(pauseDuration)
        state = if (shouldReset) {
            val ms = phaseDurationMs(state.phase)
            state.copy(
                isPaused = false,
                pauseAgeSec = 0,
                phaseRemainingMs = ms,
                currentPhaseDurationMs = ms,
                phaseRemainingSec = ms / 1000,
                resetsTriggered = state.resetsTriggered + 1,
                lastResetMessage = "Fresh start for this round",
                resetMessageRemainingMs = 3000
            )
        } else {
            state.copy(
                isPaused = false,
                pauseAgeSec = 0,
                lastResetMessage = null,
                resetMessageRemainingMs = 0
            )
        }
        pausedAtSec = null
    }

    fun tick(deltaMs: Int = 1000) {
        if (!state.isRunning || state.isPaused || state.phase == ArukikathaPhase.COMPLETED) return

        val remainingMs = (state.phaseRemainingMs - deltaMs).coerceAtLeast(0)
        elapsedMsAccumulator += deltaMs
        val elapsedSecIncrement = elapsedMsAccumulator / 1000
        elapsedMsAccumulator %= 1000

        val messageRemaining = (state.resetMessageRemainingMs - deltaMs).coerceAtLeast(0)
        val message = if (messageRemaining > 0) state.lastResetMessage else null

        var next = state.copy(
            phaseRemainingMs = remainingMs,
            phaseRemainingSec = (remainingMs + 999) / 1000,
            totalElapsedSec = state.totalElapsedSec + elapsedSecIncrement,
            lastResetMessage = message,
            resetMessageRemainingMs = messageRemaining
        )

        if (remainingMs == 0) {
            next = transition(next)
        }

        state = next
    }

    private fun transition(current: ActiveSessionState): ActiveSessionState {
        return when (current.phase) {
            ArukikathaPhase.BRISK -> {
                val briskCount = current.completedBriskCount + 1
                val successfulMinutes = ProgressCalculator.successfulMinutes(briskCount, current.completedNormalCount)
                if (successfulMinutes >= config.targetSuccessfulMinutes) {
                    current.copy(
                        phase = ArukikathaPhase.COMPLETED,
                        phaseRemainingMs = 0,
                        currentPhaseDurationMs = 0,
                        phaseRemainingSec = 0,
                        isRunning = false,
                        successfulMinutes = successfulMinutes,
                        completedBriskCount = briskCount,
                        lastResetMessage = null,
                        resetMessageRemainingMs = 0
                    )
                } else {
                    val ms = config.pauseSeconds * 1000
                    current.copy(
                        phase = ArukikathaPhase.PAUSE_TO_NORMAL,
                        phaseRemainingMs = ms,
                        currentPhaseDurationMs = ms,
                        phaseRemainingSec = config.pauseSeconds,
                        successfulMinutes = successfulMinutes,
                        completedBriskCount = briskCount
                    )
                }
            }

            ArukikathaPhase.PAUSE_TO_NORMAL -> {
                val ms = config.normalSeconds * 1000
                current.copy(
                    phase = ArukikathaPhase.NORMAL,
                    phaseRemainingMs = ms,
                    currentPhaseDurationMs = ms,
                    phaseRemainingSec = config.normalSeconds
                )
            }

            ArukikathaPhase.NORMAL -> {
                val normalCount = current.completedNormalCount + 1
                val successfulMinutes = ProgressCalculator.successfulMinutes(current.completedBriskCount, normalCount)
                if (successfulMinutes >= config.targetSuccessfulMinutes) {
                    current.copy(
                        phase = ArukikathaPhase.COMPLETED,
                        phaseRemainingMs = 0,
                        currentPhaseDurationMs = 0,
                        phaseRemainingSec = 0,
                        isRunning = false,
                        successfulMinutes = successfulMinutes,
                        completedNormalCount = normalCount,
                        lastResetMessage = null,
                        resetMessageRemainingMs = 0
                    )
                } else {
                    val ms = config.pauseSeconds * 1000
                    current.copy(
                        phase = ArukikathaPhase.PAUSE_TO_BRISK,
                        phaseRemainingMs = ms,
                        currentPhaseDurationMs = ms,
                        phaseRemainingSec = config.pauseSeconds,
                        successfulMinutes = successfulMinutes,
                        completedNormalCount = normalCount
                    )
                }
            }

            ArukikathaPhase.PAUSE_TO_BRISK -> {
                val ms = config.briskSeconds * 1000
                current.copy(
                    phase = ArukikathaPhase.BRISK,
                    phaseRemainingMs = ms,
                    currentPhaseDurationMs = ms,
                    phaseRemainingSec = config.briskSeconds
                )
            }

            ArukikathaPhase.COMPLETED -> current
        }
    }

    private fun phaseDurationMs(phase: ArukikathaPhase): Int {
        return when (phase) {
            ArukikathaPhase.BRISK -> config.briskSeconds * 1000
            ArukikathaPhase.NORMAL -> config.normalSeconds * 1000
            ArukikathaPhase.PAUSE_TO_NORMAL,
            ArukikathaPhase.PAUSE_TO_BRISK -> config.pauseSeconds * 1000
            ArukikathaPhase.COMPLETED -> 0
        }
    }
}
