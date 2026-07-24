package io.github.hmqyhm.hyperoskeyboardfix.hook

import android.content.ComponentName
import android.os.IBinder
import android.view.InputDevice
import android.view.KeyEvent
import io.github.hmqyhm.hyperoskeyboardfix.config.ConfigKeys
import io.github.hmqyhm.hyperoskeyboardfix.utils.HookLog
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedBridge
import de.robv.android.xposed.XposedHelpers
import java.lang.reflect.Method

object ExactMiuiPolicyDiagnosticHook {
    private const val TARGET_CLASS =
        "com.android.server.policy.BaseMiuiPhoneWindowManager"
    private const val KEY_COMBINATION_MANAGER_CLASS =
        "com.android.server.policy.KeyCombinationManager"

    fun install(classLoader: ClassLoader) {
        val clazz = try {
            XposedHelpers.findClassIfExists(TARGET_CLASS, classLoader)
        } catch (error: Throwable) {
            logHookException("<class_lookup>", error)
            null
        }
        if (clazz == null) {
            HookLog.i("EXACT_HOOK_SKIPPED class=$TARGET_CLASS reason=not_found")
            return
        }

        installExact(
            clazz = clazz,
            methodName = "interceptKeyBeforeDispatching",
            parameterTypes = arrayOf(
                IBinder::class.java,
                KeyEvent::class.java,
                Integer.TYPE,
            ),
            returnType = java.lang.Long.TYPE,
        )
        installExact(
            clazz = clazz,
            methodName = "handleAltTab",
            parameterTypes = arrayOf(KeyEvent::class.java),
            returnType = Void.TYPE,
        )
        installExact(
            clazz = clazz,
            methodName = "handleMetaKey",
            parameterTypes = arrayOf(KeyEvent::class.java),
            returnType = Void.TYPE,
        )
        installExact(
            clazz = clazz,
            methodName = "disableAOSPShortcut",
            parameterTypes = arrayOf(KeyEvent::class.java),
            returnType = java.lang.Boolean.TYPE,
        )
        installKeyCombinationManager(classLoader)
    }

    private fun installKeyCombinationManager(classLoader: ClassLoader) {
        val clazz = try {
            XposedHelpers.findClassIfExists(
                KEY_COMBINATION_MANAGER_CLASS,
                classLoader,
            )
        } catch (error: Throwable) {
            logHookException(KEY_COMBINATION_MANAGER_CLASS, "<class_lookup>", error)
            null
        }
        if (clazz == null) {
            HookLog.i(
                "EXACT_HOOK_SKIPPED class=$KEY_COMBINATION_MANAGER_CLASS " +
                    "method=interceptKey reason=not_found",
            )
            return
        }
        installExact(
            clazz = clazz,
            methodName = "interceptKey",
            parameterTypes = arrayOf(
                KeyEvent::class.java,
                java.lang.Boolean.TYPE,
            ),
            returnType = java.lang.Boolean.TYPE,
        )
    }

    private fun installExact(
        clazz: Class<*>,
        methodName: String,
        parameterTypes: Array<Class<*>>,
        returnType: Class<*>,
    ) {
        try {
            val method = clazz.getDeclaredMethod(methodName, *parameterTypes)
            if (method.returnType != returnType) {
                HookLog.i(
                    "EXACT_HOOK_SKIPPED class=${clazz.name} method=$methodName " +
                        "reason=return_type_mismatch expected=${returnType.name} " +
                        "actual=${method.returnType.name}",
                )
                return
            }
            XposedBridge.hookMethod(method, exactCallback(method))
            HookLog.i(
                "EXACT_HOOK_INSTALLED " +
                    "class=${method.declaringClass.name} " +
                    "method=${method.name} " +
                    "signature=${signature(method)}",
            )
        } catch (error: NoSuchMethodException) {
            HookLog.i(
                "EXACT_HOOK_SKIPPED class=${clazz.name} method=$methodName " +
                    "reason=not_found",
            )
        } catch (error: Throwable) {
            logHookException(methodName, error)
        }
    }

