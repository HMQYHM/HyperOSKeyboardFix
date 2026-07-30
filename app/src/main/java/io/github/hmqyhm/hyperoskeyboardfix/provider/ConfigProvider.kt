package io.github.hmqyhm.hyperoskeyboardfix.provider

import android.content.ContentProvider
import android.content.ContentValues
import android.database.Cursor
import android.net.Uri
import android.os.Binder
import android.os.Bundle
import android.os.Process
import android.util.Log
import io.github.hmqyhm.hyperoskeyboardfix.config.ConfigContract
import io.github.hmqyhm.hyperoskeyboardfix.config.ConfigKeys
import io.github.hmqyhm.hyperoskeyboardfix.config.ModulePreferences
import java.util.concurrent.atomic.AtomicBoolean

class ConfigProvider : ContentProvider() {
    private lateinit var preferences: ModulePreferences

    override fun onCreate(): Boolean {
        val providerContext = context ?: return false
        preferences = ModulePreferences(providerContext)
        return true
    }

    override fun call(method: String, arg: String?, extras: Bundle?): Bundle {
        enforceAllowedCaller()
        return when (method) {
            ConfigContract.METHOD_PING -> Bundle().apply {
                putBoolean(ConfigContract.BUNDLE_AVAILABLE, true)
                putString(ConfigContract.BUNDLE_AUTHORITY, ConfigContract.authority)
            }

            ConfigContract.METHOD_GET_VERSION -> Bundle().apply {
                putLong(
                    ConfigContract.BUNDLE_CONFIG_VERSION,
                    preferences.configVersion(),
                )
            }

            ConfigContract.METHOD_GET_CONFIG -> buildConfigBundle()
            else -> throw IllegalArgumentException("Unsupported provider method: $method")
        }
    }

    private fun buildConfigBundle(): Bundle {
        val bundle = Bundle().apply {
            putLong(ConfigContract.BUNDLE_CONFIG_VERSION, preferences.configVersion())
            putBoolean(
                ConfigContract.BUNDLE_MASTER_ENABLED,
                preferences.isMasterEnabled(),
            )
            putStringArrayList(
                ConfigContract.BUNDLE_WHITELIST,
                ArrayList(preferences.whitelist()),
            )
            putBoolean(
                ConfigKeys.ALL_SHORTCUTS_ENABLED,
                preferences.areAllShortcutsEnabled(),
            )
        }
        logProviderResponse(bundle)
        return bundle
    }

    private fun logProviderResponse(bundle: Bundle) {
        try {
            val whitelist = bundle
                .getStringArrayList(ConfigContract.BUNDLE_WHITELIST)
                .orEmpty()
                .sorted()
            Log.i(
                TAG,
                "CONFIG_PROVIDER_RESPONSE " +
                    "version=${bundle.getLong(ConfigContract.BUNDLE_CONFIG_VERSION, -1L)} " +
                    "masterEnabled=${bundle.getBoolean(
                        ConfigContract.BUNDLE_MASTER_ENABLED,
                        false,
                    )} " +
                    "whitelistSize=${whitelist.size} " +
                    "allShortcutsEnabled=${bundle.getBoolean(
                        ConfigKeys.ALL_SHORTCUTS_ENABLED,
                        false,
                    )} " +
                    "bundleKeys=${bundle.keySet().sorted()}",
            )
            whitelist.forEachIndexed { index, packageName ->
                Log.i(TAG, "WHITELIST_CONTENT [$index]=$packageName")
            }
        } catch (error: Throwable) {
            Log.w(
                TAG,
                "CONFIG_PROVIDER_RESPONSE_FAILED exception=${error.javaClass.simpleName}",
            )
        }
    }

    private fun enforceAllowedCaller() {
        val providerContext = context ?: throw SecurityException("Provider unavailable")
        val callingUid = Binder.getCallingUid()
        val allowed = callingUid == Process.SYSTEM_UID ||
            callingUid == providerContext.applicationInfo.uid ||
            providerContext.packageManager.getPackagesForUid(callingUid)
                .orEmpty()
                .contains(SYSTEM_UI_PACKAGE)
        if (allowed) return

        if (securityRejectionLogged.compareAndSet(false, true)) {
            Log.w(TAG, "Rejected configuration caller uid=$callingUid")
        }
        throw SecurityException("UID $callingUid is not allowed to read module configuration")
    }

    override fun getType(uri: Uri): String? = null

    override fun query(
        uri: Uri,
        projection: Array<out String>?,
        selection: String?,
        selectionArgs: Array<out String>?,
        sortOrder: String?,
    ): Cursor? = throw UnsupportedOperationException("Read configuration with call()")

    override fun insert(uri: Uri, values: ContentValues?): Uri? =
        throw UnsupportedOperationException("Configuration provider is read-only")

    override fun delete(
        uri: Uri,
        selection: String?,
        selectionArgs: Array<out String>?,
    ): Int = throw UnsupportedOperationException("Configuration provider is read-only")

    override fun update(
        uri: Uri,
        values: ContentValues?,
        selection: String?,
        selectionArgs: Array<out String>?,
    ): Int = throw UnsupportedOperationException("Configuration provider is read-only")

    private companion object {
        const val TAG = "HyperOSKeyboardFix"
        const val SYSTEM_UI_PACKAGE = "com.android.systemui"
        val securityRejectionLogged = AtomicBoolean(false)
    }
}
