package com.corbymaupin.jobsitespanish.tts

import android.content.Context
import android.speech.tts.TextToSpeech
import android.util.Log
import java.util.Locale

/**
 * Thin Android TextToSpeech wrapper for Spanish (and English) utterances.
 * Init / missing locales must never crash the process.
 */
class Speech(context: Context) : TextToSpeech.OnInitListener {

    private var tts: TextToSpeech? = try {
        TextToSpeech(context.applicationContext, this)
    } catch (t: Throwable) {
        Log.w(TAG, "TTS construct failed", t)
        null
    }
    @Volatile private var ready: Boolean = false

    override fun onInit(status: Int) {
        ready = status == TextToSpeech.SUCCESS
        if (!ready) return
        // Prefer MX (jobsite dialect), then ES, then any Spanish — never throw.
        try {
            val engine = tts ?: return
            val candidates = listOf(
                Locale("es", "MX"),
                Locale("es", "ES"),
                Locale("es"),
                Locale.US
            )
            for (locale in candidates) {
                val result = engine.setLanguage(locale)
                if (result != TextToSpeech.LANG_MISSING_DATA &&
                    result != TextToSpeech.LANG_NOT_SUPPORTED
                ) {
                    return
                }
            }
            ready = false
        } catch (t: Throwable) {
            Log.w(TAG, "TTS language setup failed", t)
            ready = false
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
        try {
            val result = engine.setLanguage(locale)
            if (result == TextToSpeech.LANG_MISSING_DATA ||
                result == TextToSpeech.LANG_NOT_SUPPORTED
            ) {
                // Fall back to whatever language was set in onInit
            }
            engine.speak(text, TextToSpeech.QUEUE_FLUSH, null, "jobsite-${text.hashCode()}")
        } catch (t: Throwable) {
            Log.w(TAG, "speak failed", t)
        }
    }

    fun stop() {
        try {
            tts?.stop()
        } catch (_: Throwable) {
        }
    }

    fun shutdown() {
        try {
            tts?.stop()
            tts?.shutdown()
        } catch (_: Throwable) {
        }
        tts = null
        ready = false
    }

    companion object {
        private const val TAG = "JobsiteSpeech"
    }
}
