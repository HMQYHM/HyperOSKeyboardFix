package io.github.hmqyhm.hyperoskeyboardfix.ui.settings

import android.content.Context
import android.view.inputmethod.InputMethodInfo
import android.view.inputmethod.InputMethodManager
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import io.github.hmqyhm.hyperoskeyboardfix.R
import io.github.hmqyhm.hyperoskeyboardfix.config.ModulePreferences

@Composable
fun AutoImeScreen(
    preferences: ModulePreferences,
    contentPadding: PaddingValues,
    onBack: () -> Unit,
) {
    FeatureList(
        contentPadding = contentPadding,
        title = stringResource(R.string.roadmap_auto_ime),
        onBack = onBack,
    ) {
        item { AutoImeSettingsSection(preferences) }
    }
}

@Composable
fun AutoImeSettingsSection(preferences: ModulePreferences) {
    val context = LocalContext.current
    val inputMethods = remember { installedInputMethods(context) }
    var enabled by remember { mutableStateOf(preferences.isAutoImeEnabled()) }
    var switchOnce by remember { mutableStateOf(preferences.isAutoImeSwitchOnce()) }
    var selectedId by remember { mutableStateOf(preferences.autoImeId()) }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text(
            text = stringResource(R.string.roadmap_auto_ime),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
        )
        Text(
            text = stringResource(R.string.roadmap_auto_ime_description),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        SettingSwitchCard(
            title = stringResource(R.string.auto_ime_switch),
            description = stringResource(R.string.auto_ime_switch_description),
            checked = enabled,
            onCheckedChange = {
                enabled = it
                preferences.setAutoImeEnabled(it)
            },
        )
        SettingSwitchCard(
            title = stringResource(R.string.auto_ime_once),
            description = stringResource(R.string.auto_ime_once_description),
            checked = switchOnce,
            onCheckedChange = {
                switchOnce = it
                preferences.setAutoImeSwitchOnce(it)
            },
        )
        Text(
            text = stringResource(R.string.auto_ime_choose),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
        )
        if (inputMethods.isEmpty()) {
            Text(stringResource(R.string.auto_ime_none))
        } else {
            inputMethods.forEach { info ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            selectedId = info.id
                            preferences.setAutoImeId(info.id)
                        },
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        RadioButton(
                            selected = selectedId == info.id,
                            onClick = {
                                selectedId = info.id
                                preferences.setAutoImeId(info.id)
                            },
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = info.loadLabel(context.packageManager).toString(),
                                fontWeight = FontWeight.SemiBold,
                            )
                            if (isRecommendedIme(info)) {
                                Text(
                                    text = stringResource(R.string.auto_ime_recommended),
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.primary,
                                )
                            }
                            Text(
                                text = info.packageName,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }
        }
        Text(
            text = stringResource(R.string.auto_ime_security_note),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/* Fn mapping was removed because the keyboard firmware does not expose Fn state.
@Composable
fun FnMappingScreen(
    preferences: ModulePreferences,
    contentPadding: PaddingValues,
    onBack: () -> Unit,
) {
    var enabled by remember { mutableStateOf(false) }
    FeatureList(
        contentPadding = contentPadding,
        title = stringResource(R.string.roadmap_fn_mapping),
        onBack = onBack,
    ) {
        item {
            SettingSwitchCard(
                title = stringResource(R.string.fn_mapping_switch),
                description = stringResource(R.string.fn_mapping_switch_description),
                checked = enabled,
                onCheckedChange = {
                    enabled = it
                },
            )
        }
        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(
                        text = stringResource(R.string.fn_mapping_table_title),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text("Fn + 1 → F1    Fn + 2 → F2    Fn + 3 → F3")
                    Text("Fn + 4 → F4    Fn + 5 → F5    Fn + 6 → F6")
                    Text("Fn + 7 → F7    Fn + 8 → F8    Fn + 9 → F9")
                    Text("Fn + 0 → F10")
                }
            }
        }
        item {
            Text(
                text = stringResource(R.string.fn_mapping_note),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
*/

@Composable
private fun SettingSwitchCard(
    title: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(18.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleMedium)
                Text(
                    description,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
            Switch(checked = checked, onCheckedChange = onCheckedChange)
        }
    }
}

@Composable
private fun FeatureList(
    contentPadding: PaddingValues,
    title: String,
    onBack: () -> Unit,
    content: androidx.compose.foundation.lazy.LazyListScope.() -> Unit,
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(contentPadding),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                TextButton(onClick = onBack) { Text(stringResource(R.string.back)) }
                Text(
                    text = title,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                )
            }
        }
        content()
    }
}

private fun installedInputMethods(context: Context): List<InputMethodInfo> {
    val manager = context.getSystemService(InputMethodManager::class.java)
    return manager.inputMethodList
        .sortedWith(
            compareByDescending<InputMethodInfo>(::isRecommendedIme)
                .thenBy { it.loadLabel(context.packageManager).toString() },
        )
}

private fun isRecommendedIme(info: InputMethodInfo): Boolean =
    RECOMMENDED_IME_PACKAGE_HINTS.any(info.packageName.lowercase()::contains)

private val RECOMMENDED_IME_PACKAGE_HINTS = listOf("sogou", "baidu", "iflytek")

private val SUPPORTED_IME_HINTS = listOf(
    "sogou", "搜狗", "baidu", "百度", "iflytek", "讯飞", "訊飛",
)
