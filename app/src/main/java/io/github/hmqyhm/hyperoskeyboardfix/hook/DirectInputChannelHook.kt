package io.github.hmqyhm.hyperoskeyboardfix.hook

import android.os.IBinder
import android.view.InputDevice
import android.view.KeyEvent
import io.github.hmqyhm.hyperoskeyboardfix.utils.HookLog
import io.github.hmqyhm.hyperoskeyboardfix.config.ConfigKeys
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedHelpers

/** Passes physical keyboard input around HyperOS policy only for allowlisted apps. */
object DirectInputChannelHook {
    private const val CALLBACK_CLASS = "com.android.server.wm.InputManagerCallback"
    private const val ACTION_PASS_TO_USER = 1

    fun install(classLoader: ClassLoader) {
        try {
            val callbackClass = XposedHelpers.findClass(CALLBACK_CLASS, classLoader)
            XposedHelpers.findAndHookMethod(
                callbackClass,
                "interceptKeyBeforeQueueing",
                KeyEvent::class.java,
                Int::class.javaPrimitiveType,
                object : XC_MethodHook() {
                    override fun beforeHookedMethod(param: MethodHookParam) {
                        try {
                            val event = param.args.getOrNull(0) as? KeyEvent ?: return
                            if (MacroExecutor.consumeInjectedEvent(event)) {
                                param.result = ACTION_PASS_TO_USER
                                return
                            }
                            val channel = directChannelContext(event) ?: return
                            if (
                                MacroExecutor.handle(
                                    event,
                                    channel.foregroundPackage,
                                    channel.config.keyboardMacros,
                                )
                            ) {
                                param.result = 0
                                return
                            }

                            // Skip HyperOS policy side effects while keeping native dispatch alive.
                            param.result = ACTION_PASS_TO_USER
                        } catch (error: Throwable) {
                            HookLog.i(
                                "ERROR direct_channel exception=${error.javaClass.simpleName}: " +
                                    error.message,
                            )
                        }
                    }
                },
            )
            XposedHelpers.findAndHookMethod(
                callbackClass,
                "interceptKeyBeforeDispatching",
                IBinder::class.java,
                KeyEvent::class.java,
                Int::class.javaPrimitiveType,
                object : XC_MethodHook() {
                    override fun beforeHookedMethod(param: MethodHookParam) {
                        try {
                            val event = param.args.getOrNull(1) as? KeyEvent ?: return
                            if (MacroExecutor.consumeInjectedEvent(event)) {
                                param.result = 0L
                                return
                            }
                            if (directChannelContext(event) == null) return
                            param.result = 0L
                        } catch (error: Throwable) {
                            HookLog.i(
                                "ERROR direct_channel exception=${error.javaClass.simpleName}: " +
                                    error.message,
                            )
                        }
                    }
                },
            )
            HookLog.i(
                "EXACT_HOOK_INSTALLED class=$CALLBACK_CLASS " +
                    "method=directInputChannel " +
                    "signature=[interceptKeyBeforeQueueing,interceptKeyBeforeDispatching] " +
                    "mode=direct_channel",
            )
        } catch (error: Throwable) {
            HookLog.i(
                "EXACT_HOOK_SKIPPED class=$CALLBACK_CLASS " +
                    "method=interceptKeyBeforeQueueing " +
                    "reason=${error.javaClass.simpleName}",
            )
        }
    }

    private fun isPhysicalKeyboard(event: KeyEvent): Boolean =
        event.deviceId >= 0 &&
            event.source and InputDevice.SOURCE_KEYBOARD == InputDevice.SOURCE_KEYBOARD

    private fun directChannelContext(event: KeyEvent): ChannelContext? {
        if (!isPhysicalKeyboard(event)) return null
        val config = ConfigProviderClient.snapshot()
        if (!config.masterEnabled || config.inputMode != ConfigKeys.INPUT_MODE_DIRECT) {
            return null
        }
        val foreground = AutoImeController.currentForegroundPackage() ?: return null
        if (foreground !in config.whitelist) return null
        return ChannelContext(config, foreground)
    }

    private data class ChannelContext(
        val config: ConfigProviderClient.ConfigSnapshot,
        val foregroundPackage: String,
    )
}
