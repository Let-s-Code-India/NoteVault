package com.example.platform

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

interface PlatformSettings {
    fun getString(key: String, default: String? = null): String?
    fun putString(key: String, value: String)
    fun getBoolean(key: String, default: Boolean = false): Boolean
    fun putBoolean(key: String, value: Boolean)
    fun getLong(key: String, default: Long = 0L): Long
    fun putLong(key: String, value: Long)
    fun contains(key: String): Boolean
    fun remove(key: String)
    fun clear()

    companion object {
        const val KEY_ONBOARDING_CONSENTED = "onboarding_consent_agreed_v1"
        const val KEY_APP_PIN_HASH = "app_pin_salted_hash"
        const val KEY_PIN_SALT = "pin_salt_b64"
        const val KEY_BIOMETRIC_ENABLED = "biometric_enabled"
        const val KEY_DB_PASSPHRASE = "db_passphrase_b64"
        const val KEY_DARK_THEME_MODE = "dark_theme_mode"
    }
}

class AndroidPlatformSettings(private val context: Context) : PlatformSettings {
    private val prefs: SharedPreferences by lazy {
        try {
            val masterKey = MasterKey.Builder(context)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build()
            EncryptedSharedPreferences.create(
                context,
                "notevault_kmp_secure_prefs",
                masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            )
        } catch (e: Exception) {
            context.getSharedPreferences("notevault_kmp_secure_prefs", Context.MODE_PRIVATE)
        }
    }

    override fun getString(key: String, default: String?): String? = prefs.getString(key, default)

    override fun putString(key: String, value: String) {
        prefs.edit().putString(key, value).apply()
    }

    override fun getBoolean(key: String, default: Boolean): Boolean = prefs.getBoolean(key, default)

    override fun putBoolean(key: String, value: Boolean) {
        prefs.edit().putBoolean(key, value).apply()
    }

    override fun getLong(key: String, default: Long): Long = prefs.getLong(key, default)

    override fun putLong(key: String, value: Long) {
        prefs.edit().putLong(key, value).apply()
    }

    override fun contains(key: String): Boolean = prefs.contains(key)

    override fun remove(key: String) {
        prefs.edit().remove(key).apply()
    }

    override fun clear() {
        prefs.edit().clear().apply()
    }
}

private var androidAppContext: Context? = null

fun initAndroidPlatformContext(context: Context) {
    androidAppContext = context.applicationContext
}

fun getAndroidPlatformContext(): Context {
    return androidAppContext ?: error("Android Platform context not initialized")
}

fun createPlatformSettings(): PlatformSettings {
    val ctx = androidAppContext ?: error("Android context not initialized before calling createPlatformSettings")
    return AndroidPlatformSettings(ctx)
}
