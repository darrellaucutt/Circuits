package net.aucutt.circuits.wear.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import androidx.wear.ongoing.OngoingActivity
import androidx.wear.ongoing.Status
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import net.aucutt.circuits.sync.SyncTimerState
import net.aucutt.circuits.wear.MainActivity
import net.aucutt.circuits.wear.R
import net.aucutt.circuits.wear.sync.WearMirrorRepository

/** Keeps the workout visible on the watch face while a circuit is running. */
class WearWorkoutDisplayService : Service() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var observeJob: Job? = null
    private var wakeLock: PowerManager.WakeLock? = null
    private var ongoingActivity: OngoingActivity? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP -> {
                stopForegroundAndSelf()
                return START_NOT_STICKY
            }
            else -> {
                val state = WearMirrorRepository.timerState.value
                if (state?.isRunning != true) {
                    stopSelf()
                    return START_NOT_STICKY
                }
                try {
                    promoteToForeground(state)
                    beginObserving()
                } catch (t: Throwable) {
                    Log.e(TAG, "Unable to start workout display service", t)
                    stopSelf()
                    return START_NOT_STICKY
                }
            }
        }
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        observeJob?.cancel()
        serviceScope.cancel()
        releaseWakeLock()
        ongoingActivity = null
        super.onDestroy()
    }

    private fun beginObserving() {
        if (observeJob?.isActive == true) return
        acquireWakeLock()
        observeJob = serviceScope.launch {
            WearMirrorRepository.timerState.collect { state ->
                if (state?.isRunning == true) {
                    updateForeground(state)
                } else {
                    stopForegroundAndSelf()
                }
            }
        }
    }

    private fun promoteToForeground(state: SyncTimerState) {
        val builder = buildNotificationBuilder(state)
        ServiceCompat.startForeground(
            this,
            NOTIFICATION_ID,
            builder.build(),
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
            } else {
                0
            },
        )
        try {
            applyOngoingActivity(builder, state)
        } catch (t: Throwable) {
            Log.w(TAG, "Ongoing activity unavailable; continuing with notification only", t)
        }
    }

    private fun updateForeground(state: SyncTimerState) {
        val builder = buildNotificationBuilder(state)
        getSystemService(NotificationManager::class.java)?.notify(NOTIFICATION_ID, builder.build())
        try {
            applyOngoingActivity(builder, state)
        } catch (t: Throwable) {
            Log.w(TAG, "Unable to update ongoing activity", t)
        }
    }

    private fun applyOngoingActivity(
        notificationBuilder: NotificationCompat.Builder,
        state: SyncTimerState,
    ) {
        val openApp = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val status = Status.Builder()
            .addTemplate("#time#")
            .addPart("time", Status.TextPart(formatTime(state.remainingSeconds)))
            .build()

        ongoingActivity = OngoingActivity.Builder(
            applicationContext,
            NOTIFICATION_ID,
            notificationBuilder,
        )
            .setStaticIcon(R.drawable.ic_launcher_foreground)
            .setTouchIntent(openApp)
            .setStatus(status)
            .build()
            .also { it.apply(applicationContext) }
    }

    private fun buildNotificationBuilder(state: SyncTimerState): NotificationCompat.Builder {
        val openApp = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val title = when (state.phase) {
            "PreWorkout" -> getString(R.string.phase_pre_workout)
            "Work" -> getString(R.string.phase_work)
            "Cooldown" -> getString(R.string.phase_cooldown)
            else -> getString(R.string.app_name)
        }
        val text = getString(
            R.string.notification_timer_text,
            formatTime(state.remainingSeconds),
            state.currentRound,
            state.repeats,
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(title)
            .setContentText(text)
            .setContentIntent(openApp)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setCategory(NotificationCompat.CATEGORY_WORKOUT)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
    }

    private fun acquireWakeLock() {
        if (wakeLock?.isHeld == true) return
        val manager = getSystemService(PowerManager::class.java) ?: return
        wakeLock = manager.newWakeLock(
            PowerManager.PARTIAL_WAKE_LOCK,
            "Circuits::WorkoutDisplay",
        ).apply { acquire() }
    }

    private fun releaseWakeLock() {
        wakeLock?.let { lock ->
            if (lock.isHeld) lock.release()
        }
        wakeLock = null
    }

    private fun stopForegroundAndSelf() {
        observeJob?.cancel()
        observeJob = null
        releaseWakeLock()
        ongoingActivity = null
        ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            getString(R.string.notification_channel_name),
            NotificationManager.IMPORTANCE_HIGH,
        ).apply {
            description = getString(R.string.notification_channel_description)
            setSound(null, null)
        }
        getSystemService(NotificationManager::class.java)?.createNotificationChannel(channel)
    }

    companion object {
        private const val TAG = "WearWorkoutDisplay"
        private const val CHANNEL_ID = "wear_workout_display"
        private const val NOTIFICATION_ID = 3001

        private const val ACTION_STOP = "net.aucutt.circuits.wear.action.STOP_DISPLAY"

        fun sync(context: Context) {
            val state = WearMirrorRepository.timerState.value
            if (state?.isRunning == true) {
                context.startForegroundService(Intent(context, WearWorkoutDisplayService::class.java))
            } else {
                context.startService(
                    Intent(context, WearWorkoutDisplayService::class.java).setAction(ACTION_STOP),
                )
            }
        }

        fun stop(context: Context) {
            context.startService(
                Intent(context, WearWorkoutDisplayService::class.java).setAction(ACTION_STOP),
            )
        }
    }
}

private fun formatTime(totalSeconds: Int): String {
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "%d:%02d".format(minutes, seconds)
}
