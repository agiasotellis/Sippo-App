package com.sippo.app.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import org.json.JSONArray
import org.json.JSONObject

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "sippo")

data class Stored(val settings: Settings, val entries: List<Entry>)

/** Everything Sippo knows lives in one on-device DataStore. No account, no server. */
class SippoRepository private constructor(private val context: Context) {

    private object K {
        val onboarded = booleanPreferencesKey("onboarded")
        val mascot = stringPreferencesKey("mascot")
        val accessory = stringPreferencesKey("accessory")
        val weight = intPreferencesKey("weight")
        val activity = intPreferencesKey("activity")
        val units = stringPreferencesKey("units")
        val goal = intPreferencesKey("goal")
        val remind = booleanPreferencesKey("remind")
        val interval = intPreferencesKey("interval")
        val smart = booleanPreferencesKey("smart")
        val theme = stringPreferencesKey("theme")
        val premium = booleanPreferencesKey("premium")
        val lastDrink = stringPreferencesKey("last_drink")
        val sheetAmount = intPreferencesKey("sheet_amount")
        val celebratedDay = longPreferencesKey("celebrated_day")
        val crownEarned = booleanPreferencesKey("crown_earned")
        val entries = stringPreferencesKey("entries")
    }

    val data: Flow<Stored> = context.dataStore.data.map { p -> Stored(p.toSettings(), decodeEntries(p[K.entries])) }

    suspend fun snapshot(): Stored = data.first()

    suspend fun saveSettings(s: Settings) {
        context.dataStore.edit { p ->
            p[K.onboarded] = s.onboarded
            p[K.mascot] = s.mascot.key
            p[K.accessory] = s.accessory.key
            p[K.weight] = s.weightKg
            p[K.activity] = s.activity
            p[K.units] = s.units.name
            p[K.goal] = s.goalMl
            p[K.remind] = s.remind
            p[K.interval] = s.intervalMin
            p[K.smart] = s.smart
            p[K.theme] = s.theme
            p[K.premium] = s.premium
            p[K.lastDrink] = s.lastDrink.key
            p[K.sheetAmount] = s.sheetAmount
            p[K.celebratedDay] = s.celebratedDay
            p[K.crownEarned] = s.crownEarned
        }
    }

    suspend fun saveEntries(entries: List<Entry>) {
        context.dataStore.edit { it[K.entries] = encodeEntries(entries) }
    }

    /** Appends an entry atomically; used by the widget, which has no view model. */
    suspend fun addEntry(entry: Entry) {
        context.dataStore.edit { p -> p[K.entries] = encodeEntries(decodeEntries(p[K.entries]) + entry) }
    }

    private fun Preferences.toSettings(): Settings {
        val d = Settings()
        return Settings(
            onboarded = this[K.onboarded] ?: d.onboarded,
            mascot = Mascot.of(this[K.mascot] ?: d.mascot.key),
            accessory = Accessory.of(this[K.accessory] ?: d.accessory.key),
            weightKg = this[K.weight] ?: d.weightKg,
            activity = this[K.activity] ?: d.activity,
            units = runCatching { Units.valueOf(this[K.units] ?: "") }.getOrDefault(d.units),
            goalMl = this[K.goal] ?: d.goalMl,
            remind = this[K.remind] ?: d.remind,
            intervalMin = this[K.interval] ?: d.intervalMin,
            smart = this[K.smart] ?: d.smart,
            theme = this[K.theme] ?: d.theme,
            premium = this[K.premium] ?: d.premium,
            lastDrink = Drink.of(this[K.lastDrink] ?: d.lastDrink.key),
            sheetAmount = this[K.sheetAmount] ?: d.sheetAmount,
            celebratedDay = this[K.celebratedDay] ?: d.celebratedDay,
            crownEarned = this[K.crownEarned] ?: d.crownEarned,
        )
    }

    companion object {
        @Volatile private var instance: SippoRepository? = null

        fun get(context: Context): SippoRepository =
            instance ?: synchronized(this) {
                instance ?: SippoRepository(context.applicationContext).also { instance = it }
            }

        fun encodeEntries(entries: List<Entry>): String {
            val arr = JSONArray()
            entries.forEach { e ->
                arr.put(JSONObject().put("id", e.id).put("d", e.day).put("m", e.minute).put("t", e.drink.key).put("ml", e.ml))
            }
            return arr.toString()
        }

        fun decodeEntries(raw: String?): List<Entry> {
            if (raw.isNullOrEmpty()) return emptyList()
            return runCatching {
                val arr = JSONArray(raw)
                List(arr.length()) { i ->
                    val o = arr.getJSONObject(i)
                    Entry(o.getLong("id"), o.getLong("d"), o.getInt("m"), Drink.of(o.getString("t")), o.getInt("ml"))
                }
            }.getOrDefault(emptyList())
        }
    }
}
