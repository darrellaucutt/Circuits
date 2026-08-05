package net.aucutt.circuits.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import net.aucutt.circuits.MainActivity
import net.aucutt.circuits.R
import net.aucutt.circuits.distance.DistanceAnnouncement
import net.aucutt.circuits.distance.DistanceCircuitEngine
import net.aucutt.circuits.location.DistanceLocationTracker
import net.aucutt.circuits.tts.TtsSpeaker
import net.aucutt.circuits.ui.distance.DistanceMiles
import net.aucutt.circuits.ui.distance.DistancePhase
import net.aucutt.circuits.ui.distance.DistanceUiState
import kotlin.time.Duration.Companion.seconds

class DistanceCircuitService : Service() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var ttsSpeaker: TtsSpeaker? = null
    private var observeJob: Job? = null
    private var locationTracker: DistanceLocationTracker? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        ttsSpeaker = TtsSpeaker(applicationContext)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> {
                promoteToForeground(DistanceCircuitEngine.uiState.value)
                beginObserving()
                DistanceCircuitEngine.start()
            }

            ACTION_PAUSE -> {
                locationTracker?.stop()
                DistanceCircuitEngine.pause()
            }

            ACTION_RESUME -> {
                DistanceCircuitEngine.resume()
                ensureForeground(DistanceCircuitEngine.uiState.value)
                syncLocationTracking(DistanceCircuitEngine.uiState.value)
            }

            ACTION_STOP -> {
                locationTracker?.stop()
                locationTracker = null
                DistanceCircuitEngine.stop()
                ttsSpeaker?.stop()
                stopForegroundAndSelf()
            }

            ACTION_RESET -> {
                locationTracker?.stop()
                locationTracker = null
                DistanceCircuitEngine.resetToSetup()
                ttsSpeaker?.stop()
                stopForegroundAndSelf()
            }

            else -> {
                val state = DistanceCircuitEngine.uiState.value
                if (state.isRunning) {
                    promoteToForeground(state)
                    beginObserving()
                    syncLocationTracking(state)
                } else {
                    stopSelf()
                }
            }
        }
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        locationTracker?.stop()
        locationTracker = null
        observeJob?.cancel()
        serviceScope.cancel()
        ttsSpeaker?.shutdown()
        ttsSpeaker = null
        super.onDestroy()
    }

    private fun beginObserving() {
        if (observeJob?.isActive == true) return
        observeJob = serviceScope.launch {
            launch {
                DistanceCircuitEngine.uiState.collect { state ->
                    syncLocationTracking(state)
                    when (state.phase) {
                        DistancePhase.Idle -> stopForegroundAndSelf()
                        DistancePhase.Finished -> {
                            updateNotification(state)
                            delay(3.seconds)
                            if (DistanceCircuitEngine.uiState.value.phase == DistancePhase.Finished) {
                                stopForegroundAndSelf()
                            }
                        }
                        DistancePhase.Work, DistancePhase.Cooldown, DistancePhase.PreWorkout -> updateNotification(state)
                    }
                }
            }
            launch {
                DistanceCircuitEngine.announcements.collect { announcement ->
                    val text = when (announcement) {
                        DistanceAnnouncement.PreWorkout ->
                            getString(R.string.tts_pre_workout)
                        is DistanceAnnouncement.WorkStart ->
                            getString(R.string.tts_workout_starting, announcement.round)
                        is DistanceAnnouncement.HalfMileMarker ->
                            getString(
                                R.string.tts_distance_marker,
                                DistanceMiles.formatForTts(announcement.halfMiles),
                            )
                        DistanceAnnouncement.WorkComplete -> getString(R.string.tts_distance_work_complete)
                        DistanceAnnouncement.Cooldown -> getString(R.string.tts_cooldown)
                        DistanceAnnouncement.Complete -> getRandomCompleteMessage()
                    }
                    ttsSpeaker?.speak(text)
                }
            }
        }
    }

    private fun syncLocationTracking(state: DistanceUiState) {
        if (state.phase == DistancePhase.Work && !state.isPaused) {
            ensureLocationTracker().start()
        } else {
            locationTracker?.stop(resetFix = state.phase != DistancePhase.Work)
        }
    }

    private fun ensureLocationTracker(): DistanceLocationTracker {
        return locationTracker ?: DistanceLocationTracker(
            context = applicationContext,
            onFirstFix = { DistanceCircuitEngine.onGpsFixAcquired() },
            onDistanceDelta = { delta -> DistanceCircuitEngine.addDistance(delta) },
        ).also { locationTracker = it }
    }

    private fun getRandomCompleteMessage(): String {
        return when ((0..4).random()) {
            0 -> getString(R.string.tts_circuit_complete_0)
            1 -> getString(R.string.tts_circuit_complete_1)
            2 -> getString(R.string.tts_circuit_complete_2)
            3 -> getString(R.string.tts_circuit_complete_3)
            else -> getString(R.string.tts_circuit_complete_4)
        }
    }

    private fun promoteToForeground(state: DistanceUiState) {
        val notification = buildNotification(state)
        val serviceType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION or
                ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK
        } else {
            0
        }
        ServiceCompat.startForeground(this, NOTIFICATION_ID, notification, serviceType)
    }

    private fun ensureForeground(state: DistanceUiState) {
        if (state.isRunning) {
            promoteToForeground(state)
        }
    }

    private fun updateNotification(state: DistanceUiState) {
        getSystemService(NotificationManager::class.java)
            ?.notify(NOTIFICATION_ID, buildNotification(state))
    }

    private fun buildNotification(state: DistanceUiState): Notification {
        val openApp = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            },
            pendingIntentFlags(),
        )

        val title: String
        val text: String
        when (state.phase) {
            DistancePhase.PreWorkout -> {
                title = getString(R.string.notification_pre_workout_title)
                text = getString(
                    R.string.notification_pre_workout_text,
                    formatTime(state.remainingSeconds),
                )
            }

            DistancePhase.Work -> {
                title = getString(R.string.notification_distance_work_title)
                text = if (state.hasGpsFix) {
                    getString(
                        R.string.notification_distance_work_text,
                        DistanceMiles.formatDecimal(state.remainingWorkMeters / DistanceMiles.METERS_PER_MILE),
                    )
                } else {
                    getString(R.string.status_acquiring_gps)
                }
            }

            DistancePhase.Cooldown -> {
                title = getString(R.string.notification_cooldown_title)
                text = getString(
                    R.string.notification_timer_text,
                    formatTime(state.remainingSeconds),
                    state.currentRound,
                    state.config.repeats,
                )
            }

            DistancePhase.Finished -> {
                title = getString(R.string.notification_complete_title)
                text = getString(R.string.notification_complete_text, state.config.repeats)
            }

            DistancePhase.Idle -> {
                title = getString(R.string.app_name)
                text = getString(R.string.notification_idle_text)
            }
        }

        val builder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_menu_mylocation)
            .setContentTitle(title)
            .setContentText(text)
            .setContentIntent(openApp)
            .setOnlyAlertOnce(true)
            .setOngoing(state.isRunning)
            .setCategory(NotificationCompat.CATEGORY_PROGRESS)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setPriority(NotificationCompat.PRIORITY_LOW)

        if (state.isRunning) {
            if (state.isPaused) {
                builder.addAction(
                    0,
                    getString(R.string.action_resume),
                    servicePendingIntent(ACTION_RESUME, 1),
                )
            } else {
                builder.addAction(
                    0,
                    getString(R.string.action_pause),
                    servicePendingIntent(ACTION_PAUSE, 2),
                )
            }
            builder.addAction(
                0,
                getString(R.string.action_stop),
                servicePendingIntent(ACTION_STOP, 3),
            )
        }

        return builder.build()
    }

    private fun servicePendingIntent(action: String, requestCode: Int): PendingIntent {
        return PendingIntent.getService(
            this,
            requestCode,
            Intent(this, DistanceCircuitService::class.java).setAction(action),
            pendingIntentFlags(),
        )
    }

    private fun pendingIntentFlags(): Int {
        return PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    }

    private fun stopForegroundAndSelf() {
        locationTracker?.stop()
        locationTracker = null
        observeJob?.cancel()
        observeJob = null
        ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            getString(R.string.notification_distance_channel_name),
            NotificationManager.IMPORTANCE_LOW,
        ).apply {
            description = getString(R.string.notification_distance_channel_description)
            setSound(null, null)
        }
        getSystemService(NotificationManager::class.java)?.createNotificationChannel(channel)
    }

    companion object {
        const val ACTION_START = "net.aucutt.circuits.action.DISTANCE_START"
        const val ACTION_PAUSE = "net.aucutt.circuits.action.DISTANCE_PAUSE"
        const val ACTION_RESUME = "net.aucutt.circuits.action.DISTANCE_RESUME"
        const val ACTION_STOP = "net.aucutt.circuits.action.DISTANCE_STOP"
        const val ACTION_RESET = "net.aucutt.circuits.action.DISTANCE_RESET"

        private const val CHANNEL_ID = "distance_circuit"
        private const val NOTIFICATION_ID = 1002

        fun start(context: Context) {
            val intent = Intent(context, DistanceCircuitService::class.java).setAction(ACTION_START)
            context.startForegroundService(intent)
        }

        fun pause(context: Context) {
            context.startService(
                Intent(context, DistanceCircuitService::class.java).setAction(ACTION_PAUSE),
            )
        }

        fun resume(context: Context) {
            context.startForegroundService(
                Intent(context, DistanceCircuitService::class.java).setAction(ACTION_RESUME),
            )
        }

        fun stop(context: Context) {
            context.startService(
                Intent(context, DistanceCircuitService::class.java).setAction(ACTION_STOP),
            )
        }

        fun reset(context: Context) {
            context.startService(
                Intent(context, DistanceCircuitService::class.java).setAction(ACTION_RESET),
            )
        }
    }
}

private fun formatTime(totalSeconds: Int): String {
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "%d:%02d".format(minutes, seconds)
}
