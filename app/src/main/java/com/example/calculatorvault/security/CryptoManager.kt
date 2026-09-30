package com.example.calculatorvault.security

import android.content.Context
import androidx.security.crypto.EncryptedFile
import androidx.security.crypto.MasterKey
import java.io.File

/**
 * Wraps Jetpack Security's EncryptedFile so every vault item on disk is
 * individually encrypted with AES256-GCM, using a key protected by the
 * Android Keystore (MasterKey). Files are written only under the app's
 * private internal storage (filesDir), never to shared/public/external
 * storage, and never left as an unencrypted temp copy.
 */
class CryptoManager(private val context: Context) {

    private val masterKey: MasterKey by lazy {
        MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
    }

    private val vaultDir: File by lazy {
        File(context.filesDir, "vault_store").apply { if (!exists()) mkdirs() }
    }

    fun vaultDirectory(): File = vaultDir

    fun writeEncrypted(fileName: String, plaintext: ByteArray) {
        val target = File(vaultDir, fileName)
        if (target.exists()) target.delete() // EncryptedFile refuses to overwrite existing files
        val encryptedFile = EncryptedFile.Builder(
            context,
            target,
            masterKey,
            EncryptedFile.FileEncryptionScheme.AES256_GCM_HKDF_4KB
        ).build()
        encryptedFile.openFileOutput().use { it.write(plaintext) }
    }

    fun readDecrypted(fileName: String): ByteArray {
        val target = File(vaultDir, fileName)
        val encryptedFile = EncryptedFile.Builder(
            context,
            target,
            masterKey,
            EncryptedFile.FileEncryptionScheme.AES256_GCM_HKDF_4KB
        ).build()
        return encryptedFile.openFileInput().use { it.readBytes() }
    }

    fun delete(fileName: String): Boolean = File(vaultDir, fileName).delete()

    fun listFiles(): List<String> = vaultDir.list()?.toList().orEmpty()
}
