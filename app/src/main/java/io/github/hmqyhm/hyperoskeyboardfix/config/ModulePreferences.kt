package io.github.hmqyhm.hyperoskeyboardfix.config

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import java.util.Locale

class ModulePreferences(private val context: Context) {
    private val preferences: SharedPreferences =
        context.getSharedPreferences(ConfigKeys.FILE_NAME, Context.MODE_PRIVATE)

    init {
        migrateLegacyPreferences()
        initializeDefaultWhitelist()
        initializeShortcutDefaults()
        initializeLanguage()
        ensureConfigVersion()
        removeLegacyMacroConfiguration()
    }

    fun isMasterEnabled(): Boolean =
        preferences.getBoolean(ConfigKeys.MASTER_ENABLED, false)

    fun setMasterEnabled(enabled: Boolean) {
        commitConfigurationChange { editor ->
            editor.putBoolean(ConfigKeys.MASTER_ENABLED, enabled)
        }
    }

    fun areAllShortcutsEnabled(): Boolean =
        preferences.getBoolean(ConfigKeys.ALL_SHORTCUTS_ENABLED, true)

    fun setAllShortcutsEnabled(enabled: Boolean) {
        commitConfigurationChange { editor ->
            editor.putBoolean(ConfigKeys.ALL_SHORTCUTS_ENABLED, enabled)
        }
    }

    fun setShortcutTakeoverEnabled(enabled: Boolean) {
        commitConfigurationChange { editor ->
            editor
                .putBoolean(ConfigKeys.MASTER_ENABLED, enabled)
                .putBoolean(ConfigKeys.ALL_SHORTCUTS_ENABLED, enabled)
        }
    }

