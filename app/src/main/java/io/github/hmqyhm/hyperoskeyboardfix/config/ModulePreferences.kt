package io.github.hmqyhm.hyperoskeyboardfix.config

import android.content.Context
import android.content.SharedPreferences
import io.github.hmqyhm.hyperoskeyboardfix.model.ShortcutOption

class ModulePreferences(private val context: Context) {
    private val preferences: SharedPreferences =
        context.getSharedPreferences(ConfigKeys.FILE_NAME, Context.MODE_PRIVATE)

    init {
        migrateLegacyPreferences()
        initializeDefaultWhitelist()
        initializeShortcutDefaults()
        initializeLanguage()
        ensureConfigVersion()
    }

    fun isMasterEnabled(): Boolean =
        preferences.getBoolean(ConfigKeys.MASTER_ENABLED, false)

    fun setMasterEnabled(enabled: Boolean) {
        commitConfigurationChange { editor ->
            editor.putBoolean(ConfigKeys.MASTER_ENABLED, enabled)
        }
    }

    fun isShortcutEnabled(key: String): Boolean =
        preferences.getBoolean(key, true)

    fun setShortcutEnabled(key: String, enabled: Boolean) {
        val allEnabled = SHORTCUTS.all { option ->
            if (option.preferenceKey == key) enabled else isShortcutEnabled(option.preferenceKey)
        }
        commitConfigurationChange { editor ->
            editor
                .putBoolean(key, enabled)
                .putBoolean(ConfigKeys.ALL_SHORTCUTS_ENABLED, allEnabled)
        }
    }

    fun areAllShortcutsEnabled(): Boolean =
        preferences.getBoolean(ConfigKeys.ALL_SHORTCUTS_ENABLED, true)

    fun setAllShortcutsEnabled(enabled: Boolean) {
        commitConfigurationChange { editor ->
            editor.putBoolean(ConfigKeys.ALL_SHORTCUTS_ENABLED, enabled)
            SHORTCUTS.forEach { option -> editor.putBoolean(option.preferenceKey, enabled) }
        }
    }

    fun enabledShortcutCount(): Int =
        SHORTCUTS.count { isShortcutEnabled(it.preferenceKey) }

    fun whitelist(): Set<String> =
        preferences.getStringSet(ConfigKeys.REMOTE_APP_WHITELIST, emptySet())
            ?.toSet()
            .orEmpty()

    fun saveWhitelist(packages: Set<String>) {
        commitConfigurationChange { editor ->
            editor.putStringSet(ConfigKeys.REMOTE_APP_WHITELIST, packages.toSet())
        }
    }

    fun configVersion(): Long =
        preferences.getLong(ConfigKeys.CONFIG_VERSION, 0L)

    fun languageTag(): String =
        preferences.getString(
            ConfigKeys.UI_LANGUAGE,
            ConfigKeys.LANGUAGE_SIMPLIFIED_CHINESE,
        ) ?: ConfigKeys.LANGUAGE_SIMPLIFIED_CHINESE

    fun setLanguageTag(languageTag: String) {
        preferences.edit()
            .putString(ConfigKeys.UI_LANGUAGE, languageTag)
            .apply()
    }

    fun recordLaunchAndShouldShowStarPrompt(): Boolean {
        if (preferences.getBoolean(ConfigKeys.STAR_PROMPT_SHOWN, false)) {
            return false
        }
        val launchCount = preferences.getInt(ConfigKeys.APP_LAUNCH_COUNT, 0) + 1
        preferences.edit()
            .putInt(ConfigKeys.APP_LAUNCH_COUNT, launchCount)
            .apply()
        return launchCount == STAR_PROMPT_LAUNCH_COUNT
    }

    fun markStarPromptShown() {
        preferences.edit()
            .putBoolean(ConfigKeys.STAR_PROMPT_SHOWN, true)
            .apply()
    }

    private fun initializeDefaultWhitelist() {
        if (preferences.getBoolean(KEY_DEFAULTS_INITIALIZED, false)) return
        val installedDefaults = DEFAULT_REMOTE_APPS.filterTo(linkedSetOf()) { packageName ->
            context.packageManager.getLaunchIntentForPackage(packageName) != null
        }
        commitConfigurationChange { editor ->
            editor
                .putStringSet(ConfigKeys.REMOTE_APP_WHITELIST, installedDefaults)
                .putBoolean(KEY_DEFAULTS_INITIALIZED, true)
        }
    }

