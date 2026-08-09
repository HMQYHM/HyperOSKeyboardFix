package io.github.hmqyhm.hyperoskeyboardfix.ui.settings

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import io.github.hmqyhm.hyperoskeyboardfix.R
import io.github.hmqyhm.hyperoskeyboardfix.config.ConfigContract
import io.github.hmqyhm.hyperoskeyboardfix.config.ConfigKeys
import io.github.hmqyhm.hyperoskeyboardfix.config.ModulePreferences
import io.github.hmqyhm.hyperoskeyboardfix.config.ProviderDiagnostics
import io.github.hmqyhm.hyperoskeyboardfix.config.ProjectLinks
import kotlinx.coroutines.delay

@Composable
fun MainSettingsScreen(
    preferences: ModulePreferences,
    contentPadding: PaddingValues,
    onLanguageSelected: (String) -> Unit,
    autoImeFocusRequest: Int,
) {
    val context = LocalContext.current
    val uriHandler = LocalUriHandler.current
    val listState = rememberLazyListState()
    var inputMode by remember { mutableStateOf(preferences.inputMode()) }
    var showResetConfirmation by remember { mutableStateOf(false) }
    var selectedLanguage by remember { mutableStateOf(preferences.languageTag()) }
    var notificationsEnabled by remember { mutableStateOf(preferences.notificationsEnabled()) }
    var notifyAppOpen by remember { mutableStateOf(preferences.notifyAppOpen()) }
    var notifyAutoIme by remember { mutableStateOf(preferences.notifyAutoIme()) }
    var notifyMacro by remember { mutableStateOf(preferences.notifyMacro()) }
    var notificationAutoDismiss by remember {
        mutableStateOf(preferences.notificationAutoDismiss())
    }
    var notificationTitle by remember { mutableStateOf(preferences.notificationTitle()) }
    var notificationPermissionGranted by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS,
            ) == PackageManager.PERMISSION_GRANTED,
        )
    }
    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted ->
        notificationPermissionGranted = granted
        if (granted) {
            notificationsEnabled = true
            notifyAppOpen = true
            notifyAutoIme = true
            notifyMacro = true
            preferences.setNotificationSettings(
                enabled = true,
                appOpen = true,
                autoIme = true,
                macro = true,
                autoDismiss = notificationAutoDismiss,
                title = notificationTitle,
            )
        }
    }
    fun saveNotificationSettings() {
        preferences.setNotificationSettings(
            enabled = notificationsEnabled,
            appOpen = notifyAppOpen,
            autoIme = notifyAutoIme,
            macro = notifyMacro,
            autoDismiss = notificationAutoDismiss,
            title = notificationTitle,
        )
    }

    LaunchedEffect(Unit) {
        if (!notificationPermissionGranted) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }
    LaunchedEffect(autoImeFocusRequest) {
        if (autoImeFocusRequest > 0) {
            delay(180L)
            listState.animateScrollToItem(AUTO_IME_SECTION_INDEX)
        }
    }

    LazyColumn(
        state = listState,
        modifier = Modifier
            .fillMaxSize()
            .padding(contentPadding),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(
                    text = stringResource(R.string.navigation_other),
                    style = MaterialTheme.typography.headlineLarge,
                )
                Text(
                    text = stringResource(R.string.settings_subtitle),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        item {
            CurrentModeCard(
                inputMode = inputMode,
                onModeChange = {
                    inputMode = it
                    preferences.setInputMode(it)
                },
            )
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
            AutoImeSettingsSection(preferences)
        }

        item {
            NotificationSettingsCard(
                enabled = notificationsEnabled,
                appOpen = notifyAppOpen,
                autoIme = notifyAutoIme,
                macro = notifyMacro,
                autoDismiss = notificationAutoDismiss,
                title = notificationTitle,
                permissionGranted = notificationPermissionGranted,
                onPermissionRequest = {
                    notificationPermissionLauncher.launch(
                        Manifest.permission.POST_NOTIFICATIONS,
                    )
                },
                onEnabledChange = {
                    notificationsEnabled = it
                    saveNotificationSettings()
                },
                onAppOpenChange = {
                    notifyAppOpen = it
                    saveNotificationSettings()
                },
                onAutoImeChange = {
                    notifyAutoIme = it
                    saveNotificationSettings()
                },
                onMacroChange = {
                    notifyMacro = it
                    saveNotificationSettings()
                },
                onAutoDismissChange = {
                    notificationAutoDismiss = it
                    saveNotificationSettings()
                },
                onTitleChange = { notificationTitle = it },
                onSaveTitle = ::saveNotificationSettings,
                onSendTestNotification = {
                    saveNotificationSettings()
                    context.contentResolver.call(
                        ConfigContract.baseUri,
                        ConfigContract.METHOD_NOTIFY_EVENT,
                        ConfigContract.EVENT_TEST,
                        null,
                    )
                },
            )
        }

        item {
            Button(
                onClick = { showResetConfirmation = true },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer,
                    contentColor = MaterialTheme.colorScheme.onErrorContainer,
                ),
            ) {
                Text(stringResource(R.string.restore_defaults))
            }
        }

        item {
            ProjectSection(
                onOpenMainProject = {
                    uriHandler.openUri(ProjectLinks.GITHUB_REPOSITORY)
                },
                onOpenRelatedProject = {
                    uriHandler.openUri(ProjectLinks.FOCUS_PEN_PRO_X)
                },
            )
        }

    }

    if (showResetConfirmation) {
        AlertDialog(
            onDismissRequest = { showResetConfirmation = false },
            title = { Text(stringResource(R.string.restore_defaults_title)) },
            text = { Text(stringResource(R.string.restore_defaults_message)) },
            confirmButton = {
                TextButton(onClick = {
                    preferences.resetToDefaults()
                    inputMode = ConfigKeys.INPUT_MODE_DIRECT
                    notificationsEnabled = true
                    notifyAppOpen = true
                    notifyAutoIme = true
                    notifyMacro = true
                    notificationAutoDismiss = true
                    notificationTitle = ""
                    showResetConfirmation = false
                }) { Text(stringResource(R.string.confirm_restore)) }
            },
            dismissButton = {
                TextButton(onClick = { showResetConfirmation = false }) {
                    Text(stringResource(R.string.cancel))
                }
            },
        )
    }
}

