package io.github.hmqyhm.hyperoskeyboardfix.utils

import android.util.Log
import de.robv.android.xposed.XposedBridge

object HookLog {
    private const val TAG = "HyperOSKeyboardFix"

    fun i(message: String) {
        Log.i(TAG, message)
        XposedBridge.log("$TAG: $message")
    }

    fun e(message: String, error: Throwable) {
        val detail = "ERROR: $message: ${error.javaClass.simpleName}: ${error.message}"
        Log.e(TAG, detail)
        XposedBridge.log("$TAG: $detail")
    }
}
