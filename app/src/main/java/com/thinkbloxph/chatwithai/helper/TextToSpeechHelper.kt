package com.thinkbloxph.chatwithai.helper

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.SoundPool
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import com.thinkbloxph.chatwithai.R
import java.util.*

class TextToSpeechHelper private constructor(private val context: Context) {

    companion object {
        private const val SOUND_PRIORITY = 1
        private const val SOUND_QUALITY = 100
        private const val MAX_STREAMS = 1
        private const val DEFAULT_SPEECH_RATE = 1.0f
        private val listeners = mutableListOf<TextToSpeechListener>()

        private lateinit var instance: TextToSpeechHelper

        fun initialize(context: Context) {
            instance = TextToSpeechHelper(context)
        }

        fun getInstance(): TextToSpeechHelper {
            return instance
        }
    }

    private var textToSpeech: TextToSpeech? = null
    private var soundPool: SoundPool? = null
    private var soundId: Int = 0

    init {
        // Initialize the TextToSpeech engine
        textToSpeech = TextToSpeech(context, TextToSpeech.OnInitListener { status ->
            if (status != TextToSpeech.ERROR) {
                // Set language and voice
                textToSpeech?.language = Locale.US
                textToSpeech?.voice = getFemaleVoice()

                // Set utterance progress listener
                textToSpeech?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                    override fun onStart(utteranceId: String?) {}

                    override fun onDone(utteranceId: String?) {
                        soundPool?.play(soundId, 1.0f, 1.0f, SOUND_PRIORITY, 0, 1.0f)
                        listeners.forEach { it.onSpeechDone() }
                    }

                    override fun onError(utteranceId: String?) {}
                })

                // Initialize SoundPool
                val audioAttributes = AudioAttributes.Builder()
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .setUsage(AudioAttributes.USAGE_ALARM)
                    .build()
                soundPool = SoundPool.Builder()
                    .setMaxStreams(MAX_STREAMS)
                    .setAudioAttributes(audioAttributes)
                    .build()
                soundId = soundPool!!.load(context, R.raw.notification, SOUND_PRIORITY)
            }
        })
    }

    fun addListener(listener: TextToSpeechListener) {
        listeners.add(listener)
    }

    fun speak(text: String) {
        val utteranceId = UUID.randomUUID().toString()
        textToSpeech?.setSpeechRate(DEFAULT_SPEECH_RATE)
        textToSpeech?.speak(text, TextToSpeech.QUEUE_FLUSH, null, utteranceId)
    }

    fun stop() {
        textToSpeech?.stop()
    }

    fun shutdown() {
        textToSpeech?.shutdown()
        soundPool?.release()
    }

    private fun getFemaleVoice(): android.speech.tts.Voice? {
        if (textToSpeech == null) {
            return null
        }

        val voices = textToSpeech!!.voices
        for (voice in voices) {
            if (voice.name == "en-us-x-sfg#female_1-local") {
                return voice
            }
        }
        // If no female voice is found, return the default voice
        return textToSpeech!!.voice ?: textToSpeech!!.defaultVoice
    }
}

interface TextToSpeechListener {
    fun onSpeechDone()
}
