package com.example.assistant

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import com.example.ai.GeminiApiClient
import com.example.ai.LuciferResponse
import com.example.util.AppSettings
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.Locale

class LuciferVoiceAssistant(
    private val context: Context,
    private val geminiClient: GeminiApiClient,
    private val settings: AppSettings,
    private val onCommandAction: (command: String, argument: String?) -> Unit
) : RecognitionListener {

    private var speechRecognizer: SpeechRecognizer? = null
    private var tts: TextToSpeech? = null
    private var isTtsReady = false

    private val _isListening = MutableStateFlow(false)
    val isListening: StateFlow<Boolean> = _isListening.asStateFlow()

    private val _latestSpokenText = MutableStateFlow("Lucifer ready hai, Boss!")
    val latestSpokenText: StateFlow<String> = _latestSpokenText.asStateFlow()

    private val coroutineScope = CoroutineScope(Dispatchers.Main)

    init {
        initTts()
    }

    private fun initTts() {
        tts = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                isTtsReady = true
                tts?.language = Locale("hi", "IN")
                tts?.setPitch(1.05f)
                tts?.setSpeechRate(1.02f)
            }
        }
    }

    fun speak(text: String) {
        if (!settings.isSystemActive || !settings.isVoiceAssistantEnabled) return
        _latestSpokenText.value = text
        if (isTtsReady) {
            tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "lucifer_utterance")
        }
        triggerHaptic()
    }

    fun onSystemPowerChanged(active: Boolean) {
        if (!active) {
            stopListening()
            tts?.stop()
            _latestSpokenText.value = "Lucifer Standby (OFF)"
            triggerHaptic()
        } else {
            _latestSpokenText.value = "Lucifer active hai, Boss!"
            if (settings.isVoiceAssistantEnabled) {
                if (isTtsReady) {
                    tts?.speak("Boss, Lucifer aur system active ho gaye hain!", TextToSpeech.QUEUE_FLUSH, null, "lucifer_power_on")
                }
            }
            triggerHaptic()
        }
    }

    fun startListening() {
        if (!settings.isSystemActive) return
        if (!SpeechRecognizer.isRecognitionAvailable(context)) {
            speak("Speech recognition available nahi hai device par")
            return
        }

        try {
            speechRecognizer?.destroy()
            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context).apply {
                setRecognitionListener(this@LuciferVoiceAssistant)
            }

            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, "hi-IN")
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, "hi-IN")
                putExtra(RecognizerIntent.EXTRA_ONLY_RETURN_LANGUAGE_PREFERENCE, "hi-IN")
                putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)
            }

            speechRecognizer?.startListening(intent)
            _isListening.value = true
        } catch (e: Exception) {
            _isListening.value = false
        }
    }

    fun stopListening() {
        try {
            speechRecognizer?.stopListening()
        } catch (_: Exception) {}
        _isListening.value = false
    }

    fun processUserInput(userInput: String, contextInfo: String) {
        // Direct offline fallback commands first for zero latency!
        val lower = userInput.lowercase(Locale.ROOT)
        if (lower.contains("system off") || lower.contains("app off") || lower.contains("band karo") || lower.contains("power off") || lower.contains("lucifer off")) {
            onCommandAction("POWER_OFF", null)
            return
        } else if (lower.contains("system on") || lower.contains("app on") || lower.contains("chalu karo") || lower.contains("power on") || lower.contains("lucifer on")) {
            onCommandAction("POWER_ON", null)
            return
        } else if (lower.contains("ruk jao") || lower.contains("stop")) {
            stopListening()
            speak("Theek hai Boss, listening paused!")
            return
        } else if (lower.contains("quotex")) {
            speak("Quotex khol rahi hoon Boss!")
            onCommandAction("OPEN_BROKER", "https://qxbroker.com/en/sign-in")
            return
        } else if (lower.contains("record") || lower.contains("screen")) {
            speak("Screen recording start kar rahi hoon!")
            onCommandAction("START_RECORDING", null)
            return
        } else if (lower.contains("signal") || lower.contains("generate")) {
            speak("Signal analyze ho raha hai!")
            onCommandAction("GENERATE_SIGNAL", null)
            return
        } else if (lower.contains("1 minute") || lower.contains("ek minute")) {
            speak("1-minute expiry lock kar di hai!")
            onCommandAction("SET_EXPIRY", "1")
            return
        } else if (lower.contains("5 minute") || lower.contains("paanch minute")) {
            speak("5-minute expiry lock kar di hai!")
            onCommandAction("SET_EXPIRY", "5")
            return
        }

        // Call Gemini Live Brain
        coroutineScope.launch {
            val response = geminiClient.chatWithLucifer(userInput, contextInfo)
            speak(response.reply)
            if (response.detectedCommand != null) {
                onCommandAction(response.detectedCommand, response.commandArgument)
            }
        }
    }

    fun triggerHaptic() {
        if (!settings.isVibrationEnabled) return
        try {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
                val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                vibratorManager?.defaultVibrator?.vibrate(
                    VibrationEffect.createPredefined(VibrationEffect.EFFECT_HEAVY_CLICK)
                )
            } else {
                @Suppress("DEPRECATION")
                val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
                vibrator?.vibrate(100)
            }
        } catch (_: Exception) {}
    }

    override fun onResults(results: Bundle?) {
        _isListening.value = false
        val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
        val text = matches?.firstOrNull() ?: return
        processUserInput(text, "Pair: ${settings.selectedPairName}, Expiry: ${settings.defaultExpiryMinutes}m")
    }

    override fun onError(error: Int) {
        _isListening.value = false
    }

    override fun onReadyForSpeech(params: Bundle?) {}
    override fun onBeginningOfSpeech() {}
    override fun onRmsChanged(rmsdB: Float) {}
    override fun onBufferReceived(buffer: ByteArray?) {}
    override fun onEndOfSpeech() { _isListening.value = false }
    override fun onPartialResults(partialResults: Bundle?) {}
    override fun onEvent(eventType: Int, params: Bundle?) {}

    fun destroy() {
        try {
            speechRecognizer?.destroy()
            tts?.stop()
            tts?.shutdown()
        } catch (_: Exception) {}
    }
}
