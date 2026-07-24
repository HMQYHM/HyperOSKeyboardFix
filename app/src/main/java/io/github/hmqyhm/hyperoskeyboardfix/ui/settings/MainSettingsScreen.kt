package io.github.hmqyhm.hyperoskeyboardfix.ui.settings

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
import io.github.hmqyhm.hyperoskeyboardfix.config.ProjectLinks

@Composable
fun MainSettingsScreen(
    preferences: ModulePreferences,
    contentPadding: PaddingValues,
    onBack: () -> Unit,
    onOpenWhitelist: () -> Unit,
    onOpenShortcuts: () -> Unit,
    onLanguageSelected: (String) -> Unit,
) {
    val context = LocalContext.current
    val uriHandler = LocalUriHandler.current
    var masterEnabled by remember { mutableStateOf(preferences.isMasterEnabled()) }
    var whitelistCount by remember { mutableStateOf(preferences.whitelist().size) }
    val enabledShortcutCount = preferences.enabledShortcutCount()
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
            OutlinedButton(
                onClick = {
                    uriHandler.openUri(ProjectLinks.GITHUB_REPOSITORY)
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
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onOpenShortcuts),
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 18.dp, vertical = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Text(
                        text = stringResource(R.string.shortcut_settings),
                        style = MaterialTheme.typography.titleMedium,
                    )
                    Text(
                        text = stringResource(
                            R.string.enabled_shortcuts_count,
                            enabledShortcutCount,
                        ),
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
            StatusCard(
                masterEnabled = masterEnabled,
                whitelistCount = whitelistCount,
                enabledShortcutCount = enabledShortcutCount,
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

private data class ProviderUiResult(
    val success: Boolean,
    val version: Long?,
)
