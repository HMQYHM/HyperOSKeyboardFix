package io.github.hmqyhm.hyperoskeyboardfix.hook

import android.content.ComponentName
import android.content.Context
import android.os.Handler
import android.os.HandlerThread
import android.provider.Settings
import io.github.hmqyhm.hyperoskeyboardfix.utils.HookLog
import io.github.hmqyhm.hyperoskeyboardfix.config.ConfigContract
import de.robv.android.xposed.XposedHelpers

object AutoImeController {
    private var previousIme: String? = null
    private var switchedByModule = false
    private var switchAttemptedForSession = false
    private var lastAppliedIme: String? = null
    private var manuallyChangedAfterSwitch = false
    private var imeSessionPackage: String? = null
    @Volatile
    private var lastForegroundPackage: String? = null

    fun currentForegroundPackage(): String? = lastForegroundPackage

    fun initialize(context: Context) {
        val appContext = context.applicationContext ?: context
        val worker = Handler(
            HandlerThread("HyperOSKeyboardFix-IME").apply { start() }.looper,
        )
        worker.post(object : Runnable {
            override fun run() {
                try {
                    update(appContext)
                } catch (error: Throwable) {
                    HookLog.i(
                        "ERROR auto_ime exception=${error.javaClass.simpleName}: ${error.message}",
                    )
                } finally {
                    worker.postDelayed(this, CHECK_INTERVAL_MS)
                }
            }
        })
    }

    private fun update(context: Context) {
        val config = ConfigProviderClient.snapshot()
        val foreground = foregroundPackage()
        if (
            foreground != lastForegroundPackage &&
            config.masterEnabled && foreground in config.whitelist &&
            config.notificationsEnabled && config.notifyAppOpen
        ) {
            ConfigProviderClient.notifyEvent(ConfigContract.EVENT_APP_OPEN)
        }
        lastForegroundPackage = foreground
        val shouldSwitch = config.masterEnabled && config.autoImeEnabled &&
            config.autoImeId.isNotBlank() && foreground in config.whitelist
        val current = Settings.Secure.getString(
            context.contentResolver,
            Settings.Secure.DEFAULT_INPUT_METHOD,
        ).orEmpty()
        if (shouldSwitch) {
            if (imeSessionPackage != foreground) {
                imeSessionPackage = foreground
                switchAttemptedForSession = false
                manuallyChangedAfterSwitch = false
                lastAppliedIme = null
            }
            if (!isEnabled(context, config.autoImeId)) return
            if (!switchedByModule) previousIme = current
            if (
                config.autoImeSwitchOnce && switchAttemptedForSession &&
                lastAppliedIme != null && current != lastAppliedIme
            ) {
                manuallyChangedAfterSwitch = true
            }
            if (config.autoImeSwitchOnce && switchAttemptedForSession) return
            if (current != config.autoImeId && setIme(context, config.autoImeId)) {
                HookLog.i("IME_SWITCHED package=$foreground")
                if (config.notificationsEnabled && config.notifyAutoIme) {
                    ConfigProviderClient.notifyEvent(ConfigContract.EVENT_AUTO_IME)
                }
            }
            switchedByModule = true
            switchAttemptedForSession = true
            lastAppliedIme = config.autoImeId
        } else if (switchedByModule) {
            val restore = previousIme.orEmpty()
            if (
                !manuallyChangedAfterSwitch && restore.isNotBlank() &&
                isEnabled(context, restore)
            ) {
                setIme(context, restore)
                HookLog.i("IME_RESTORED")
            }
            previousIme = null
            switchedByModule = false
            switchAttemptedForSession = false
            manuallyChangedAfterSwitch = false
            lastAppliedIme = null
            imeSessionPackage = null
        }
    }

    private fun isEnabled(context: Context, id: String): Boolean {
        val enabled = Settings.Secure.getString(
            context.contentResolver,
            Settings.Secure.ENABLED_INPUT_METHODS,
        ).orEmpty()
        return enabled.split(':').any { it.substringBefore(';') == id }
    }

    private fun setIme(context: Context, id: String): Boolean {
        try {
            val localServices = XposedHelpers.findClass(
                "com.android.server.LocalServices",
                null,
            )
            val internalClass = XposedHelpers.findClass(
                "com.android.server.inputmethod.InputMethodManagerInternal",
                null,
            )
            val service = XposedHelpers.callStaticMethod(
                localServices,
                "getService",
                internalClass,
            )
            if (service != null) {
                val result = XposedHelpers.callMethod(
                    service,
                    "switchToInputMethod",
                    id,
                    currentUserId(),
                )
                if (result !is Boolean || result) return true
            }
        } catch (_: Throwable) {
            // HyperOS builds may omit the internal switch method; use Settings fallback.
        }
        return Settings.Secure.putString(
            context.contentResolver,
            Settings.Secure.DEFAULT_INPUT_METHOD,
            id,
        )
    }

    private fun currentUserId(): Int = try {
        val activityManager = XposedHelpers.findClass("android.app.ActivityManager", null)
        XposedHelpers.callStaticMethod(activityManager, "getCurrentUser") as Int
    } catch (_: Throwable) {
        0
    }

    private fun foregroundPackage(): String? = try {
        val activityTaskManager = XposedHelpers.findClass("android.app.ActivityTaskManager", null)
        val service = XposedHelpers.callStaticMethod(activityTaskManager, "getService") ?: return null
        val taskInfo = XposedHelpers.callMethod(service, "getFocusedRootTaskInfo") ?: return null
        (XposedHelpers.getObjectField(taskInfo, "topActivity") as? ComponentName)?.packageName
    } catch (_: Throwable) {
        null
    }

    private const val CHECK_INTERVAL_MS = 1_000L
}
