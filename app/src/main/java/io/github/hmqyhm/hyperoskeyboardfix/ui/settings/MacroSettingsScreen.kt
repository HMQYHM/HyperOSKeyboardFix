package io.github.hmqyhm.hyperoskeyboardfix.ui.settings

import android.content.Context
import android.content.Intent
import android.view.KeyEvent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import io.github.hmqyhm.hyperoskeyboardfix.BuildConfig
import io.github.hmqyhm.hyperoskeyboardfix.R
import io.github.hmqyhm.hyperoskeyboardfix.config.ModulePreferences
import io.github.hmqyhm.hyperoskeyboardfix.model.KeyboardMacro
import io.github.hmqyhm.hyperoskeyboardfix.model.MacroActionStep
import io.github.hmqyhm.hyperoskeyboardfix.model.MacroActionStepType
import io.github.hmqyhm.hyperoskeyboardfix.model.MacroActionType
import io.github.hmqyhm.hyperoskeyboardfix.model.MacroKeyStep
import io.github.hmqyhm.hyperoskeyboardfix.model.MacroMouseStep
import io.github.hmqyhm.hyperoskeyboardfix.model.MacroRepeatMode
import java.util.UUID
import java.io.File
import org.json.JSONObject

@Composable
fun MacroSettingsScreen(
    preferences: ModulePreferences,
    contentPadding: PaddingValues,
) {
    val context = LocalContext.current
    var macros by remember { mutableStateOf(preferences.keyboardMacros()) }
    var editingMacro by remember { mutableStateOf<KeyboardMacro?>(null) }
    var creating by remember { mutableStateOf(false) }
    var importing by remember { mutableStateOf(false) }
    var showImportWarning by remember { mutableStateOf(false) }
    var importError by remember { mutableStateOf<String?>(null) }
    var pendingDelete by remember { mutableStateOf<KeyboardMacro?>(null) }
    var macrosEnabled by remember { mutableStateOf(preferences.areKeyboardMacrosEnabled()) }
    val importLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent(),
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        try {
            val text = context.contentResolver.openInputStream(uri)
                ?.bufferedReader()?.use { it.readText() }
                ?: error("empty file")
            editingMacro = KeyboardMacro.fromJson(JSONObject(text.trim())).copy(
                id = UUID.randomUUID().toString(),
            )
            creating = true
            importing = true
        } catch (error: Throwable) {
            importError = error.javaClass.simpleName
        }
    }

    fun persist(updated: List<KeyboardMacro>) {
        macros = updated
        preferences.saveKeyboardMacros(updated)
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(contentPadding),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Column {
                Text(
                    stringResource(R.string.roadmap_keyboard_macro),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    stringResource(R.string.macro_screen_description),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.padding(horizontal = 18.dp, vertical = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(3.dp),
                    ) {
                        Text(
                            stringResource(R.string.keyboard_macro_switch),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                        )
                        Text(
                            stringResource(R.string.keyboard_macro_switch_description),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Switch(
                        checked = macrosEnabled,
                        onCheckedChange = {
                            macrosEnabled = it
                            preferences.setKeyboardMacrosEnabled(it)
                        },
                    )
                }
            }
        }
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Button(
                    onClick = {
                        importing = false
                        creating = true
                    },
                    modifier = Modifier.weight(1f),
                ) { Text(stringResource(R.string.macro_add)) }
                OutlinedButton(
                    onClick = { showImportWarning = true },
                    modifier = Modifier.weight(1f),
                ) { Text(stringResource(R.string.macro_import)) }
            }
        }
        if (macros.isEmpty()) {
            item { Text(stringResource(R.string.macro_empty)) }
        } else {
            items(macros, key = { it.id }) { macro ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            importing = false
                            editingMacro = macro
                        },
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(macro.name, fontWeight = FontWeight.SemiBold)
                                val target = if (macro.packageNames.isEmpty()) {
                                    stringResource(R.string.macro_global)
                                } else {
                                    macro.packageNames.sorted().joinToString()
                                }
                                Text(
                                    "${triggerName(macro.triggerKeyCode, macro.triggerMetaState)} · $target",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            Switch(
                                checked = macro.enabled,
                                onCheckedChange = { checked ->
                                    persist(macros.map {
                                        if (it.id == macro.id) it.copy(enabled = checked) else it
                                    })
                                },
                            )
                        }
                        Text(
                            stringResource(
                                R.string.macro_mixed_summary,
                                macro.effectiveActions().size,
                            ),
                            style = MaterialTheme.typography.bodyMedium,
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(onClick = {
                                importing = false
                                editingMacro = macro
                            }) {
                                Text(stringResource(R.string.macro_edit))
                            }
                            OutlinedButton(onClick = {
                                shareMacroAsTextFile(context, macro)
                            }) { Text(stringResource(R.string.macro_share)) }
                            TextButton(onClick = {
                                pendingDelete = macro
                            }) { Text(stringResource(R.string.macro_delete)) }
                        }
                    }
                }
            }
        }
    }

    if (creating || editingMacro != null) {
        MacroEditorDialog(
            initial = editingMacro,
            whitelist = preferences.whitelist(),
            onDismiss = {
                creating = false
                importing = false
                editingMacro = null
            },
            onSave = { macro ->
                val replacedBackground = editingMacro?.mouseBackgroundFile
                persist(
                    if (editingMacro == null || importing) macros + macro
                    else macros.map { if (it.id == macro.id) macro else it },
                )
                if (!importing && replacedBackground != macro.mouseBackgroundFile) {
                    deleteMacroBackground(context, replacedBackground)
                }
                creating = false
                importing = false
                editingMacro = null
            },
        )
    }

    if (showImportWarning) {
        AlertDialog(
            onDismissRequest = { showImportWarning = false },
            title = { Text(stringResource(R.string.macro_import_warning_title)) },
            text = { Text(stringResource(R.string.macro_import_warning_message)) },
            confirmButton = {
                TextButton(onClick = {
                    showImportWarning = false
                    importLauncher.launch("text/plain")
                }) { Text(stringResource(R.string.macro_import_continue)) }
            },
            dismissButton = {
                TextButton(onClick = { showImportWarning = false }) {
                    Text(stringResource(R.string.cancel))
                }
            },
        )
    }
    importError?.let { reason ->
        AlertDialog(
            onDismissRequest = { importError = null },
            title = { Text(stringResource(R.string.macro_import_failed)) },
            text = { Text(reason) },
            confirmButton = {
                TextButton(onClick = { importError = null }) { Text(stringResource(R.string.confirm)) }
            },
        )
    }
    pendingDelete?.let { macro ->
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            title = { Text(stringResource(R.string.macro_delete_confirm_title)) },
            text = { Text(stringResource(R.string.macro_delete_confirm_message, macro.name)) },
            confirmButton = {
                TextButton(onClick = {
                    persist(macros.filterNot { it.id == macro.id })
                    deleteMacroBackground(context, macro.mouseBackgroundFile)
                    pendingDelete = null
                }) { Text(stringResource(R.string.confirm_delete)) }
            },
            dismissButton = {
                TextButton(onClick = { pendingDelete = null }) {
                    Text(stringResource(R.string.cancel))
                }
            },
        )
    }
}

