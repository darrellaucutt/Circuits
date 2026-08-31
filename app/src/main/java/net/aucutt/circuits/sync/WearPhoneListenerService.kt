package net.aucutt.circuits.sync

import com.google.android.gms.wearable.MessageEvent
import com.google.android.gms.wearable.WearableListenerService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import net.aucutt.circuits.service.CircuitTimerService

class WearPhoneListenerService : WearableListenerService() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onMessageReceived(messageEvent: MessageEvent) {
        when (messageEvent.path) {
            WearSyncPaths.COMMAND -> handleCommand(messageEvent)
            WearSyncPaths.TIMER_STATE_REQUEST -> handleStateRequest(messageEvent)
        }
    }

    private fun handleCommand(messageEvent: MessageEvent) {
        val payload = messageEvent.data?.toString(Charsets.UTF_8) ?: return
        val command = WearSyncCodec.commandFromJson(payload) ?: return

        when (command.action) {
            WearCommandAction.PAUSE -> CircuitTimerService.pause(applicationContext)
            WearCommandAction.RESUME -> CircuitTimerService.resume(applicationContext)
            WearCommandAction.STOP -> CircuitTimerService.stop(applicationContext)
        }

        scope.launch {
            WearStatePublisher.getInstance(applicationContext).sendStateToNode(messageEvent.sourceNodeId)
        }
    }

    private fun handleStateRequest(messageEvent: MessageEvent) {
        scope.launch {
            WearStatePublisher.getInstance(applicationContext).sendStateToNode(messageEvent.sourceNodeId)
        }
    }
}