    fun enabledShortcutCount(): Int = if (areAllShortcutsEnabled()) 1 else 0

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
        val catalogVersion = preferences.getInt(
            KEY_DEFAULT_WHITELIST_CATALOG_VERSION,
            0,
        )
        if (catalogVersion >= DEFAULT_WHITELIST_CATALOG_VERSION) return
        val installedDefaults = resolveInstalledDefaultRemoteApps()
        val mergedWhitelist = whitelist() + installedDefaults
        commitConfigurationChange { editor ->
            editor
                .putStringSet(ConfigKeys.REMOTE_APP_WHITELIST, mergedWhitelist)
                .putBoolean(KEY_DEFAULTS_INITIALIZED, true)
                .putInt(
                    KEY_DEFAULT_WHITELIST_CATALOG_VERSION,
                    DEFAULT_WHITELIST_CATALOG_VERSION,
                )
        }
    }

    private fun resolveInstalledDefaultRemoteApps(): Set<String> {
        val packageManager = context.packageManager
        val installedPackages = DEFAULT_REMOTE_APPS
            .filterTo(linkedSetOf()) { packageName ->
                packageManager.getLaunchIntentForPackage(packageName) != null
            }
        val launcherIntent = Intent(Intent.ACTION_MAIN)
            .addCategory(Intent.CATEGORY_LAUNCHER)
        packageManager.queryIntentActivities(
            launcherIntent,
            PackageManager.ResolveInfoFlags.of(0),
        ).forEach { resolveInfo ->
            val packageName = resolveInfo.activityInfo?.packageName ?: return@forEach
            val normalizedLabel = normalizeAppLabel(
                resolveInfo.loadLabel(packageManager).toString(),
            )
            if (
                DEFAULT_REMOTE_APP_LABELS.any { alias ->
                    normalizedLabel == alias ||
                        normalizedLabel.startsWith(alias) ||
                        normalizedLabel.endsWith(alias)
                }
            ) {
                installedPackages += packageName
            }
        }
        return installedPackages
    }

    private fun normalizeAppLabel(label: String): String =
        label.lowercase(Locale.ROOT).filter(Char::isLetterOrDigit)

    private fun initializeShortcutDefaults() {
        if (preferences.getBoolean(KEY_SHORTCUT_DEFAULTS_INITIALIZED, false)) return
        commitConfigurationChange { editor ->
            editor
                .putBoolean(
                    ConfigKeys.ALL_SHORTCUTS_ENABLED,
                    preferences.getBoolean(ConfigKeys.ALL_SHORTCUTS_ENABLED, true),
                )
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

    private fun removeLegacyMacroConfiguration() {
        if (!preferences.contains(LEGACY_KEYBOARD_MAPPINGS)) return
        commitConfigurationChange { editor ->
            editor.remove(LEGACY_KEYBOARD_MAPPINGS)
        }
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
        private const val KEY_DEFAULT_WHITELIST_CATALOG_VERSION =
            "default_whitelist_catalog_version"
        private const val DEFAULT_WHITELIST_CATALOG_VERSION = 2
        private const val KEY_SHORTCUT_DEFAULTS_INITIALIZED =
            "shortcut_defaults_initialized"
        private const val LEGACY_FILE_NAME = "hyperos_keyboard_fix"
        private const val LEGACY_KEYBOARD_MAPPINGS = "keyboard_mappings"
        private const val STAR_PROMPT_LAUNCH_COUNT = 3

        val DEFAULT_REMOTE_APPS = setOf(
            "com.valvesoftware.steamlink",
            "com.netease.uuremote",
            "com.microsoft.rdc.androidx",
            "com.microsoft.rdc.android",
            "com.microsoft.rdc.android.beta",
            "com.carriez.flutter_hbb",
            "com.limelight",
            "com.parsec.client",
            "tv.parsec.client",
            "com.anydesk.anydeskandroid",
            "com.teamviewer.teamviewer.market.mobile",
            "com.google.chromeremotedesktop",
            "com.splashtop.remote.pad.v2",
            "com.splashtop.remote",
            "com.p5sys.android.jump",
            "com.realvnc.viewer.android",
            "com.nomachine.nxplayer",
            "com.aomei.anyviewer",
            "com.prosoftnet.rpc",
            "com.citrixonline.gotomypc",
            "com.logmein.logmeinpro2",
            "com.zoho.assist",
            "com.islonline.isllight.mobile.android",
            "it.nanosystems.supremo",
            "com.oray.sunlogin",
            "youqu.android.todesk",
            "com.example.raylink_flutter",
            "com.zuler.deskin",
            "com.sand.airdroid",
            "com.sand.airmirror",
            "com.sand.aircast",
            "com.koushikdutta.vysor",
            "com.samsung.android.galaxycontinuity",
            "com.microsoft.appmanager",
            "org.kde.kdeconnect_tp",
            "com.playstation.remoteplay",
            "com.microsoft.xboxone.smartglass",
            "com.metallic.chiaki",
            "com.streetpea.chiaki4deck",
            "com.studio08.xbgamestream.xbox.game.stream",
            "com.nvidia.geforcenow",
            "com.gamepass",
            "com.amazon.spider",
            "com.boosteroid.streaming",
            "com.blacknut.app",
            "com.blade.shadowcloudgaming",
            "com.netboom.cloudgaming.vortex_stadia",
            "com.chikii.game",
            "cn.emagsoftware.gamehall",
            "com.tencent.start",
            "com.netease.cloudgame",
            "com.netease.android.cloudgame",
        )

        private val DEFAULT_REMOTE_APP_LABELS = setOf(
            "steamlink",
            "网易uu远程",
            "windowsapp",
            "moonlight",
            "moonlightgamestreaming",
            "parsec",
            "anydesk",
            "anydeskremotedesktop",
            "teamviewer",
            "teamviewerremotecontrol",
            "rustdesk",
            "chromeremotedesktop",
            "microsoftremotedesktop",
            "splashtop",
            "splashtoppersonal",
            "jumpdesktop",
            "realvncviewer",
            "vncviewer",
            "nomachine",
            "anyviewer",
            "anyviewerremotedesktop",
            "remotepc",
            "remotepcviewer",
            "gotomypc",
            "logmein",
            "zohoassist",
            "isllight",
            "supremo",
            "向日葵远程控制",
            "向日葵",
            "todesk",
            "raylink",
            "raylink远程控制",
            "deskin",
            "deskinremotedesktop",
            "gameviewer",
            "爱思远控",
            "airdroid",
            "airmirror",
            "airdroidcast",
            "vysor",
            "samsungflow",
            "linktowindows",
            "kdeconnect",
            "psremoteplay",
            "xbox",
            "chiaki",
            "chiaking",
            "xbplay",
            "xstreaming",
            "nvidiageforcenow",
            "geforcenow",
            "xboxcloudgaming",
            "amazonluna",
            "boosteroid",
            "blacknut",
            "shadowpc",
            "netboom",
            "chikii",
            "咪咕快游",
            "start云游戏",
            "网易云游戏",
            "腾讯先锋",
            "随乐游",
            "达龙云电脑",
            "极云普惠云电脑",
            "海马云电脑",
        ).mapTo(linkedSetOf()) { label ->
            label.lowercase(Locale.ROOT).filter(Char::isLetterOrDigit)
        }
    }
}
