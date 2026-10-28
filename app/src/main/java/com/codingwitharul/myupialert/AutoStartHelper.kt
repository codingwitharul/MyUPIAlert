package com.codingwitharul.myupialert

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import java.util.Locale

object AutoStartHelper {

    @JvmStatic
    fun getAutoStartIntent(context: Context): Intent? {
        val manufacturer = Build.MANUFACTURER.lowercase(Locale.ROOT)
        val intent = Intent()

        try {
            when (manufacturer) {
                "xiaomi", "poco", "redmi" -> {
                    intent.component = ComponentName("com.miui.securitycenter", "com.miui.permcenter.autostart.AutoStartManagementActivity")
                }
                "oppo", "realme", "oneplus" -> {
                    intent.component = ComponentName("com.coloros.safecenter", "com.coloros.safecenter.startupapp.StartupAppListActivity")
                    if (!isIntentResolved(context, intent)) {
                        intent.component = ComponentName("com.coloros.safecenter", "com.coloros.privacypermissionsentry.PermissionTopActivity")
                    }
                    if (!isIntentResolved(context, intent)) {
                        intent.component = ComponentName("com.oppo.safe", "com.oppo.safe.permission.startup.StartupAppListActivity")
                    }
                }
                "vivo", "iqoo" -> {
                    intent.component = ComponentName("com.vivo.permissionmanager", "com.vivo.permissionmanager.activity.BgStartUpManagerActivity")
                    if (!isIntentResolved(context, intent)) {
                        intent.component = ComponentName("com.iqoo.secure", "com.iqoo.secure.ui.phoneoptimize.AddWhiteListActivity")
                    }
                    if (!isIntentResolved(context, intent)) {
                        intent.component = ComponentName("com.iqoo.secure", "com.iqoo.secure.ui.phoneoptimize.BgStartUpManager")
                    }
                }
                "huawei", "honor" -> {
                    intent.component = ComponentName("com.huawei.systemmanager", "com.huawei.systemmanager.optimize.process.ProtectActivity")
                    if (!isIntentResolved(context, intent)) {
                        intent.component = ComponentName("com.huawei.systemmanager", "com.huawei.systemmanager.startupmgr.ui.StartupNormalAppListActivity")
                    }
                    if (!isIntentResolved(context, intent)) {
                        intent.component = ComponentName("com.huawei.systemmanager", "com.huawei.systemmanager.appcontrol.activity.StartupAppControlActivity")
                    }
                }
                "asus" -> {
                    intent.component = ComponentName("com.asus.mobilemanager", "com.asus.mobilemanager.entry.FunctionActivity")
                    if (!isIntentResolved(context, intent)) {
                        intent.component = ComponentName("com.asus.mobilemanager", "com.asus.mobilemanager.autostart.AutoStartActivity")
                    }
                }
                "letv" -> {
                    intent.component = ComponentName("com.letv.android.letvsafe", "com.letv.android.letvsafe.AutobootManageActivity")
                }
                "samsung" -> {
                    // Samsung's direct battery intent is broken in One UI 5/6. Fallback to App Info page so user can click Battery -> Unrestricted.
                    return Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:" + context.packageName))
                }
                else -> return null
            }

            if (intent.component != null && !isIntentResolved(context, intent) && manufacturer != "samsung") {
                return null
            }
        } catch (e: Exception) {
            return null
        }

        return intent
    }

    private fun isIntentResolved(context: Context, intent: Intent): Boolean {
        val pm = context.packageManager
        return pm.resolveActivity(intent, PackageManager.MATCH_DEFAULT_ONLY) != null
    }
}
