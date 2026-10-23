package io.github.gythialy.sporteditor.ui

import android.content.Context
import android.content.pm.PackageManager
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import io.github.gythialy.sporteditor.data.AppTarget
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class AppIconState(val icon: ImageBitmap?, val installed: Boolean)

/**
 * Loads the installed application icon off the main thread, so every supported app shows its
 * real launcher icon without bundling artwork in the module APK. Falls back to the app's letter
 * badge when the package is not installed or cannot be read.
 */
@Composable
fun rememberAppIcon(target: AppTarget): AppIconState {
    val context = LocalContext.current
    return produceState(initialValue = AppIconState(null, true), target.packageName) {
        value = withContext(Dispatchers.IO) { loadAppIcon(context, target.packageName) }
    }.value
}

private fun loadAppIcon(context: Context, packageName: String): AppIconState {
    val pm = context.packageManager
    return try {
        val info: android.content.pm.ApplicationInfo = pm.getApplicationInfo(packageName, 0)
        val bitmap = pm.getApplicationIcon(info)
            .toBitmap(width = 144, height = 144)
            .asImageBitmap()
        AppIconState(bitmap, true)
    } catch (_: PackageManager.NameNotFoundException) {
        AppIconState(null, false)
    } catch (_: Throwable) {
        AppIconState(null, false)
    }
}

@Composable
fun AppIcon(target: AppTarget, state: AppIconState, modifier: Modifier = Modifier, size: Dp = 48.dp, contentAlpha: Float = 1f) {
    val shape = RoundedCornerShape(size * 0.3f)
    if (state.icon != null) {
        Image(
            bitmap = state.icon,
            contentDescription = stringResource(target.labelRes()),
            modifier = modifier
                .size(size)
                .alpha(contentAlpha)
                .clip(shape),
            contentScale = ContentScale.Crop
        )
    } else {
        Box(
            modifier = modifier
                .size(size)
                .alpha(contentAlpha)
                .clip(shape)
                .background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = target.badge,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
        }
    }
}
