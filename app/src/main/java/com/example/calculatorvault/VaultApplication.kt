package com.example.calculatorvault

import android.app.Application
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import com.example.calculatorvault.util.VaultSession

/**
 * Registers a process-wide lifecycle observer so the vault is locked the moment
 * the WHOLE APP (not just one Activity) leaves the foreground - e.g. user hits
 * Home, switches app, screen turns off, or a phone call interrupts.
 *
 * This is the mechanism behind requirement #6 (auto-lock on background).
 */
class VaultApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        ProcessLifecycleOwner.get().lifecycle.addObserver(object : DefaultLifecycleObserver {
            override fun onStop(owner: LifecycleOwner) {
                // App went to background (all activities stopped): lock immediately.
                VaultSession.lock()
            }
        })
    }
}