@Composable
private fun MacroEditorDialog(
    initial: KeyboardMacro?,
    whitelist: Set<String>,
    onDismiss: () -> Unit,
    onSave: (KeyboardMacro) -> Unit,
) {
    val context = LocalContext.current
    var name by remember(initial?.id) { mutableStateOf(initial?.name.orEmpty()) }
    var selectedPackages by remember(initial?.id) {
        mutableStateOf(initial?.packageNames.orEmpty())
    }
    var triggerKeyCode by remember(initial?.id) {
        mutableIntStateOf(initial?.triggerKeyCode ?: 0)
    }
    var triggerMetaState by remember(initial?.id) {
        mutableIntStateOf(initial?.triggerMetaState ?: 0)
    }
    var actions by remember(initial?.id) {
        mutableStateOf(initial?.effectiveActions().orEmpty())
    }
    var textValue by remember(initial?.id) { mutableStateOf("") }
    var mouseBackgroundFile by remember(initial?.id) {
        mutableStateOf(initial?.mouseBackgroundFile)
    }
    var repeatMode by remember(initial?.id) {
        mutableStateOf(initial?.repeatMode ?: MacroRepeatMode.ONCE)
    }
    var repeatCount by remember(initial?.id) {
        mutableStateOf((initial?.repeatCount ?: 2).toString())
    }
    var repeatInterval by remember(initial?.id) {
        mutableStateOf((initial?.repeatIntervalMs ?: 80L).toString())
    }
    var error by remember { mutableStateOf(false) }
    var showRecorder by remember { mutableStateOf(false) }
    var showMouseRecorder by remember { mutableStateOf(false) }
    var showAppChooser by remember { mutableStateOf(false) }
    var showCancelConfirmation by remember { mutableStateOf(false) }
    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) { focusRequester.requestFocus() }

    AlertDialog(
        onDismissRequest = { showCancelConfirmation = true },
        title = {
            Text(stringResource(if (initial == null) R.string.macro_add else R.string.macro_edit))
        },
        text = {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                item {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text(stringResource(R.string.macro_name)) },
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
                item {
                    OutlinedTextField(
                        value = if (triggerKeyCode == 0) "" else triggerName(
                            triggerKeyCode,
                            triggerMetaState,
                        ),
                        onValueChange = {},
                        readOnly = true,
                        label = { Text(stringResource(R.string.macro_press_trigger)) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .focusRequester(focusRequester)
                            .onPreviewKeyEvent { composeEvent ->
                                val event = composeEvent.nativeKeyEvent
                                if (event.action == KeyEvent.ACTION_DOWN && event.keyCode !in MODIFIER_KEYS) {
                                    triggerKeyCode = event.keyCode
                                    triggerMetaState = normalizeMeta(event.metaState)
                                }
                                true
                            }
                            .focusable(),
                    )
                }
                item {
                    OutlinedButton(
                        onClick = { showAppChooser = true },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(
                            stringResource(
                                R.string.macro_choose_apps_count,
                                selectedPackages.size,
                            ),
                        )
                    }
                    Text(
                        stringResource(R.string.macro_choose_apps_hint),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        OutlinedTextField(
                            value = textValue,
                            onValueChange = { textValue = it },
                            label = { Text(stringResource(R.string.macro_text_content)) },
                            modifier = Modifier.weight(1f),
                        )
                        OutlinedButton(
                            enabled = textValue.isNotBlank(),
                            onClick = {
                                actions = actions + MacroActionStep.text(textValue)
                                textValue = ""
                            },
                        ) { Text(stringResource(R.string.macro_add_text_action)) }
                        OutlinedButton(onClick = { showRecorder = true }) {
                            Icon(
                                painter = painterResource(R.drawable.ic_keyboard),
                                contentDescription = stringResource(R.string.macro_keyboard_title),
                            )
                        }
                        OutlinedButton(onClick = { showMouseRecorder = true }) {
                            Icon(
                                painter = painterResource(R.drawable.ic_mouse_toolbar),
                                contentDescription = stringResource(R.string.macro_mouse_canvas_title),
                            )
                        }
                    }
                }
                item {
                    Text(
                        stringResource(R.string.macro_unified_timeline, actions.size),
                        style = MaterialTheme.typography.titleMedium,
                    )
                }
                if (actions.isEmpty()) {
                    item {
                        Text(
                            stringResource(R.string.macro_unified_empty),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                } else {
                    items(actions.size) { index ->
                        val action = actions[index]
                        Card(modifier = Modifier.fillMaxWidth()) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                            ) {
                                Text("${index + 1}. ${macroActionDisplayName(action)}", Modifier.weight(1f))
                                TextButton(
                                    enabled = index > 0,
                                    onClick = { actions = actions.moveItem(index, index - 1) },
                                ) {
                                    Icon(
                                        painterResource(R.drawable.ic_arrow_up),
                                        contentDescription = null,
                                    )
                                }
                                TextButton(
                                    enabled = index < actions.lastIndex,
                                    onClick = { actions = actions.moveItem(index, index + 1) },
                                ) {
                                    Icon(
                                        painterResource(R.drawable.ic_arrow_down),
                                        contentDescription = null,
                                    )
                                }
                                TextButton(onClick = {
                                    actions = actions.toMutableList().also { it.removeAt(index) }
                                }) { Text(stringResource(R.string.macro_delete)) }
                            }
                        }
                    }
                }
                item {
                    OutlinedTextField(
                        value = repeatInterval,
                        onValueChange = { repeatInterval = it.filter(Char::isDigit) },
                        label = { Text(stringResource(R.string.macro_repeat_interval)) },
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
                item {
                    Column {
                        Text(stringResource(R.string.macro_repeat_mode))
                        MacroRepeatMode.entries.forEach { mode ->
                            FilterChip(
                                selected = repeatMode == mode,
                                onClick = { repeatMode = mode },
                                label = { Text(repeatModeLabel(mode)) },
                            )
                        }
                    }
                }
                if (repeatMode == MacroRepeatMode.FIXED) {
                    item {
                        OutlinedTextField(
                            value = repeatCount,
                            onValueChange = { repeatCount = it.filter(Char::isDigit) },
                            label = { Text(stringResource(R.string.macro_repeat_count)) },
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }
                if (error) {
                    item {
                        Text(
                            stringResource(R.string.macro_invalid),
                            color = MaterialTheme.colorScheme.error,
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val validAction = actions.isNotEmpty()
                if (name.isBlank() || triggerKeyCode == 0 || !validAction) {
                    error = true
                } else {
                    onSave(
                        KeyboardMacro(
                            id = initial?.id ?: UUID.randomUUID().toString(),
                            name = name.trim(),
                            packageName = selectedPackages.singleOrNull(),
                            packageNames = selectedPackages,
                            triggerKeyCode = triggerKeyCode,
                            triggerMetaState = triggerMetaState,
                            actionType = MacroActionType.SEQUENCE,
                            text = "",
                            steps = emptyList(),
                            mouseSteps = emptyList(),
                            mouseBackgroundFile = mouseBackgroundFile,
                            actions = actions,
                            repeatMode = repeatMode,
                            repeatCount = repeatCount.toIntOrNull()?.coerceIn(1, 100) ?: 1,
                            repeatIntervalMs = repeatInterval.toLongOrNull()
                                ?.coerceIn(0L, 10_000L) ?: 80L,
                            enabled = initial?.enabled ?: true,
                        ),
                    )
                }
            }) { Text(stringResource(R.string.save_selection)) }
        },
        dismissButton = {
            TextButton(onClick = { showCancelConfirmation = true }) {
                Text(stringResource(R.string.cancel))
            }
        },
    )

    if (showCancelConfirmation) {
        AlertDialog(
            onDismissRequest = { showCancelConfirmation = false },
            title = { Text(stringResource(R.string.macro_cancel_edit_title)) },
            text = { Text(stringResource(R.string.macro_cancel_edit_message)) },
            confirmButton = {
                TextButton(onClick = {
                    showCancelConfirmation = false
                    if (mouseBackgroundFile != initial?.mouseBackgroundFile) {
                        deleteMacroBackground(context, mouseBackgroundFile)
                    }
                    onDismiss()
                }) { Text(stringResource(R.string.confirm_discard_macro)) }
            },
            dismissButton = {
                TextButton(onClick = { showCancelConfirmation = false }) {
                    Text(stringResource(R.string.continue_editing))
                }
            },
        )
    }

    if (showRecorder) {
        MacroKeyboardRecorderDialog(
            initialSteps = actions.mapNotNull { it.keyStep },
            onDismiss = { showRecorder = false },
            onConfirm = {
                actions = replaceActionsOfType(
                    actions,
                    MacroActionStepType.KEY,
                    it.map(MacroActionStep::key),
                )
                showRecorder = false
            },
        )
    }

    if (showMouseRecorder) {
        MouseMacroCanvasDialog(
            initialSteps = actions.mapNotNull { it.mouseStep },
            initialBackgroundFile = mouseBackgroundFile,
            onDismiss = { showMouseRecorder = false },
            onConfirm = { recordedSteps, backgroundFile ->
                if (mouseBackgroundFile != initial?.mouseBackgroundFile &&
                    mouseBackgroundFile != backgroundFile
                ) {
                    deleteMacroBackground(context, mouseBackgroundFile)
                }
                actions = replaceActionsOfType(
                    actions,
                    MacroActionStepType.MOUSE,
                    recordedSteps.map(MacroActionStep::mouse),
                )
                mouseBackgroundFile = backgroundFile
                showMouseRecorder = false
            },
        )
    }

    if (showAppChooser) {
        var draftSelection by remember(showAppChooser) { mutableStateOf(selectedPackages) }
        AlertDialog(
            onDismissRequest = { showAppChooser = false },
            title = { Text(stringResource(R.string.macro_choose_apps)) },
            text = {
                if (whitelist.isEmpty()) {
                    Text(stringResource(R.string.macro_whitelist_empty))
                } else {
                    LazyColumn {
                        items(whitelist.sorted()) { packageName ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        draftSelection = if (packageName in draftSelection) {
                                            draftSelection - packageName
                                        } else {
                                            draftSelection + packageName
                                        }
                                    },
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Checkbox(
                                    checked = packageName in draftSelection,
                                    onCheckedChange = null,
                                )
                                Text(packageName)
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    selectedPackages = draftSelection
                    showAppChooser = false
                }) { Text(stringResource(R.string.save_selection)) }
            },
            dismissButton = {
                TextButton(onClick = { showAppChooser = false }) {
                    Text(stringResource(R.string.cancel))
                }
            },
        )
    }
}

@Composable
private fun repeatModeLabel(mode: MacroRepeatMode): String = stringResource(
    when (mode) {
        MacroRepeatMode.ONCE -> R.string.macro_repeat_once
        MacroRepeatMode.FIXED -> R.string.macro_repeat_fixed
        MacroRepeatMode.WHILE_HELD -> R.string.macro_repeat_held
        MacroRepeatMode.TOGGLE -> R.string.macro_repeat_toggle
    },
)

@Composable
private fun macroActionDisplayName(action: MacroActionStep): String = when (action.type) {
    MacroActionStepType.TEXT ->
        "${stringResource(R.string.macro_action_text)}: ${action.text.take(32)}"
    MacroActionStepType.KEY -> action.keyStep?.let(::stepDisplayName).orEmpty()
    MacroActionStepType.MOUSE -> action.mouseStep?.let { mouseStepDisplayName(it) }.orEmpty()
}

private fun List<MacroActionStep>.moveItem(from: Int, to: Int): List<MacroActionStep> =
    toMutableList().also { items ->
        val item = items.removeAt(from)
        items.add(to, item)
    }

private fun replaceActionsOfType(
    current: List<MacroActionStep>,
    type: MacroActionStepType,
    replacements: List<MacroActionStep>,
): List<MacroActionStep> {
    val pending = ArrayDeque(replacements)
    val merged = buildList {
        current.forEach { action ->
            if (action.type == type) {
                pending.removeFirstOrNull()?.let(::add)
            } else {
                add(action)
            }
        }
        addAll(pending)
    }
    return merged
}

internal fun triggerName(keyCode: Int, metaState: Int): String = buildList {
    if (metaState and KeyEvent.META_CTRL_ON != 0) add("Ctrl")
    if (metaState and KeyEvent.META_ALT_ON != 0) add("Alt")
    if (metaState and KeyEvent.META_SHIFT_ON != 0) add("Shift")
    if (metaState and KeyEvent.META_META_ON != 0) add("Meta")
    add(KeyEvent.keyCodeToString(keyCode).removePrefix("KEYCODE_"))
}.joinToString(" + ")

private fun normalizeMeta(meta: Int): Int = meta and (
    KeyEvent.META_CTRL_ON or KeyEvent.META_ALT_ON or
        KeyEvent.META_SHIFT_ON or KeyEvent.META_META_ON
    )

private val MODIFIER_KEYS = setOf(
    KeyEvent.KEYCODE_CTRL_LEFT, KeyEvent.KEYCODE_CTRL_RIGHT,
    KeyEvent.KEYCODE_ALT_LEFT, KeyEvent.KEYCODE_ALT_RIGHT,
    KeyEvent.KEYCODE_SHIFT_LEFT, KeyEvent.KEYCODE_SHIFT_RIGHT,
    KeyEvent.KEYCODE_META_LEFT, KeyEvent.KEYCODE_META_RIGHT,
)

private fun shareMacroAsTextFile(context: Context, macro: KeyboardMacro) {
    val safeName = macro.name.trim()
        .replace(Regex("[\\\\/:*?\"<>|]"), "_")
        .ifBlank { "keyboard_macro" }
    val directory = File(context.cacheDir, "shared_macros").apply { mkdirs() }
    val file = File(directory, "$safeName.txt")
    file.writeText(
        macro.copy(mouseBackgroundFile = null).toJson().toString(2),
        Charsets.UTF_8,
    )
    val uri = FileProvider.getUriForFile(
        context,
        "${BuildConfig.APPLICATION_ID}.files",
        file,
    )
    val share = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_SUBJECT, macro.name)
        putExtra(Intent.EXTRA_STREAM, uri)
        clipData = android.content.ClipData.newUri(context.contentResolver, macro.name, uri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    context.startActivity(Intent.createChooser(share, context.getString(R.string.macro_share)))
}

private fun deleteMacroBackground(context: Context, fileName: String?) {
    if (fileName.isNullOrBlank() || '/' in fileName || '\\' in fileName) return
    runCatching {
        File(File(context.filesDir, "macro_mouse_backgrounds"), fileName).delete()
    }
}
