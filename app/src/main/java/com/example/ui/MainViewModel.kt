package com.example.ui

import android.app.Application
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.graphics.Bitmap
import android.os.Build
import android.os.IBinder
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.ai.GeminiApiClient
import com.example.assistant.LuciferVoiceAssistant
import com.example.data.AppDatabase
import com.example.data.entity.BrokerEntity
import com.example.data.entity.CandleEntity
import com.example.data.entity.OtcPairEntity
import com.example.data.entity.PluginEntity
import com.example.data.entity.SignalEntity
import com.example.data.repository.TradingRepository
import com.example.plugin.PluginSandbox
import com.example.service.SignalEngineService
import com.example.util.AppSettings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed class ScreenDestination {
    object Splash : ScreenDestination()
    object PermissionWizard : ScreenDestination()
    object Dashboard : ScreenDestination()
    object BrokerSelector : ScreenDestination()
    data class BrokerWebView(val broker: BrokerEntity) : ScreenDestination()
    object PairSelector : ScreenDestination()
    object SignalHistory : ScreenDestination()
    object FeatureBuilder : ScreenDestination()
    object Settings : ScreenDestination()
    object Disclaimer : ScreenDestination()
}

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getDatabase(application)
    val repository = TradingRepository(db)
    val settings = AppSettings(application)
    val pluginSandbox = PluginSandbox(application)

    private val _isSystemActive = MutableStateFlow(settings.isSystemActive)
    val isSystemActive: StateFlow<Boolean> = _isSystemActive.asStateFlow()

    val geminiClient = GeminiApiClient { settings.geminiApiKey }

    private val _currentScreen = MutableStateFlow<ScreenDestination>(
        if (settings.hasCompletedPermissionWizard) ScreenDestination.Dashboard else ScreenDestination.Splash
    )
    val currentScreen: StateFlow<ScreenDestination> = _currentScreen.asStateFlow()

    // Service binding
    private var signalService: SignalEngineService? = null
    private val _isServiceBound = MutableStateFlow(false)
    val isServiceBound: StateFlow<Boolean> = _isServiceBound.asStateFlow()

    val secondsRemaining: StateFlow<Int> = MutableStateFlow(60)
    private val _liveActiveSignal = MutableStateFlow<SignalEntity?>(null)
    val liveActiveSignal: StateFlow<SignalEntity?> = _liveActiveSignal.asStateFlow()

    private val _liveCandles = MutableStateFlow<List<CandleEntity>>(emptyList())
    val liveCandles: StateFlow<List<CandleEntity>> = _liveCandles.asStateFlow()

    private val _capturedChartFrame = MutableStateFlow<Bitmap?>(null)
    val capturedChartFrame: StateFlow<Bitmap?> = _capturedChartFrame.asStateFlow()

    private val _visionAnalysisResult = MutableStateFlow<String?>(null)
    val visionAnalysisResult: StateFlow<String?> = _visionAnalysisResult.asStateFlow()

    private val _isAnalyzingVision = MutableStateFlow(false)
    val isAnalyzingVision: StateFlow<Boolean> = _isAnalyzingVision.asStateFlow()

    // Database reactive streams
    val brokers: StateFlow<List<BrokerEntity>> = repository.allBrokers
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val pairs: StateFlow<List<OtcPairEntity>> = repository.allPairs
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val recentSignals: StateFlow<List<SignalEntity>> = repository.recentSignals
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val winCount: StateFlow<Int> = repository.winCount
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val lossCount: StateFlow<Int> = repository.lossCount
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val tieCount: StateFlow<Int> = repository.tieCount
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val totalProfit: StateFlow<Double?> = repository.totalProfit
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    val plugins: StateFlow<List<PluginEntity>> = repository.allPlugins
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Voice Assistant
    lateinit var lucifer: LuciferVoiceAssistant
        private set

    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            val binder = service as SignalEngineService.LocalBinder
            val svc = binder.getService()
            signalService = svc
            _isServiceBound.value = true

            viewModelScope.launch {
                svc.secondsRemainingInCandle.collect {
                    (secondsRemaining as MutableStateFlow).value = it
                }
            }
            viewModelScope.launch {
                svc.activeSignal.collect {
                    _liveActiveSignal.value = it
                }
            }
            viewModelScope.launch {
                svc.candleBuffer.collect {
                    _liveCandles.value = it
                }
            }
            viewModelScope.launch {
                svc.captureManager.latestFrame.collect {
                    _capturedChartFrame.value = it
                }
            }

            svc.setSystemActive(_isSystemActive.value)
            svc.onSignalGeneratedListener = { sig ->
                if (_isSystemActive.value) {
                    lucifer.speak("Boss, ${sig.type} signal ready on ${sig.pairName}, confidence ${sig.confidence} percent!")
                    lucifer.triggerHaptic()
                }
            }
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            signalService = null
            _isServiceBound.value = false
        }
    }

    init {
        initLucifer()
        bindSignalService()
        viewModelScope.launch {
            repository.ensureInitialized()
        }
    }

    private fun initLucifer() {
        lucifer = LuciferVoiceAssistant(
            context = getApplication(),
            geminiClient = geminiClient,
            settings = settings
        ) { command, argument ->
            handleVoiceCommand(command, argument)
        }
    }

    private fun handleVoiceCommand(command: String, argument: String?) {
        when (command) {
            "OPEN_BROKER" -> {
                val url = argument ?: "https://qxbroker.com/en/sign-in"
                val b = brokers.value.firstOrNull { it.url == url } ?: BrokerEntity(name = "Broker", url = url)
                navigateTo(ScreenDestination.BrokerWebView(b))
            }
            "START_RECORDING" -> {
                lucifer.speak("Dashboard par START RECORDING button tap karo Boss, permission confirm ke liye!")
                navigateTo(ScreenDestination.Dashboard)
            }
            "GENERATE_SIGNAL" -> {
                generateImmediateSignal()
            }
            "SET_EXPIRY" -> {
                val exp = argument?.toIntOrNull() ?: 1
                settings.defaultExpiryMinutes = exp
                lucifer.speak("Expiry set to $exp minute!")
            }
            "SELECT_PAIR" -> {
                val pair = argument ?: "USD/INR OTC"
                settings.selectedPairName = pair
                lucifer.speak("Pair $pair selected!")
            }
            "GET_STATS" -> {
                val wins = winCount.value
                val losses = lossCount.value
                lucifer.speak("Boss, total $wins Wins aur $losses Losses hain!")
            }
            "ADD_FEATURE" -> {
                if (argument != null) {
                    generateAiFeature(argument)
                }
            }
            "POWER_OFF" -> {
                setSystemPower(false)
            }
            "POWER_ON" -> {
                setSystemPower(true)
            }
        }
    }

    fun toggleSystemPower() {
        setSystemPower(!_isSystemActive.value)
    }

    fun setSystemPower(active: Boolean) {
        _isSystemActive.value = active
        settings.isSystemActive = active
        signalService?.setSystemActive(active)
        lucifer.onSystemPowerChanged(active)
        if (!active) {
            stopScreenCapture()
            try {
                getApplication<Application>().stopService(
                    Intent(getApplication(), com.example.service.FloatingBubbleService::class.java)
                )
            } catch (_: Exception) {}
        }
    }

    fun bindSignalService() {
        val intent = Intent(getApplication(), SignalEngineService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            getApplication<Application>().startForegroundService(intent)
        } else {
            getApplication<Application>().startService(intent)
        }
        getApplication<Application>().bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE)
    }

    fun navigateTo(screen: ScreenDestination) {
        _currentScreen.value = screen
    }

    fun generateImmediateSignal() {
        if (!_isSystemActive.value) {
            lucifer.speak("Boss, system abhi OFF hai. Pehle master button se ON karo!")
            return
        }
        viewModelScope.launch {
            signalService?.generateSignalNow(settings.selectedPairName, settings.defaultExpiryMinutes)
        }
    }

    fun startScreenCapture(resultCode: Int, data: Intent) {
        signalService?.startCaptureWithResult(resultCode, data)
    }

    fun startScreenCapture(projection: android.media.projection.MediaProjection) {
        signalService?.promoteToMediaProjection(projection)
    }

    fun stopScreenCapture() {
        signalService?.demoteFromMediaProjection()
    }

    fun runVisionAnalysisOnFrame() {
        val frame = _capturedChartFrame.value ?: return
        viewModelScope.launch {
            _isAnalyzingVision.value = true
            val result = geminiClient.analyzeChartVision(frame, settings.selectedPairName)
            _visionAnalysisResult.value = result
            _isAnalyzingVision.value = false
            lucifer.speak(result.take(120))
        }
    }

    fun generateAiFeature(prompt: String) {
        viewModelScope.launch {
            lucifer.speak("AI Feature code generate ho raha hai, ek second...")
            val code = geminiClient.generateFeaturePluginCode(prompt)
            val plugin = PluginEntity(
                name = prompt.take(30),
                description = prompt,
                code = code,
                isEnabled = true
            )
            repository.insertPlugin(plugin)
            lucifer.speak("Naya feature sandbox mein load ho gaya hai Boss!")
        }
    }

    fun saveBrokerCookies(brokerId: Int, cookies: String) {
        viewModelScope.launch {
            repository.saveCookies(brokerId, cookies)
        }
    }

    fun addCustomBroker(name: String, url: String) {
        viewModelScope.launch {
            repository.insertBroker(
                BrokerEntity(
                    name = name,
                    url = if (url.startsWith("http")) url else "https://$url",
                    iconName = "link"
                )
            )
        }
    }

    fun addCustomPair(name: String, category: String = "Currencies") {
        viewModelScope.launch {
            val formatted = if (name.contains("OTC", ignoreCase = true)) name else "$name OTC"
            repository.insertPair(
                OtcPairEntity(
                    name = formatted,
                    category = category,
                    payoutPercent = 90,
                    currentPrice = 1.0000
                )
            )
        }
    }

    fun togglePlugin(plugin: PluginEntity) {
        viewModelScope.launch {
            repository.updatePlugin(plugin.copy(isEnabled = !plugin.isEnabled))
        }
    }

    fun deletePlugin(id: Int) {
        viewModelScope.launch {
            repository.deletePlugin(id)
        }
    }

    override fun onCleared() {
        try {
            getApplication<Application>().unbindService(serviceConnection)
        } catch (_: Exception) {}
        lucifer.destroy()
        super.onCleared()
    }
}
