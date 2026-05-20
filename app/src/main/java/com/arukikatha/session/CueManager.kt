package com.arukikatha.session

import android.media.AudioManager
import android.media.ToneGenerator
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator

interface CueManager {
    fun playBriskCue()
    fun playNormalCue()
    fun playResetCue()
    fun playCompletionCue()
    fun vibrateShort()
}

class AndroidCueManager(private val vibrator: Vibrator) : CueManager {

    private val tone = ToneGenerator(AudioManager.STREAM_ALARM, 100)

    override fun playBriskCue() {
        tone.startTone(ToneGenerator.TONE_CDMA_ALERT_CALL_GUARD, 300)
    }

    override fun playNormalCue() {
        tone.startTone(ToneGenerator.TONE_PROP_BEEP2, 300)
    }

    override fun playResetCue() {
        tone.startTone(ToneGenerator.TONE_PROP_BEEP, 200)
    }

    override fun playCompletionCue() {
        tone.startTone(ToneGenerator.TONE_CDMA_ABBR_ALERT, 500)
    }

    override fun vibrateShort() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(VibrationEffect.createOneShot(250, VibrationEffect.DEFAULT_AMPLITUDE))
        } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(250)
        }
    }
}