private const val AUTO_IME_SECTION_INDEX = 3

@Composable
private fun ProjectSection(
    onOpenMainProject: () -> Unit,
    onOpenRelatedProject: () -> Unit,
) {
    val dropProgress = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        dropProgress.animateTo(
            targetValue = 1f,
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessLow,
            ),
        )
    }
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = stringResource(R.string.project_section_title),
                modifier = Modifier.fillMaxWidth(),
                style = MaterialTheme.typography.titleLarge,
            )
            Text(
                text = stringResource(R.string.project_star_falling),
                modifier = Modifier
                    .fillMaxWidth()
                    .graphicsLayer {
                        val progress = dropProgress.value.coerceIn(0f, 1f)
                        translationY = (progress - 1f) * 42.dp.toPx()
                        scaleX = 0.9f + progress * 0.1f
                        scaleY = 0.9f + progress * 0.1f
                        alpha = progress
                    },
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            OutlinedButton(
                onClick = onOpenMainProject,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(stringResource(R.string.project_home))
                    Text(
                        stringResource(R.string.project_home_pending),
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }
            OutlinedButton(
                onClick = onOpenRelatedProject,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(stringResource(R.string.focus_pen_project))
                    Text(
                        stringResource(R.string.focus_pen_project_description),
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }
        }
    }
}

