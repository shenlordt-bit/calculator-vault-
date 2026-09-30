package com.example.calculatorvault.vault

import com.example.calculatorvault.security.CryptoManager
import java.nio.charset.StandardCharsets

data class VaultNote(
    val fileName: String,
    val createdAtMillis: Long,
    val preview: String
)

/**
 * V1 "basic local vault" = simple encrypted text notes.
 * Each note is its own AES-GCM encrypted file (see CryptoManager). There is
 * intentionally no separate plaintext index/database of note titles or
 * contents - the file list itself (timestamps only) is the only metadata,
 * and even that lives inside the app's private storage, inaccessible to
 * other apps without root.
 *
 * Photo/video vault, import/export and a decoy vault are explicitly out of
 * scope for V1 (see README, "Tahapan").
 */
class VaultRepository(private val crypto: CryptoManager) {

    fun listNotes(): List<VaultNote> {
        return crypto.listFiles()
            .filter { it.startsWith(PREFIX) && it.endsWith(SUFFIX) }
            .mapNotNull { fileName -> runCatching { toNote(fileName) }.getOrNull() }
            .sortedByDescending { it.createdAtMillis }
    }

    fun readNoteContent(fileName: String): String {
        return String(crypto.readDecrypted(fileName), StandardCharsets.UTF_8)
    }

    fun addNote(content: String) {
        val timestamp = System.currentTimeMillis()
        val fileName = "$PREFIX${timestamp}$SUFFIX"
        crypto.writeEncrypted(fileName, content.toByteArray(StandardCharsets.UTF_8))
    }

    fun deleteNote(fileName: String) {
        crypto.delete(fileName)
    }

    private fun toNote(fileName: String): VaultNote {
        val timestampPart = fileName.removePrefix(PREFIX).removeSuffix(SUFFIX)
        val timestamp = timestampPart.toLong()
        val content = readNoteContent(fileName)
        val preview = content.take(60).replace("\n", " ")
        return VaultNote(fileName, timestamp, preview)
    }

    companion object {
        private const val PREFIX = "note_"
        private const val SUFFIX = ".enc"
    }
}
