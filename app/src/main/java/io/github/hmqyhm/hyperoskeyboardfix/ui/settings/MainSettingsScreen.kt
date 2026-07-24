package io.github.hmqyhm.hyperoskeyboardfix.ui.settings

import androidx.annotation.StringRes
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import io.github.hmqyhm.hyperoskeyboardfix.R
import io.github.hmqyhm.hyperoskeyboardfix.config.ConfigContract
import io.github.hmqyhm.hyperoskeyboardfix.config.ConfigKeys
import io.github.hmqyhm.hyperoskeyboardfix.config.ModulePreferences
import io.github.hmqyhm.hyperoskeyboardfix.config.ProviderDiagnostics

@Composable
fun MainSettingsScreen(
    preferences: ModulePreferences,
    contentPadding: PaddingValues,
    onBack: () -> Unit,
    onOpenWhitelist: () -> Unit,
    onLanguageSelected: (String) -> Unit,
) {
    val context = LocalContext.current
    val uriHandler = LocalUriHandler.current
    var masterEnabled by remember { mutableStateOf(preferences.isMasterEnabled()) }
    var whitelistCount by remember { mutableStateOf(preferences.whitelist().size) }
    var shortcutStates by remember {
        mutableStateOf(
            ModulePreferences.SHORTCUTS.associate { option ->
                option.preferenceKey to preferences.isShortcutEnabled(option.preferenceKey)
            },
        )
    }
    var allShortcutsEnabled by remember {
        mutableStateOf(preferences.areAllShortcutsEnabled())
    }
    var configVersion by remember { mutableStateOf(preferences.configVersion()) }
    var selectedLanguage by remember { mutableStateOf(preferences.languageTag()) }
    var providerResult by remember { mutableStateOf<ProviderUiResult?>(null) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(contentPadding),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                TextButton(onClick = onBack) {
                    Text(stringResource(R.string.back))
                }
                Text(
                    text = stringResource(R.string.settings_title),
                    style = MaterialTheme.typography.headlineMedium,
                )
            }
        }

        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .animateContentSize(),
            ) {
                SettingSwitchRow(
                    title = stringResource(R.string.master_title),
                    description = stringResource(R.string.master_description),
                    checked = masterEnabled,
                    onCheckedChange = { enabled ->
                        preferences.setMasterEnabled(enabled)
                        masterEnabled = enabled
                        configVersion = preferences.configVersion()
                    },
                )
            }
        }

        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        whitelistCount = preferences.whitelist().size
                        onOpenWhitelist()
                    },
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 18.dp, vertical = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Text(
                        text = stringResource(R.string.choose_apps),
                        style = MaterialTheme.typography.titleMedium,
                    )
                    Text(
                        text = stringResource(R.string.selected_apps_count, whitelistCount),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }

        item {
            LanguageCard(
                selectedLanguage = selectedLanguage,
                onSelected = { languageTag ->
                    selectedLanguage = languageTag
                    preferences.setLanguageTag(languageTag)
                    onLanguageSelected(languageTag)
                },
            )
        }

        item {
            Text(
                text = stringResource(R.string.shortcut_settings),
                modifier = Modifier.padding(top = 8.dp),
                style = MaterialTheme.typography.titleLarge,
            )
        }

        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                SettingSwitchRow(
                    title = stringResource(R.string.all_shortcuts),
                    description = stringResource(R.string.all_shortcuts_description),
                    checked = allShortcutsEnabled,
                    onCheckedChange = { enabled ->
                        preferences.setAllShortcutsEnabled(enabled)
                        allShortcutsEnabled = enabled
                        shortcutStates = ModulePreferences.SHORTCUTS.associate {
                            it.preferenceKey to enabled
                        }
                        configVersion = preferences.configVersion()
                    },
                )
            }
        }

        items(
            items = ModulePreferences.SHORTCUTS,
            key = { it.preferenceKey },
        ) { option ->
            SettingSwitchRow(
                title = stringResource(shortcutTitleResource(option.preferenceKey)),
                checked = shortcutStates[option.preferenceKey] == true,
                onCheckedChange = { enabled ->
                    preferences.setShortcutEnabled(option.preferenceKey, enabled)
                    shortcutStates = shortcutStates.toMutableMap().apply {
                        this[option.preferenceKey] = enabled
                    }
                    allShortcutsEnabled = shortcutStates.values.all { it }
                    configVersion = preferences.configVersion()
                },
            )
        }

        item {
            StatusCard(
                masterEnabled = masterEnabled,
                whitelistCount = whitelistCount,
                enabledShortcutCount = shortcutStates.values.count { it },
                configVersion = configVersion,
                providerResult = providerResult,
                onTestProvider = {
                    val result = ProviderDiagnostics.test(context)
                    providerResult = ProviderUiResult(
                        success = result.connected && result.bundleComplete,
                        version = result.version,
                    )
                    configVersion = preferences.configVersion()
                },
            )
        }

        item {
            OutlinedButton(
                onClick = {
                    uriHandler.openUri(PROJECT_HOMEPAGE)
                },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(
                    modifier = Modifier.padding(vertical = 4.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(stringResource(R.string.project_home))
                    Text(
                        text = stringResource(R.string.project_home_pending),
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }
        }
    }
}

@Composable
private fun LanguageCard(
    selectedLanguage: String,
    onSelected: (String) -> Unit,
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(horizontal = 18.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(
                text = stringResource(R.string.language_title),
                style = MaterialTheme.typography.titleMedium,
            )
            Text(
                text = stringResource(R.string.language_description),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                LanguageChip(
                    text = stringResource(R.string.language_simplified),
                    languageTag = ConfigKeys.LANGUAGE_SIMPLIFIED_CHINESE,
                    selectedLanguage = selectedLanguage,
                    onSelected = onSelected,
                )
                LanguageChip(
                    text = stringResource(R.string.language_traditional),
                    languageTag = ConfigKeys.LANGUAGE_TRADITIONAL_CHINESE,
                    selectedLanguage = selectedLanguage,
                    onSelected = onSelected,
                )
                LanguageChip(
                    text = stringResource(R.string.language_english),
                    languageTag = ConfigKeys.LANGUAGE_ENGLISH,
                    selectedLanguage = selectedLanguage,
                    onSelected = onSelected,
                )
            }
        }
    }
}

