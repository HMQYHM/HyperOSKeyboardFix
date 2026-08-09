package io.github.hmqyhm.hyperoskeyboardfix.model

import org.json.JSONArray
import org.json.JSONObject

data class KeyboardMacro(
    val id: String,
    val name: String,
    val packageName: String?,
    val packageNames: Set<String> = packageName?.let(::setOf).orEmpty(),
    val triggerKeyCode: Int,
    val triggerMetaState: Int,
    val actionType: MacroActionType,
    val text: String,
    val steps: List<MacroKeyStep>,
    val repeatMode: MacroRepeatMode,
    val repeatCount: Int,
    val repeatIntervalMs: Long,
    val enabled: Boolean,
) {
    fun toJson(): JSONObject = JSONObject().apply {
        put("id", id)
        put("name", name)
        put("packageName", packageName ?: JSONObject.NULL)
        put("packageNames", JSONArray().apply { packageNames.sorted().forEach(::put) })
        put("triggerKeyCode", triggerKeyCode)
        put("triggerMetaState", triggerMetaState)
        put("actionType", actionType.name)
        put("text", text)
        put("repeatMode", repeatMode.name)
        put("repeatCount", repeatCount)
        put("repeatIntervalMs", repeatIntervalMs)
        put("enabled", enabled)
        put("steps", JSONArray().apply { steps.forEach { put(it.toJson()) } })
    }

    companion object {
        fun fromJson(value: JSONObject): KeyboardMacro {
            val legacyPackage = if (value.isNull("packageName")) {
                null
            } else {
                value.optString("packageName")
                    .takeIf { it.isNotBlank() && it != "null" }
            }
            val packages = value.optJSONArray("packageNames")?.let { array ->
                buildSet {
                    for (index in 0 until array.length()) {
                        array.optString(index).takeIf(String::isNotBlank)?.let(::add)
                    }
                }
            }.orEmpty().ifEmpty { legacyPackage?.let(::setOf).orEmpty() }
            return KeyboardMacro(
            id = value.optString("id"),
            name = value.optString("name"),
            packageName = legacyPackage,
            packageNames = packages,
            triggerKeyCode = value.optInt("triggerKeyCode"),
            triggerMetaState = value.optInt("triggerMetaState"),
            actionType = enumValueOrDefault(
                value.optString("actionType"),
                MacroActionType.SEQUENCE,
            ),
            text = value.optString("text"),
            steps = value.optJSONArray("steps")?.let { array ->
                buildList {
                    for (index in 0 until array.length()) {
                        array.optJSONObject(index)?.let { stepJson ->
                            MacroKeyStep.fromJson(stepJson)
                                .takeIf { it.keyCode != 0 }
                                ?.let(::add)
                        }
                    }
                }
            }.orEmpty(),
            repeatMode = enumValueOrDefault(
                value.optString("repeatMode"),
                MacroRepeatMode.ONCE,
            ),
            repeatCount = value.optInt("repeatCount", 1).coerceIn(1, 100),
            repeatIntervalMs = value.optLong("repeatIntervalMs", 80L)
                .coerceIn(0L, 10_000L),
            enabled = value.optBoolean("enabled", true),
        )
        }

        fun encode(items: List<KeyboardMacro>): String =
            JSONArray().apply { items.forEach { put(it.toJson()) } }.toString()

        fun decode(json: String): List<KeyboardMacro> = try {
            val array = JSONArray(json)
            buildList {
                for (index in 0 until array.length()) {
                    array.optJSONObject(index)?.let { add(fromJson(it)) }
                }
            }
        } catch (_: Throwable) {
            emptyList()
        }

        private inline fun <reified T : Enum<T>> enumValueOrDefault(
            name: String,
            default: T,
        ): T = enumValues<T>().firstOrNull { it.name == name } ?: default
    }

    fun appliesTo(packageName: String): Boolean =
        packageNames.isEmpty() || packageName in packageNames
}

data class MacroKeyStep(
    val keyCode: Int,
    val metaState: Int,
    val delayAfterMs: Long,
    val holdDurationMs: Long = 40L,
) {
    fun toJson(): JSONObject = JSONObject().apply {
        put("keyCode", keyCode)
        put("metaState", metaState)
        put("delayAfterMs", delayAfterMs)
        put("holdDurationMs", holdDurationMs)
    }

    companion object {
        fun fromJson(value: JSONObject) = MacroKeyStep(
            keyCode = value.optInt("keyCode"),
            metaState = value.optInt("metaState"),
            delayAfterMs = value.optLong("delayAfterMs", 50L).coerceIn(0L, 10_000L),
            holdDurationMs = value.optLong("holdDurationMs", 40L).coerceIn(0L, 10_000L),
        )
    }
}

enum class MacroActionType { TEXT, SEQUENCE }

enum class MacroRepeatMode { ONCE, FIXED, WHILE_HELD, TOGGLE }
