package net.aucutt.circuits.wear.sync

import android.content.Context
import com.google.android.gms.wearable.Wearable
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import net.aucutt.circuits.sync.WearCommand
import net.aucutt.circuits.sync.WearCommandAction
import net.aucutt.circuits.sync.WearSyncCodec
import net.aucutt.circuits.sync.WearSyncPaths

class WearCommandSender(context: Context) {

    private val appContext = context.applicationContext
    private val messageClient = Wearable.getMessageClient(appContext)
    private val nodeClient = Wearable.getNodeClient(appContext)
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    fun send(action: WearCommandAction) {
        scope.launch {
            val nodes = runCatching { nodeClient.connectedNodes.await() }.getOrNull().orEmpty()
            if (nodes.isEmpty()) {
                WearMirrorRepository.markDisconnected()
                return@launch
            }

            val payload = WearSyncCodec.commandToJson(WearCommand(action)).toByteArray()
            nodes.forEach { node ->
                runCatching {
                    messageClient.sendMessage(node.id, WearSyncPaths.COMMAND, payload).await()
                }
            }
        }
    }
}
