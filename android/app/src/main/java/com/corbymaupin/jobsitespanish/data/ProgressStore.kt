// app/src/main/java/com/corbymaupin/jobsitespanish/data/ProgressStore.kt
package com.corbymaupin.jobsitespanish.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.corbymaupin.jobsitespanish.ui.LearningDirection
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import org.json.JSONObject
import java.io.IOException

private val Context.progressDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "jobsite_spanish_v3"
)

/**
 * DataStore-backed progress mirroring web localStorage key jobsite-spanish-v3:
 * progress[cardKey] -> { box, introduced, nextReview }, streak, session.
 *
 * 1.0.3 adds one plain-string preference in the same store: "learning_direction"
 * ("learn_spanish" / "learn_english"). progress_json, streak_json and session_json
 * and their JSON shapes are unchanged.
 */
class ProgressStore(private val context: Context) {


    private val keyProgress = stringPreferencesKey("progress_json")
    private val keyStreak = stringPreferencesKey("streak_json")
    private val keySession = stringPreferencesKey("session_json")
    private val keyLearningDirection = stringPreferencesKey("learning_direction")

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

    /**
     * Saved learning direction as its storage value. Missing -> LearningDirection.DEFAULT.
     * A read error also falls back to the default instead of crashing.
     * Map to the enum with LearningDirection.fromStorage(value).
     */
    val learningDirectionFlow: Flow<String> =
        context.progressDataStore.data
            .map { prefs -> prefs[keyLearningDirection] ?: LearningDirection.DEFAULT.storageValue }
            .catch { e ->
                if (e is IOException) emit(LearningDirection.DEFAULT.storageValue) else throw e
            }
            .distinctUntilChanged()


    /**
     * One-shot read of the current progress map.
     * (The 1.0.2 version collected the endless DataStore flow and never returned.)
     */
    suspend fun getProgress(): Map<String, CardProgress> = progressFlow.first()

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

    /** Persist the learning direction (pass LearningDirection.storageValue). Touches only this key. */
    suspend fun setLearningDirection(storageValue: String) {
        context.progressDataStore.edit { prefs ->
            prefs[keyLearningDirection] = storageValue
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
