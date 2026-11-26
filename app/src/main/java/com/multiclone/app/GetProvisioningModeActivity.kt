package com.multiclone.app

import android.app.Activity
import android.content.Intent
import android.os.Bundle

class GetProvisioningModeActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // 2 = PROVISIONING_MODE_MANAGED_PROFILE (work profile)
        val result = Intent().putExtra("android.app.extra.PROVISIONING_MODE", 2)
        setResult(RESULT_OK, result)
        finish()
    }
}
