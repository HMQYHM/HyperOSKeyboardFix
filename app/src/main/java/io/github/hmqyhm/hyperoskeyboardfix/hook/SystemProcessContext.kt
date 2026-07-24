package io.github.hmqyhm.hyperoskeyboardfix.hook

import android.content.Context
import de.robv.android.xposed.XposedHelpers

object SystemProcessContext {
    fun obtain(): Context? {
        return try {
            val activityThreadClass = XposedHelpers.findClass(
                "android.app.ActivityThread",
                null,
            )
            val application = XposedHelpers.callStaticMethod(
                activityThreadClass,
                "currentApplication",
            ) as? Context
            if (application != null) {
                return application.applicationContext ?: application
            }
            val activityThread = XposedHelpers.callStaticMethod(
                activityThreadClass,
                "currentActivityThread",
            ) ?: return null
            XposedHelpers.callMethod(activityThread, "getSystemContext") as? Context
        } catch (_: Throwable) {
            null
        }
    }
}
