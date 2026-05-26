package com.arukikatha.session

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.os.VibrationEffect
import android.os.Vibrator
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import java.util.Locale

interface CueManager {
    fun playBriskCue()
    fun playNormalCue()
    fun playResetCue()
    fun playCompletionCue()
    fun vibrateShort()
    fun release()
}

class AndroidCueManager(
    context: Context,
    private val vibrator: Vibrator
) : CueManager {

    private val audioManager = context.getSystemService(AudioManager::class.java)
    private val audioAttributes = AudioAttributes.Builder()
        .setUsage(AudioAttributes.USAGE_ASSISTANCE_NAVIGATION_GUIDANCE)
        .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
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

    override fun playResetCue() {
        speak("Change")
    }

    override fun playCompletionCue() {
        speak("Session complete")
    }

    override fun vibrateShort() {
        vibrator.vibrate(VibrationEffect.createOneShot(250, VibrationEffect.DEFAULT_AMPLITUDE))
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
