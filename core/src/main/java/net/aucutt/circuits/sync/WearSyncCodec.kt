package net.aucutt.circuits.sync

import org.json.JSONObject

object WearSyncCodec {

    fun timerStateToJson(state: SyncTimerState): String {
        return JSONObject()
            .put("phase", state.phase)
            .put("remainingSeconds", state.remainingSeconds)
            .put("currentRound", state.currentRound)
            .put("isPaused", state.isPaused)
            .put("intervalMinutes", state.intervalMinutes)
            .put("cooldownMinutes", state.cooldownMinutes)
            .put("repeats", state.repeats)
            .put("circuitName", state.circuitName)
            .put("updatedAt", state.updatedAt)
            .toString()
    }

    fun timerStateFromJson(json: String): SyncTimerState? {
        if (json.isBlank()) return null
        val item = JSONObject(json)
        return SyncTimerState(
            phase = item.getString("phase"),
            remainingSeconds = item.getInt("remainingSeconds"),
            currentRound = item.getInt("currentRound"),
            isPaused = item.optBoolean("isPaused", false),
            intervalMinutes = item.getInt("intervalMinutes"),
            cooldownMinutes = item.getInt("cooldownMinutes"),
            repeats = item.getInt("repeats"),
            circuitName = item.optString("circuitName", ""),
            updatedAt = item.optLong("updatedAt", System.currentTimeMillis()),
        )
    }

    fun commandToJson(command: WearCommand): String {
        return JSONObject()
            .put("action", command.action.name)
            .put("timestamp", command.timestamp)
            .toString()
    }

    fun commandFromJson(json: String): WearCommand? {
        if (json.isBlank()) return null
        val item = JSONObject(json)
        val action = runCatching {
            WearCommandAction.valueOf(item.getString("action"))
        }.getOrNull() ?: return null
        return WearCommand(
            action = action,
            timestamp = item.optLong("timestamp", System.currentTimeMillis()),
        )
    }
}
