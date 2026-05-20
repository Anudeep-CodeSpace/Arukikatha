package com.arukikatha.session

import android.media.AudioManager
import android.media.ToneGenerator
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator

interface CueManager {
    fun playBriskCue()
    fun playNormalCue()
    fun playCompletionCue()
    fun vibrateShort()
}

class AndroidCueManager(private val vibrator: Vibrator) : CueManager {

    private val tone = ToneGenerator(AudioManager.STREAM_NOTIFICATION, 90)

    override fun playBriskCue() {
        tone.startTone(ToneGenerator.TONE_CDMA_ALERT_CALL_GUARD, 220)
    }

    override fun playNormalCue() {
        tone.startTone(ToneGenerator.TONE_PROP_BEEP2, 220)
    }

    override fun playCompletionCue() {
        tone.startTone(ToneGenerator.TONE_CDMA_ABBR_ALERT, 350)
    }

    override fun vibrateShort() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(VibrationEffect.createOneShot(180, VibrationEffect.DEFAULT_AMPLITUDE))
        } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(180)
        }
    }
}
