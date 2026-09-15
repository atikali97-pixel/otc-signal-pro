package com.example.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "brokers")
data class BrokerEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val url: String,
    val iconName: String = "trending_up",
    val isDefault: Boolean = false,
    val sessionCookies: String = "",
    val lastUsed: Long = System.currentTimeMillis()
)

@Entity(tableName = "otc_pairs")
data class OtcPairEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val category: String, // "Currencies", "Crypto", "Commodities"
    val isMonitored: Boolean = true,
    val payoutPercent: Int = 92,
    val currentPrice: Double = 1.0000
)

@Entity(tableName = "signals")
data class SignalEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val pairName: String,
    val type: String, // "CALL" or "PUT"
    val expiryMinutes: Int, // 1 or 5
    val confidence: Int, // 0 - 100%
    val confluenceReasons: String,
    val timestamp: Long = System.currentTimeMillis(),
    val entryPrice: Double = 0.0,
    val exitPrice: Double = 0.0,
    val result: String = "PENDING", // "WIN", "LOSS", "TIE", "PENDING"
    val profitAmount: Double = 0.0
)

@Entity(tableName = "candles")
data class CandleEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val pairName: String,
    val timestamp: Long,
    val open: Double,
    val high: Double,
    val low: Double,
    val close: Double,
    val isBullish: Boolean,
    val bodySize: Double,
    val upperWick: Double,
    val lowerWick: Double
)

@Entity(tableName = "plugins")
data class PluginEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val description: String,
    val code: String,
    val isEnabled: Boolean = true,
    val version: String = "1.0.0",
    val createdAt: Long = System.currentTimeMillis()
)
