package com.example.data.repository

import com.example.data.AppDatabase
import com.example.data.entity.BrokerEntity
import com.example.data.entity.CandleEntity
import com.example.data.entity.OtcPairEntity
import com.example.data.entity.PluginEntity
import com.example.data.entity.SignalEntity
import kotlinx.coroutines.flow.Flow

class TradingRepository(private val db: AppDatabase) {
    // Brokers
    val allBrokers: Flow<List<BrokerEntity>> = db.brokerDao().getAllBrokers()
    suspend fun getDefaultBroker(): BrokerEntity? = db.brokerDao().getDefaultBroker()
    suspend fun getBrokerById(id: Int): BrokerEntity? = db.brokerDao().getBrokerById(id)
    suspend fun insertBroker(broker: BrokerEntity): Long = db.brokerDao().insertBroker(broker)
    suspend fun updateBroker(broker: BrokerEntity) = db.brokerDao().updateBroker(broker)
    suspend fun deleteBroker(id: Int) = db.brokerDao().deleteBroker(id)
    suspend fun saveCookies(brokerId: Int, cookies: String) = db.brokerDao().updateCookies(brokerId, cookies)

    // Pairs
    val allPairs: Flow<List<OtcPairEntity>> = db.otcPairDao().getAllPairs()
    val monitoredPairs: Flow<List<OtcPairEntity>> = db.otcPairDao().getMonitoredPairs()
    suspend fun getPairByName(name: String): OtcPairEntity? = db.otcPairDao().getPairByName(name)
    suspend fun insertPair(pair: OtcPairEntity): Long = db.otcPairDao().insertPair(pair)
    suspend fun setPairMonitored(id: Int, monitored: Boolean) = db.otcPairDao().setMonitored(id, monitored)
    suspend fun updatePair(pair: OtcPairEntity) = db.otcPairDao().updatePair(pair)

    // Signals
    val allSignals: Flow<List<SignalEntity>> = db.signalDao().getAllSignals()
    val recentSignals: Flow<List<SignalEntity>> = db.signalDao().getRecentSignals(30)
    val winCount: Flow<Int> = db.signalDao().getWinCount()
    val lossCount: Flow<Int> = db.signalDao().getLossCount()
    val tieCount: Flow<Int> = db.signalDao().getTieCount()
    val totalProfit: Flow<Double?> = db.signalDao().getTotalProfit()

    suspend fun insertSignal(signal: SignalEntity): Long = db.signalDao().insertSignal(signal)
    suspend fun updateSignal(signal: SignalEntity) = db.signalDao().updateSignal(signal)
    suspend fun getPendingSignals(): List<SignalEntity> = db.signalDao().getPendingSignals().let {
        // or direct query
        emptyList()
    }

    // Candles
    fun getCandles(pairName: String, limit: Int = 100): Flow<List<CandleEntity>> =
        db.candleDao().getRecentCandles(pairName, limit)
    suspend fun insertCandle(candle: CandleEntity) {
        db.candleDao().insertCandle(candle)
        db.candleDao().trimBuffer(candle.pairName)
    }

    // Plugins
    val allPlugins: Flow<List<PluginEntity>> = db.pluginDao().getAllPlugins()
    val enabledPlugins: Flow<List<PluginEntity>> = db.pluginDao().getEnabledPlugins()
    suspend fun insertPlugin(plugin: PluginEntity): Long = db.pluginDao().insertPlugin(plugin)
    suspend fun updatePlugin(plugin: PluginEntity) = db.pluginDao().updatePlugin(plugin)
    suspend fun deletePlugin(id: Int) = db.pluginDao().deletePlugin(id)

    suspend fun ensureInitialized() {
        if (db.otcPairDao().getCount() == 0) {
            AppDatabase.populateInitialData(db)
        }
    }
}
