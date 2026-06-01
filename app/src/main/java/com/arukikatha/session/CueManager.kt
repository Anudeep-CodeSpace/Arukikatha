package com.arukikatha.session

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.os.Build
import android.os.VibrationAttributes
import android.os.VibrationEffect
import android.os.Vibrator
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import java.util.Locale

interface CueManager {
    fun playBriskCue()
    fun playNormalCue()
    fun playPauseCue()
    fun playResumeCue()
    fun playTransitionCue()
    fun playResetCue()
    fun playCompletionCue()
    fun vibrateModeStart()
    fun vibrateCompletion()
    fun release()
}

class AndroidCueManager(
    context: Context,
    private val vibrator: Vibrator?
) : CueManager {

    private val audioManager = context.getSystemService(AudioManager::class.java)
    private val audioAttributes = AudioAttributes.Builder()
        .setUsage(AudioAttributes.USAGE_ASSISTANCE_NAVIGATION_GUIDANCE)
        .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
        .build()
    private val vibrationAudioAttributes = AudioAttributes.Builder()
        .setUsage(AudioAttributes.USAGE_ALARM)
        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
        .build()
    private val audioFocusRequest = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK)
        .setAudioAttributes(audioAttributes)
        .setOnAudioFocusChangeListener { }
        .build()

    private var isSpeechReady = false
    private val textToSpeech: TextToSpeech

    init {
        textToSpeech = TextToSpeech(context.applicationContext) { status ->
            isSpeechReady = status == TextToSpeech.SUCCESS
            if (isSpeechReady) {
                textToSpeech.language = Locale.US
                textToSpeech.setSpeechRate(0.95f)
                textToSpeech.setPitch(1.0f)
                textToSpeech.setAudioAttributes(audioAttributes)
                textToSpeech.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                    override fun onStart(utteranceId: String?) = Unit

                    override fun onDone(utteranceId: String?) {
                        audioManager.abandonAudioFocusRequest(audioFocusRequest)
                    }

                    @Deprecated("Deprecated by Android's TextToSpeech API.")
                    override fun onError(utteranceId: String?) {
                        audioManager.abandonAudioFocusRequest(audioFocusRequest)
                    }
                })
            }
        }
    }

    override fun playBriskCue() {
        speak("Brisk Mode")
    }

    override fun playNormalCue() {
        speak("Normal Mode")
    }

    override fun playPauseCue() {
        speak("Paused")
    }

    override fun playResumeCue() {
        speak("Resumed")
    }

    override fun playTransitionCue() {
        speak("Change")
    }

    override fun playResetCue() {
        speak("Round reset")
    }

    override fun playCompletionCue() {
        speak("Session complete")
    }

    override fun vibrateModeStart() {
        vibrate(
            timings = longArrayOf(0, 220, 100, 220),
            amplitudes = intArrayOf(0, 255, 0, 255)
        )
    }

    override fun vibrateCompletion() {
        vibrate(
            timings = longArrayOf(0, 140, 80, 140, 80, 320),
            amplitudes = intArrayOf(0, 220, 0, 220, 0, 255)
        )
    }

    private fun vibrate(timings: LongArray, amplitudes: IntArray) {
        vibrator?.let { v ->
            if (v.hasVibrator()) {
                val effect = VibrationEffect.createWaveform(timings, amplitudes, -1)
                v.cancel()
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    val attrs = VibrationAttributes.Builder()
                        .setUsage(VibrationAttributes.USAGE_ALARM)
                        .build()
                    v.vibrate(effect, attrs)
                } else {
                    @Suppress("DEPRECATION")
                    v.vibrate(effect, vibrationAudioAttributes)
                }
            }
        }
    }

    override fun release() {
        audioManager.abandonAudioFocusRequest(audioFocusRequest)
        textToSpeech.stop()
        textToSpeech.shutdown()
    }

    private fun speak(message: String) {
        if (!isSpeechReady) return
        audioManager.requestAudioFocus(audioFocusRequest)

        val params = android.os.Bundle().apply {
            putFloat(TextToSpeech.Engine.KEY_PARAM_VOLUME, 1.0f)
        }
        textToSpeech.speak(message, TextToSpeech.QUEUE_FLUSH, params, message)
    }
}
