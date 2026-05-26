package com.arukikatha.session

import android.os.SystemClock
import com.arukikatha.domain.ActiveSessionState
import com.arukikatha.domain.ArukikathaPhase
import com.arukikatha.domain.ArukikathaSessionEngine
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

object SessionOrchestrator {
    private const val TICK_MS = 100L
    private const val RESET_ANIMATION_HOLD_MS = 600L

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val engine = ArukikathaSessionEngine()
    private val _state = MutableStateFlow(engine.state())
    val state: StateFlow<ActiveSessionState> = _state.asStateFlow()

    private var tickerJob: Job? = null
    private var holdTicksUntilRealtimeMs: Long = 0L
    private var pauseStartRealtimeMs: Long? = null

    fun start() {
        holdTicksUntilRealtimeMs = 0L
        pauseStartRealtimeMs = null
        engine.start()
        _state.value = engine.state()
        ensureTicker()
    }

    fun pause() {
        val currentState = _state.value
        if (currentState.isRunning && !currentState.isPaused) {
            pauseStartRealtimeMs = SystemClock.elapsedRealtime()
            engine.pause(currentState.totalElapsedSec)
            _state.value = engine.state()
        }
    }

    fun resume() {
        val currentState = _state.value
        if (currentState.isRunning && currentState.isPaused) {
            val pauseStart = pauseStartRealtimeMs ?: SystemClock.elapsedRealtime()
            val pauseMs = (SystemClock.elapsedRealtime() - pauseStart).coerceAtLeast(0)
            val pauseSec = (pauseMs / 1000L).toInt()
            
            engine.resume(currentState.totalElapsedSec + pauseSec)
            _state.value = engine.state()
            
            pauseStartRealtimeMs = null
            // We don't set holdTicksUntilRealtimeMs here to ensure the timer
            // starts ticking immediately after a reset/resume, giving better feedback.
            ensureTicker()
        }
    }

    fun stop() {
        holdTicksUntilRealtimeMs = 0L
        pauseStartRealtimeMs = null
        engine.stop()
        _state.value = engine.state()
        tickerJob?.cancel()
        tickerJob = null
    }

    private fun ensureTicker() {
        if (tickerJob?.isActive == true) return
        tickerJob = scope.launch {
            while (true) {
                delay(TICK_MS)
                
                val now = SystemClock.elapsedRealtime()
                val remainingHoldMs = holdTicksUntilRealtimeMs - now
                if (remainingHoldMs > 0L) {
                    delay(remainingHoldMs)
                    continue
                }

                val before = engine.state()
                engine.tick(TICK_MS.toInt())
                val after = engine.state()
                _state.value = after

                if (after.phase != before.phase && after.phase != ArukikathaPhase.COMPLETED) {
                    holdTicksUntilRealtimeMs = SystemClock.elapsedRealtime() + RESET_ANIMATION_HOLD_MS
                }
                if (after.phase == ArukikathaPhase.COMPLETED || !after.isRunning) break
            }
        }
    }

    fun shutdown() {
        tickerJob?.cancel()
        scope.cancel()
    }
}
