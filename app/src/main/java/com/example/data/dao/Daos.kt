package com.example.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.entity.BrokerEntity
import com.example.data.entity.CandleEntity
import com.example.data.entity.OtcPairEntity
import com.example.data.entity.PluginEntity
import com.example.data.entity.SignalEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface BrokerDao {
    @Query("SELECT * FROM brokers ORDER BY isDefault DESC, lastUsed DESC")
    fun getAllBrokers(): Flow<List<BrokerEntity>>

    @Query("SELECT * FROM brokers WHERE id = :id LIMIT 1")
    suspend fun getBrokerById(id: Int): BrokerEntity?

    @Query("SELECT * FROM brokers WHERE isDefault = 1 LIMIT 1")
    suspend fun getDefaultBroker(): BrokerEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBroker(broker: BrokerEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBrokers(brokers: List<BrokerEntity>)

    @Update
    suspend fun updateBroker(broker: BrokerEntity)

    @Query("DELETE FROM brokers WHERE id = :id")
    suspend fun deleteBroker(id: Int)

    @Query("UPDATE brokers SET sessionCookies = :cookies WHERE id = :brokerId")
    suspend fun updateCookies(brokerId: Int, cookies: String)
}

@Dao
interface OtcPairDao {
    @Query("SELECT * FROM otc_pairs ORDER BY category ASC, name ASC")
    fun getAllPairs(): Flow<List<OtcPairEntity>>

    @Query("SELECT * FROM otc_pairs WHERE isMonitored = 1")
    fun getMonitoredPairs(): Flow<List<OtcPairEntity>>

    @Query("SELECT * FROM otc_pairs WHERE name = :name LIMIT 1")
    suspend fun getPairByName(name: String): OtcPairEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPairs(pairs: List<OtcPairEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPair(pair: OtcPairEntity): Long

    @Update
    suspend fun updatePair(pair: OtcPairEntity)

    @Query("UPDATE otc_pairs SET isMonitored = :monitored WHERE id = :id")
    suspend fun setMonitored(id: Int, monitored: Boolean)

    @Query("SELECT COUNT(*) FROM otc_pairs")
    suspend fun getCount(): Int
}

@Dao
interface SignalDao {
    @Query("SELECT * FROM signals ORDER BY timestamp DESC")
    fun getAllSignals(): Flow<List<SignalEntity>>

    @Query("SELECT * FROM signals ORDER BY timestamp DESC LIMIT :limit")
    fun getRecentSignals(limit: Int): Flow<List<SignalEntity>>

    @Query("SELECT * FROM signals WHERE result = 'PENDING' ORDER BY timestamp DESC")
    fun getPendingSignals(): Flow<List<SignalEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSignal(signal: SignalEntity): Long

    @Update
    suspend fun updateSignal(signal: SignalEntity)

    @Query("SELECT COUNT(*) FROM signals WHERE result = 'WIN'")
    fun getWinCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM signals WHERE result = 'LOSS'")
    fun getLossCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM signals WHERE result = 'TIE'")
    fun getTieCount(): Flow<Int>

    @Query("SELECT SUM(profitAmount) FROM signals")
    fun getTotalProfit(): Flow<Double?>
}

@Dao
interface CandleDao {
    @Query("SELECT * FROM candles WHERE pairName = :pairName ORDER BY timestamp DESC LIMIT :limit")
    fun getRecentCandles(pairName: String, limit: Int = 100): Flow<List<CandleEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCandles(candles: List<CandleEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCandle(candle: CandleEntity)

    @Query("DELETE FROM candles WHERE pairName = :pairName AND id NOT IN (SELECT id FROM candles WHERE pairName = :pairName ORDER BY timestamp DESC LIMIT 100)")
    suspend fun trimBuffer(pairName: String)
}

@Dao
interface PluginDao {
    @Query("SELECT * FROM plugins ORDER BY id DESC")
    fun getAllPlugins(): Flow<List<PluginEntity>>

    @Query("SELECT * FROM plugins WHERE isEnabled = 1")
    fun getEnabledPlugins(): Flow<List<PluginEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlugin(plugin: PluginEntity): Long

    @Update
    suspend fun updatePlugin(plugin: PluginEntity)

    @Query("DELETE FROM plugins WHERE id = :id")
    suspend fun deletePlugin(id: Int)
}
