package io.github.hmqyhm.hyperoskeyboardfix.ui.settings

import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import androidx.activity.compose.PredictiveBackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import io.github.hmqyhm.hyperoskeyboardfix.R
import io.github.hmqyhm.hyperoskeyboardfix.config.ModulePreferences
import io.github.hmqyhm.hyperoskeyboardfix.model.InstalledApp
import java.util.Locale
import java.util.concurrent.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.withContext

@Composable
fun WhitelistScreen(
    preferences: ModulePreferences,
    contentPadding: PaddingValues,
    onBack: () -> Unit,
    onSaved: () -> Unit,
) {
    val context = LocalContext.current
    var apps by remember { mutableStateOf<List<InstalledApp>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var query by remember { mutableStateOf("") }
    var showSystemApps by remember { mutableStateOf(false) }
    var savedPackages by remember { mutableStateOf(preferences.whitelist()) }
    var selectedPackages by remember { mutableStateOf(savedPackages) }
    var displayOrder by remember { mutableStateOf<List<String>>(emptyList()) }
    var showExitConfirmation by remember { mutableStateOf(false) }
    val hasUnsavedChanges = selectedPackages != savedPackages

    LaunchedEffect(Unit) {
        val loadedApps = withContext(Dispatchers.IO) {
            loadLaunchableApps(context.packageManager)
        }
        apps = loadedApps
        displayOrder = loadedApps
            .sortedWith(
                compareByDescending<InstalledApp> { it.packageName in savedPackages }
                    .thenBy { it.label.lowercase(Locale.getDefault()) }
                    .thenBy { it.packageName },
            )
            .map(InstalledApp::packageName)
        loading = false
    }

    PredictiveBackHandler(enabled = hasUnsavedChanges) { events ->
        try {
            events.collect { }
            showExitConfirmation = true
        } catch (_: CancellationException) {
            // A cancelled gesture keeps editing without changing state.
        }
    }

    fun saveSelection() {
        preferences.saveWhitelist(selectedPackages)
        if (selectedPackages.isEmpty()) {
            preferences.setShortcutTakeoverEnabled(false)
        }
        savedPackages = selectedPackages
        onSaved()
    }

    fun requestBack() {
        if (hasUnsavedChanges) showExitConfirmation = true else onBack()
    }

    val normalizedQuery = query.trim().lowercase(Locale.getDefault())
    val orderIndex = displayOrder.withIndex().associate { it.value to it.index }
    val visibleApps = apps
        .asSequence()
        .filter { app ->
            (showSystemApps || !app.isSystemApp) &&
                (normalizedQuery.isEmpty() ||
                    app.label.lowercase(Locale.getDefault()).contains(normalizedQuery) ||
                    app.packageName.lowercase(Locale.ROOT).contains(normalizedQuery))
        }
        .sortedBy { orderIndex[it.packageName] ?: Int.MAX_VALUE }
        .toList()

    Box(
        modifier = Modifier.fillMaxSize().padding(contentPadding),
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                TextButton(onClick = ::requestBack) { Text(stringResource(R.string.back)) }
                Text(
                    text = stringResource(R.string.whitelist_title),
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.titleLarge,
                )
                Text(stringResource(R.string.selected_short, selectedPackages.size))
            }

            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text(stringResource(R.string.search_apps)) },
                singleLine = true,
            )

            Card(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(stringResource(R.string.show_system_apps), modifier = Modifier.weight(1f))
                    Switch(checked = showSystemApps, onCheckedChange = { showSystemApps = it })
                }
            }

            if (loading) {
                Spacer(modifier = Modifier.weight(1f))
                CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally))
                Spacer(modifier = Modifier.weight(1f))
            } else {
                AnimatedVisibility(
                    visible = visibleApps.isEmpty(),
                    enter = fadeIn(),
                    exit = fadeOut(),
                ) {
                    Text(
                        text = stringResource(R.string.no_apps),
                        modifier = Modifier.padding(24.dp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(top = 4.dp, bottom = 92.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    items(visibleApps, key = { it.packageName }) { app ->
                        AppSelectionRow(
                            app = app,
                            selected = app.packageName in selectedPackages,
                            onSelectedChange = { selected ->
                                selectedPackages = if (selected) {
                                    val selectedBefore = displayOrder.filter {
                                        it in selectedPackages && it != app.packageName
                                    }
                                    val remaining = displayOrder.filter {
                                        it !in selectedPackages && it != app.packageName
                                    }
                                    displayOrder = listOf(app.packageName) + selectedBefore + remaining
                                    selectedPackages + app.packageName
                                } else {
                                    selectedPackages - app.packageName
                                }
                            },
                            modifier = Modifier.animateItem(
                                fadeInSpec = spring(stiffness = Spring.StiffnessMediumLow),
                                placementSpec = spring(
                                    dampingRatio = 0.86f,
                                    stiffness = Spring.StiffnessMediumLow,
                                ),
                                fadeOutSpec = spring(stiffness = Spring.StiffnessMediumLow),
                            ),
                        )
                    }
                }
            }
        }

        FloatingActionButton(
            onClick = ::saveSelection,
            modifier = Modifier.align(Alignment.BottomEnd).padding(20.dp),
            containerColor = if (hasUnsavedChanges) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.surfaceContainerHighest
            },
            contentColor = if (hasUnsavedChanges) {
                MaterialTheme.colorScheme.onPrimary
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            },
        ) {
            Text("✓", style = MaterialTheme.typography.headlineSmall)
        }
    }

    if (showExitConfirmation) {
        AlertDialog(
            onDismissRequest = { showExitConfirmation = false },
            title = { Text(stringResource(R.string.unsaved_whitelist_title)) },
            text = { Text(stringResource(R.string.unsaved_whitelist_message)) },
            confirmButton = {
                TextButton(onClick = {
                    showExitConfirmation = false
                    saveSelection()
                    onBack()
                }) { Text(stringResource(R.string.save_and_exit)) }
            },
            dismissButton = {
                Row {
                    TextButton(onClick = {
                        showExitConfirmation = false
                        onBack()
                    }) { Text(stringResource(R.string.discard_changes)) }
                    TextButton(onClick = { showExitConfirmation = false }) {
                        Text(stringResource(R.string.continue_editing))
                    }
                }
            },
        )
    }
}

