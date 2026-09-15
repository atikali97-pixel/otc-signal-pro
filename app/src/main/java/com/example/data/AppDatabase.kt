package com.example.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.data.dao.BrokerDao
import com.example.data.dao.CandleDao
import com.example.data.dao.OtcPairDao
import com.example.data.dao.PluginDao
import com.example.data.dao.SignalDao
import com.example.data.entity.BrokerEntity
import com.example.data.entity.CandleEntity
import com.example.data.entity.OtcPairEntity
import com.example.data.entity.PluginEntity
import com.example.data.entity.SignalEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [
        BrokerEntity::class,
        OtcPairEntity::class,
        SignalEntity::class,
        CandleEntity::class,
        PluginEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun brokerDao(): BrokerDao
    abstract fun otcPairDao(): OtcPairDao
    abstract fun signalDao(): SignalDao
    abstract fun candleDao(): CandleDao
    abstract fun pluginDao(): PluginDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "otc_signal_pro_database"
                )
                    .fallbackToDestructiveMigration()
                    .addCallback(DatabaseCallback())
                    .build()
                INSTANCE = instance
                instance
            }
        }

        private class DatabaseCallback : RoomDatabase.Callback() {
            override fun onCreate(db: SupportSQLiteDatabase) {
                super.onCreate(db)
                INSTANCE?.let { database ->
                    CoroutineScope(Dispatchers.IO).launch {
                        populateInitialData(database)
                    }
                }
            }
        }

        suspend fun populateInitialData(db: AppDatabase) {
            val brokerDao = db.brokerDao()
            val otcPairDao = db.otcPairDao()
            val pluginDao = db.pluginDao()

            // 1. Initial Brokers
            val initialBrokers = listOf(
                BrokerEntity(
                    name = "Quotex",
                    url = "https://qxbroker.com/en/sign-in",
                    iconName = "account_balance",
                    isDefault = true
                ),
                BrokerEntity(
                    name = "IQ Option",
                    url = "https://iqoption.com/en/login",
                    iconName = "candlestick_chart",
                    isDefault = false
                ),
                BrokerEntity(
                    name = "Pocket Option",
                    url = "https://pocketoption.com/en/login",
                    iconName = "show_chart",
                    isDefault = false
                ),
                BrokerEntity(
                    name = "Olymp Trade",
                    url = "https://olymptrade.com/en/login",
                    iconName = "stacked_line_chart",
                    isDefault = false
                ),
                BrokerEntity(
                    name = "Binary.com",
                    url = "https://binary.com",
                    iconName = "analytics",
                    isDefault = false
                )
            )
            brokerDao.insertBrokers(initialBrokers)

            // 2. All 35+ OTC Pairs
            val initialPairs = listOf(
                // Major & Common Currencies
                OtcPairEntity(name = "USD/INR OTC", category = "Currencies", payoutPercent = 93, currentPrice = 83.45),
                OtcPairEntity(name = "EUR/USD OTC", category = "Currencies", payoutPercent = 92, currentPrice = 1.0850),
                OtcPairEntity(name = "GBP/USD OTC", category = "Currencies", payoutPercent = 90, currentPrice = 1.2720),
                OtcPairEntity(name = "USD/JPY OTC", category = "Currencies", payoutPercent = 91, currentPrice = 155.80),
                OtcPairEntity(name = "AUD/USD OTC", category = "Currencies", payoutPercent = 88, currentPrice = 0.6650),
                OtcPairEntity(name = "USD/CAD OTC", category = "Currencies", payoutPercent = 89, currentPrice = 1.3680),
                OtcPairEntity(name = "USD/CHF OTC", category = "Currencies", payoutPercent = 87, currentPrice = 0.9020),
                OtcPairEntity(name = "NZD/USD OTC", category = "Currencies", payoutPercent = 86, currentPrice = 0.6120),
                OtcPairEntity(name = "EUR/GBP OTC", category = "Currencies", payoutPercent = 88, currentPrice = 0.8530),
                OtcPairEntity(name = "EUR/JPY OTC", category = "Currencies", payoutPercent = 90, currentPrice = 169.10),
                OtcPairEntity(name = "GBP/JPY OTC", category = "Currencies", payoutPercent = 91, currentPrice = 198.30),
                OtcPairEntity(name = "AUD/JPY OTC", category = "Currencies", payoutPercent = 87, currentPrice = 103.50),
                OtcPairEntity(name = "CAD/JPY OTC", category = "Currencies", payoutPercent = 87, currentPrice = 113.80),
                OtcPairEntity(name = "CHF/JPY OTC", category = "Currencies", payoutPercent = 86, currentPrice = 172.60),
                OtcPairEntity(name = "NZD/JPY OTC", category = "Currencies", payoutPercent = 85, currentPrice = 95.30),
                OtcPairEntity(name = "EUR/CHF OTC", category = "Currencies", payoutPercent = 85, currentPrice = 0.9780),
                OtcPairEntity(name = "EUR/AUD OTC", category = "Currencies", payoutPercent = 88, currentPrice = 1.6310),
                OtcPairEntity(name = "EUR/CAD OTC", category = "Currencies", payoutPercent = 87, currentPrice = 1.4840),
                OtcPairEntity(name = "GBP/AUD OTC", category = "Currencies", payoutPercent = 89, currentPrice = 1.9120),
                OtcPairEntity(name = "GBP/CAD OTC", category = "Currencies", payoutPercent = 88, currentPrice = 1.7400),
                OtcPairEntity(name = "AUD/CAD OTC", category = "Currencies", payoutPercent = 85, currentPrice = 0.9100),
                OtcPairEntity(name = "AUD/CHF OTC", category = "Currencies", payoutPercent = 84, currentPrice = 0.6010),
                OtcPairEntity(name = "AUD/NZD OTC", category = "Currencies", payoutPercent = 85, currentPrice = 1.0860),
                OtcPairEntity(name = "NZD/CAD OTC", category = "Currencies", payoutPercent = 84, currentPrice = 0.8380),
                OtcPairEntity(name = "NZD/CHF OTC", category = "Currencies", payoutPercent = 83, currentPrice = 0.5520),
                OtcPairEntity(name = "CAD/CHF OTC", category = "Currencies", payoutPercent = 85, currentPrice = 0.6590),
                OtcPairEntity(name = "USD/SGD OTC", category = "Currencies", payoutPercent = 86, currentPrice = 1.3520),
                OtcPairEntity(name = "USD/ZAR OTC", category = "Currencies", payoutPercent = 88, currentPrice = 18.25),
                OtcPairEntity(name = "USD/TRY OTC", category = "Currencies", payoutPercent = 85, currentPrice = 32.80),
                OtcPairEntity(name = "USD/MXN OTC", category = "Currencies", payoutPercent = 87, currentPrice = 18.15),
                OtcPairEntity(name = "USD/BDT OTC", category = "Currencies", payoutPercent = 90, currentPrice = 117.50),
                OtcPairEntity(name = "USD/PKR OTC", category = "Currencies", payoutPercent = 90, currentPrice = 278.40),
                OtcPairEntity(name = "USD/NPR OTC", category = "Currencies", payoutPercent = 89, currentPrice = 133.60),

                // Crypto & Commodities
                OtcPairEntity(name = "Bitcoin OTC", category = "Crypto", payoutPercent = 92, currentPrice = 64250.0),
                OtcPairEntity(name = "Ethereum OTC", category = "Crypto", payoutPercent = 91, currentPrice = 3480.0),
                OtcPairEntity(name = "Gold OTC", category = "Commodities", payoutPercent = 94, currentPrice = 2335.50),
                OtcPairEntity(name = "Silver OTC", category = "Commodities", payoutPercent = 90, currentPrice = 29.85),
                OtcPairEntity(name = "Oil OTC", category = "Commodities", payoutPercent = 89, currentPrice = 81.20)
            )
            otcPairDao.insertPairs(initialPairs)

            // 3. Sample Feature Plugins
            val initialPlugins = listOf(
                PluginEntity(
                    name = "15-Second Turbo Scalp",
                    description = "Ultra-fast micro wick-rejection analysis on sub-minute OTC spikes",
                    code = "function analyzeTurbo(candle) { return candle.wickRatio > 0.45 ? 'REVERSAL' : 'CONTINUE'; }",
                    isEnabled = true
                ),
                PluginEntity(
                    name = "Break Of Structure (BOS) Filter",
                    description = "Filters signals only when previous higher-high or lower-low is broken with momentum",
                    code = "function filterBOS(trend, lastHigh, close) { return close > lastHigh && trend === 'UP'; }",
                    isEnabled = true
                ),
                PluginEntity(
                    name = "Martingale Calculator",
                    description = "Auto-calculates next stake at 2.2x to guarantee profit recovery upon loss",
                    code = "function getNextStake(lastResult, prevStake) { return lastResult === 'LOSS' ? prevStake * 2.2 : 10; }",
                    isEnabled = false
                )
            )
            for (plugin in initialPlugins) {
                pluginDao.insertPlugin(plugin)
            }
        }
    }
}
