package com.multiclone.app

import android.app.admin.DeviceAdminReceiver
import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent

class AdminReceiver : DeviceAdminReceiver() {

    // Clone space ban jane ke baad ye clone space ke andar chalta hai
    override fun onProfileProvisioningComplete(context: Context, intent: Intent) {
        val dpm = context.getSystemService(DevicePolicyManager::class.java)
        val admin = ComponentName(context, AdminReceiver::class.java)
        try {
            dpm.setProfileName(admin, "Multi Clone Space")
        } catch (e: Exception) {
            e.printStackTrace()
        }
        ProfileSetup.prepareIfProfileOwner(context)
        try {
            dpm.setProfileEnabled(admin)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
