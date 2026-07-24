package io.github.hmqyhm.hyperoskeyboardfix

import io.github.hmqyhm.hyperoskeyboardfix.hook.ConfigProviderClient
import io.github.hmqyhm.hyperoskeyboardfix.hook.ExactMiuiPolicyDiagnosticHook
import io.github.hmqyhm.hyperoskeyboardfix.hook.SystemProcessContext
import io.github.hmqyhm.hyperoskeyboardfix.utils.HookLog
import de.robv.android.xposed.IXposedHookLoadPackage
import de.robv.android.xposed.callbacks.XC_LoadPackage

class MainHook : IXposedHookLoadPackage {
    override fun handleLoadPackage(lpparam: XC_LoadPackage.LoadPackageParam) {
        if (lpparam.packageName != "android" &&
            lpparam.packageName != "com.android.systemui"
        ) {
            return
        }
        HookLog.i("MODULE_LOADED")
        try {
            val context = SystemProcessContext.obtain()
            if (context == null) {
                HookLog.i(
                    "CONFIG_READ_FAILED\n" +
                        "stage=context_acquisition\n" +
                        "exception=ContextUnavailable",
                )
            } else {
                ConfigProviderClient.initialize(context)
            }
            if (lpparam.packageName == "android") {
                ExactMiuiPolicyDiagnosticHook.install(lpparam.classLoader)
            }
        } catch (error: Throwable) {
            HookLog.e("Initialization failed", error)
        }
    }
}
