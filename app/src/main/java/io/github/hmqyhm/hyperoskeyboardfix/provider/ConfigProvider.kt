package io.github.hmqyhm.hyperoskeyboardfix.provider

import android.content.ContentProvider
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.ContentValues
import android.database.Cursor
import android.net.Uri
import android.os.Binder
import android.os.Bundle
import android.os.Process
import android.os.UserManager
import android.os.Handler
import android.os.Looper
import android.util.Log
import io.github.hmqyhm.hyperoskeyboardfix.config.ConfigContract
import io.github.hmqyhm.hyperoskeyboardfix.config.ConfigKeys
import io.github.hmqyhm.hyperoskeyboardfix.config.ModulePreferences
import io.github.hmqyhm.hyperoskeyboardfix.R
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference

class ConfigProvider : ContentProvider() {
    @Volatile
    private var preferences: ModulePreferences? = null
    private val publishedConfig = AtomicReference<Bundle?>(null)

    override fun onCreate(): Boolean {
        val providerContext = context ?: return false
        publishedConfig.set(loadPersistedSnapshot(providerContext))
        return true
    }

    override fun call(method: String, arg: String?, extras: Bundle?): Bundle {
        if (method == ConfigContract.METHOD_PUBLISH_CONFIG) {
            enforceSelfCaller()
            val snapshot = extras?.let(::Bundle)
                ?: throw IllegalArgumentException("Missing configuration snapshot")
            persistSnapshot(snapshot)
            publishedConfig.set(snapshot)
            context?.contentResolver?.notifyChange(ConfigContract.changedUri, null)
            return Bundle().apply {
                putBoolean(ConfigContract.BUNDLE_AVAILABLE, true)
                putLong(
                    ConfigContract.BUNDLE_CONFIG_VERSION,
                    snapshot.getLong(ConfigContract.BUNDLE_CONFIG_VERSION, -1L),
                )
            }
        }
        enforceAllowedCaller()
        return when (method) {
            ConfigContract.METHOD_PING -> Bundle().apply {
                putBoolean(ConfigContract.BUNDLE_AVAILABLE, true)
                putString(ConfigContract.BUNDLE_AUTHORITY, ConfigContract.authority)
            }

            ConfigContract.METHOD_GET_VERSION -> Bundle().apply {
                putLong(
                    ConfigContract.BUNDLE_CONFIG_VERSION,
                    currentConfig().getLong(ConfigContract.BUNDLE_CONFIG_VERSION, 0L),
                )
            }

            ConfigContract.METHOD_GET_CONFIG -> currentConfig().also(::logProviderResponse)
            ConfigContract.METHOD_NOTIFY_EVENT -> {
                showTemporaryNotification(arg.orEmpty())
                Bundle().apply { putBoolean(ConfigContract.BUNDLE_AVAILABLE, true) }
            }
            else -> throw IllegalArgumentException("Unsupported provider method: $method")
        }
    }

    private fun currentConfig(): Bundle = Bundle(
        publishedConfig.get() ?: preferences().configBundle(),
    )

    private fun persistSnapshot(snapshot: Bundle) {
        val providerContext = context ?: return
        val editor = providerContext.getSharedPreferences(
            SNAPSHOT_FILE_NAME,
            android.content.Context.MODE_PRIVATE,
        ).edit().clear()
        snapshot.keySet().forEach { key ->
            when (val value = snapshot.get(key)) {
                is Boolean -> editor.putBoolean(key, value)
                is Long -> editor.putLong(key, value)
                is Int -> editor.putInt(key, value)
                is String -> editor.putString(key, value)
                is ArrayList<*> -> editor.putStringSet(
                    key,
                    value.filterIsInstance<String>().toSet(),
                )
            }
        }
        if (!editor.commit()) {
            throw IllegalStateException("Unable to persist configuration snapshot")
        }
    }

