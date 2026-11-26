package app.grillprobe

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build

object Permissions {
    private val REQUIRED = listOf(Manifest.permission.BLUETOOTH_SCAN, Manifest.permission.BLUETOOTH_CONNECT)

    fun hasRequired(context: Context): Boolean = REQUIRED.all { granted(context, it) }

    fun missing(
        context: Context,
        includeNotifications: Boolean,
    ): List<String> {
        val notifications =
            if (includeNotifications && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                Manifest.permission.POST_NOTIFICATIONS.takeIf { !granted(context, it) }
            } else {
                null
            }
        return REQUIRED.filterNot { granted(context, it) } + listOfNotNull(notifications)
    }

    private fun granted(
        context: Context,
        permission: String,
    ) = context.checkSelfPermission(permission) == PackageManager.PERMISSION_GRANTED
}