    private fun exactCallback(method: Method) = object : XC_MethodHook() {
        override fun beforeHookedMethod(param: MethodHookParam) {
            try {
                val event = param.args.filterIsInstance<KeyEvent>().firstOrNull() ?: return
                if (!isPhysicalKeyboardEvent(event)) return

                val logExactPath = isRequestedDiagnosticEvent(event)
                val logDisableResult = method.name == "disableAOSPShortcut" &&
                    event.keyCode in DISABLE_RESULT_KEYS
                val configurableEvent = shortcutKey(event) != null ||
                    isMetaKey(event.keyCode) ||
                    isAltKey(event.keyCode)
                if (!logExactPath && !logDisableResult && !configurableEvent) return

                val context = buildContext(event)
                param.setObjectExtra(EXTRA_CONTEXT, context)
                param.setObjectExtra(EXTRA_LOG_EXACT_PATH, logExactPath)
                if (logExactPath) {
                    HookLog.i(pathEnter(method, context))
                }
                when (method.name) {
                    "interceptKeyBeforeDispatching" -> {
                        if (
                            context.takeoverEnabled &&
                            context.metaModeEnabled &&
                            (
                                isMetaKey(event.keyCode) ||
                                    event.metaState and KeyEvent.META_META_ON != 0
                                )
                        ) {
                            param.result = 0L
                        }
                    }

                    "handleAltTab" -> {
                        if (
                            context.altTabModeEnabled &&
                            (isAltTab(event) || isAltKey(event.keyCode))
                        ) {
                            param.result = null
                        }
                    }

                    "handleMetaKey" -> {
                        if (
                            context.takeoverEnabled &&
                            context.metaModeEnabled &&
                            (
                                isMetaKey(event.keyCode) ||
                                    event.metaState and KeyEvent.META_META_ON != 0
                                )
                        ) {
                            param.result = null
                        }
                    }

                    "disableAOSPShortcut" -> {
                        if (context.takeoverEnabled) {
                            param.result = true
                        }
                    }

                    "interceptKey" -> {
                        if (
                            context.takeoverEnabled &&
                            shortcutKey(event) in ConfigKeys.META_SHORTCUTS
                        ) {
                            param.result = false
                            HookLog.i(
                                "SYSTEM_SHORTCUT_BLOCKED " +
                                    "keyName=${KeyEvent.keyCodeToString(event.keyCode)}",
                            )
                        }
                    }
                }
            } catch (error: Throwable) {
                logHookException(method.declaringClass.name, method.name, error)
            }
        }

        override fun afterHookedMethod(param: MethodHookParam) {
            val context = param.getObjectExtra(EXTRA_CONTEXT) as? ExactContext ?: return
            try {
                val originalResult = originalResult(method, param)
                if (param.getObjectExtra(EXTRA_LOG_EXACT_PATH) == true) {
                    HookLog.i(
                        "EXACT_PATH_EXIT " +
                            "class=${method.declaringClass.name} " +
                            "method=${method.name} " +
                            "originalResult=$originalResult",
                    )
                }
                if (method.name == "disableAOSPShortcut") {
                    HookLog.i(
                        "DISABLE_AOSP_SHORTCUT_RESULT " +
                            "keyName=${KeyEvent.keyCodeToString(context.event.keyCode)} " +
                            "originalResult=$originalResult",
                    )
                }
                if (
                    method.name == "interceptKeyBeforeDispatching" &&
                    context.takeoverEnabled &&
                    param.result == -1L
                ) {
                    param.result = 0L
                }
            } catch (error: Throwable) {
                logHookException(method.declaringClass.name, method.name, error)
            }
        }
    }

    private fun buildContext(event: KeyEvent): ExactContext {
        val config = ConfigProviderClient.snapshot()
        val foregroundPackage = lookupForegroundPackage()
        val shortcutKey = shortcutKey(event)
        val metaShortcutEnabled = ConfigKeys.META_SHORTCUTS.any { key ->
            config.shortcutStates[key] == true
        }
        val altTabShortcutEnabled =
            config.shortcutStates[ConfigKeys.SHORTCUT_ALT_TAB] == true
        val whitelist = config.whitelist.sorted()
        whitelist.forEachIndexed { index, candidate ->
            HookLog.i(
                "WHITELIST_COMPARE package=${foregroundPackage ?: "null"} " +
                    "candidate[$index]=$candidate",
            )
        }
        val whitelisted = foregroundPackage != null &&
            foregroundPackage in config.whitelist
        val baseEnabled = config.masterEnabled && whitelisted
        val metaModeEnabled = baseEnabled && metaShortcutEnabled
        val altTabModeEnabled = baseEnabled && altTabShortcutEnabled
        val shortcutEnabled = shortcutKey != null &&
            config.shortcutStates[shortcutKey] == true
        val takeoverEnabled = when {
            isMetaKey(event.keyCode) -> metaModeEnabled
            isAltKey(event.keyCode) -> altTabModeEnabled
            shortcutKey != null -> baseEnabled && shortcutEnabled
            else -> false
        }
        HookLog.i(
            "WHITELIST_COMPARE package=${foregroundPackage ?: "null"} " +
                "contains=$whitelisted",
        )
        if (shortcutKey != null) {
            val defaultValueUsed = shortcutKey !in config.presentShortcutKeys
            HookLog.i(
                "SHORTCUT_COMPARE lookupKey=$shortcutKey " +
                    "storedValue=${config.shortcutStates[shortcutKey] == true} " +
                    "defaultValueUsed=$defaultValueUsed",
            )
        }
        return ExactContext(
            event = event,
            masterEnabled = config.masterEnabled,
            foregroundPackage = foregroundPackage,
            whitelisted = whitelisted,
            shortcutEnabled = shortcutEnabled,
            takeoverEnabled = takeoverEnabled,
            metaModeEnabled = metaModeEnabled,
            altTabModeEnabled = altTabModeEnabled,
        )
    }

