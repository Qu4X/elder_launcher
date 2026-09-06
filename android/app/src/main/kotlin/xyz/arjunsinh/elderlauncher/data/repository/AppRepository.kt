package xyz.arjunsinh.elderlauncher.data.repository

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.drawable.AdaptiveIconDrawable
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.os.Build
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import xyz.arjunsinh.elderlauncher.data.model.LauncherApp

class AppRepository(private val context: Context) {
    private val packageManager: PackageManager = context.packageManager

    suspend fun getInstalledApps(): List<LauncherApp> = withContext(Dispatchers.IO) {
        val intent = Intent(Intent.ACTION_MAIN, null).apply {
            addCategory(Intent.CATEGORY_LAUNCHER)
        }
        val apps = packageManager.queryIntentActivities(intent, 0).map { resolveInfo ->
            val rawIcon = resolveInfo.loadIcon(packageManager)
            LauncherApp(
                packageName = resolveInfo.activityInfo.packageName,
                activityName = resolveInfo.activityInfo.name ?: "",
                label = resolveInfo.loadLabel(packageManager).toString(),
                resolveInfo = resolveInfo,
                icon = createUnmaskedIcon(rawIcon)
            )
        }
        apps.distinctBy { it.key }.sortedBy { it.label.lowercase() }
    }

    /**
     * Renders both adaptive and legacy icons into an unmasked, full-bleed square BitmapDrawable.
     * For AdaptiveIconDrawable, layers are rendered directly without the OEM mask so that
     * Compose can apply custom mask shapes (Circle, RoundedSquare) cleanly.
     * For legacy icons, a standard white background is drawn with the icon inset to the safe zone.
     */
    private fun createUnmaskedIcon(drawable: Drawable?): Drawable? {
        if (drawable == null) return null
        val size = 192
        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && drawable is AdaptiveIconDrawable) {
            val offset = (size * 0.25f).toInt()
            val bounds = Rect(-offset, -offset, size + offset, size + offset)

            val bg = drawable.background
            if (bg != null) {
                bg.bounds = bounds
                bg.draw(canvas)
            } else {
                canvas.drawColor(Color.WHITE)
            }

            drawable.foreground?.let { fg ->
                fg.bounds = bounds
                fg.draw(canvas)
            }
        } else {
            // Legacy icon
            val bgPaint = Paint().apply {
                color = Color.WHITE
                isAntiAlias = true
            }
            canvas.drawRect(0f, 0f, size.toFloat(), size.toFloat(), bgPaint)

            val inset = (size * 0.22f).toInt()
            drawable.setBounds(inset, inset, size - inset, size - inset)
            drawable.draw(canvas)
        }

        return BitmapDrawable(context.resources, bitmap)
    }
}

