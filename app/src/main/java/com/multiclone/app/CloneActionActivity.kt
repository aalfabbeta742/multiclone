package com.multiclone.app

import android.app.Activity
import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.widget.Toast

/**
 * Ye activity clone space (work profile) ke andar chalti hai.
 * Main app is ko cross-profile intent se bulati hai.
 * op: install | launch | repair | remove
 */
class CloneActionActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        try {
            handle()
        } catch (e: Exception) {
            toast("Error: ${e.message}")
        }
        finish()
    }

    private fun handle() {
        val dpm = getSystemService(DevicePolicyManager::class.java)
        if (!dpm.isProfileOwnerApp(packageName)) {
            toast("Ye sirf clone space mein chalta hai")
            return
        }
        val admin = ComponentName(this, AdminReceiver::class.java)
        val pkg = intent.getStringExtra("pkg") ?: return

        when (intent.getStringExtra("op")) {
            "install" -> {
                if (ensureInstalled(dpm, admin, pkg)) toast("Clone taiyar hai")
                else toast("Clone install nahi ho saka")
            }
            "launch" -> {
                if (!ensureInstalled(dpm, admin, pkg)) {
                    toast("Clone install nahi ho saka")
                    return
                }
                val launch = packageManager.getLaunchIntentForPackage(pkg)
                if (launch != null) startActivity(launch)
                else toast("Clone taiyar ho raha hai, thori der baad dobara tap karein")
            }
            "repair" -> {
                if (dpm.isApplicationHidden(admin, pkg)) dpm.setApplicationHidden(admin, pkg, false)
                val ok = dpm.installExistingPackage(admin, pkg)
                toast(if (ok) "Repair mukammal" else "Repair nahi ho saka")
            }
            "remove" -> {
                // Clone space se is app ki copy hatao (system confirm poochega)
                startActivity(
                    Intent(Intent.ACTION_DELETE, Uri.parse("package:$pkg"))
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                )
            }
        }
    }

    private fun isInstalled(pkg: String): Boolean = try {
        packageManager.getPackageInfo(pkg, 0)
        true
    } catch (e: PackageManager.NameNotFoundException) {
        false
    }

    /** Main user mein installed app ki copy clone space mein install karta hai. */
    private fun ensureInstalled(dpm: DevicePolicyManager, admin: ComponentName, pkg: String): Boolean {
        if (dpm.isApplicationHidden(admin, pkg)) dpm.setApplicationHidden(admin, pkg, false)
        if (isInstalled(pkg)) return true
        return dpm.installExistingPackage(admin, pkg)
    }

    private fun toast(msg: String) = Toast.makeText(applicationContext, msg, Toast.LENGTH_LONG).show()
}
