package io.github.hmqyhm.hyperoskeyboardfix.hook

import android.content.Context
import android.database.ContentObserver
import android.os.Bundle
import android.os.DeadObjectException
import android.os.Handler
import android.os.HandlerThread
import android.os.RemoteException
import android.os.SystemClock
import io.github.hmqyhm.hyperoskeyboardfix.config.ConfigContract
import io.github.hmqyhm.hyperoskeyboardfix.config.ConfigKeys
import io.github.hmqyhm.hyperoskeyboardfix.utils.HookLog
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference

object ConfigProviderClient {
    data class ConfigSnapshot(
        val version: Long,
        val masterEnabled: Boolean,
        val whitelist: Set<String>,
        val allShortcutsEnabled: Boolean,
        val lastRefreshTime: Long,
    ) {
        companion object {
            val SAFE_DEFAULT = ConfigSnapshot(
                version = 0L,
                masterEnabled = false,
                whitelist = emptySet(),
                allShortcutsEnabled = false,
                lastRefreshTime = 0L,
            )
        }
    }

    private val initialized = AtomicBoolean(false)
    private val connected = AtomicBoolean(false)
    private val observerRegistered = AtomicBoolean(false)
    private val reconnectScheduled = AtomicBoolean(false)
    private val cache = AtomicReference(ConfigSnapshot.SAFE_DEFAULT)
    private lateinit var applicationContext: Context
    private lateinit var workerThread: HandlerThread
    private lateinit var worker: Handler

    fun initialize(context: Context) {
        if (!initialized.compareAndSet(false, true)) return
        applicationContext = context.applicationContext ?: context
        HookLog.i("CONFIG_PROVIDER_INIT")
        workerThread = HandlerThread("HyperOSKeyboardFix-Config").apply { start() }
        worker = Handler(workerThread.looper)
        worker.post {
            connectAndLoad()
            registerObserver()
            scheduleVersionCheck()
        }
    }

    fun snapshot(): ConfigSnapshot = cache.get()

    private fun connectAndLoad() {
        try {
            val ping = call(ConfigContract.METHOD_PING)
            if (!ping.getBoolean(ConfigContract.BUNDLE_AVAILABLE, false)) {
                throw IllegalStateException("Provider ping returned unavailable")
            }
            markConnected(ping.getString(ConfigContract.BUNDLE_AUTHORITY))
            refreshConfig("initial_load")
        } catch (error: Throwable) {
            logReadFailure("provider_ping", error)
            scheduleReconnect()
        }
    }

    private fun scheduleReconnect() {
        if (!reconnectScheduled.compareAndSet(false, true)) return
        worker.postDelayed(
            {
                reconnectScheduled.set(false)
                if (!connected.get()) {
                    connectAndLoad()
                    registerObserver()
                }
            },
            INITIAL_RECONNECT_DELAY_MS,
        )
    }

    private fun refreshConfig(stage: String) {
        try {
            val bundle = call(ConfigContract.METHOD_GET_CONFIG)
            logProviderResponse(bundle)
            HookLog.i(
                "CONFIG_BUNDLE_KEYS keys=${bundle.keySet().sorted().joinToString(",")}",
            )
            val cacheVersion = cache.get().version
            val providerVersion = bundle.getLong(
                ConfigContract.BUNDLE_CONFIG_VERSION,
                -1L,
            )
            HookLog.i(
                "CONFIG_CACHE_COMPARE stage=$stage " +
                    "cacheVersion=$cacheVersion providerVersion=$providerVersion",
            )
            val next = parseConfig(bundle)
            compareProviderAndSnapshot(bundle, next)
            cache.set(next)
            HookLog.i(
                "CONFIG_LOADED " +
                    "version=${next.version} " +
                    "masterEnabled=${next.masterEnabled} " +
                    "whitelistSize=${next.whitelist.size} " +
                    "allShortcutsEnabled=${next.allShortcutsEnabled}",
            )
            logSnapshot(next, stage)
        } catch (error: Throwable) {
            logReadFailure(stage, error)
        }
    }

    private fun parseConfig(bundle: Bundle): ConfigSnapshot {
        require(bundle.containsKey(ConfigContract.BUNDLE_CONFIG_VERSION)) {
            "Missing configVersion"
        }
        require(bundle.containsKey(ConfigContract.BUNDLE_MASTER_ENABLED)) {
            "Missing masterEnabled"
        }
        require(bundle.containsKey(ConfigContract.BUNDLE_WHITELIST)) {
            "Missing whitelist"
        }
        val whitelist = bundle.getStringArrayList(ConfigContract.BUNDLE_WHITELIST)
            ?: throw IllegalStateException("Whitelist has invalid type")
        return ConfigSnapshot(
            version = bundle.getLong(ConfigContract.BUNDLE_CONFIG_VERSION),
            masterEnabled = bundle.getBoolean(ConfigContract.BUNDLE_MASTER_ENABLED),
            whitelist = whitelist.toSet(),
            allShortcutsEnabled = bundle.getBoolean(
                ConfigKeys.ALL_SHORTCUTS_ENABLED,
                false,
            ),
            lastRefreshTime = SystemClock.elapsedRealtime(),
        )
    }

