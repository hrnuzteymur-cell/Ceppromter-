package com.cepprompter.app.control

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import java.text.Normalizer
import java.util.Locale

class VoiceFollower(
    private val context: Context,
    script: String,
    private val onProgress: (Float) -> Unit,
    private val onState: (String) -> Unit
) : RecognitionListener {
    private val words = normalize(script).split(' ').filter { it.isNotBlank() }
    private var recognizer: SpeechRecognizer? = null
    private var cursor = 0
    private var shouldRun = false

    fun start() {
        if (!SpeechRecognizer.isRecognitionAvailable(context)) {
            onState("Bu cihazda konuşma tanıma hizmeti yok")
            return
        }
        shouldRun = true
        if (recognizer == null) recognizer = SpeechRecognizer.createSpeechRecognizer(context).also { it.setRecognitionListener(this) }
        listen()
    }

    fun stop() {
        shouldRun = false
        recognizer?.cancel()
        onState("Sesle takip kapalı")
    }

    fun destroy() { shouldRun = false; recognizer?.destroy(); recognizer = null }

    private fun listen() {
        if (!shouldRun) return
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "tr-TR")
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)
        }
        runCatching { recognizer?.startListening(intent); onState("Dinliyor…") }
            .onFailure { onState("Mikrofon başlatılamadı") }
    }

    private fun match(bundle: Bundle, partial: Boolean) {
        val candidates = bundle.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION).orEmpty()
        val spoken = candidates.firstOrNull()?.let(::normalize)?.split(' ')?.filter { it.isNotBlank() }.orEmpty()
        if (spoken.isEmpty() || words.isEmpty()) return
        val needle = spoken.takeLast(5)
        val from = (cursor - 12).coerceAtLeast(0)
        val to = (cursor + 80).coerceAtMost(words.size)
        var bestIndex = -1
        var bestScore = 0
        for (i in from until to) {
            var score = 0
            for (n in needle.indices) if (i + n < words.size && words[i + n] == needle[n]) score++
            if (score > bestScore) { bestScore = score; bestIndex = i }
        }
        if (bestIndex >= 0 && bestScore >= minOf(2, needle.size)) {
            cursor = (bestIndex + needle.size).coerceAtMost(words.size - 1)
            onProgress(cursor.toFloat() / words.size.coerceAtLeast(1))
            onState(if (partial) "Konuşma izleniyor" else "Cümle bulundu")
        }
    }

    override fun onPartialResults(partialResults: Bundle) = match(partialResults, true)
    override fun onResults(results: Bundle) { match(results, false); listen() }
    override fun onError(error: Int) {
        if (!shouldRun) return
        onState(if (error == SpeechRecognizer.ERROR_NO_MATCH || error == SpeechRecognizer.ERROR_SPEECH_TIMEOUT) "Konuşmanız bekleniyor…" else "Dinleme yeniden başlatılıyor…")
        android.os.Handler(context.mainLooper).postDelayed({ listen() }, 350)
    }
    override fun onReadyForSpeech(params: Bundle?) { onState("Dinliyor…") }
    override fun onBeginningOfSpeech() { onState("Konuşma algılandı") }
    override fun onEndOfSpeech() = Unit
    override fun onRmsChanged(rmsdB: Float) = Unit
    override fun onBufferReceived(buffer: ByteArray?) = Unit
    override fun onEvent(eventType: Int, params: Bundle?) = Unit

    private fun normalize(value: String): String = Normalizer.normalize(value.lowercase(Locale("tr", "TR")), Normalizer.Form.NFD)
        .replace(Regex("\\p{Mn}+"), "").replace(Regex("[^a-z0-9çğıöşü ]"), " ").replace(Regex("\\s+"), " ").trim()
}