    private fun initializeShortcutDefaults() {
        if (preferences.getBoolean(KEY_SHORTCUT_DEFAULTS_INITIALIZED, false)) return
        commitConfigurationChange { editor ->
            SHORTCUTS.forEach { option ->
                if (!preferences.contains(option.preferenceKey)) {
                    editor.putBoolean(option.preferenceKey, true)
                }
            }
            val allEnabled = SHORTCUTS.all { option ->
                if (preferences.contains(option.preferenceKey)) {
                    preferences.getBoolean(option.preferenceKey, false)
                } else {
                    true
                }
            }
            editor
                .putBoolean(ConfigKeys.ALL_SHORTCUTS_ENABLED, allEnabled)
                .putBoolean(KEY_SHORTCUT_DEFAULTS_INITIALIZED, true)
        }
    }

    private fun initializeLanguage() {
        if (preferences.contains(ConfigKeys.UI_LANGUAGE)) return
        preferences.edit()
            .putString(
                ConfigKeys.UI_LANGUAGE,
                ConfigKeys.LANGUAGE_SIMPLIFIED_CHINESE,
            )
            .apply()
    }

    private fun migrateLegacyPreferences() {
        if (preferences.all.isNotEmpty()) return
        val legacy = context.getSharedPreferences(LEGACY_FILE_NAME, Context.MODE_PRIVATE)
        if (legacy.all.isEmpty()) return
        val editor = preferences.edit()
        legacy.all.forEach { (key, value) ->
            when (value) {
                is Boolean -> editor.putBoolean(key, value)
                is String -> editor.putString(key, value)
                is Int -> editor.putInt(key, value)
                is Long -> editor.putLong(key, value)
                is Float -> editor.putFloat(key, value)
                is Set<*> -> editor.putStringSet(key, value.filterIsInstance<String>().toSet())
            }
        }
        editor.commit()
    }

    private fun ensureConfigVersion() {
        if (preferences.contains(ConfigKeys.CONFIG_VERSION)) return
        commitConfigurationChange { }
    }

    private fun commitConfigurationChange(
        update: (SharedPreferences.Editor) -> Unit,
    ) {
        synchronized(preferences) {
            val nextVersion = preferences.getLong(ConfigKeys.CONFIG_VERSION, 0L) + 1L
            val editor = preferences.edit()
            update(editor)
            val committed = editor
                .putLong(ConfigKeys.CONFIG_VERSION, nextVersion)
                .commit()
            if (committed) {
                context.contentResolver.notifyChange(ConfigContract.changedUri, null)
            }
        }
    }

    companion object {
        private const val KEY_DEFAULTS_INITIALIZED = "whitelist_defaults_initialized"
        private const val KEY_SHORTCUT_DEFAULTS_INITIALIZED =
            "shortcut_defaults_initialized"
        private const val LEGACY_FILE_NAME = "hyperos_keyboard_fix"
        private const val STAR_PROMPT_LAUNCH_COUNT = 3

        val SHORTCUTS = listOf(
            ShortcutOption("Meta + Tab", ConfigKeys.SHORTCUT_META_TAB),
            ShortcutOption("Meta + D", ConfigKeys.SHORTCUT_META_D),
            ShortcutOption("Meta + E", ConfigKeys.SHORTCUT_META_E),
            ShortcutOption("Meta + R", ConfigKeys.SHORTCUT_META_R),
            ShortcutOption("Meta + L", ConfigKeys.SHORTCUT_META_L),
            ShortcutOption("Meta + W", ConfigKeys.SHORTCUT_META_W),
            ShortcutOption("Meta + M", ConfigKeys.SHORTCUT_META_M),
            ShortcutOption("Meta + N", ConfigKeys.SHORTCUT_META_N),
            ShortcutOption("Meta + S", ConfigKeys.SHORTCUT_META_S),
            ShortcutOption("Meta + A", ConfigKeys.SHORTCUT_META_A),
            ShortcutOption("Meta + C", ConfigKeys.SHORTCUT_META_C),
            ShortcutOption("Meta + V", ConfigKeys.SHORTCUT_META_V),
            ShortcutOption("Meta + X", ConfigKeys.SHORTCUT_META_X),
            ShortcutOption("Meta + Left", ConfigKeys.SHORTCUT_META_LEFT),
            ShortcutOption("Meta + Right", ConfigKeys.SHORTCUT_META_RIGHT),
            ShortcutOption("Meta + Up", ConfigKeys.SHORTCUT_META_UP),
            ShortcutOption("Meta + Down", ConfigKeys.SHORTCUT_META_DOWN),
            ShortcutOption("Alt + Tab", ConfigKeys.SHORTCUT_ALT_TAB),
        )

        val DEFAULT_REMOTE_APPS = setOf(
            "com.microsoft.rdc.androidx",
            "com.carriez.flutter_hbb",
            "com.limelight",
            "com.parsec.client",
            "com.valvesoftware.steamlink",
        )
    }
}
