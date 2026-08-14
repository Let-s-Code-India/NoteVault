package com.example.data.security

import android.content.Context
import android.util.Base64
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import java.security.MessageDigest
import java.security.SecureRandom

object SecurityManager {
    private const val PREF_FILE = "notevault_encrypted_prefs"
    private const val KEY_DB_PASSPHRASE = "db_passphrase_b64"
    private const val KEY_APP_PIN_HASH = "app_pin_salted_hash"
    private const val KEY_PIN_SALT = "pin_salt_b64"
    private const val KEY_BIOMETRIC_ENABLED = "biometric_enabled"

    private fun getEncryptedPreferences(context: Context) = try {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
        EncryptedSharedPreferences.create(
            context,
            PREF_FILE,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    } catch (e: Exception) {
        context.getSharedPreferences(PREF_FILE, Context.MODE_PRIVATE)
    }

    fun getDatabasePassphrase(context: Context): ByteArray {
        val prefs = getEncryptedPreferences(context)
        var passphraseB64 = prefs.getString(KEY_DB_PASSPHRASE, null)
        if (passphraseB64 == null) {
            val randomBytes = ByteArray(32)
            SecureRandom().nextBytes(randomBytes)
            passphraseB64 = Base64.encodeToString(randomBytes, Base64.NO_WRAP)
            prefs.edit().putString(KEY_DB_PASSPHRASE, passphraseB64).apply()
        }
        return Base64.decode(passphraseB64, Base64.NO_WRAP)
    }

    fun getSalt(context: Context): String {
        val prefs = getEncryptedPreferences(context)
        var salt = prefs.getString(KEY_PIN_SALT, null)
        if (salt == null) {
            val randomBytes = ByteArray(16)
            SecureRandom().nextBytes(randomBytes)
            salt = Base64.encodeToString(randomBytes, Base64.NO_WRAP)
            prefs.edit().putString(KEY_PIN_SALT, salt).apply()
        }
        return salt
    }

    fun hashPin(pin: String, salt: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val combined = "$salt:$pin".toByteArray(Charsets.UTF_8)
        val hash = digest.digest(combined)
        return Base64.encodeToString(hash, Base64.NO_WRAP)
    }

    fun saveAppPin(context: Context, pin: String) {
        val salt = getSalt(context)
        val hash = hashPin(pin, salt)
        getEncryptedPreferences(context).edit().putString(KEY_APP_PIN_HASH, hash).apply()
    }

    fun verifyAppPin(context: Context, inputPin: String): Boolean {
        val prefs = getEncryptedPreferences(context)
        val storedHash = prefs.getString(KEY_APP_PIN_HASH, null) ?: return false
        val salt = getSalt(context)
        return hashPin(inputPin, salt) == storedHash
    }

    fun hasAppPin(context: Context): Boolean {
        return getEncryptedPreferences(context).contains(KEY_APP_PIN_HASH)
    }

    fun verifyNotePin(context: Context, inputPin: String, notePinHash: String?): Boolean {
        if (notePinHash.isNull_or_blank()) return true
        val salt = getSalt(context)
        val inputHash = hashPin(inputPin, salt)
        return inputHash == notePinHash
    }

    fun setBiometricEnabled(context: Context, enabled: Boolean) {
        getEncryptedPreferences(context).edit().putBoolean(KEY_BIOMETRIC_ENABLED, enabled).apply()
    }

    fun isBiometricEnabled(context: Context): Boolean {
        return getEncryptedPreferences(context).getBoolean(KEY_BIOMETRIC_ENABLED, false)
    }

    fun isBiometricAvailable(context: Context): Boolean {
        val biometricManager = BiometricManager.from(context)
        val result = biometricManager.canAuthenticate(
            BiometricManager.Authenticators.BIOMETRIC_STRONG or BiometricManager.Authenticators.BIOMETRIC_WEAK
        )
        return result == BiometricManager.BIOMETRIC_SUCCESS
    }

    fun showBiometricPrompt(
        activity: FragmentActivity,
        title: String = "Vault Biometric Unlock",
        subtitle: String = "Confirm fingerprint or face to unlock",
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        val executor = ContextCompat.getMainExecutor(activity)
        val callback = object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                super.onAuthenticationSucceeded(result)
                onSuccess()
            }

            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                super.onAuthenticationError(errorCode, errString)
                onError(errString.toString())
            }
        }

        val prompt = BiometricPrompt(activity, executor, callback)
        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle(title)
            .setSubtitle(subtitle)
            .setNegativeButtonText("Use PIN")
            .setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_STRONG or BiometricManager.Authenticators.BIOMETRIC_WEAK)
            .build()

        prompt.authenticate(promptInfo)
    }

    private fun String?.isNull_or_blank(): Boolean = this == null || this.trim().isEmpty()
}
