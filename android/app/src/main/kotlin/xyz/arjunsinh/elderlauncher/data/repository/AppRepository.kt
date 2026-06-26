package xyz.arjunsinh.elderlauncher.data.repository

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import xyz.arjunsinh.elderlauncher.data.model.LauncherApp

class AppRepository(private val context: Context) {
    private val packageManager: PackageManager = context.packageManager

    suspend fun getInstalledApps(): List<LauncherApp> = withContext(Dispatchers.IO) {
        val intent = Intent(Intent.ACTION_MAIN, null).apply {
            addCategory(Intent.CATEGORY_LAUNCHER)
        }
        packageManager.queryIntentActivities(intent, 0).map { resolveInfo ->
            LauncherApp(
                packageName = resolveInfo.activityInfo.packageName,
                label = resolveInfo.loadLabel(packageManager).toString(),
                resolveInfo = resolveInfo,
                icon = resolveInfo.loadIcon(packageManager)
            )
        }.sortedBy { it.label.lowercase() }
    }
}
