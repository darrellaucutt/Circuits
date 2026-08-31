package net.aucutt.circuits.sync

import android.net.Uri
import com.google.android.gms.wearable.DataMap

/** Reads timer state payloads from Wear Data Layer items. */
fun parseTimerStatePayload(dataMap: DataMap): SyncTimerState? {
    val json = dataMap.getString("payload")
        ?: dataMap.getByteArray("payload")?.toString(Charsets.UTF_8)
    return json?.let(WearSyncCodec::timerStateFromJson)
}

fun timerStateDataUri(): Uri = Uri.Builder()
    .scheme("wear")
    .path(WearSyncPaths.TIMER_STATE)
    .build()
