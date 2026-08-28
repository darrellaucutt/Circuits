package net.aucutt.circuits.wear.feedback

import android.content.Context
import android.speech.tts.TextToSpeech
import java.util.Locale
import java.util.concurrent.atomic.AtomicBoolean

class TtsSpeaker(context: Context) : TextToSpeech.OnInitListener {

    private val ready = AtomicBoolean(false)
    private val pending = mutableListOf<String>()
    private val tts = TextToSpeech(context.applicationContext, this)

    override fun onInit(status: Int) {
        if (status != TextToSpeech.SUCCESS) return
        tts.language = Locale.getDefault()
        tts.setPitch(0.72f)
        tts.setSpeechRate(0.92f)
        ready.set(true)
        synchronized(pending) {
            pending.forEach { tts.speak(it, TextToSpeech.QUEUE_FLUSH, null, it.hashCode().toString()) }
            pending.clear()
        }
    }

    fun speak(text: String) {
        if (text.isBlank()) return
        if (!ready.get()) {
            synchronized(pending) { pending += text }
            return
        }
        tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, text.hashCode().toString())
    }

    fun shutdown() {
        ready.set(false)
        tts.shutdown()
    }
}
