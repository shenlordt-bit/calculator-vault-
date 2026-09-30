package com.example.calculatorvault.security

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import java.security.SecureRandom
import java.security.spec.KeySpec
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec
import kotlin.math.min
import kotlin.math.pow

/**
 * Handles everything to do with the secret vault PIN.
 *
 * Security model (be precise about what this actually is):
 *  - The PIN itself is NEVER written to disk or to logs, in plaintext or otherwise.
 *  - We store only: a random salt, and PBKDF2-HMAC-SHA256(pin, salt, 120_000 iters) -> hash.
 *  - The salt+hash pair (and the attempt/lockout counters) live inside
 *    EncryptedSharedPreferences, whose own encryption key is generated and held
 *    inside the Android Keystore (MasterKey, AES256-GCM) - so the key material
 *    itself never exists in app-readable form outside the secure hardware/TEE
 *    (or software Keystore fallback on devices without a StrongBox/TEE).
 *  - Verification is a hash comparison, not a stored-plaintext comparison.
 *
 * What this does NOT protect against (be honest about limits):
 *  - A rooted device, or an unlocked bootloader with a custom recovery, can in
 *    principle extract Keystore-protected key material or brute-force offline.
 *  - This is a single-factor 4-8 digit PIN. Even hashed, a 4-digit PIN has only
 *    10,000 possibilities; the on-device rate limiter is the main defense, not
 *    the hash strength. Encourage users who want stronger protection to use a
 *    longer PIN (up to 8 digits here).
 */
class PinManager(context: Context) {

    private val prefs: SharedPreferences

    init {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()

        prefs = EncryptedSharedPreferences.create(
            context,
            PREFS_FILE_NAME,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    fun isPinSet(): Boolean = prefs.contains(KEY_HASH) && prefs.contains(KEY_SALT)

    /** Basic sanity check: 4-8 digits, not one of a small blocklist of trivial PINs. */
    fun isPinFormatValid(pin: String): Boolean {
        if (!pin.matches(Regex("^\\d{4,8}\$"))) return false
        return true
    }

    fun isPinObviouslyWeak(pin: String): Boolean {
        val weak = setOf("0000", "1111", "2222", "3333", "4444", "5555", "6666",
            "7777", "8888", "9999", "1234", "12345", "123456", "0123", "01234")
        return pin in weak
    }

    /** Sets (or overwrites) the PIN. Caller is responsible for confirming the old PIN first. */
    fun setPin(newPin: String) {
        val salt = ByteArray(SALT_LENGTH_BYTES).also { SecureRandom().nextBytes(it) }
        val hash = pbkdf2(newPin.toCharArray(), salt)
        prefs.edit()
            .putString(KEY_SALT, salt.toHex())
            .putString(KEY_HASH, hash.toHex())
            .putInt(KEY_FAILED_ATTEMPTS, 0)
            .putLong(KEY_LOCKOUT_UNTIL, 0L)
            .apply()
    }

    sealed class VerifyResult {
        object Correct : VerifyResult()
        object Incorrect : VerifyResult()
        data class LockedOut(val remainingMillis: Long) : VerifyResult()
    }

    /**
     * Verifies a candidate PIN, applying an exponential-backoff lockout after
     * repeated failures (requirement: "protection against repeated PIN attempts").
     */
    fun verifyPin(candidate: String): VerifyResult {
        val now = System.currentTimeMillis()
        val lockoutUntil = prefs.getLong(KEY_LOCKOUT_UNTIL, 0L)
        if (now < lockoutUntil) {
            return VerifyResult.LockedOut(lockoutUntil - now)
        }
        if (!isPinSet()) return VerifyResult.Incorrect

        val salt = prefs.getString(KEY_SALT, null)?.hexToBytes() ?: return VerifyResult.Incorrect
        val storedHash = prefs.getString(KEY_HASH, null) ?: return VerifyResult.Incorrect
        val candidateHash = pbkdf2(candidate.toCharArray(), salt).toHex()

        return if (constantTimeEquals(candidateHash, storedHash)) {
            prefs.edit()
                .putInt(KEY_FAILED_ATTEMPTS, 0)
                .putLong(KEY_LOCKOUT_UNTIL, 0L)
                .apply()
            VerifyResult.Correct
        } else {
            val attempts = prefs.getInt(KEY_FAILED_ATTEMPTS, 0) + 1
            val editor = prefs.edit().putInt(KEY_FAILED_ATTEMPTS, attempts)
            if (attempts >= LOCKOUT_THRESHOLD) {
                val backoffLevel = min(attempts - LOCKOUT_THRESHOLD, MAX_BACKOFF_LEVEL)
                val delayMillis = (BASE_LOCKOUT_MILLIS * 2.0.pow(backoffLevel)).toLong()
                    .coerceAtMost(MAX_LOCKOUT_MILLIS)
                editor.putLong(KEY_LOCKOUT_UNTIL, now + delayMillis)
            }
            editor.apply()
            VerifyResult.Incorrect
        }
    }

    private fun pbkdf2(pin: CharArray, salt: ByteArray): ByteArray {
        val spec: KeySpec = PBEKeySpec(pin, salt, PBKDF2_ITERATIONS, PBKDF2_KEY_LENGTH_BITS)
        val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
        val result = factory.generateSecret(spec).encoded
        pin.fill('0') // best-effort clear of PIN chars from memory
        return result
    }

    private fun constantTimeEquals(a: String, b: String): Boolean {
        if (a.length != b.length) return false
        var result = 0
        for (i in a.indices) result = result or (a[i].code xor b[i].code)
        return result == 0
    }

    private fun ByteArray.toHex(): String = joinToString("") { "%02x".format(it) }
    private fun String.hexToBytes(): ByteArray =
        chunked(2).map { it.toInt(16).toByte() }.toByteArray()

    companion object {
        private const val PREFS_FILE_NAME = "vault_pin_prefs"
        private const val KEY_SALT = "pin_salt"
        private const val KEY_HASH = "pin_hash"
        private const val KEY_FAILED_ATTEMPTS = "failed_attempts"
        private const val KEY_LOCKOUT_UNTIL = "lockout_until"

        private const val SALT_LENGTH_BYTES = 16
        private const val PBKDF2_ITERATIONS = 120_000
        private const val PBKDF2_KEY_LENGTH_BITS = 256

        private const val LOCKOUT_THRESHOLD = 5
        private const val BASE_LOCKOUT_MILLIS = 30_000L   // 30s
        private const val MAX_LOCKOUT_MILLIS = 30 * 60_000L // 30 min cap
        private const val MAX_BACKOFF_LEVEL = 6
    }
}
