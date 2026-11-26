package com.multiclone.app

import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager

object ProfileSetup {

    fun actionName(ctx: Context) = ctx.packageName + ".CLONE_ACTION"

    fun isProfileOwner(ctx: Context): Boolean {
        val dpm = ctx.getSystemService(DevicePolicyManager::class.java)
        return dpm.isProfileOwnerApp(ctx.packageName)
    }

    /** Sirf clone space ki copy mein kaam karta hai (jahan hum profile owner hain). */
    fun prepareIfProfileOwner(ctx: Context) {
        if (!isProfileOwner(ctx)) return
        try {
            val dpm = ctx.getSystemService(DevicePolicyManager::class.java)
            val admin = ComponentName(ctx, AdminReceiver::class.java)

            ctx.packageManager.setComponentEnabledSetting(
                ComponentName(ctx, CloneActionActivity::class.java),
                PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
                PackageManager.DONT_KILL_APP
            )

            // Asli (main) app is action ke zariye clone space mein kaam bhej sakti hai
            val filter = IntentFilter(actionName(ctx))
            filter.addCategory(Intent.CATEGORY_DEFAULT)
            dpm.clearCrossProfileIntentFilters(admin)
            dpm.addCrossProfileIntentFilter(
                admin, filter, DevicePolicyManager.FLAG_PARENT_CAN_ACCESS_MANAGED
            )
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
