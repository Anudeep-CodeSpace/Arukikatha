package com.arukikatha.session

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import com.arukikatha.MainActivity
import com.arukikatha.domain.ActiveSessionState
import com.arukikatha.domain.ArukikathaPhase

class SessionNotificationHelper(private val context: Context) {

    private val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    fun createChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Arukikatha Session",
            NotificationManager.IMPORTANCE_LOW
        )
        manager.createNotificationChannel(channel)
    }

    fun build(state: ActiveSessionState): Notification {
        val openIntent = PendingIntent.getActivity(
            context,
            0,
            Intent(context, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val pauseResumeAction = if (state.isPaused || !state.isRunning) {
            val startResumeAction = if (!state.isRunning) ArukikathaSessionService.ACTION_START else ArukikathaSessionService.ACTION_RESUME
            val resumeIntent = Intent(context, ArukikathaSessionService::class.java).apply {
                action = startResumeAction
            }
            val resumePendingIntent = PendingIntent.getService(
                context, 1, resumeIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            NotificationCompat.Action.Builder(
                com.arukikatha.R.drawable.ic_play_solid, 
                if (!state.isRunning) "Start" else "Resume", 
                resumePendingIntent
            ).build()
        } else {
            val pauseIntent = Intent(context, ArukikathaSessionService::class.java).apply {
                action = ArukikathaSessionService.ACTION_PAUSE
            }
            val pausePendingIntent = PendingIntent.getService(
                context, 2, pauseIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            NotificationCompat.Action.Builder(
                com.arukikatha.R.drawable.ic_pause_solid, "Pause", pausePendingIntent
            ).build()
        }

        val stopIntent = Intent(context, ArukikathaSessionService::class.java).apply {
            action = ArukikathaSessionService.ACTION_STOP
        }
        val stopPendingIntent = PendingIntent.getService(
            context, 3, stopIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val stopAction = NotificationCompat.Action.Builder(
            com.arukikatha.R.drawable.ic_stop_solid, "Stop", stopPendingIntent
        ).build()

        val timeStr = formatTime(state.phaseRemainingMs)
        val phaseLabel = when (state.phase) {
            ArukikathaPhase.BRISK -> "Brisk Walk"
            ArukikathaPhase.NORMAL -> "Normal Walk"
            ArukikathaPhase.PAUSE_TO_NORMAL, ArukikathaPhase.PAUSE_TO_BRISK -> "Breathing"
            ArukikathaPhase.COMPLETED -> "Finished"
        }
        val round = state.completedBriskCount + state.completedNormalCount + 1
        
        return NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_media_play)
            .setContentTitle("$phaseLabel • $timeStr")
            .setContentText("Round $round • ${state.successfulMinutes}/30 min successful")
            .setContentIntent(openIntent)
            .setOnlyAlertOnce(true)
            .setOngoing(true)
            .addAction(pauseResumeAction)
            .addAction(stopAction)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .build()
    }

    private fun formatTime(ms: Int): String {
        val totalSec = ms / 1000
        val min = totalSec / 60
        val sec = totalSec % 60
        return "%02d:%02d".format(min, sec)
    }

    companion object {
        const val CHANNEL_ID = "arukikatha_session"
        const val NOTIFICATION_ID = 1001
    }
}
