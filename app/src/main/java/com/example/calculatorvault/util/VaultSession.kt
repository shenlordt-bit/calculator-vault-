package com.example.calculatorvault.util

/**
 * Purely in-memory session flag. Nothing here is persisted to disk, so a
 * process restart (e.g. app killed by the system) always starts locked,
 * which is the safe default.
 */
object VaultSession {

    @Volatile
    private var unlocked: Boolean = false

    fun unlock() {
        unlocked = true
    }

    fun lock() {
        unlocked = false
    }

    fun isUnlocked(): Boolean = unlocked
}