    private fun pathEnter(method: Method, context: ExactContext): String {
        val event = context.event
        val isMetaPressed = event.metaState and KeyEvent.META_META_ON != 0 ||
            event.keyCode == KeyEvent.KEYCODE_META_LEFT ||
            event.keyCode == KeyEvent.KEYCODE_META_RIGHT
        val isAltPressed = event.metaState and KeyEvent.META_ALT_ON != 0 ||
            event.keyCode == KeyEvent.KEYCODE_ALT_LEFT ||
            event.keyCode == KeyEvent.KEYCODE_ALT_RIGHT
        return "EXACT_PATH_ENTER " +
            "class=${method.declaringClass.name} " +
            "method=${method.name} " +
            "action=${event.action} " +
            "keyCode=${event.keyCode} " +
            "keyName=${KeyEvent.keyCodeToString(event.keyCode)} " +
            "metaState=${metaStateName(event.metaState)} " +
            "isMetaPressed=$isMetaPressed " +
            "isAltPressed=$isAltPressed " +
            "foregroundPackage=${context.foregroundPackage ?: "null"} " +
            "whitelisted=${context.whitelisted} " +
            "shortcutEnabled=${context.shortcutEnabled}"
    }

    private fun originalResult(
        method: Method,
        param: XC_MethodHook.MethodHookParam,
    ): String {
        if (method.returnType == Void.TYPE) return "void"
        if (param.hasThrowable()) return "<throws:${param.throwable.javaClass.name}>"
        return when (val result = param.result) {
            null -> "null"
            is Boolean, is Int, is Long -> result.toString()
            is KeyEvent -> "KeyEvent(action=${result.action},keyCode=${result.keyCode})"
            else -> "<${result.javaClass.name}>"
        }
    }

    private fun isRequestedDiagnosticEvent(event: KeyEvent): Boolean {
        val metaPressed = event.metaState and KeyEvent.META_META_ON != 0
        val altPressed = event.metaState and KeyEvent.META_ALT_ON != 0
        return when {
            event.keyCode == KeyEvent.KEYCODE_META_LEFT ||
                event.keyCode == KeyEvent.KEYCODE_META_RIGHT -> true
            event.keyCode == KeyEvent.KEYCODE_TAB && (metaPressed || altPressed) -> true
            event.keyCode == KeyEvent.KEYCODE_D && metaPressed -> true
            else -> false
        }
    }

    private fun isAltTab(event: KeyEvent): Boolean =
        event.keyCode == KeyEvent.KEYCODE_TAB &&
            event.metaState and KeyEvent.META_ALT_ON != 0

    private fun isMetaKey(keyCode: Int): Boolean =
        keyCode == KeyEvent.KEYCODE_META_LEFT ||
            keyCode == KeyEvent.KEYCODE_META_RIGHT

    private fun isAltKey(keyCode: Int): Boolean =
        keyCode == KeyEvent.KEYCODE_ALT_LEFT ||
            keyCode == KeyEvent.KEYCODE_ALT_RIGHT