    private fun loadPersistedSnapshot(providerContext: android.content.Context): Bundle? {
        val stored = providerContext.getSharedPreferences(
            SNAPSHOT_FILE_NAME,
            android.content.Context.MODE_PRIVATE,
        )
        if (!stored.contains(ConfigContract.BUNDLE_CONFIG_VERSION)) return null
        return Bundle().apply {
            stored.all.forEach { (key, value) ->
                when (value) {
                    is Boolean -> putBoolean(key, value)
                    is Long -> putLong(key, value)
                    is Int -> putInt(key, value)
                    is String -> putString(key, value)
                    is Set<*> -> putStringArrayList(
                        key,
                        ArrayList(value.filterIsInstance<String>()),
                    )
                }
            }
        }
    }

    private fun showTemporaryNotification(event: String) {
        val config = currentConfig()
        if (
            event != ConfigContract.EVENT_TEST &&
            !config.getBoolean(ConfigKeys.NOTIFICATIONS_ENABLED, true)
        ) return
        val allowed = when (event) {
            ConfigContract.EVENT_APP_OPEN -> config.getBoolean(ConfigKeys.NOTIFY_APP_OPEN, true)
            ConfigContract.EVENT_AUTO_IME -> config.getBoolean(ConfigKeys.NOTIFY_AUTO_IME, true)
            ConfigContract.EVENT_MACRO -> config.getBoolean(ConfigKeys.NOTIFY_MACRO, true)
            ConfigContract.EVENT_TEST -> true
            else -> false
        }
        if (!allowed) return
        val providerContext = context ?: return
        val manager = providerContext.getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(
            NotificationChannel(
                NOTIFICATION_CHANNEL_ID,
                providerContext.getString(R.string.notification_channel_name),
                NotificationManager.IMPORTANCE_LOW,
            ),
        )
        val message = providerContext.getString(
            when (event) {
                ConfigContract.EVENT_APP_OPEN -> R.string.notification_app_open_message
                ConfigContract.EVENT_AUTO_IME -> R.string.notification_auto_ime_message
                ConfigContract.EVENT_TEST -> R.string.notification_test_message
                else -> R.string.notification_macro_message
            },
        )
        val title = config.getString(ConfigKeys.NOTIFICATION_TITLE).orEmpty().ifBlank {
            providerContext.getString(R.string.app_name)
        }
        val id = event.hashCode()
        val autoDismiss = config.getBoolean(ConfigKeys.NOTIFICATION_AUTO_DISMISS, true)
        val notificationBuilder = Notification.Builder(providerContext, NOTIFICATION_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(title)
            .setContentText(message)
            .setAutoCancel(true)
        if (autoDismiss) {
            notificationBuilder.setTimeoutAfter(NOTIFICATION_TIMEOUT_MS)
        }
        manager.notify(
            id,
            notificationBuilder.build(),
        )
        if (autoDismiss) {
            Handler(Looper.getMainLooper()).postDelayed(
                { manager.cancel(id) },
                NOTIFICATION_TIMEOUT_MS,
            )
        }
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

    private fun enforceSelfCaller() {
        val providerContext = context ?: throw SecurityException("Provider unavailable")
        val callingUid = Binder.getCallingUid()
        if (callingUid != providerContext.applicationInfo.uid) {
            throw SecurityException("Only the module app may publish configuration")
        }
    }

    private fun preferences(): ModulePreferences {
        preferences?.let { return it }
        val providerContext = context ?: throw IllegalStateException("Provider unavailable")
        val userManager = providerContext.getSystemService(UserManager::class.java)
        if (userManager?.isUserUnlocked != true) {
            throw IllegalStateException("User storage is locked")
        }
        return synchronized(this) {
            preferences ?: ModulePreferences(
                providerContext,
                publishChanges = false,
            ).also { preferences = it }
        }
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
        const val NOTIFICATION_CHANNEL_ID = "keyboard_activity"
        const val NOTIFICATION_TIMEOUT_MS = 3_000L
        const val SNAPSHOT_FILE_NAME = "provider_config_snapshot"
    }
}
