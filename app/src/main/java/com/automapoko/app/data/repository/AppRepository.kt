package com.automapoko.app.data.repository

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class InstalledApp(
    val name: String,
    val packageName: String,
    val icon: Drawable? = null
)

class AppRepository(private val context: Context) {
    suspend fun getInstalledApps(): List<InstalledApp> = withContext(Dispatchers.IO) {
        val pm = context.packageManager
        val intent = Intent(Intent.ACTION_MAIN, null).apply { addCategory(Intent.CATEGORY_LAUNCHER) }
        val resolveInfos = pm.queryIntentActivities(intent, PackageManager.MATCH_ALL)
        resolveInfos.mapNotNull { resolveInfo ->
            val pkg = resolveInfo.activityInfo.packageName
            if (pkg != context.packageName) {
                InstalledApp(resolveInfo.loadLabel(pm).toString(), pkg, resolveInfo.loadIcon(pm))
            } else null
        }.sortedBy { it.name.lowercase() }
    }
}