    private fun shortcutKey(event: KeyEvent): String? {
        val metaPressed = event.metaState and KeyEvent.META_META_ON != 0
        val altPressed = event.metaState and KeyEvent.META_ALT_ON != 0
        return when {
            event.keyCode == KeyEvent.KEYCODE_TAB && altPressed ->
                ConfigKeys.SHORTCUT_ALT_TAB
            event.keyCode == KeyEvent.KEYCODE_TAB && metaPressed ->
                ConfigKeys.SHORTCUT_META_TAB
            event.keyCode == KeyEvent.KEYCODE_D && metaPressed ->
                ConfigKeys.SHORTCUT_META_D
            event.keyCode == KeyEvent.KEYCODE_E && metaPressed ->
                ConfigKeys.SHORTCUT_META_E
            event.keyCode == KeyEvent.KEYCODE_R && metaPressed ->
                ConfigKeys.SHORTCUT_META_R
            event.keyCode == KeyEvent.KEYCODE_L && metaPressed ->
                ConfigKeys.SHORTCUT_META_L
            event.keyCode == KeyEvent.KEYCODE_W && metaPressed ->
                ConfigKeys.SHORTCUT_META_W
            event.keyCode == KeyEvent.KEYCODE_M && metaPressed ->
                ConfigKeys.SHORTCUT_META_M
            event.keyCode == KeyEvent.KEYCODE_N && metaPressed ->
                ConfigKeys.SHORTCUT_META_N
            event.keyCode == KeyEvent.KEYCODE_S && metaPressed ->
                ConfigKeys.SHORTCUT_META_S
            event.keyCode == KeyEvent.KEYCODE_A && metaPressed ->
                ConfigKeys.SHORTCUT_META_A
            event.keyCode == KeyEvent.KEYCODE_C && metaPressed ->
                ConfigKeys.SHORTCUT_META_C
            event.keyCode == KeyEvent.KEYCODE_V && metaPressed ->
                ConfigKeys.SHORTCUT_META_V
            event.keyCode == KeyEvent.KEYCODE_X && metaPressed ->
                ConfigKeys.SHORTCUT_META_X
            event.keyCode == KeyEvent.KEYCODE_DPAD_LEFT && metaPressed ->
                ConfigKeys.SHORTCUT_META_LEFT
            event.keyCode == KeyEvent.KEYCODE_DPAD_RIGHT && metaPressed ->
                ConfigKeys.SHORTCUT_META_RIGHT
            event.keyCode == KeyEvent.KEYCODE_DPAD_UP && metaPressed ->
                ConfigKeys.SHORTCUT_META_UP
            event.keyCode == KeyEvent.KEYCODE_DPAD_DOWN && metaPressed ->
                ConfigKeys.SHORTCUT_META_DOWN
            else -> null
        }
    }

    private fun isPhysicalKeyboardEvent(event: KeyEvent): Boolean =
        event.deviceId >= 0 &&
            event.source and InputDevice.SOURCE_KEYBOARD == InputDevice.SOURCE_KEYBOARD

    private fun lookupForegroundPackage(): String? = try {
        val activityTaskManager = XposedHelpers.findClass(
            "android.app.ActivityTaskManager",
            null,
        )
        val service = XposedHelpers.callStaticMethod(
            activityTaskManager,
            "getService",
        ) ?: return null
        val taskInfo = XposedHelpers.callMethod(
            service,
            "getFocusedRootTaskInfo",
        ) ?: return null
        (
            XposedHelpers.getObjectField(taskInfo, "topActivity") as? ComponentName
            )?.packageName
    } catch (_: Throwable) {
        null
    }

    private fun metaStateName(metaState: Int): String {
        val names = buildList {
            if (metaState and KeyEvent.META_META_ON != 0) add("META_META_ON")
            if (metaState and KeyEvent.META_META_LEFT_ON != 0) add("META_META_LEFT_ON")
            if (metaState and KeyEvent.META_META_RIGHT_ON != 0) add("META_META_RIGHT_ON")
            if (metaState and KeyEvent.META_ALT_ON != 0) add("META_ALT_ON")
            if (metaState and KeyEvent.META_ALT_LEFT_ON != 0) add("META_ALT_LEFT_ON")
            if (metaState and KeyEvent.META_ALT_RIGHT_ON != 0) add("META_ALT_RIGHT_ON")
        }
        return if (names.isEmpty()) "0" else names.joinToString("|")
    }

    private fun signature(method: Method): String =
        "${method.returnType.name} ${method.name}(" +
            method.parameterTypes.joinToString { it.name } + ")"

    private fun logHookException(methodName: String, error: Throwable) {
        logHookException(TARGET_CLASS, methodName, error)
    }

    private fun logHookException(
        className: String,
        methodName: String,
        error: Throwable,
    ) {
        HookLog.i(
            "HOOK_EXCEPTION class=$className method=$methodName " +
                "exception=${error.javaClass.name}: ${error.message}",
        )
    }

    private data class ExactContext(
        val event: KeyEvent,
        val masterEnabled: Boolean,
        val foregroundPackage: String?,
        val whitelisted: Boolean,
        val shortcutEnabled: Boolean,
        val takeoverEnabled: Boolean,
        val metaModeEnabled: Boolean,
        val altTabModeEnabled: Boolean,
    )

    private val DISABLE_RESULT_KEYS = setOf(
        KeyEvent.KEYCODE_D,
        KeyEvent.KEYCODE_TAB,
    )

    private const val EXTRA_CONTEXT = "hyperos_keyboard_fix_exact_context"
    private const val EXTRA_LOG_EXACT_PATH = "hyperos_keyboard_fix_exact_path"
}
