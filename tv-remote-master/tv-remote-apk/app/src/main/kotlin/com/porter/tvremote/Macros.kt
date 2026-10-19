package com.porter.tvremote

import android.content.Context
import android.util.Log
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.util.concurrent.atomic.AtomicBoolean

/** A saved macro: a name plus ordered steps such as "up", "ok", "home" or "wait:3" (seconds). */
@Serializable
data class Macro(val name: String, val steps: List<String>)

/** Persists macros as one JSON string in the app's SharedPreferences. */
internal object MacroStore {
    private const val KEY = "macros_json"
    private val json = Json { ignoreUnknownKeys = true }

    const val MAX_STEPS = 100
    const val MAX_NAME = 40
    const val STEP_DELAY_MS = 350L

    val KEYCODES = mapOf(
        "up" to 19, "down" to 20, "left" to 21, "right" to 22,
        "home" to 3, "ok" to 23, "back" to 4,
    )

    fun isValidStep(step: String): Boolean {
        if (step in KEYCODES) return true
        val secs = step.removePrefix("wait:").takeIf { step.startsWith("wait:") }?.toIntOrNull()
        return secs != null && secs in 1..60
    }

    fun isValid(macro: Macro): Boolean =
        macro.name.isNotBlank() && macro.name.length <= MAX_NAME &&
            macro.steps.isNotEmpty() && macro.steps.size <= MAX_STEPS &&
            macro.steps.all(::isValidStep)

    fun list(context: Context): List<Macro> {
        val raw = Preferences.of(context).getString(KEY, null) ?: return emptyList()
        return runCatching { json.decodeFromString<List<Macro>>(raw) }.getOrDefault(emptyList())
    }

    @Synchronized
    fun save(context: Context, macro: Macro) {
        require(isValid(macro)) { "Invalid macro" }
        val updated = list(context).filterNot { it.name == macro.name } + macro
        Preferences.of(context).edit().putString(KEY, json.encodeToString(updated)).apply()
    }

    @Synchronized
    fun delete(context: Context, name: String) {
        val updated = list(context).filterNot { it.name == name }
        Preferences.of(context).edit().putString(KEY, json.encodeToString(updated)).apply()
    }
}

/** Plays a macro on a background thread; only one macro runs at a time. */
internal class MacroRunner(private val adb: AdbController) {
    private val running = AtomicBoolean(false)

    /** Returns false if another macro is still running. */
    fun start(steps: List<String>): Boolean {
        if (!running.compareAndSet(false, true)) return false
        Thread({
            try {
                for (step in steps) {
                    if (step.startsWith("wait:")) {
                        Thread.sleep(step.removePrefix("wait:").toLong() * 1000)
                    } else {
                        adb.keyEvent(MacroStore.KEYCODES.getValue(step))
                        Thread.sleep(MacroStore.STEP_DELAY_MS)
                    }
                }
            } catch (e: Exception) {
                Log.w("MacroRunner", "Macro aborted: ${e.message}")
            } finally {
                running.set(false)
            }
        }, "macro-runner").start()
        return true
    }
}
