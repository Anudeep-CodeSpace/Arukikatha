package com.arukikatha.session

import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.os.Vibrator
import com.arukikatha.domain.ArukikathaPhase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class ArukikathaSessionService : Service() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var observerJob: Job? = null

    private lateinit var notificationHelper: SessionNotificationHelper
    private lateinit var cueManager: CueManager

    private var previousPhase: ArukikathaPhase = ArukikathaPhase.BRISK

    override fun onCreate() {
        super.onCreate()
        notificationHelper = SessionNotificationHelper(this)
        val vibrator = getSystemService(VIBRATOR_SERVICE) as Vibrator
        cueManager = AndroidCueManager(vibrator)
        notificationHelper.createChannel()
        observeSessionState()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            "com.arukikatha.INIT" -> {
                // Just starts the service to show notification
            }
            ACTION_START -> SessionOrchestrator.start()
            ACTION_PAUSE -> SessionOrchestrator.pause()
            ACTION_RESUME -> SessionOrchestrator.resume()
            ACTION_STOP -> SessionOrchestrator.stop()
        }
        return START_STICKY
    }

    private fun observeSessionState() {
        observerJob?.cancel()
        observerJob = serviceScope.launch {
            SessionOrchestrator.state.collectLatest { state ->
                startForeground(SessionNotificationHelper.NOTIFICATION_ID, notificationHelper.build(state))

                if (state.phase != previousPhase) {
                    when (state.phase) {
                        ArukikathaPhase.BRISK -> {
                            cueManager.playBriskCue()
                            cueManager.vibrateShort()
                        }

                        ArukikathaPhase.NORMAL -> {
                            cueManager.playNormalCue()
                            cueManager.vibrateShort()
                        }

                        ArukikathaPhase.COMPLETED -> {
                            cueManager.playCompletionCue()
                            cueManager.vibrateShort()
                        }

                        else -> Unit
                    }
                    previousPhase = state.phase
                }
            }
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        observerJob?.cancel()
        serviceScope.cancel()
        super.onDestroy()
    }

    companion object {
        const val ACTION_START = "com.arukikatha.START"
        const val ACTION_PAUSE = "com.arukikatha.PAUSE"
        const val ACTION_RESUME = "com.arukikatha.RESUME"
        const val ACTION_STOP = "com.arukikatha.STOP"
    }
}
