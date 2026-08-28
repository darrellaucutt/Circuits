package net.aucutt.circuits.wear.sync

import com.google.android.gms.wearable.DataEvent
import com.google.android.gms.wearable.DataEventBuffer
import com.google.android.gms.wearable.DataMapItem
import com.google.android.gms.wearable.WearableListenerService
import net.aucutt.circuits.sync.WearSyncCodec
import net.aucutt.circuits.sync.WearSyncPaths

class WearStateListenerService : WearableListenerService() {

    override fun onDataChanged(dataEvents: DataEventBuffer) {
        dataEvents.forEach { event ->
            if (event.type != DataEvent.TYPE_CHANGED) return@forEach
            val path = event.dataItem.uri.path ?: return@forEach
            if (path != WearSyncPaths.TIMER_STATE) return@forEach

            val payload = DataMapItem.fromDataItem(event.dataItem).dataMap
                .getByteArray("payload")
                ?.toString(Charsets.UTF_8)
                ?: return@forEach

            WearSyncCodec.timerStateFromJson(payload)?.let(WearMirrorRepository::updateTimerState)
        }
    }
}
