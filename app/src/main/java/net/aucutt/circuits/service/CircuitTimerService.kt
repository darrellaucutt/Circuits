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
import net.aucutt.circuits.timer.CircuitTimerEngine
import net.aucutt.circuits.timer.TimerAnnouncement
import net.aucutt.circuits.tts.TtsSpeaker
import net.aucutt.circuits.ui.timer.TimerPhase
import net.aucutt.circuits.ui.timer.TimerUiState
import kotlin.time.Duration.Companion.seconds

class CircuitTimerService : Service() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var ttsSpeaker: TtsSpeaker? = null
    private var observeJob: Job? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        ttsSpeaker = TtsSpeaker(applicationContext)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> {
                promoteToForeground(CircuitTimerEngine.uiState.value)
                beginObserving()
                CircuitTimerEngine.start()
            }

            ACTION_PAUSE -> CircuitTimerEngine.pause()

            ACTION_RESUME -> {
                CircuitTimerEngine.resume()
                ensureForeground(CircuitTimerEngine.uiState.value)
            }

            ACTION_STOP -> {
                CircuitTimerEngine.stop()
                ttsSpeaker?.stop()
                stopForegroundAndSelf()
            }

            ACTION_RESET -> {
                CircuitTimerEngine.resetToSetup()
                ttsSpeaker?.stop()
                stopForegroundAndSelf()
            }

            else -> {
                val state = CircuitTimerEngine.uiState.value
                if (state.isRunning) {
                    promoteToForeground(state)
                    beginObserving()
                } else {
                    stopSelf()
                }
            }
        }
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
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
                CircuitTimerEngine.uiState.collect { state ->
                    when (state.phase) {
                        TimerPhase.Idle -> stopForegroundAndSelf()
                        TimerPhase.Finished -> {
                            updateNotification(state)
                            delay(3.seconds)
                            if (CircuitTimerEngine.uiState.value.phase == TimerPhase.Finished) {
                                stopForegroundAndSelf()
                            }
                        }
                        TimerPhase.PreWorkout, TimerPhase.Work, TimerPhase.Cooldown -> updateNotification(state)
                    }
                }
            }
            launch {
                CircuitTimerEngine.announcements.collect { announcement ->
                    val text = when (announcement) {
                        TimerAnnouncement.PreWorkout ->
                            getString(R.string.tts_pre_workout)
                        is TimerAnnouncement.Work ->
                            getString(R.string.tts_work_round, announcement.round)
                        TimerAnnouncement.Cooldown -> getString(R.string.tts_cooldown)
                        TimerAnnouncement.Complete -> getRandomCompleteMessage()
                    }
                    ttsSpeaker?.speak(text)
                }
            }
        }
    }

    private fun getRandomCompleteMessage() : String {
        //TODO make this more dynamic, read the number of platitudes from strings.xml
        //maybe using an array
        val rand = (0..4).random()
        return when (rand)  {
            0 -> getString(R.string.tts_circuit_complete_0)
            1 -> getString(R.string.tts_circuit_complete_1)
            2 -> getString(R.string.tts_circuit_complete_2)
            3 -> getString(R.string.tts_circuit_complete_3)
            else -> getString(R.string.tts_circuit_complete_4)
        }
    }

    private fun promoteToForeground(state: TimerUiState) {
        val notification = buildNotification(state)
        ServiceCompat.startForeground(
            this,
            NOTIFICATION_ID,
            notification,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK
            } else {
                0
            },
        )
    }

    private fun ensureForeground(state: TimerUiState) {
        if (state.isRunning) {
            promoteToForeground(state)
        }
    }

    private fun updateNotification(state: TimerUiState) {
        val manager = getSystemService(NotificationManager::class.java) ?: return
        manager.notify(NOTIFICATION_ID, buildNotification(state))
    }

    private fun buildNotification(state: TimerUiState): Notification {
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
            TimerPhase.PreWorkout -> {
                title = getString(R.string.notification_pre_workout_title)
                text = getString(
                    R.string.notification_pre_workout_text,
                    formatTime(state.remainingSeconds),
                )
            }

            TimerPhase.Work -> {
                title = getString(R.string.notification_work_title)
                text = getString(
                    R.string.notification_timer_text,
                    formatTime(state.remainingSeconds),
                    state.currentRound,
                    state.config.repeats,
                )
            }

            TimerPhase.Cooldown -> {
                title = getString(R.string.notification_cooldown_title)
                text = getString(
                    R.string.notification_timer_text,
                    formatTime(state.remainingSeconds),
                    state.currentRound,
                    state.config.repeats,
                )
            }

            TimerPhase.Finished -> {
                title = getString(R.string.notification_complete_title)
                text = getString(R.string.notification_complete_text, state.config.repeats)
            }

            TimerPhase.Idle -> {
                title = getString(R.string.app_name)
                text = getString(R.string.notification_idle_text)
            }
        }

        val builder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_media_play)
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
            Intent(this, CircuitTimerService::class.java).setAction(action),
            pendingIntentFlags(),
        )
    }

    private fun pendingIntentFlags(): Int {
        return PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    }

    private fun stopForegroundAndSelf() {
        observeJob?.cancel()
        observeJob = null
        ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            getString(R.string.notification_channel_name),
            NotificationManager.IMPORTANCE_LOW,
        ).apply {
            description = getString(R.string.notification_channel_description)
            setSound(null, null)
        }
        getSystemService(NotificationManager::class.java)?.createNotificationChannel(channel)
    }

    companion object {
        const val ACTION_START = "net.aucutt.circuits.action.START"
        const val ACTION_PAUSE = "net.aucutt.circuits.action.PAUSE"
        const val ACTION_RESUME = "net.aucutt.circuits.action.RESUME"
        const val ACTION_STOP = "net.aucutt.circuits.action.STOP"
        const val ACTION_RESET = "net.aucutt.circuits.action.RESET"

        private const val CHANNEL_ID = "circuit_timer"
        private const val NOTIFICATION_ID = 1001

        fun start(context: Context) {
            val intent = Intent(context, CircuitTimerService::class.java).setAction(ACTION_START)
            context.startForegroundService(intent)
        }

        fun pause(context: Context) {
            context.startService(
                Intent(context, CircuitTimerService::class.java).setAction(ACTION_PAUSE)
            )
        }

        fun resume(context: Context) {
            context.startForegroundService(
                Intent(context, CircuitTimerService::class.java).setAction(ACTION_RESUME)
            )
        }

        fun stop(context: Context) {
            context.startService(
                Intent(context, CircuitTimerService::class.java).setAction(ACTION_STOP)
            )
        }

        fun reset(context: Context) {
            context.startService(
                Intent(context, CircuitTimerService::class.java).setAction(ACTION_RESET)
            )
        }
    }
}

private fun formatTime(totalSeconds: Int): String {
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "%d:%02d".format(minutes, seconds)
}
