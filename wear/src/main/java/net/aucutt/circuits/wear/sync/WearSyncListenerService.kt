package net.aucutt.circuits.wear.sync

import com.google.android.gms.wearable.DataEvent
import com.google.android.gms.wearable.DataEventBuffer
import com.google.android.gms.wearable.DataMapItem
import com.google.android.gms.wearable.WearableListenerService
import net.aucutt.circuits.sync.WearSyncCodec
import net.aucutt.circuits.sync.WearSyncPaths

class WearSyncListenerService : WearableListenerService() {

    override fun onDataChanged(dataEvents: DataEventBuffer) {
        dataEvents.forEach { event ->
            if (event.type != DataEvent.TYPE_CHANGED) return@forEach
            val item = event.dataItem
            val path = item.uri.path ?: return@forEach
            val payload = DataMapItem.fromDataItem(item).dataMap
                .getByteArray("payload")
                ?.toString(Charsets.UTF_8)
                ?: return@forEach

            when (path) {
                WearSyncPaths.CIRCUITS -> {
                    WearSyncRepository.updateCircuits(WearSyncCodec.circuitsFromJson(payload))
                }
                WearSyncPaths.CONFIG -> {
                    WearSyncCodec.configFromJson(payload)?.let(WearSyncRepository::updateConfig)
                }
                WearSyncPaths.COMMAND -> {
                    WearSyncCodec.commandFromJson(payload)?.let(WearSyncRepository::handleCommand)
                }
            }
        }
    }
}
