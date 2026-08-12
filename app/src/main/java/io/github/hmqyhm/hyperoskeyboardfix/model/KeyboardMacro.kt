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
    val mouseSteps: List<MacroMouseStep> = emptyList(),
    val mouseBackgroundFile: String? = null,
    val actions: List<MacroActionStep> = emptyList(),
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
        put("mouseSteps", JSONArray().apply { mouseSteps.forEach { put(it.toJson()) } })
        put("mouseBackgroundFile", mouseBackgroundFile ?: JSONObject.NULL)
        put("actions", JSONArray().apply { effectiveActions().forEach { put(it.toJson()) } })
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
            mouseSteps = value.optJSONArray("mouseSteps")?.let { array ->
                buildList {
                    for (index in 0 until array.length()) {
                        array.optJSONObject(index)?.let { add(MacroMouseStep.fromJson(it)) }
                    }
                }
            }.orEmpty(),
            mouseBackgroundFile = if (value.isNull("mouseBackgroundFile")) {
                null
            } else {
                value.optString("mouseBackgroundFile").takeIf {
                    it.isNotBlank() && '/' !in it && '\\' !in it
                }
            },
            actions = value.optJSONArray("actions")?.let { array ->
                buildList {
                    for (index in 0 until array.length()) {
                        array.optJSONObject(index)?.let { actionJson ->
                            MacroActionStep.fromJson(actionJson)?.let(::add)
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

    fun effectiveActions(): List<MacroActionStep> {
        if (actions.isNotEmpty()) return actions
        return when (actionType) {
            MacroActionType.TEXT -> text.takeIf(String::isNotBlank)?.let {
                listOf(MacroActionStep.text(it))
            }.orEmpty()
            MacroActionType.SEQUENCE -> steps.map(MacroActionStep::key)
            MacroActionType.MOUSE -> mouseSteps.map(MacroActionStep::mouse)
        }
    }
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

data class MacroMouseStep(
    val action: MacroMouseAction,
    val xFraction: Float,
    val yFraction: Float,
    val holdDurationMs: Long = 40L,
    val gestureDurationMs: Long = 300L,
    val distanceFraction: Float = 0.18f,
    val delayAfterMs: Long = 50L,
) {
    fun toJson(): JSONObject = JSONObject().apply {
        put("action", action.name)
        put("xFraction", xFraction.toDouble())
        put("yFraction", yFraction.toDouble())
        put("holdDurationMs", holdDurationMs)
        put("gestureDurationMs", gestureDurationMs)
        put("distanceFraction", distanceFraction.toDouble())
        put("delayAfterMs", delayAfterMs)
    }

    companion object {
        fun fromJson(value: JSONObject) = MacroMouseStep(
            action = enumValues<MacroMouseAction>().firstOrNull {
                it.name == value.optString("action")
            } ?: MacroMouseAction.LEFT_CLICK,
            xFraction = value.optDouble("xFraction", 0.5).toFloat().coerceIn(0f, 1f),
            yFraction = value.optDouble("yFraction", 0.5).toFloat().coerceIn(0f, 1f),
            holdDurationMs = value.optLong("holdDurationMs", 40L).coerceIn(10L, 10_000L),
            gestureDurationMs = value.optLong("gestureDurationMs", 300L)
                .coerceIn(10L, 10_000L),
            distanceFraction = value.optDouble("distanceFraction", 0.18).toFloat()
                .coerceIn(0.01f, 1f),
            delayAfterMs = value.optLong("delayAfterMs", 50L).coerceIn(0L, 10_000L),
        )
    }
}

data class MacroActionStep(
    val type: MacroActionStepType,
    val text: String = "",
    val keyStep: MacroKeyStep? = null,
    val mouseStep: MacroMouseStep? = null,
) {
    fun toJson(): JSONObject = JSONObject().apply {
        put("type", type.name)
        when (type) {
            MacroActionStepType.TEXT -> put("text", text)
            MacroActionStepType.KEY -> put("keyStep", keyStep?.toJson() ?: JSONObject.NULL)
            MacroActionStepType.MOUSE -> put(
                "mouseStep",
                mouseStep?.toJson() ?: JSONObject.NULL,
            )
        }
    }

    companion object {
        fun text(value: String) = MacroActionStep(
            type = MacroActionStepType.TEXT,
            text = value,
        )

        fun key(value: MacroKeyStep) = MacroActionStep(
            type = MacroActionStepType.KEY,
            keyStep = value,
        )

        fun mouse(value: MacroMouseStep) = MacroActionStep(
            type = MacroActionStepType.MOUSE,
            mouseStep = value,
        )

        fun fromJson(value: JSONObject): MacroActionStep? {
            val type = enumValues<MacroActionStepType>().firstOrNull {
                it.name == value.optString("type")
            } ?: return null
            return when (type) {
                MacroActionStepType.TEXT -> value.optString("text")
                    .takeIf(String::isNotBlank)?.let(::text)
                MacroActionStepType.KEY -> value.optJSONObject("keyStep")
                    ?.let(MacroKeyStep::fromJson)
                    ?.takeIf { it.keyCode != 0 }
                    ?.let(::key)
                MacroActionStepType.MOUSE -> value.optJSONObject("mouseStep")
                    ?.let(MacroMouseStep::fromJson)
                    ?.let(::mouse)
            }
        }
    }
}

enum class MacroActionType { TEXT, SEQUENCE, MOUSE }

enum class MacroMouseAction { LEFT_CLICK, RIGHT_CLICK, SCROLL_UP, SCROLL_DOWN }

enum class MacroActionStepType { TEXT, KEY, MOUSE }

enum class MacroRepeatMode { ONCE, FIXED, WHILE_HELD, TOGGLE }
