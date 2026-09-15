package com.example.ai

import android.graphics.Bitmap
import android.util.Base64
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.util.concurrent.TimeUnit

class GeminiApiClient(private val apiKeyProvider: () -> String) {

    private val client = OkHttpClient.Builder()
        .connectTimeout(45, TimeUnit.SECONDS)
        .readTimeout(45, TimeUnit.SECONDS)
        .writeTimeout(45, TimeUnit.SECONDS)
        .build()

    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()

    /**
     * Multimodal chart vision analysis using Gemini Vision
     */
    suspend fun analyzeChartVision(bitmap: Bitmap, pairName: String): String = withContext(Dispatchers.IO) {
        val apiKey = apiKeyProvider()
        if (apiKey.isBlank()) {
            return@withContext "Boss, pehle Settings mein Gemini API key daalo!"
        }

        try {
            val base64Image = bitmapToBase64(bitmap)
            val prompt = """
                You are Lucifer, an elite OTC binary price action trading analyst.
                Analyze this real-time candlestick chart screenshot for $pairName:
                1. Identify candlestick patterns (Pin bar, Hammer, Engulfing, Marubozu, Doji).
                2. Check Support & Resistance levels and rejection wicks.
                3. Check Break of Structure (BOS) and momentum direction.
                4. Give a definitive conclusion: CALL (BUY) or PUT (SELL) with confidence percentage and reasons.
                Speak in a crisp, confident Hinglish tone as Lucifer.
            """.trimIndent()

            val requestJson = JSONObject().apply {
                val contents = JSONArray().apply {
                    val contentObj = JSONObject().apply {
                        val parts = JSONArray().apply {
                            put(JSONObject().put("text", prompt))
                            put(JSONObject().apply {
                                val inlineData = JSONObject().apply {
                                    put("mimeType", "image/jpeg")
                                    put("data", base64Image)
                                }
                                put("inlineData", inlineData)
                            })
                        }
                        put("parts", parts)
                    }
                    put(contentObj)
                }
                put("contents", contents)
            }

            val url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent?key=$apiKey"
            val request = Request.Builder()
                .url(url)
                .post(requestJson.toString().toRequestBody(jsonMediaType))
                .build()

            val response = client.newCall(request).execute()
            val responseBody = response.body?.string() ?: ""
            if (!response.isSuccessful) {
                return@withContext "Analysis API error: ${response.code} (Check API key in Settings)"
            }

            parseGeminiTextResponse(responseBody)
        } catch (e: Exception) {
            "Vision analysis error: ${e.localizedMessage ?: "Unknown error"}"
        }
    }

    /**
     * Lucifer Voice & Command Chat
     */
    suspend fun chatWithLucifer(userMessage: String, contextInfo: String): LuciferResponse = withContext(Dispatchers.IO) {
        val apiKey = apiKeyProvider()
        if (apiKey.isBlank()) {
            return@withContext LuciferResponse(
                reply = "Boss, pehle Settings mein Gemini API key daalo!",
                detectedCommand = null,
                commandArgument = null
            )
        }

        try {
            val systemPrompt = """
                You are LUCIFER, the ultra-smart, confident, friendly, and witty AI trading voice assistant inside "OTC Signal Pro".
                You speak fluent Hinglish (Hindi + English mix), friendly and direct ("Boss, ...").
                
                Current app context:
                $contextInfo
                
                You can execute voice commands. If user wants to execute an action, output a special command tag at the very end of your reply:
                Commands available:
                [[CMD:OPEN_BROKER:url]] -> e.g. [[CMD:OPEN_BROKER:https://qxbroker.com/en/sign-in]]
                [[CMD:START_RECORDING]]
                [[CMD:STOP_RECORDING]]
                [[CMD:GENERATE_SIGNAL:pair]]
                [[CMD:SET_EXPIRY:minutes]] -> e.g. [[CMD:SET_EXPIRY:1]] or [[CMD:SET_EXPIRY:5]]
                [[CMD:SELECT_PAIR:pairName]] -> e.g. [[CMD:SELECT_PAIR:EUR/USD OTC]]
                [[CMD:GET_STATS]]
                [[CMD:ADD_FEATURE:featureDescription]]
                [[CMD:STOP_LISTENING]]
                
                Keep your spoken response under 2 sentences, crisp and energetic.
            """.trimIndent()

            val requestJson = JSONObject().apply {
                val contents = JSONArray().apply {
                    val contentObj = JSONObject().apply {
                        val parts = JSONArray().apply {
                            put(JSONObject().put("text", userMessage))
                        }
                        put("parts", parts)
                    }
                    put(contentObj)
                }
                put("contents", contents)
                put("systemInstruction", JSONObject().apply {
                    put("parts", JSONArray().apply {
                        put(JSONObject().put("text", systemPrompt))
                    })
                })
            }

            val url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent?key=$apiKey"
            val request = Request.Builder()
                .url(url)
                .post(requestJson.toString().toRequestBody(jsonMediaType))
                .build()

            val response = client.newCall(request).execute()
            val responseBody = response.body?.string() ?: ""
            if (!response.isSuccessful) {
                return@withContext LuciferResponse(
                    reply = "Boss, API connect nahi ho pa raha: error ${response.code}",
                    detectedCommand = null,
                    commandArgument = null
                )
            }

            val fullText = parseGeminiTextResponse(responseBody)
            extractCommandAndReply(fullText)
        } catch (e: Exception) {
            LuciferResponse(
                reply = "Network issue ho gaya boss: ${e.localizedMessage}",
                detectedCommand = null,
                commandArgument = null
            )
        }
    }