@Composable
private fun LanguageChip(
    text: String,
    languageTag: String,
    selectedLanguage: String,
    onSelected: (String) -> Unit,
) {
    FilterChip(
        selected = selectedLanguage == languageTag,
        onClick = { onSelected(languageTag) },
        label = { Text(text) },
    )
}

@Composable
private fun StatusCard(
    masterEnabled: Boolean,
    whitelistCount: Int,
    enabledShortcutCount: Int,
    configVersion: Long,
    providerResult: ProviderUiResult?,
    onTestProvider: () -> Unit,
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(horizontal = 18.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text(stringResource(R.string.current_status), style = MaterialTheme.typography.titleMedium)
            Text(
                stringResource(
                    R.string.module_switch_status,
                    stringResource(if (masterEnabled) R.string.state_on else R.string.state_off),
                ),
            )
            Text(stringResource(R.string.selected_apps_count, whitelistCount))
            Text(stringResource(R.string.enabled_shortcuts_count, enabledShortcutCount))
            Text(
                text = stringResource(R.string.foreground_notice),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodyMedium,
            )
            HorizontalDivider(modifier = Modifier.padding(vertical = 6.dp))
            Text(
                stringResource(R.string.config_communication),
                style = MaterialTheme.typography.titleMedium,
            )
            Text(stringResource(R.string.provider_authority, ConfigContract.authority))
            Text(stringResource(R.string.config_version, configVersion))
            Button(onClick = onTestProvider) {
                Text(stringResource(R.string.test_provider))
            }
            Text(
                text = when {
                    providerResult == null -> stringResource(R.string.provider_not_tested)
                    providerResult.success -> stringResource(
                        R.string.provider_success,
                        providerResult.version ?: configVersion,
                    )
                    else -> stringResource(R.string.provider_failed)
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun SettingSwitchRow(
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    description: String? = null,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .padding(horizontal = 14.dp, vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(3.dp),
        ) {
            Text(text = title, style = MaterialTheme.typography.bodyLarge)
            if (description != null) {
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@StringRes
private fun shortcutTitleResource(key: String): Int = when (key) {
    ConfigKeys.SHORTCUT_META_TAB -> R.string.shortcut_meta_tab
    ConfigKeys.SHORTCUT_META_D -> R.string.shortcut_meta_d
    ConfigKeys.SHORTCUT_META_E -> R.string.shortcut_meta_e
    ConfigKeys.SHORTCUT_META_R -> R.string.shortcut_meta_r
    ConfigKeys.SHORTCUT_META_L -> R.string.shortcut_meta_l
    ConfigKeys.SHORTCUT_META_W -> R.string.shortcut_meta_w
    ConfigKeys.SHORTCUT_META_M -> R.string.shortcut_meta_m
    ConfigKeys.SHORTCUT_META_N -> R.string.shortcut_meta_n
    ConfigKeys.SHORTCUT_META_S -> R.string.shortcut_meta_s
    ConfigKeys.SHORTCUT_META_A -> R.string.shortcut_meta_a
    ConfigKeys.SHORTCUT_META_C -> R.string.shortcut_meta_c
    ConfigKeys.SHORTCUT_META_V -> R.string.shortcut_meta_v
    ConfigKeys.SHORTCUT_META_X -> R.string.shortcut_meta_x
    ConfigKeys.SHORTCUT_META_LEFT -> R.string.shortcut_meta_left
    ConfigKeys.SHORTCUT_META_RIGHT -> R.string.shortcut_meta_right
    ConfigKeys.SHORTCUT_META_UP -> R.string.shortcut_meta_up
    ConfigKeys.SHORTCUT_META_DOWN -> R.string.shortcut_meta_down
    ConfigKeys.SHORTCUT_ALT_TAB -> R.string.shortcut_alt_tab
    else -> R.string.shortcut_settings
}

private data class ProviderUiResult(
    val success: Boolean,
    val version: Long?,
)

private const val PROJECT_HOMEPAGE =
    "https://github.com/HMQYHM/HyperOS-Keyboard-Fix"