@Composable
private fun AppSelectionRow(
    app: InstalledApp,
    selected: Boolean,
    onSelectedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    val icon = remember(app.packageName) {
        app.icon.toBitmap(width = 96, height = 96).asImageBitmap()
    }
    Card(modifier = modifier.fillMaxWidth().clickable { onSelectedChange(!selected) }) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Image(bitmap = icon, contentDescription = null, modifier = Modifier.size(44.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(text = app.label, style = MaterialTheme.typography.bodyLarge)
                Text(
                    text = app.packageName,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Checkbox(checked = selected, onCheckedChange = onSelectedChange)
        }
    }
}

private fun loadLaunchableApps(packageManager: PackageManager): List<InstalledApp> {
    val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
    return packageManager.queryIntentActivities(intent, PackageManager.ResolveInfoFlags.of(0))
        .mapNotNull { resolveInfo ->
            val activityInfo = resolveInfo.activityInfo ?: return@mapNotNull null
            val applicationInfo = activityInfo.applicationInfo ?: return@mapNotNull null
            InstalledApp(
                label = resolveInfo.loadLabel(packageManager).toString(),
                packageName = activityInfo.packageName,
                icon = resolveInfo.loadIcon(packageManager),
                isSystemApp = applicationInfo.flags and
                    (ApplicationInfo.FLAG_SYSTEM or ApplicationInfo.FLAG_UPDATED_SYSTEM_APP) != 0,
            )
        }
        .distinctBy { it.packageName }
        .sortedWith(
            compareBy<InstalledApp> { it.label.lowercase(Locale.getDefault()) }
                .thenBy { it.packageName },
        )
}
