package io.github.hmqyhm.hyperoskeyboardfix.config

import android.content.Context

data class ProviderTestResult(
    val connected: Boolean,
    val bundleComplete: Boolean,
    val version: Long?,
    val message: String,
)

object ProviderDiagnostics {
    fun test(context: Context): ProviderTestResult = try {
        val resolver = context.contentResolver
        val ping = resolver.call(
            ConfigContract.baseUri,
            ConfigContract.METHOD_PING,
            null,
            null,
        )
        val connected = ping?.getBoolean(ConfigContract.BUNDLE_AVAILABLE, false) == true
        val config = resolver.call(
            ConfigContract.baseUri,
            ConfigContract.METHOD_GET_CONFIG,
            null,
            null,
        )
        val complete = config != null &&
            config.containsKey(ConfigContract.BUNDLE_CONFIG_VERSION) &&
            config.containsKey(ConfigContract.BUNDLE_MASTER_ENABLED) &&
            config.containsKey(ConfigContract.BUNDLE_WHITELIST) &&
            config.containsKey(ConfigKeys.ALL_SHORTCUTS_ENABLED)
        val version = if (complete) {
            config?.getLong(ConfigContract.BUNDLE_CONFIG_VERSION)
        } else {
            null
        }
        ProviderTestResult(
            connected = connected,
            bundleComplete = complete,
            version = version,
            message = if (connected && complete) {
                "Connected; configuration bundle is complete; version=$version"
            } else {
                "Connection failed or configuration bundle is incomplete"
            },
        )
    } catch (error: Throwable) {
        ProviderTestResult(
            connected = false,
            bundleComplete = false,
            version = null,
            message = "Test failed: ${error.javaClass.simpleName}: ${error.message}",
        )
    }
}
