package com.example.chinese2.pronunciation

import android.content.Context
import android.speech.tts.TextToSpeech
import java.util.Locale

/** System TTS with offline Chinese voice preference (zh-TW > zh-CN > zh). */
class TtsSpeaker(appContext: Context) {
    private var tts: TextToSpeech? = null
    var ready: Boolean = false
        private set

    init {
        tts = TextToSpeech(appContext.applicationContext) { status ->
            if (status == TextToSpeech.SUCCESS) {
                val t = tts ?: return@TextToSpeech
                val locales = listOf(Locale("zh", "TW"), Locale("zh", "CN"), Locale.CHINESE)
                for (loc in locales) {
                    val r = t.isLanguageAvailable(loc)
                    if (r >= TextToSpeech.LANG_AVAILABLE) {
                        t.language = loc
                        ready = true
                        break
                    }
                }
            }
        }
    }

    fun speak(text: String) {
        val t = tts ?: return
        if (text.length > TextToSpeech.getMaxSpeechInputLength()) return
        t.speak(text, TextToSpeech.QUEUE_FLUSH, null, "chinese2-utterance")
    }

    fun stop() {
        runCatching { tts?.stop() }
    }

    fun release() {
        runCatching { tts?.shutdown() }
        tts = null
    }
}
