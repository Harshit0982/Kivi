package com.example.geminilive

import android.content.Context
import android.content.SharedPreferences
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

data class ApiKey(
    val id: String,
    val value: String,
    val label: String,
    var rateLimitedUntil: Long = 0L,
    var usageCount: Int = 0,
    var lastUsed: Long = 0L
)

class KeyManager private constructor(context: Context) {

    private val prefs: SharedPreferences = context.applicationContext
        .getSharedPreferences("gemini_keys_prefs", Context.MODE_PRIVATE)

    companion object {
        @Volatile private var INSTANCE: KeyManager? = null

        fun getInstance(context: Context): KeyManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: KeyManager(context).also { INSTANCE = it }
            }
        }

        const val PREFS_NAME = "gemini_keys_prefs"
        const val KEY_LIST = "keys_json"
    }

    fun addKey(value: String, label: String) {
        val value = value.trim()
        if (value.isEmpty()) return

        val keys = getAllKeys().toMutableList()

        // Avoid duplicates
        if (keys.any { it.value == value }) return

        keys.add(
            ApiKey(
                id = UUID.randomUUID().toString(),
                value = value,
                label = label.ifBlank { "Account ${keys.size + 1}" }
            )
        )
        saveKeys(keys)
    }

    fun removeKey(id: String) {
        val keys = getAllKeys().filter { it.id != id }
        saveKeys(keys)
    }

    fun updateLabel(id: String, newLabel: String) {
        val keys = getAllKeys().toMutableList()
        val idx = keys.indexOfFirst { it.id == id }
        if (idx >= 0) {
            keys[idx] = keys[idx].copy(label = newLabel.ifBlank { keys[idx].label })
            saveKeys(keys)
        }
    }

    fun getAllKeys(): List<ApiKey> {
        val json = prefs.getString(KEY_LIST, "[]") ?: "[]"
        return try {
            val arr = JSONArray(json)
            val list = mutableListOf<ApiKey>()
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                list.add(
                    ApiKey(
                        id = obj.getString("id"),
                        value = obj.getString("value"),
                        label = obj.optString("label", "Key"),
                        rateLimitedUntil = obj.optLong("rateLimitedUntil", 0L),
                        usageCount = obj.optInt("usageCount", 0),
                        lastUsed = obj.optLong("lastUsed", 0L)
                    )
                )
            }
            list
        } catch (e: Exception) {
            emptyList()
        }
    }

    /** Returns next available key (round-robin: least recently used + not rate-limited) */
    fun getNextAvailableKey(): ApiKey? {
        val now = System.currentTimeMillis()
        val candidates = getAllKeys().filter { it.rateLimitedUntil < now }
        if (candidates.isEmpty()) return null
        // Pick least recently used to spread load evenly
        return candidates.minByOrNull { it.lastUsed }
    }

    fun markRateLimited(id: String, durationMs: Long = 60_000L) {
        val keys = getAllKeys().toMutableList()
        val idx = keys.indexOfFirst { it.id == id }
        if (idx >= 0) {
            keys[idx] = keys[idx].copy(
                rateLimitedUntil = System.currentTimeMillis() + durationMs
            )
            saveKeys(keys)
        }
    }

    fun markUsed(id: String) {
        val keys = getAllKeys().toMutableList()
        val idx = keys.indexOfFirst { it.id == id }
        if (idx >= 0) {
            keys[idx] = keys[idx].copy(
                usageCount = keys[idx].usageCount + 1,
                lastUsed = System.currentTimeMillis()
            )
            saveKeys(keys)
        }
    }

    fun hasKeys(): Boolean = getAllKeys().isNotEmpty()

    fun getStats(): String {
        val keys = getAllKeys()
        if (keys.isEmpty()) return "No keys"
        val now = System.currentTimeMillis()
        val active = keys.count { it.rateLimitedUntil < now }
        val limited = keys.size - active
        return "Total: ${keys.size} | Active: $active | Limited: $limited"
    }

    private fun saveKeys(keys: List<ApiKey>) {
        val arr = JSONArray()
        keys.forEach { k ->
            arr.put(
                JSONObject().apply {
                    put("id", k.id)
                    put("value", k.value)
                    put("label", k.label)
                    put("rateLimitedUntil", k.rateLimitedUntil)
                    put("usageCount", k.usageCount)
                    put("lastUsed", k.lastUsed)
                }
            )
        }
        prefs.edit().putString(KEY_LIST, arr.toString()).apply()
    }
}
