package net.aucutt.circuits.sync

import com.google.android.gms.wearable.MessageEvent
import com.google.android.gms.wearable.WearableListenerService
import net.aucutt.circuits.service.CircuitTimerService
import net.aucutt.circuits.timer.CircuitTimerEngine

class WearCommandListenerService : WearableListenerService() {

    override fun onMessageReceived(messageEvent: MessageEvent) {
        if (messageEvent.path != WearSyncPaths.COMMAND) return

        val payload = messageEvent.data?.toString(Charsets.UTF_8) ?: return
        val command = WearSyncCodec.commandFromJson(payload) ?: return

        when (command.action) {
            WearCommandAction.PAUSE -> CircuitTimerService.pause(applicationContext)
            WearCommandAction.RESUME -> CircuitTimerService.resume(applicationContext)
            WearCommandAction.STOP -> CircuitTimerService.stop(applicationContext)
        }

        WearStatePublisher.getInstance(applicationContext).publish(
            state = CircuitTimerEngine.uiState.value,
            circuitName = "",
        )
    }
}
