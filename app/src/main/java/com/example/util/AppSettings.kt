package com.example.util

import android.content.Context
import android.content.SharedPreferences
import com.example.BuildConfig

class AppSettings(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("otc_signal_pro_prefs", Context.MODE_PRIVATE)

    var geminiApiKey: String
        get() {
            val userKey = prefs.getString("user_gemini_api_key", "") ?: ""
            if (userKey.isNotBlank()) return userKey
            // Fallback to BuildConfig injected key if provided in .env
            val buildConfigKey = BuildConfig.GEMINI_API_KEY
            return if (buildConfigKey != "MY_GEMINI_API_KEY") buildConfigKey else ""
        }
        set(value) {
            prefs.edit().putString("user_gemini_api_key", value.trim()).apply()
        }

    var defaultExpiryMinutes: Int
        get() = prefs.getInt("default_expiry_minutes", 1)
        set(value) = prefs.edit().putInt("default_expiry_minutes", value).apply()

    var isSystemActive: Boolean
        get() = prefs.getBoolean("is_system_active", true)
        set(value) = prefs.edit().putBoolean("is_system_active", value).apply()

    var isVoiceAssistantEnabled: Boolean
        get() = prefs.getBoolean("voice_assistant_enabled", true)
        set(value) = prefs.edit().putBoolean("voice_assistant_enabled", value).apply()

    var isVibrationEnabled: Boolean
        get() = prefs.getBoolean("vibration_enabled", true)
        set(value) = prefs.edit().putBoolean("vibration_enabled", value).apply()

    var isNotificationsEnabled: Boolean
        get() = prefs.getBoolean("notifications_enabled", true)
        set(value) = prefs.edit().putBoolean("notifications_enabled", value).apply()

    var selectedPairName: String
        get() = prefs.getString("selected_pair_name", "USD/INR OTC") ?: "USD/INR OTC"
        set(value) = prefs.edit().putString("selected_pair_name", value).apply()

    var hasCompletedPermissionWizard: Boolean
        get() = prefs.getBoolean("has_completed_permission_wizard", false)
        set(value) = prefs.edit().putBoolean("has_completed_permission_wizard", value).apply()

    fun hasValidApiKey(): Boolean = geminiApiKey.isNotBlank()
}
