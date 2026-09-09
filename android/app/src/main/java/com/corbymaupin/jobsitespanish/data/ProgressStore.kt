package com.corbymaupin.jobsitespanish.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.json.JSONObject

private val Context.progressDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "jobsite_spanish_v3"
)

/**
 * DataStore-backed progress mirroring web localStorage key jobsite-spanish-v3:
 * progress[cardKey] -> { box, introduced, nextReview }, streak, session.
 */
class ProgressStore(private val context: Context) {

    private val keyProgress = stringPreferencesKey("progress_json")
    private val keyStreak = stringPreferencesKey("streak_json")
    private val keySession = stringPreferencesKey("session_json")

    val progressFlow: Flow<Map<String, CardProgress>> =
        context.progressDataStore.data.map { prefs ->
            parseProgress(prefs[keyProgress])
        }

    val streakFlow: Flow<StreakState> =
        context.progressDataStore.data.map { prefs ->
            parseStreak(prefs[keyStreak])
        }

    val sessionFlow: Flow<SessionSnapshot?> =
        context.progressDataStore.data.map { prefs ->
            parseSession(prefs[keySession])
        }

    suspend fun getProgress(): Map<String, CardProgress> {
        var result: Map<String, CardProgress> = emptyMap()
        context.progressDataStore.data.map { parseProgress(it[keyProgress]) }
            .collect { result = it; return@collect }
        return result
    }

    suspend fun commitCard(id: String, progress: CardProgress) {
        context.progressDataStore.edit { prefs ->
            val map = parseProgress(prefs[keyProgress]).toMutableMap()
            map[id] = progress
            prefs[keyProgress] = encodeProgress(map)
        }
    }

    suspend fun setProgressMap(map: Map<String, CardProgress>) {
        context.progressDataStore.edit { prefs ->
            prefs[keyProgress] = encodeProgress(map)
        }
    }

    suspend fun setStreak(streak: StreakState) {
        context.progressDataStore.edit { prefs ->
            prefs[keyStreak] = JSONObject()
                .put("last", streak.last)
                .put("days", streak.days)
                .toString()
        }
    }

    suspend fun saveSession(session: SessionSnapshot?) {
        context.progressDataStore.edit { prefs ->
            if (session == null) {
                prefs.remove(keySession)
            } else {
                prefs[keySession] = encodeSession(session)
            }
        }
    }

    companion object {
        fun parseProgress(raw: String?): Map<String, CardProgress> {
            if (raw.isNullOrBlank()) return emptyMap()
            val obj = JSONObject(raw)
            val out = mutableMapOf<String, CardProgress>()
            val keys = obj.keys()
            while (keys.hasNext()) {
                val k = keys.next()
                val o = obj.getJSONObject(k)
                out[k] = CardProgress(
                    box = o.optInt("box", 1).coerceIn(1, 5),
                    introduced = o.optBoolean("introduced", false),
                    nextReview = if (o.has("nextReview") && !o.isNull("nextReview")) o.optString("nextReview").takeIf { it.isNotBlank() } else null
                )
            }
            return out
        }

        fun encodeProgress(map: Map<String, CardProgress>): String {
            val obj = JSONObject()
            map.forEach { (k, v) ->
                obj.put(
                    k,
                    JSONObject()
                        .put("box", v.box)
                        .put("introduced", v.introduced)
                        .put("nextReview", v.nextReview)
                )
            }
            return obj.toString()
        }

        fun parseStreak(raw: String?): StreakState {
            if (raw.isNullOrBlank()) return StreakState()
            val o = JSONObject(raw)
            return StreakState(
                last = o.optString("last", ""),
                days = o.optInt("days", 0)
            )
        }

        fun parseSession(raw: String?): SessionSnapshot? {
            if (raw.isNullOrBlank()) return null
            val o = JSONObject(raw)
            val keysArr = o.optJSONArray("keys") ?: return null
            val keys = List(keysArr.length()) { keysArr.getString(it) }
            val gradsObj = o.optJSONObject("graduations") ?: JSONObject()
            val grads = mutableMapOf<String, Int>()
            val gKeys = gradsObj.keys()
            while (gKeys.hasNext()) {
                val gk = gKeys.next()
                grads[gk] = gradsObj.getInt(gk)
            }
            return SessionSnapshot(
                trade = o.optString("trade", "All"),
                keys = keys,
                graduations = grads,
                isCategoryMode = o.optBoolean("isCategoryMode", false)
            )
        }

        fun encodeSession(s: SessionSnapshot): String {
            val grads = JSONObject()
            s.graduations.forEach { (k, v) -> grads.put(k, v) }
            val keys = org.json.JSONArray()
            s.keys.forEach { keys.put(it) }
            return JSONObject()
                .put("trade", s.trade)
                .put("keys", keys)
                .put("graduations", grads)
                .put("isCategoryMode", s.isCategoryMode)
                .toString()
        }
    }
}
