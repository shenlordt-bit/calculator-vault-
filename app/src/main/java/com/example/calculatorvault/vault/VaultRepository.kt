package com.example.calculatorvault.vault

import com.example.calculatorvault.security.CryptoManager
import java.nio.charset.StandardCharsets

data class VaultNote(
    val fileName: String,
    val createdAtMillis: Long,
    val preview: String
)

class VaultRepository(private val crypto: CryptoManager) {

    fun listNotes(): List<VaultNote> {
        return parseIndex(readIndexRaw()).sortedByDescending { it.createdAtMillis }
    }

    fun readNoteContent(fileName: String): String {
        return String(crypto.readDecrypted(fileName), StandardCharsets.UTF_8)
    }

    fun addNote(content: String) {
        val timestamp = System.currentTimeMillis()
        val fileName = "$PREFIX${timestamp}$SUFFIX"
        crypto.writeEncrypted(fileName, content.toByteArray(StandardCharsets.UTF_8))
        val preview = content.take(60).replace("\n", " ").replace("\t", " ")
        appendToIndex(fileName, timestamp, preview)
    }

    fun deleteNote(fileName: String) {
        crypto.delete(fileName)
        removeFromIndex(fileName)
    }

    private fun parseIndex(raw: String): List<VaultNote> {
        if (raw.isBlank()) return emptyList()
        return raw.lines().mapNotNull { line ->
            if (line.isBlank()) return@mapNotNull null
            val parts = line.split('\t')
            if (parts.size < 2) return@mapNotNull null
            val fileName = parts[0]
            val timestamp = parts[1].toLongOrNull() ?: return@mapNotNull null
            val preview = if (parts.size >= 3) parts.subList(2, parts.size).joinToString("\t") else ""
            VaultNote(fileName, timestamp, preview)
        }
    }

    private fun readIndexRaw(): String {
        return try {
            String(crypto.readDecrypted(INDEX_FILE_NAME), StandardCharsets.UTF_8)
        } catch (e: Exception) {
            ""
        }
    }

    private fun writeIndexRaw(content: String) {
        crypto.writeEncrypted(INDEX_FILE_NAME, content.toByteArray(StandardCharsets.UTF_8))
    }

    private fun appendToIndex(fileName: String, timestamp: Long, preview: String) {
        val current = readIndexRaw()
        val newLine = "$fileName\t$timestamp\t$preview"
        writeIndexRaw(if (current.isBlank()) newLine else "$current\n$newLine")
    }

    private fun removeFromIndex(fileName: String) {
        val current = readIndexRaw()
        val updated = current.lines().filter { !it.startsWith("$fileName\t") }.joinToString("\n")
        writeIndexRaw(updated)
    }

    companion object {
        private const val PREFIX = "note_"
        private const val SUFFIX = ".enc"
        private const val INDEX_FILE_NAME = "index.enc"
    }
}