@Composable
private fun CurrentModeCard(
    inputMode: String,
    onModeChange: (String) -> Unit,
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(
                text = stringResource(R.string.current_mode_title),
                style = MaterialTheme.typography.titleMedium,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = inputMode == ConfigKeys.INPUT_MODE_COMPATIBILITY,
                    onClick = { onModeChange(ConfigKeys.INPUT_MODE_COMPATIBILITY) },
                    label = { Text(stringResource(R.string.mode_compatibility)) },
                )
                FilterChip(
                    selected = inputMode == ConfigKeys.INPUT_MODE_DIRECT,
                    onClick = { onModeChange(ConfigKeys.INPUT_MODE_DIRECT) },
                    label = { Text(stringResource(R.string.mode_direct)) },
                )
            }
            Text(
                text = stringResource(
                    if (inputMode == ConfigKeys.INPUT_MODE_DIRECT) {
                        R.string.mode_direct_description
                    } else {
                        R.string.mode_compatibility_description
                    },
                ),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun NotificationSettingsCard(
    enabled: Boolean,
    appOpen: Boolean,
    autoIme: Boolean,
    macro: Boolean,
    autoDismiss: Boolean,
    title: String,
    permissionGranted: Boolean,
    onPermissionRequest: () -> Unit,
    onEnabledChange: (Boolean) -> Unit,
    onAppOpenChange: (Boolean) -> Unit,
    onAutoImeChange: (Boolean) -> Unit,
    onMacroChange: (Boolean) -> Unit,
    onAutoDismissChange: (Boolean) -> Unit,
    onTitleChange: (String) -> Unit,
    onSaveTitle: () -> Unit,
    onSendTestNotification: () -> Unit,
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(
                stringResource(R.string.notification_settings),
                style = MaterialTheme.typography.titleMedium,
            )
            NotificationSwitchRow(
                stringResource(R.string.notification_master),
                enabled,
                onEnabledChange,
            )
            NotificationSwitchRow(
                stringResource(R.string.notification_app_open),
                appOpen,
                onAppOpenChange,
                enabled,
            )
            NotificationSwitchRow(
                stringResource(R.string.notification_auto_ime),
                autoIme,
                onAutoImeChange,
                enabled,
            )
            NotificationSwitchRow(
                stringResource(R.string.notification_macro),
                macro,
                onMacroChange,
                enabled,
            )
            NotificationSwitchRow(
                stringResource(R.string.notification_auto_dismiss),
                autoDismiss,
                onAutoDismissChange,
                enabled,
            )
            OutlinedTextField(
                value = title,
                onValueChange = onTitleChange,
                label = { Text(stringResource(R.string.notification_custom_title)) },
                supportingText = {
                    Text(stringResource(R.string.notification_custom_title_hint))
                },
                modifier = Modifier.fillMaxWidth(),
            )
            BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
                val spacing = 8.dp
                val buttonWidth = (maxWidth - spacing) / 2
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(spacing),
                    verticalArrangement = Arrangement.spacedBy(spacing),
                ) {
                    Button(
                        onClick = onSaveTitle,
                        modifier = Modifier.width(buttonWidth).height(56.dp),
                    ) {
                        Text(stringResource(R.string.notification_save))
                    }
                    Button(
                        onClick = {
                            if (permissionGranted) onSendTestNotification()
                            else onPermissionRequest()
                        },
                        modifier = Modifier.width(buttonWidth).height(56.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (permissionGranted) {
                                MaterialTheme.colorScheme.primaryContainer
                            } else {
                                MaterialTheme.colorScheme.errorContainer
                            },
                            contentColor = if (permissionGranted) {
                                MaterialTheme.colorScheme.onPrimaryContainer
                            } else {
                                MaterialTheme.colorScheme.onErrorContainer
                            },
                        ),
                    ) {
                        Text(
                            stringResource(
                                if (permissionGranted) {
                                    R.string.notification_send_test
                                } else {
                                    R.string.notification_grant_permission
                                },
                            ),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun NotificationSwitchRow(
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    enabled: Boolean = true,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(title, modifier = Modifier.weight(1f))
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            enabled = enabled,
        )
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
                    stringResource(
                        if (masterEnabled) {
                            R.string.module_state_on
                        } else {
                            R.string.module_state_off
                        },
                    ),
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

private data class ProviderUiResult(
    val success: Boolean,
    val version: Long?,
)
