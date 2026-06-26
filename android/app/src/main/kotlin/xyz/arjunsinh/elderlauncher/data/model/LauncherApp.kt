package xyz.arjunsinh.elderlauncher.data.model

import android.content.pm.ResolveInfo
import android.graphics.drawable.Drawable
import androidx.compose.runtime.Immutable

@Immutable
data class LauncherApp(
    val packageName: String,
    val label: String,
    val resolveInfo: ResolveInfo,
    val icon: Drawable? = null
)
