package net.aucutt.circuits.tts

import android.content.Context
import android.speech.tts.TextToSpeech
import android.util.Log
import java.util.Locale
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Thin wrapper around Android's built-in [TextToSpeech] engine.
 */
class TtsSpeaker(context: Context) : TextToSpeech.OnInitListener {

    private val ready = AtomicBoolean(false)
    private val pendingUtterances = mutableListOf<String>()
    private val tts: TextToSpeech = TextToSpeech(context.applicationContext, this)

    override fun onInit(status: Int) {
        if (status != TextToSpeech.SUCCESS) {
            Log.w(TAG, "TextToSpeech init failed with status=$status")
            return
        }
        val result = tts.setLanguage(Locale.getDefault())
        if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
            Log.w(TAG, "Default locale not supported; falling back to US English")
            tts.language = Locale.US
        }
        ready.set(true)
        synchronized(pendingUtterances) {
            pendingUtterances.forEach { enqueue(it) }
            pendingUtterances.clear()
        }
    }

    fun speak(text: String) {
        if (text.isBlank()) return
        if (!ready.get()) {
            synchronized(pendingUtterances) {
                pendingUtterances += text
            }
            return
        }
        enqueue(text)
    }

    fun stop() {
        if (ready.get()) {
            tts.stop()
        }
        synchronized(pendingUtterances) {
            pendingUtterances.clear()
        }
    }

    fun shutdown() {
        stop()
        ready.set(false)
        tts.shutdown()
    }

    private fun enqueue(text: String) {
        tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, text.hashCode().toString())
    }

    companion object {
        private const val TAG = "TtsSpeaker"
    }
}
