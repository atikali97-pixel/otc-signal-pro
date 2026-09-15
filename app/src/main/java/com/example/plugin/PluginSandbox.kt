package com.example.plugin

import android.content.Context
import android.webkit.WebView
import com.example.data.entity.CandleEntity
import com.example.data.entity.PluginEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import org.json.JSONObject
import kotlin.coroutines.resume

interface FeaturePlugin {
    val name: String
    val description: String
    val isEnabled: Boolean
    suspend fun execute(candle: CandleEntity, currentSignal: String): PluginResult
}

data class PluginResult(
    val modifiedAction: String, // "CALL", "PUT", or "PASS"
    val reason: String,
    val confidenceAdjustment: Int
)

class PluginSandbox(private val context: Context) {

    private var headlessWebView: WebView? = null

    private fun getOrCreateWebView(): WebView? {
        if (headlessWebView == null) {
            try {
                headlessWebView = WebView(context).apply {
                    settings.javaScriptEnabled = true
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        return headlessWebView
    }

    suspend fun runPlugin(plugin: PluginEntity, candle: CandleEntity, currentSignal: String): PluginResult =
        withContext(Dispatchers.Main) {
            val webView = getOrCreateWebView() ?: return@withContext PluginResult(currentSignal, "Sandbox unavailable", 0)

            val candleJson = JSONObject().apply {
                put("open", candle.open)
                put("high", candle.high)
                put("low", candle.low)
                put("close", candle.close)
                put("isBullish", candle.isBullish)
                put("bodySize", candle.bodySize)
                put("upperWick", candle.upperWick)
                put("lowerWick", candle.lowerWick)
                put("wickRatio", if (candle.high - candle.low > 0) (candle.upperWick + candle.lowerWick) / (candle.high - candle.low) else 0.0)
            }

            val script = """
                (function() {
                    try {
                        ${plugin.code}
                        var candle = $candleJson;
                        var currentSignal = '$currentSignal';
                        if (typeof executePlugin === 'function') {
                            return JSON.stringify(executePlugin(candle, currentSignal));
                        } else if (typeof analyzeTurbo === 'function') {
                            var res = analyzeTurbo(candle);
                            return JSON.stringify({ modifiedAction: currentSignal, reason: 'Turbo: ' + res, confidenceAdjustment: 5 });
                        } else {
                            return JSON.stringify({ modifiedAction: currentSignal, reason: 'Default pass-through', confidenceAdjustment: 0 });
                        }
                    } catch(err) {
                        return JSON.stringify({ modifiedAction: currentSignal, reason: 'Error: ' + err.message, confidenceAdjustment: 0 });
                    }
                })();
            """.trimIndent()

            suspendCancellableCoroutine { continuation ->
                webView.evaluateJavascript(script) { resultString ->
                    try {
                        val cleanJson = if (resultString != null && resultString.startsWith("\"") && resultString.endsWith("\"")) {
                            // Unescape quotes
                            resultString.substring(1, resultString.length - 1).replace("\\\"", "\"")
                        } else {
                            resultString ?: "{}"
                        }
                        val json = JSONObject(cleanJson)
                        val action = json.optString("modifiedAction", currentSignal)
                        val reason = json.optString("reason", "Plugin evaluated successfully")
                        val confAdj = json.optInt("confidenceAdjustment", 0)
                        continuation.resume(PluginResult(action, reason, confAdj))
                    } catch (e: Exception) {
                        continuation.resume(PluginResult(currentSignal, "Plugin JS parse: ${e.message}", 0))
                    }
                }
            }
        }
}
