package io.github.hmqyhm.hyperoskeyboardfix.ui.settings

import androidx.annotation.StringRes
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
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import io.github.hmqyhm.hyperoskeyboardfix.R
import io.github.hmqyhm.hyperoskeyboardfix.config.ConfigKeys
import io.github.hmqyhm.hyperoskeyboardfix.config.ModulePreferences

@Composable
fun ShortcutSettingsScreen(
    preferences: ModulePreferences,
    contentPadding: PaddingValues,
    onBack: () -> Unit,
) {
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

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(contentPadding),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                TextButton(onClick = onBack) {
                    Text(stringResource(R.string.back))
                }
                Column {
                    Text(
                        text = stringResource(R.string.shortcut_settings),
                        style = MaterialTheme.typography.headlineMedium,
                    )
                    Text(
                        text = stringResource(
                            R.string.enabled_shortcuts_count,
                            shortcutStates.values.count { it },
                        ),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }

        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                ShortcutSwitchRow(
                    title = stringResource(R.string.all_shortcuts),
                    description = stringResource(R.string.all_shortcuts_description),
                    checked = allShortcutsEnabled,
                    onCheckedChange = { enabled ->
                        preferences.setAllShortcutsEnabled(enabled)
                        allShortcutsEnabled = enabled
                        shortcutStates = ModulePreferences.SHORTCUTS.associate {
                            it.preferenceKey to enabled
                        }
                    },
                )
            }
        }

        items(
            items = ModulePreferences.SHORTCUTS,
            key = { it.preferenceKey },
        ) { option ->
            Card(modifier = Modifier.fillMaxWidth()) {
                ShortcutSwitchRow(
                    title = stringResource(shortcutTitleResource(option.preferenceKey)),
                    checked = shortcutStates[option.preferenceKey] == true,
                    onCheckedChange = { enabled ->
                        preferences.setShortcutEnabled(option.preferenceKey, enabled)
                        shortcutStates = shortcutStates.toMutableMap().apply {
                            this[option.preferenceKey] = enabled
                        }
                        allShortcutsEnabled = shortcutStates.values.all { it }
                    },
                )
            }
        }
    }
}

@Composable
private fun ShortcutSwitchRow(
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
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
        )
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
