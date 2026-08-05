package net.aucutt.circuits.tts

import android.content.Context
import android.speech.tts.TextToSpeech
import android.speech.tts.Voice
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
        val locale = resolveLocale()
        configureRoboticVoice(locale)
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

    private fun resolveLocale(): Locale {
        val locale = Locale.getDefault()
        val result = tts.setLanguage(locale)
        if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
            Log.w(TAG, "Default locale not supported; falling back to US English")
            tts.language = Locale.US
            return Locale.US
        }
        return locale
    }

    private fun configureRoboticVoice(locale: Locale) {
        tts.setPitch(ROBOT_PITCH)
        tts.setSpeechRate(ROBOT_SPEECH_RATE)

        val voice = selectRoboticVoice(locale) ?: return
        if (tts.setVoice(voice) != TextToSpeech.SUCCESS) {
            Log.w(TAG, "Failed to set voice ${voice.name}")
        } else {
            Log.d(TAG, "Using robotic voice ${voice.name}")
        }
    }

    private fun selectRoboticVoice(locale: Locale): Voice? {
        val voices = tts.voices ?: return null
        val language = locale.language

        return voices
            .asSequence()
            .filter { it.locale.language.equals(language, ignoreCase = true) }
            .minWithOrNull(roboticVoiceComparator())
    }

    private fun roboticVoiceComparator(): Comparator<Voice> {
        return compareBy<Voice> { it.quality }
            .thenBy { if (it.isNetworkConnectionRequired) 1 else 0 }
            .thenBy { voice ->
                val name = voice.name.lowercase()
                when {
                    name.contains("local") -> 0
                    name.contains("male") -> 1
                    name.contains("female") -> 3
                    else -> 2
                }
            }
    }

    private fun enqueue(text: String) {
        tts.speak(text, TextToSpeech.QUEUE_ADD, null, text.hashCode().toString())
    }

    companion object {
        private const val TAG = "TtsSpeaker"
        private const val ROBOT_PITCH = 0.72f
        private const val ROBOT_SPEECH_RATE = 0.92f
    }
}
