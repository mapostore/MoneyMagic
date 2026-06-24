package com.indiewalkabout.moneymagic.core.notification

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.indiewalkabout.moneymagic.R
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

class BudgetNotificationService @Inject constructor(
    @param:ApplicationContext private val context: Context,
) {
    fun showBudgetThresholdNotification(budgetName: String, percentUsed: Int) {
        if (!canPostNotifications()) {
            return
        }

        val channelId = "budget_alerts"
        val manager = context.getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(
            NotificationChannel(channelId, "Budget alerts", NotificationManager.IMPORTANCE_DEFAULT),
        )

        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("Budget alert")
            .setContentText("$budgetName is $percentUsed% used")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()

        NotificationManagerCompat.from(context).notify(budgetName.hashCode(), notification)
    }

    private fun canPostNotifications(): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS,
            ) == PackageManager.PERMISSION_GRANTED
}
