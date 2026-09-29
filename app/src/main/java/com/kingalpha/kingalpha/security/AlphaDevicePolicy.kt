package com.kingalpha.kingalpha.security

import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context

class AlphaDevicePolicy(
    private val context: Context
) {

    private val devicePolicyManager =
        context.getSystemService(Context.DEVICE_POLICY_SERVICE)
                as DevicePolicyManager

    private val adminComponent =
        ComponentName(
            context,
            AlphaDeviceAdminReceiver::class.java
        )

    fun isAdminActive(): Boolean {
        return devicePolicyManager.isAdminActive(adminComponent)
    }

    fun requestAdmin(): Boolean {
        return !isAdminActive()
    }

    fun lockDevice(): Boolean {
        if (!isAdminActive()) {
            return false
        }

        devicePolicyManager.lockNow()
        return true
    }
}