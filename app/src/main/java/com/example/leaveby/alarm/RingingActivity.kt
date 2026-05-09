package com.example.leaveby.alarm

import android.app.KeyguardManager
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import java.time.ZonedDateTime

class RingingActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Volume keys control the ALARM stream while this screen is up
        @Suppress("DEPRECATION")
        volumeControlStream = AudioManager.STREAM_ALARM

        // Show over lockscreen + wake screen
        if (Build.VERSION.SDK_INT >= 27) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
            getSystemService(KeyguardManager::class.java)
                ?.requestDismissKeyguard(this, null)
        } else {
            @Suppress("DEPRECATION")
            window.addFlags(
                android.view.WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                        android.view.WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON or
                        android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
            )
        }

        val label = intent.getStringExtra(AlarmReceiver.EXTRA_LABEL) ?: "LeaveBy"
        val msg   = intent.getStringExtra(AlarmReceiver.EXTRA_MSG) ?: "Time to head out"

        setContent {
            MaterialTheme {
                Surface(Modifier.fillMaxSize()) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            Text("⏰ $label", style = MaterialTheme.typography.headlineMedium)
                            Text(msg, style = MaterialTheme.typography.bodyLarge)
                            Spacer(Modifier.height(10.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                Button(onClick = { dismissAlarm() }) {
                                    Text("Dismiss")
                                }
                                Button(onClick = { snooze(5, msg) }) {
                                    Text("Snooze 5m")
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Correct override to receive new intents if activity already showing
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        // no-op
    }

    private fun dismissAlarm() {
        // Stop the sound service
        stopService(Intent(this, AlarmService::class.java))
        // Clear the ongoing notification
        (getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager)
            .cancel(AlarmReceiver.NOTIF_ID)
        // Close UI
        finish()
    }

    private fun snooze(mins: Long, msg: String) {
        dismissAlarm()
        val whenAt = ZonedDateTime.now().plusMinutes(mins)
        AlarmScheduler.scheduleExactAlarm(this, whenAt, msg)
    }
}
