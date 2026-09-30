package com.example.calculatorvault.util

import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager

/**
 * DESIGN-ONLY PLACEHOLDER for the "Hide Apps" feature (V3 in the project plan).
 * Nothing in this class is wired into the UI in V1. It exists so the intended
 * architecture is on record and so nobody accidentally reaches for root,
 * Accessibility-Service abuse, or other security-bypass techniques later.
 *
 * Hard facts about what a normal, non-system Android app CAN and CANNOT do,
 * as of modern Android (8-15) without root or Device Owner / MDM enrollment:
 *
 *  CAN:
 *   - Toggle the enabled/disabled state of ITS OWN launcher activity or an
 *     <activity-alias> it declares (PackageManager.setComponentEnabledSetting).
 *     This can make *this app's own* icon disappear from the launcher - it
 *     cannot touch any other app's icon.
 *   - Query the list of installed apps it's allowed to see (subject to
 *     Android 11+ package-visibility filtering, which itself requires a
 *     <queries> manifest declaration and is not blanket access).
 *
 *  CANNOT (without root or being set as Device Owner via MDM, which requires
 *  explicit enterprise provisioning the user must consent to):
 *   - Hide or uninstall another app from the launcher, from Settings > Apps,
 *     or from Android's recents/app-switcher.
 *   - Make an installed app truly undetectable to the OS or to the user
 *     digging into Settings. Android always lists every installed package
 *     in Settings > Apps regardless of what any third-party app does.
 *   - Bypass Android's permission model, Accessibility Service consent
 *     screens, or Play Protect scanning to achieve hiding.
 *
 * This app will NEVER implement app-hiding via root, Accessibility-Service
 * abuse, exploiting undocumented OEM (POCO/MIUI/HyperOS) APIs, or any other
 * mechanism that bypasses Android's security model without the user's
 * explicit, informed consent through an official Android API.
 */
class HideAppsManager(private val context: Context) {

    /**
     * The only thing this class is actually able to do today: toggle this
     * app's OWN alias component. Not implemented/exposed in V1 UI.
     */
    fun setOwnAliasVisible(aliasComponent: ComponentName, visible: Boolean) {
        val pm = context.packageManager
        val newState = if (visible) {
            PackageManager.COMPONENT_ENABLED_STATE_ENABLED
        } else {
            PackageManager.COMPONENT_ENABLED_STATE_DISABLED
        }
        pm.setComponentEnabledSetting(aliasComponent, newState, PackageManager.DONT_KILL_APP)
    }

    /**
     * V1/V2 always returns false: hiding OTHER apps is not implemented and,
     * per the constraints above, is only ever partially possible under
     * Device Owner mode - which is out of scope unless explicitly designed,
     * disclosed, and consented to in V3.
     */
    fun canHideOtherApps(): Boolean = false
}
