package com.example.service

import android.app.Activity
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.Binder
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.R
import com.example.capture.ScreenCaptureManager
import com.example.data.AppDatabase
import com.example.data.entity.CandleEntity
import com.example.data.entity.SignalEntity
import com.example.data.repository.TradingRepository
import com.example.engine.PriceActionEngine
import com.example.util.AppSettings
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class SignalEngineService : Service() {

    private val binder = LocalBinder()
    private val scope = CoroutineScope(Dispatchers.Default + Job())
    private var timerJob: Job? = null

    private lateinit var repository: TradingRepository
    private lateinit var settings: AppSettings
    lateinit var captureManager: ScreenCaptureManager
        private set

    private val _secondsRemainingInCandle = MutableStateFlow(60)
    val secondsRemainingInCandle: StateFlow<Int> = _secondsRemainingInCandle.asStateFlow()

    private val _activeSignal = MutableStateFlow<SignalEntity?>(null)
    val activeSignal: StateFlow<SignalEntity?> = _activeSignal.asStateFlow()

    private val _candleBuffer = MutableStateFlow<List<CandleEntity>>(emptyList())
    val candleBuffer: StateFlow<List<CandleEntity>> = _candleBuffer.asStateFlow()

    var onSignalGeneratedListener: ((SignalEntity) -> Unit)? = null

    companion object {
        const val NOTIFICATION_ID = 1010
        const val CHANNEL_ID = "otc_signal_engine_channel"
        const val ACTION_START_PROJECTION = "com.example.service.ACTION_START_PROJECTION"
        const val ACTION_STOP_PROJECTION = "com.example.service.ACTION_STOP_PROJECTION"
        const val EXTRA_RESULT_CODE = "extra_result_code"
        const val EXTRA_RESULT_DATA = "extra_result_data"
    }

    inner class LocalBinder : Binder() {
        fun getService(): SignalEngineService = this@SignalEngineService
    }

    override fun onBind(intent: Intent?): IBinder = binder

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START_PROJECTION -> {
                val resultCode = intent.getIntExtra(EXTRA_RESULT_CODE, Activity.RESULT_CANCELED)
                val data = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    intent.getParcelableExtra(EXTRA_RESULT_DATA, Intent::class.java)
                } else {
                    @Suppress("DEPRECATION")
                    intent.getParcelableExtra(EXTRA_RESULT_DATA)
                }
                if (resultCode == Activity.RESULT_OK && data != null) {
                    startCaptureWithResult(resultCode, data)
                }
            }
            ACTION_STOP_PROJECTION -> {
                demoteFromMediaProjection()
            }
        }
        return START_STICKY
    }

    override fun onCreate() {
        super.onCreate()
        val db = AppDatabase.getDatabase(this)
        repository = TradingRepository(db)
        settings = AppSettings(this)
        captureManager = ScreenCaptureManager(this)

        createNotificationChannel()
        val notification = buildForegroundNotification("Signal Engine Initializing...")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC)
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }

        startTimerEngine()
    }

    fun startCaptureWithResult(resultCode: Int, data: Intent, width: Int = 720, height: Int = 1280, dpi: Int = 320) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val notification = buildForegroundNotification("Chart Recording Active")
            startForeground(
                NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC or ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION
            )
        }
        try {
            val projectionManager = getSystemService(Context.MEDIA_PROJECTION_SERVICE) as? MediaProjectionManager
            val projection = projectionManager?.getMediaProjection(resultCode, data)
            if (projection != null) {
                captureManager.startCapture(projection, width, height, dpi)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun promoteToMediaProjection(projection: MediaProjection, width: Int = 720, height: Int = 1280, dpi: Int = 320) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val notification = buildForegroundNotification("Chart Recording Active")
            startForeground(
                NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC or ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION
            )
        }
        captureManager.startCapture(projection, width, height, dpi)
    }

    fun demoteFromMediaProjection() {
        captureManager.stopCapture()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val notification = buildForegroundNotification("Signal Engine Active")
            startForeground(
                NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
            )
        }
    }

    fun setSystemActive(active: Boolean) {
        if (!active) {
            demoteFromMediaProjection()
            updateNotification("OTC Signal Pro: System Standby (Paused)")
        } else {
            updateNotification("OTC Signal Pro: Active Monitoring")
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "OTC Signal Pro Alerts",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Real-time 1-min and 5-min OTC price action signals"
                enableVibration(true)
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    private fun buildForegroundNotification(content: String): Notification {
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("OTC Signal Pro Engine")
            .setContentText(content)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()
    }

    private fun updateNotification(content: String) {
        val manager = getSystemService(NotificationManager::class.java)
        manager.notify(NOTIFICATION_ID, buildForegroundNotification(content))
    }

    private fun startTimerEngine() {
        timerJob?.cancel()
        timerJob = scope.launch {
            repository.ensureInitialized()

            // Seed initial candles if empty
            val currentPair = settings.selectedPairName
            val existing = repository.getCandles(currentPair, 30).first()
            if (existing.isEmpty()) {
                var currentPrice = 1.0850
                val seed = mutableListOf<CandleEntity>()
                for (i in 0 until 30) {
                    val candle = PriceActionEngine.generateNextCandle(currentPair, currentPrice)
                    seed.add(candle)
                    currentPrice = candle.close
                }
                seed.forEach { repository.insertCandle(it) }
                _candleBuffer.value = seed.reversed()
            } else {
                _candleBuffer.value = existing
            }

            var signalFiredForCurrentCandle = false

            while (isActive) {
                if (!settings.isSystemActive) {
                    delay(1000)
                    continue
                }

                val now = System.currentTimeMillis()
                val expiryMinutes = settings.defaultExpiryMinutes
                val candleDurationSec = expiryMinutes * 60

                // Precise candle timing
                val currentSecondOfMinute = ((now / 1000) % candleDurationSec).toInt()
                val secondsRemaining = candleDurationSec - currentSecondOfMinute
                _secondsRemainingInCandle.value = secondsRemaining

                // EXACT RULE: Signal must generate exactly 5 SECONDS before candle closes
                if (secondsRemaining == 5 && !signalFiredForCurrentCandle) {
                    signalFiredForCurrentCandle = true
                    generateSignalNow(settings.selectedPairName, expiryMinutes)
                }

                // Candle Closed! Generate new candle, evaluate previous pending signals
                if (secondsRemaining == candleDurationSec || secondsRemaining == 1) {
                    signalFiredForCurrentCandle = false
                    evaluatePendingSignals()
                    generateNewCandle()
                }

                delay(1000)
            }
        }
    }

    suspend fun generateSignalNow(pairName: String, expiryMinutes: Int) {
        val candles = _candleBuffer.value
        val signalResult = PriceActionEngine.analyze(candles)
        val currentPrice = candles.firstOrNull()?.close ?: 1.0850

        val signal = SignalEntity(
            pairName = pairName,
            type = signalResult.action,
            expiryMinutes = expiryMinutes,
            confidence = signalResult.confidence,
            confluenceReasons = signalResult.confluenceReasons.joinToString(" • "),
            timestamp = System.currentTimeMillis(),
            entryPrice = currentPrice,
            result = "PENDING"
        )

        val id = repository.insertSignal(signal)
        val saved = signal.copy(id = id.toInt())
        _activeSignal.value = saved

        updateNotification("🟢 NEW SIGNAL: ${signal.type} on $pairName (${signal.confidence}%)")
        scope.launch(Dispatchers.Main) {
            onSignalGeneratedListener?.invoke(saved)
        }
    }

    private suspend fun generateNewCandle() {
        val pairName = settings.selectedPairName
        val lastPrice = _candleBuffer.value.firstOrNull()?.close ?: 1.0850
        val newCandle = PriceActionEngine.generateNextCandle(pairName, lastPrice)
        repository.insertCandle(newCandle)
        val updated = listOf(newCandle) + _candleBuffer.value.take(49)
        _candleBuffer.value = updated
    }

    private suspend fun evaluatePendingSignals() {
        val active = _activeSignal.value ?: return
        if (active.result != "PENDING") return

        val currentPrice = _candleBuffer.value.firstOrNull()?.close ?: return
        val entry = active.entryPrice

        val resultStr: String
        val profit: Double
        if (active.type == "CALL") {
            if (currentPrice > entry) {
                resultStr = "WIN"
                profit = 92.0 // payout profit
            } else if (currentPrice < entry) {
                resultStr = "LOSS"
                profit = -100.0
            } else {
                resultStr = "TIE"
                profit = 0.0
            }
        } else {
            if (currentPrice < entry) {
                resultStr = "WIN"
                profit = 92.0
            } else if (currentPrice > entry) {
                resultStr = "LOSS"
                profit = -100.0
            } else {
                resultStr = "TIE"
                profit = 0.0
            }
        }

        val updated = active.copy(
            exitPrice = currentPrice,
            result = resultStr,
            profitAmount = profit
        )
        repository.updateSignal(updated)
        _activeSignal.value = updated
    }

    override fun onDestroy() {
        timerJob?.cancel()
        captureManager.stopCapture()
        super.onDestroy()
    }
}