    /**
     * Generate custom AI Feature Plugin code
     */
    suspend fun generateFeaturePluginCode(featureDescription: String): String = withContext(Dispatchers.IO) {
        val apiKey = apiKeyProvider()
        if (apiKey.isBlank()) {
            return@withContext "// Gemini API key required in Settings to generate plugin"
        }

        try {
            val prompt = """
                Write a JavaScript plugin for OTC Signal Pro based on this user feature request:
                "$featureDescription"
                
                The plugin function should be named executePlugin(candleData, currentSignal) and return an object:
                { modifiedAction: 'CALL'|'PUT'|'PASS', reason: 'explanation', confidenceAdjustment: number }
                Only output clean JavaScript code, no markdown backticks.
            """.trimIndent()

            val requestJson = JSONObject().apply {
                val contents = JSONArray().apply {
                    val contentObj = JSONObject().apply {
                        val parts = JSONArray().apply {
                            put(JSONObject().put("text", prompt))
                        }
                        put("parts", parts)
                    }
                    put(contentObj)
                }
                put("contents", contents)
            }

            val url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent?key=$apiKey"
            val request = Request.Builder()
                .url(url)
                .post(requestJson.toString().toRequestBody(jsonMediaType))
                .build()

            val response = client.newCall(request).execute()
            val responseBody = response.body?.string() ?: ""
            val raw = parseGeminiTextResponse(responseBody)
            raw.replace("```javascript", "").replace("```js", "").replace("```", "").trim()
        } catch (e: Exception) {
            "// Error generating feature: ${e.localizedMessage}"
        }
    }

    private fun parseGeminiTextResponse(jsonString: String): String {
        return try {
            val root = JSONObject(jsonString)
            val candidates = root.optJSONArray("candidates") ?: return "No response"
            if (candidates.length() == 0) return "No candidates"
            val candidate = candidates.getJSONObject(0)
            val content = candidate.optJSONObject("content") ?: return "No content"
            val parts = content.optJSONArray("parts") ?: return "No parts"
            val sb = StringBuilder()
            for (i in 0 until parts.length()) {
                sb.append(parts.getJSONObject(i).optString("text", ""))
            }
            sb.toString().trim()
        } catch (e: Exception) {
            "Response parse error: ${e.message}"
        }
    }

    private fun extractCommandAndReply(fullText: String): LuciferResponse {
        val cmdRegex = "\\[\\[CMD:([A-Z_]+)(?::([^\\]]+))?\\]\\]".toRegex()
        val match = cmdRegex.find(fullText)
        val cleanReply = fullText.replace(cmdRegex, "").trim()

        return if (match != null) {
            val cmd = match.groupValues.getOrNull(1)
            val arg = match.groupValues.getOrNull(2)
            LuciferResponse(cleanReply, cmd, arg)
        } else {
            LuciferResponse(cleanReply, null, null)
        }
    }

    private fun bitmapToBase64(bitmap: Bitmap): String {
        val outputStream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 75, outputStream)
        val byteArray = outputStream.toByteArray()
        return Base64.encodeToString(byteArray, Base64.NO_WRAP)
    }
}

data class LuciferResponse(
    val reply: String,
    val detectedCommand: String?,
    val commandArgument: String?
)
