package com.corbymaupin.jobsitespanish.tts

import android.content.Context
import android.speech.tts.TextToSpeech
import java.util.Locale

/**
 * Thin Android TextToSpeech wrapper for Spanish (and English) utterances.
 */
class Speech(context: Context) : TextToSpeech.OnInitListener {

    private var tts: TextToSpeech? = TextToSpeech(context.applicationContext, this)
    @Volatile private var ready: Boolean = false

    override fun onInit(status: Int) {
        ready = status == TextToSpeech.SUCCESS
        if (ready) {
            tts?.language = Locale("es", "ES")
        }
    }

    fun speakSpanish(text: String) {
        speak(text, Locale("es", "MX"))
    }

    fun speakEnglish(text: String) {
        speak(text, Locale.US)
    }

    fun speak(text: String, locale: Locale) {
        val engine = tts ?: return
        if (!ready) return
        engine.language = locale
        engine.speak(text, TextToSpeech.QUEUE_FLUSH, null, "jobsite-${text.hashCode()}")
    }

    fun stop() {
        tts?.stop()
    }

    fun shutdown() {
        tts?.stop()
        tts?.shutdown()
        tts = null
        ready = false
    }
}
