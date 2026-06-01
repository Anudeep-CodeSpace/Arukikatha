package com.arukikatha.session

import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import android.os.SystemClock
import android.os.Vibrator
import android.os.VibratorManager
import com.arukikatha.domain.ActiveSessionState
import com.arukikatha.domain.ArukikathaPhase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

class ArukikathaSessionService : Service() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var observerJob: Job? = null

    private lateinit var notificationHelper: SessionNotificationHelper
    private lateinit var cueManager: CueManager
    private var wakeLock: PowerManager.WakeLock? = null

    private var previousPhase: ArukikathaPhase? = null
    private var previousResetCount: Int = 0
    private var lastNotificationSnapshot: NotificationSnapshot? = null
    private var lastNotificationUpdateRealtimeMs: Long = 0L

    override fun onCreate() {
        super.onCreate()
        notificationHelper = SessionNotificationHelper(this)
        
        val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager = getSystemService(VibratorManager::class.java)
            vibratorManager?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            getSystemService(Vibrator::class.java)
        }

        cueManager = AndroidCueManager(this, vibrator)
        notificationHelper.createChannel()
        observeSessionState()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            "com.arukikatha.INIT" -> {
                // Just starts the service to show notification
            }
            ACTION_START -> {
                previousPhase = null
                previousResetCount = 0
                SessionOrchestrator.start()
            }
            ACTION_PAUSE -> pauseSessionWithCue()
            ACTION_RESUME -> resumeSessionWithCue()
            ACTION_STOP -> {
                previousPhase = null
                previousResetCount = 0
                SessionOrchestrator.stop()
            }
        }
        return START_STICKY
    }

    private fun observeSessionState() {
        observerJob?.cancel()
        observerJob = serviceScope.launch {
            SessionOrchestrator.state.collect { state ->
                if (shouldUpdateNotification(state)) {
                    val notification = notificationHelper.build(state)
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        startForeground(
                            SessionNotificationHelper.NOTIFICATION_ID,
                            notification,
                            ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK
                        )
                    } else {
                        startForeground(SessionNotificationHelper.NOTIFICATION_ID, notification)
                    }
                }

                // Manage WakeLock to keep timer running while screen is off
                if (state.isRunning && !state.isPaused) {
                    acquireWakeLock()
                } else {
                    releaseWakeLock()
                }

                val resetTriggered = state.resetsTriggered > previousResetCount
                val isPhaseChange = state.phase != previousPhase || resetTriggered
                val shouldPlayCue = isPhaseChange &&
                    ((!state.isPaused && state.isRunning) || state.phase == ArukikathaPhase.COMPLETED)

                if (shouldPlayCue) {
                    playCueForPhase(state.phase)
                    previousPhase = state.phase
                    previousResetCount = state.resetsTriggered
                }

                if (!state.isRunning && state.phase != ArukikathaPhase.COMPLETED) {
                    previousPhase = null
                    previousResetCount = state.resetsTriggered
                }
            }
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        observerJob?.cancel()
        serviceScope.cancel()
        cueManager.release()
        releaseWakeLock()
        super.onDestroy()
    }

    private fun playCueForPhase(phase: ArukikathaPhase) {
        when (phase) {
            ArukikathaPhase.BRISK -> playModeStartCue { cueManager.playBriskCue() }
            ArukikathaPhase.NORMAL -> playModeStartCue { cueManager.playNormalCue() }
            ArukikathaPhase.PAUSE_TO_NORMAL,
            ArukikathaPhase.PAUSE_TO_BRISK -> cueManager.playTransitionCue()

            ArukikathaPhase.COMPLETED -> playCompletionCue()
        }
    }

    private fun pauseSessionWithCue() {
        val before = SessionOrchestrator.state.value
        if (!before.isRunning || before.isPaused || before.phase == ArukikathaPhase.COMPLETED) return

        SessionOrchestrator.pause()
        cueManager.playPauseCue()
    }

    private fun resumeSessionWithCue() {
        val before = SessionOrchestrator.state.value
        if (!before.isRunning || !before.isPaused || before.phase == ArukikathaPhase.COMPLETED) return

        SessionOrchestrator.resume()
        val after = SessionOrchestrator.state.value
        val resetTriggered = after.resetsTriggered > before.resetsTriggered

        previousPhase = after.phase
        previousResetCount = after.resetsTriggered

        if (resetTriggered) {
            cueManager.playResetCue()
        } else {
            cueManager.playResumeCue()
        }
    }

    private fun playModeStartCue(playAudioCue: () -> Unit) {
        cueManager.vibrateModeStart()
        playAudioCue()
    }

    private fun playCompletionCue() {
        cueManager.vibrateCompletion()
        cueManager.playCompletionCue()
    }

    private fun shouldUpdateNotification(state: ActiveSessionState): Boolean {
        val snapshot = NotificationSnapshot(
            phase = state.phase,
            phaseRemainingSec = state.phaseRemainingSec,
            successfulMinutes = state.successfulMinutes,
            completedBriskCount = state.completedBriskCount,
            completedNormalCount = state.completedNormalCount,
            isRunning = state.isRunning,
            isPaused = state.isPaused
        )
        val now = SystemClock.elapsedRealtime()
        val contentChanged = snapshot != lastNotificationSnapshot
        val maxRefreshElapsed = now - lastNotificationUpdateRealtimeMs >= NOTIFICATION_MAX_REFRESH_MS

        return if (contentChanged || maxRefreshElapsed) {
            lastNotificationSnapshot = snapshot
            lastNotificationUpdateRealtimeMs = now
            true
        } else {
            false
        }
    }

    private fun acquireWakeLock() {
        if (wakeLock?.isHeld == true) return

        val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
        val activeWakeLock = wakeLock
            ?: powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "Arukikatha::SessionLock")
                .also { wakeLock = it }

        activeWakeLock.acquire(WAKE_LOCK_TIMEOUT_MS)
    }

    private fun releaseWakeLock() {
        wakeLock?.let {
            if (it.isHeld) {
                it.release()
            }
        }
        wakeLock = null
    }

    companion object {
        private const val WAKE_LOCK_TIMEOUT_MS = 45 * 60 * 1000L
        private const val NOTIFICATION_MAX_REFRESH_MS = 1_000L

        const val ACTION_START = "com.arukikatha.START"
        const val ACTION_PAUSE = "com.arukikatha.PAUSE"
        const val ACTION_RESUME = "com.arukikatha.RESUME"
        const val ACTION_STOP = "com.arukikatha.STOP"
    }
}

private data class NotificationSnapshot(
    val phase: ArukikathaPhase,
    val phaseRemainingSec: Int,
    val successfulMinutes: Int,
    val completedBriskCount: Int,
    val completedNormalCount: Int,
    val isRunning: Boolean,
    val isPaused: Boolean
)
