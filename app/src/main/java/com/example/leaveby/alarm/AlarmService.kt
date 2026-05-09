package com.example.leaveby.alarm

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.os.Build
import android.os.IBinder
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.core.app.NotificationCompat
import com.example.leaveby.R
import com.example.leaveby.data.Prefs
import kotlin.math.roundToInt

class AlarmService : Service() {

    private var player: MediaPlayer? = null
    private var vibrator: Vibrator? = null

    // For proper ducking / pausing of other audio
    private var audioManager: AudioManager? = null
    private var focusRequest: AudioFocusRequest? = null
    private val legacyFocusListener =
        AudioManager.OnAudioFocusChangeListener { /* no-op */ }

    // We’ll temporarily change the ALARM stream volume and restore it on destroy.
    private var previousAlarmVolume: Int? = null

    override fun onCreate() {
        super.onCreate()

        // Vibrator (VibratorManager on 12L+)
        vibrator = if (Build.VERSION.SDK_INT >= 31) {
            getSystemService(VibratorManager::class.java).defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            getSystemService(VIBRATOR_SERVICE) as Vibrator
        }

        audioManager = getSystemService(AudioManager::class.java)

        // Make sure the alarm channel exists (same ID as AlarmReceiver)
        val nm = getSystemService(NotificationManager::class.java)
        ensureChannel(nm)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val label = intent?.getStringExtra(AlarmReceiver.EXTRA_LABEL) ?: "LeaveBy"
        val msg   = intent?.getStringExtra(AlarmReceiver.EXTRA_MSG) ?: "Time to head out"

        // Full-screen tap goes to RingingActivity
        val fullPi = PendingIntent.getActivity(
            this, 2001,
            Intent(this, RingingActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                putExtra(AlarmReceiver.EXTRA_LABEL, label)
                putExtra(AlarmReceiver.EXTRA_MSG, msg)
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Foreground notification (same ID -> replaces the one from AlarmReceiver)
        val notif = NotificationCompat.Builder(this, AlarmReceiver.CHANNEL_ALARMS)
            .setSmallIcon(R.drawable.ic_stat_alarm)
            .setContentTitle(label)
            .setContentText(msg)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setOngoing(true)
            .setAutoCancel(false)
            .setFullScreenIntent(fullPi, true)
            .setContentIntent(fullPi)
            .build()

        try {
            startForeground(AlarmReceiver.NOTIF_ID, notif)
        } catch (_: Throwable) {
            // If the system blocks foreground start, at least surface the UI
            startActivity(
                Intent(this, RingingActivity::class.java)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                    .putExtra(AlarmReceiver.EXTRA_LABEL, label)
                    .putExtra(AlarmReceiver.EXTRA_MSG, msg)
            )
        }

        // --- Sound & vibration (Prefs-driven) ---
        val chosen = Prefs.soundUri(this)
            ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
            ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)

        // Percent we show in Settings: 0..100
        val percent = Prefs.volume(this).coerceIn(0, 100)
        val vibOn = Prefs.vibrate(this)

        // Request audio focus for an alarm (transient)
        try {
            val attrs = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_ALARM)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()

            if (Build.VERSION.SDK_INT >= 26) {
                focusRequest = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT)
                    .setAudioAttributes(attrs)
                    .setOnAudioFocusChangeListener { /* no-op */ }
                    .build()
                audioManager?.requestAudioFocus(focusRequest!!)
            } else {
                @Suppress("DEPRECATION")
                audioManager?.requestAudioFocus(
                    legacyFocusListener,
                    AudioManager.STREAM_ALARM,
                    AudioManager.AUDIOFOCUS_GAIN_TRANSIENT
                )
            }
        } catch (_: Throwable) { }

        // === IMPORTANT: drive the *ALARM* stream volume based on Settings ===
        try {
            val am = audioManager
            if (am != null) {
                val max = am.getStreamMaxVolume(AudioManager.STREAM_ALARM)
                val target = ((percent / 100f) * max).roundToInt().coerceIn(0, max)
                previousAlarmVolume = am.getStreamVolume(AudioManager.STREAM_ALARM)
                if (previousAlarmVolume != target) {
                    @Suppress("DEPRECATION")
                    am.setStreamVolume(AudioManager.STREAM_ALARM, target, 0)
                }
            }
        } catch (_: Throwable) { }

        // MediaPlayer on ALARM usage (loops)
        try {
            player?.release()
            player = MediaPlayer().apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ALARM) // routes to alarm stream
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                )
                setDataSource(this@AlarmService, chosen)
                isLooping = true
                // Prepare async to avoid any chance of blocking
                setOnPreparedListener {
                    // Player gain at 1.0; actual loudness governed by STREAM_ALARM we set above.
                    it.setVolume(1f, 1f)
                    it.start()
                }
                try {
                    prepareAsync()
                } catch (_: Throwable) {
                    // Fallback to sync if needed
                    prepare()
                    setVolume(1f, 1f)
                    start()
                }
            }
        } catch (_: Throwable) {
            // noop – if audio fails, user still gets fullscreen UI + vibration
        }

        if (vibOn) {
            try {
                val pattern = longArrayOf(0, 600, 300, 600, 300, 600)
                if (Build.VERSION.SDK_INT >= 26) {
                    vibrator?.vibrate(VibrationEffect.createWaveform(pattern, 0))
                } else {
                    @Suppress("DEPRECATION")
                    vibrator?.vibrate(pattern, 0)
                }
            } catch (_: Throwable) {}
        }

        return START_STICKY
    }

    override fun onDestroy() {
        try { player?.stop() } catch (_: Throwable) {}
        try { player?.release() } catch (_: Throwable) {}
        player = null
        try { vibrator?.cancel() } catch (_: Throwable) {}

        // Release audio focus
        try {
            if (Build.VERSION.SDK_INT >= 26) {
                focusRequest?.let { audioManager?.abandonAudioFocusRequest(it) }
            } else {
                @Suppress("DEPRECATION")
                audioManager?.abandonAudioFocus(legacyFocusListener)
            }
        } catch (_: Throwable) {}

        // Restore the alarm stream volume to what the user had before our alarm
        try {
            val am = audioManager
            val prev = previousAlarmVolume
            if (am != null && prev != null) {
                @Suppress("DEPRECATION")
                am.setStreamVolume(AudioManager.STREAM_ALARM, prev, 0)
            }
        } catch (_: Throwable) {}
        previousAlarmVolume = null

        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun ensureChannel(nm: NotificationManager) {
        if (nm.getNotificationChannel(AlarmReceiver.CHANNEL_ALARMS) != null) return

        val sound = Prefs.soundUri(this)
            ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)

        val attrs = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_ALARM)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()

        val ch = NotificationChannel(
            AlarmReceiver.CHANNEL_ALARMS,
            "LeaveBy Alarms",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "Alarm notifications"
            lockscreenVisibility = Notification.VISIBILITY_PUBLIC
            setBypassDnd(true)
            enableVibration(true)
            // Channel sound is only for the notification "beep"; our looping alarm uses MediaPlayer.
            setSound(sound, attrs)
        }
        nm.createNotificationChannel(ch)
    }
}
