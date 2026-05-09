package com.example.leaveby.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        // Recreate scheduled reminder after a reboot
        ReminderScheduler.rescheduleFromPrefs(context)
        AlarmScheduler.rescheduleAllAfterBoot(context.applicationContext)
    }
}