    private fun registerObserver() {
        if (observerRegistered.get()) return
        try {
            applicationContext.contentResolver.registerContentObserver(
                ConfigContract.changedUri,
                false,
                object : ContentObserver(worker) {
                    override fun onChange(selfChange: Boolean) {
                        HookLog.i("CONFIG_CHANGED_NOTIFICATION")
                        worker.post { refreshConfig("change_notification") }
                    }
                },
            )
            observerRegistered.set(true)
        } catch (error: Throwable) {
            logReadFailure("observer_register", error)
        }
    }

    private fun scheduleVersionCheck() {
        worker.postDelayed(
            object : Runnable {
                override fun run() {
                    try {
                        if (!connected.get()) {
                            connectAndLoad()
                            registerObserver()
                            return
                        }
                        val version = call(ConfigContract.METHOD_GET_VERSION)
                            .getLong(ConfigContract.BUNDLE_CONFIG_VERSION, -1L)
                        val cacheVersion = cache.get().version
                        HookLog.i(
                            "CONFIG_CACHE_COMPARE stage=version_check " +
                                "cacheVersion=$cacheVersion providerVersion=$version",
                        )
                        if (version >= 0L && version != cacheVersion) {
                            refreshConfig("version_fallback")
                        }
                    } catch (error: Throwable) {
                        connected.set(false)
                        logReadFailure("version_check", error)
                    } finally {
                        worker.postDelayed(this, VERSION_CHECK_INTERVAL_MS)
                    }
                }
            },
            VERSION_CHECK_INTERVAL_MS,
        )
    }

    private fun call(method: String): Bundle =
        applicationContext.contentResolver.call(
            ConfigContract.baseUri,
            method,
            null,
            null,
        ) ?: throw IllegalStateException("Provider returned null for $method")

    private fun markConnected(authority: String?) {
        if (connected.compareAndSet(false, true)) {
            HookLog.i(
                "CONFIG_PROVIDER_CONNECTED\n" +
                    "authority=${authority ?: ConfigContract.authority}",
            )
        }
    }

    private fun logSnapshot(snapshot: ConfigSnapshot, stage: String) {
        val whitelist = snapshot.whitelist
            .sorted()
            .joinToString(prefix = "[", postfix = "]")
        HookLog.i(
            "CONFIG_SNAPSHOT stage=$stage " +
                "version=${snapshot.version} " +
                "masterEnabled=${snapshot.masterEnabled} " +
                "whitelistSize=${snapshot.whitelist.size} " +
                "allShortcutsEnabled=${snapshot.allShortcutsEnabled} " +
                "whitelist=$whitelist",
        )
    }

    private fun logProviderResponse(bundle: Bundle) {
        val whitelist = bundle
            .getStringArrayList(ConfigContract.BUNDLE_WHITELIST)
            .orEmpty()
            .sorted()
        HookLog.i(
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
            HookLog.i("WHITELIST_CONTENT [$index]=$packageName")
        }
    }

    private fun compareProviderAndSnapshot(
        bundle: Bundle,
        snapshot: ConfigSnapshot,
    ) {
        compareField(
            field = ConfigContract.BUNDLE_CONFIG_VERSION,
            providerValue = bundle.getLong(ConfigContract.BUNDLE_CONFIG_VERSION, -1L),
            snapshotValue = snapshot.version,
        )
        compareField(
            field = ConfigContract.BUNDLE_MASTER_ENABLED,
            providerValue = bundle.getBoolean(
                ConfigContract.BUNDLE_MASTER_ENABLED,
                false,
            ),
            snapshotValue = snapshot.masterEnabled,
        )
        compareField(
            field = ConfigContract.BUNDLE_WHITELIST,
            providerValue = bundle
                .getStringArrayList(ConfigContract.BUNDLE_WHITELIST)
                .orEmpty()
                .toSet(),
            snapshotValue = snapshot.whitelist,
        )
        compareField(
            field = ConfigKeys.ALL_SHORTCUTS_ENABLED,
            providerValue = bundle.getBoolean(
                ConfigKeys.ALL_SHORTCUTS_ENABLED,
                false,
            ),
            snapshotValue = snapshot.allShortcutsEnabled,
        )
    }

    private fun compareField(
        field: String,
        providerValue: Any,
        snapshotValue: Any,
    ) {
        HookLog.i(
            "CONFIG_COMPARE field=$field " +
                "providerValue=$providerValue snapshotValue=$snapshotValue",
        )
        if (providerValue != snapshotValue) {
            HookLog.i(
                "CONFIG_MISMATCH field=$field " +
                    "providerValue=$providerValue snapshotValue=$snapshotValue",
            )
        }
    }

    private fun logReadFailure(stage: String, error: Throwable) {
        val exception = when (error) {
            is DeadObjectException -> "DeadObjectException"
            is RemoteException -> "RemoteException"
            is SecurityException -> "SecurityException"
            is IllegalArgumentException -> "IllegalArgumentException"
            else -> error.javaClass.simpleName
        }
        HookLog.i(
            "CONFIG_READ_FAILED\n" +
                "stage=$stage\n" +
                "exception=$exception: ${error.message}",
        )
    }

    private const val VERSION_CHECK_INTERVAL_MS = 60_000L
    private const val INITIAL_RECONNECT_DELAY_MS = 5_000L
}
