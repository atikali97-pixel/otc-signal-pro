package com.example

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.media.projection.MediaProjectionManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import com.example.service.FloatingBubbleService
import com.example.service.SignalEngineService
import com.example.ui.MainViewModel
import com.example.ui.ScreenDestination
import com.example.ui.screens.BrokerSelectorScreen
import com.example.ui.screens.BrokerWebViewScreen
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.DisclaimerScreen
import com.example.ui.screens.FeatureBuilderScreen
import com.example.ui.screens.PairSelectorScreen
import com.example.ui.screens.PermissionWizardScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.SignalHistoryScreen
import com.example.ui.screens.SplashScreen
import com.example.ui.theme.DarkCanvas
import com.example.ui.theme.MyApplicationTheme
import kotlinx.coroutines.flow.MutableStateFlow

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            MyApplicationTheme {
                Scaffold(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(DarkCanvas)
                ) { innerPadding ->
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                            .safeDrawingPadding()
                    ) {
                        MainContent(viewModel = viewModel, activity = this@MainActivity)
                    }
                }
            }
        }
    }
}

@Composable
fun MainContent(viewModel: MainViewModel, activity: Activity) {
    val currentScreen by viewModel.currentScreen.collectAsState()
    val pairs by viewModel.pairs.collectAsState()
    val brokers by viewModel.brokers.collectAsState()
    val recentSignals by viewModel.recentSignals.collectAsState()
    val liveSignal by viewModel.liveActiveSignal.collectAsState()
    val liveCandles by viewModel.liveCandles.collectAsState()
    val capturedFrame by viewModel.capturedChartFrame.collectAsState()
    val secondsRemaining by viewModel.secondsRemaining.collectAsState()
    val winCount by viewModel.winCount.collectAsState()
    val lossCount by viewModel.lossCount.collectAsState()
    val tieCount by viewModel.tieCount.collectAsState()
    val totalProfit by viewModel.totalProfit.collectAsState()
    val plugins by viewModel.plugins.collectAsState()

    val luciferSpeakingText by viewModel.lucifer.latestSpokenText.collectAsState()
    val isListening by viewModel.lucifer.isListening.collectAsState()

    val visionResult by viewModel.visionAnalysisResult.collectAsState()
    val isAnalyzingVision by viewModel.isAnalyzingVision.collectAsState()
    val isSystemActive by viewModel.isSystemActive.collectAsState()

    val mediaProjectionManager = activity.getSystemService(Context.MEDIA_PROJECTION_SERVICE) as? MediaProjectionManager

    val projectionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK && result.data != null) {
            val serviceIntent = Intent(activity, SignalEngineService::class.java).apply {
                action = SignalEngineService.ACTION_START_PROJECTION
                putExtra(SignalEngineService.EXTRA_RESULT_CODE, result.resultCode)
                putExtra(SignalEngineService.EXTRA_RESULT_DATA, result.data)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                activity.startForegroundService(serviceIntent)
            } else {
                activity.startService(serviceIntent)
            }
            viewModel.startScreenCapture(result.resultCode, result.data!!)
            viewModel.lucifer.speak("Screen recording active ho gayi hai!")
            if (Settings.canDrawOverlays(activity)) {
                activity.startService(Intent(activity, FloatingBubbleService::class.java))
            }
        }
    }

    // Back handling
    if (currentScreen !is ScreenDestination.Dashboard && currentScreen !is ScreenDestination.Splash) {
        BackHandler {
            viewModel.navigateTo(ScreenDestination.Dashboard)
        }
    }

    when (val screen = currentScreen) {
        is ScreenDestination.Splash -> {
            SplashScreen(
                onFinished = {
                    if (viewModel.settings.hasCompletedPermissionWizard) {
                        viewModel.navigateTo(ScreenDestination.Dashboard)
                    } else {
                        viewModel.navigateTo(ScreenDestination.PermissionWizard)
                    }
                }
            )
        }

        is ScreenDestination.PermissionWizard -> {
            PermissionWizardScreen(
                onComplete = {
                    viewModel.settings.hasCompletedPermissionWizard = true
                    viewModel.navigateTo(ScreenDestination.Dashboard)
                }
            )
        }

        is ScreenDestination.Dashboard -> {
            DashboardScreen(
                pairName = viewModel.settings.selectedPairName,
                expiryMinutes = viewModel.settings.defaultExpiryMinutes,
                secondsRemaining = secondsRemaining,
                activeSignal = liveSignal,
                candles = liveCandles,
                capturedFrame = capturedFrame,
                isRecording = capturedFrame != null,
                winCount = winCount,
                lossCount = lossCount,
                tieCount = tieCount,
                lucifer = viewModel.lucifer,
                luciferSpeakingText = luciferSpeakingText,
                isListening = isListening,
                visionResult = visionResult,
                isAnalyzingVision = isAnalyzingVision,
                isSystemActive = isSystemActive,
                onToggleSystemPower = { viewModel.toggleSystemPower() },
                onExpiryChanged = { exp -> viewModel.settings.defaultExpiryMinutes = exp },
                onSelectPairClicked = { viewModel.navigateTo(ScreenDestination.PairSelector) },
                onStartRecordingClicked = {
                    if (mediaProjectionManager != null) {
                        projectionLauncher.launch(mediaProjectionManager.createScreenCaptureIntent())
                    }
                },
                onStopRecordingClicked = {
                    viewModel.stopScreenCapture()
                    viewModel.lucifer.speak("Recording stopped!")
                },
                onOpenBrokersClicked = { viewModel.navigateTo(ScreenDestination.BrokerSelector) },
                onOpenHistoryClicked = { viewModel.navigateTo(ScreenDestination.SignalHistory) },
                onOpenPluginsClicked = { viewModel.navigateTo(ScreenDestination.FeatureBuilder) },
                onOpenSettingsClicked = { viewModel.navigateTo(ScreenDestination.Settings) },
                onAnalyzeVisionClicked = { viewModel.runVisionAnalysisOnFrame() },
                onImmediateSignalClicked = { viewModel.generateImmediateSignal() }
            )
        }

        is ScreenDestination.BrokerSelector -> {
            BrokerSelectorScreen(
                brokers = brokers,
                onBrokerSelected = { broker -> viewModel.navigateTo(ScreenDestination.BrokerWebView(broker)) },
                onAddBroker = { name, url -> viewModel.addCustomBroker(name, url) },
                onBack = { viewModel.navigateTo(ScreenDestination.Dashboard) }
            )
        }

        is ScreenDestination.BrokerWebView -> {
            BrokerWebViewScreen(
                broker = screen.broker,
                onCookiesCaptured = { cookies ->
                    viewModel.saveBrokerCookies(screen.broker.id, cookies)
                },
                onStartLiveCapture = {
                    viewModel.navigateTo(ScreenDestination.Dashboard)
                    if (mediaProjectionManager != null) {
                        projectionLauncher.launch(mediaProjectionManager.createScreenCaptureIntent())
                    }
                },
                onBack = { viewModel.navigateTo(ScreenDestination.BrokerSelector) }
            )
        }

        is ScreenDestination.PairSelector -> {
            PairSelectorScreen(
                pairs = pairs,
                selectedPairName = viewModel.settings.selectedPairName,
                onPairSelected = { pairName ->
                    viewModel.settings.selectedPairName = pairName
                    viewModel.lucifer.speak("Pair $pairName switch ho gaya!")
                    viewModel.navigateTo(ScreenDestination.Dashboard)
                },
                onAddCustomPair = { name, cat -> viewModel.addCustomPair(name, cat) },
                onBack = { viewModel.navigateTo(ScreenDestination.Dashboard) }
            )
        }

        is ScreenDestination.SignalHistory -> {
            SignalHistoryScreen(
                signals = recentSignals,
                winCount = winCount,
                lossCount = lossCount,
                tieCount = tieCount,
                totalProfit = totalProfit,
                onBack = { viewModel.navigateTo(ScreenDestination.Dashboard) }
            )
        }

        is ScreenDestination.FeatureBuilder -> {
            FeatureBuilderScreen(
                plugins = plugins,
                sandbox = viewModel.pluginSandbox,
                onGenerateFeature = { prompt -> viewModel.generateAiFeature(prompt) },
                onTogglePlugin = { plugin -> viewModel.togglePlugin(plugin) },
                onDeletePlugin = { id -> viewModel.deletePlugin(id) },
                onBack = { viewModel.navigateTo(ScreenDestination.Dashboard) }
            )
        }

        is ScreenDestination.Settings -> {
            SettingsScreen(
                settings = viewModel.settings,
                isSystemActive = isSystemActive,
                onToggleSystemPower = { viewModel.toggleSystemPower() },
                onOpenDisclaimer = { viewModel.navigateTo(ScreenDestination.Disclaimer) },
                onBack = { viewModel.navigateTo(ScreenDestination.Dashboard) }
            )
        }

        is ScreenDestination.Disclaimer -> {
            DisclaimerScreen(
                onAgreeAndBack = { viewModel.navigateTo(ScreenDestination.Settings) }
            )
        }
    }
}
