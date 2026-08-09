package io.github.hmqyhm.hyperoskeyboardfix.ui.settings

import android.view.KeyEvent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import io.github.hmqyhm.hyperoskeyboardfix.R
import io.github.hmqyhm.hyperoskeyboardfix.model.MacroKeyStep

@Composable
internal fun MacroKeyboardRecorderDialog(
    initialSteps: List<MacroKeyStep>,
    onDismiss: () -> Unit,
    onConfirm: (List<MacroKeyStep>) -> Unit,
) {
    val steps = remember { mutableStateListOf<MacroKeyStep>().apply { addAll(initialSteps) } }
    var activeMeta by remember { mutableIntStateOf(0) }
    var scale by remember { mutableFloatStateOf(0.5f) }
    var offsetX by remember { mutableFloatStateOf(0f) }
    var offsetY by remember { mutableFloatStateOf(0f) }
    val transformState = rememberTransformableState { zoom, pan, _ ->
        scale = (scale * zoom).coerceIn(0.2f, 2.5f)
        offsetX += pan.x
        offsetY += pan.y
    }

    fun addKey(keyCode: Int) {
        val modifier = modifierBit(keyCode)
        if (modifier != 0) {
            activeMeta = activeMeta xor modifier
        } else {
            steps += MacroKeyStep(
                keyCode = keyCode,
                metaState = activeMeta,
                delayAfterMs = DEFAULT_STEP_DELAY_MS,
                holdDurationMs = DEFAULT_HOLD_MS,
            )
            activeMeta = 0
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Surface(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    TextButton(onClick = onDismiss) {
                        Text(stringResource(R.string.cancel))
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            stringResource(R.string.macro_keyboard_title),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                        )
                        Text(
                            stringResource(R.string.macro_keyboard_zoom_hint),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Button(onClick = {
                        onConfirm(steps.map(::normalizeStepTiming))
                    }) {
                        Text(stringResource(R.string.save_selection))
                    }
                }

                BoxWithConstraints(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(0.58f)
                        .clipToBounds()
                        .background(
                            MaterialTheme.colorScheme.surfaceVariant,
                            RoundedCornerShape(16.dp),
                        )
                        .transformable(transformState),
                    contentAlignment = Alignment.Center,
                ) {
                    val density = LocalDensity.current
                    val fitScale = with(density) {
                        (maxWidth.toPx() / KEYBOARD_CANVAS_WIDTH.dp.toPx())
                            .coerceIn(0.2f, 0.82f)
                    }
                    LaunchedEffect(fitScale) {
                        scale = fitScale
                        offsetX = 0f
                        offsetY = 0f
                    }
                    KeyboardCanvas(
                        activeMeta = activeMeta,
                        onKey = ::addKey,
                        modifier = Modifier.graphicsLayer {
                            scaleX = scale
                            scaleY = scale
                            translationX = offsetX
                            translationY = offsetY
                        },
                    )
                }

                Text(
                    stringResource(R.string.macro_recorded_count, steps.size),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                if (steps.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(0.32f),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(stringResource(R.string.macro_recording_empty))
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.weight(0.32f),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        itemsIndexed(steps, key = { index, _ -> index }) { index, step ->
                            RecordedStepRow(
                                index = index,
                                step = step,
                                onChange = { steps[index] = it },
                                onMoveUp = if (index > 0) {
                                    {
                                        val previous = steps[index - 1]
                                        steps[index - 1] = steps[index]
                                        steps[index] = previous
                                    }
                                } else null,
                                onMoveDown = if (index < steps.lastIndex) {
                                    {
                                        val next = steps[index + 1]
                                        steps[index + 1] = steps[index]
                                        steps[index] = next
                                    }
                                } else null,
                                onDelete = { steps.removeAt(index) },
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun KeyboardCanvas(
    activeMeta: Int,
    onKey: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .width(KEYBOARD_CANVAS_WIDTH.dp)
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(7.dp),
    ) {
        keyboardRows.forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                row.forEach { key ->
                    if (key.keyCode == SPACER) {
                        Spacer(Modifier.width((KEY_WIDTH * key.units + 6 * (key.units - 1)).dp))
                    } else {
                        val selected = modifierBit(key.keyCode).let {
                            it != 0 && activeMeta and it != 0
                        }
                        Box(
                            modifier = Modifier
                                .width((KEY_WIDTH * key.units + 6 * (key.units - 1)).dp)
                                .height(48.dp)
                                .background(
                                    if (selected) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.inverseSurface,
                                    RoundedCornerShape(6.dp),
                                )
                                .border(
                                    1.dp,
                                    MaterialTheme.colorScheme.outlineVariant,
                                    RoundedCornerShape(6.dp),
                                )
                                .clickable { onKey(key.keyCode) }
                                .padding(3.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                key.label,
                                color = if (selected) MaterialTheme.colorScheme.onPrimary
                                else MaterialTheme.colorScheme.inverseOnSurface,
                                style = MaterialTheme.typography.labelSmall,
                                textAlign = TextAlign.Center,
                            )
                        }
                    }
                }
            }
        }
        Box(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.align(Alignment.CenterEnd),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(7.dp),
            ) {
                DirectionKey("↑", KeyEvent.KEYCODE_DPAD_UP, onKey)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    DirectionKey("←", KeyEvent.KEYCODE_DPAD_LEFT, onKey)
                    DirectionKey("↓", KeyEvent.KEYCODE_DPAD_DOWN, onKey)
                    DirectionKey("→", KeyEvent.KEYCODE_DPAD_RIGHT, onKey)
                }
            }
        }
    }
}

@Composable
private fun DirectionKey(
    label: String,
    keyCode: Int,
    onKey: (Int) -> Unit,
) {
    Box(
        modifier = Modifier
            .size(KEY_WIDTH.dp)
            .background(
                MaterialTheme.colorScheme.inverseSurface,
                RoundedCornerShape(6.dp),
            )
            .border(
                1.dp,
                MaterialTheme.colorScheme.outlineVariant,
                RoundedCornerShape(6.dp),
            )
            .clickable { onKey(keyCode) },
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            color = MaterialTheme.colorScheme.inverseOnSurface,
            style = MaterialTheme.typography.titleMedium,
        )
    }
}

@Composable
private fun RecordedStepRow(
    index: Int,
    step: MacroKeyStep,
    onChange: (MacroKeyStep) -> Unit,
    onMoveUp: (() -> Unit)?,
    onMoveDown: (() -> Unit)?,
    onDelete: () -> Unit,
) {
    var holdText by remember(step.holdDurationMs) {
        mutableStateOf(step.holdDurationMs.toString())
    }
    var delayText by remember(step.delayAfterMs) {
        mutableStateOf(step.delayAfterMs.toString())
    }
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceContainer,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text("${index + 1}. ${stepDisplayName(step)}", modifier = Modifier.weight(1f))
            OutlinedTextField(
                value = holdText,
                onValueChange = {
                    holdText = it.filter(Char::isDigit)
                    holdText.toLongOrNull()?.let { value ->
                        onChange(step.copy(holdDurationMs = value.coerceAtMost(MAX_TIMING_MS)))
                    }
                },
                label = { Text(stringResource(R.string.macro_hold_short)) },
                modifier = Modifier
                    .width(92.dp)
                    .onFocusChanged { state ->
                        if (!state.isFocused) {
                            val normalized = normalizeTiming(holdText)
                            holdText = normalized.toString()
                            onChange(step.copy(holdDurationMs = normalized))
                        }
                    },
                singleLine = true,
            )
            OutlinedTextField(
                value = delayText,
                onValueChange = {
                    delayText = it.filter(Char::isDigit)
                    delayText.toLongOrNull()?.let { value ->
                        onChange(step.copy(delayAfterMs = value.coerceAtMost(MAX_TIMING_MS)))
                    }
                },
                label = { Text(stringResource(R.string.macro_delay_short)) },
                modifier = Modifier
                    .width(92.dp)
                    .onFocusChanged { state ->
                        if (!state.isFocused) {
                            val normalized = normalizeTiming(delayText)
                            delayText = normalized.toString()
                            onChange(step.copy(delayAfterMs = normalized))
                        }
                    },
                singleLine = true,
            )
            TextButton(onClick = { onMoveUp?.invoke() }, enabled = onMoveUp != null) { Text("↑") }
            TextButton(onClick = { onMoveDown?.invoke() }, enabled = onMoveDown != null) { Text("↓") }
            TextButton(onClick = onDelete) { Text("×") }
        }
    }
}

private fun normalizeTiming(value: String): Long =
    value.toLongOrNull()?.coerceIn(MIN_TIMING_MS, MAX_TIMING_MS) ?: MIN_TIMING_MS

private fun normalizeStepTiming(step: MacroKeyStep): MacroKeyStep = step.copy(
    holdDurationMs = step.holdDurationMs.coerceIn(MIN_TIMING_MS, MAX_TIMING_MS),
    delayAfterMs = step.delayAfterMs.coerceIn(MIN_TIMING_MS, MAX_TIMING_MS),
)

internal fun stepDisplayName(step: MacroKeyStep): String =
    triggerName(step.keyCode, step.metaState)

private data class KeyboardKey(val label: String, val keyCode: Int, val units: Int = 1)

private fun k(label: String, keyCode: Int, units: Int = 1) = KeyboardKey(label, keyCode, units)
private fun s(units: Int = 1) = KeyboardKey("", SPACER, units)

private val keyboardRows = listOf(
    listOf(
        k("ESC", KeyEvent.KEYCODE_ESCAPE), s(),
        k("F1", KeyEvent.KEYCODE_F1), k("F2", KeyEvent.KEYCODE_F2),
        k("F3", KeyEvent.KEYCODE_F3), k("F4", KeyEvent.KEYCODE_F4), s(),
        k("F5", KeyEvent.KEYCODE_F5), k("F6", KeyEvent.KEYCODE_F6),
        k("F7", KeyEvent.KEYCODE_F7), k("F8", KeyEvent.KEYCODE_F8), s(),
        k("F9", KeyEvent.KEYCODE_F9), k("F10", KeyEvent.KEYCODE_F10),
        k("F11", KeyEvent.KEYCODE_F11), k("F12", KeyEvent.KEYCODE_F12),
    ),
    listOf(
        k("~\n`", KeyEvent.KEYCODE_GRAVE), k("!\n1", KeyEvent.KEYCODE_1),
        k("@\n2", KeyEvent.KEYCODE_2), k("#\n3", KeyEvent.KEYCODE_3),
        k("$\n4", KeyEvent.KEYCODE_4), k("%\n5", KeyEvent.KEYCODE_5),
        k("^\n6", KeyEvent.KEYCODE_6), k("&\n7", KeyEvent.KEYCODE_7),
        k("*\n8", KeyEvent.KEYCODE_8), k("(\n9", KeyEvent.KEYCODE_9),
        k(")\n0", KeyEvent.KEYCODE_0), k("_\n-", KeyEvent.KEYCODE_MINUS),
        k("+\n=", KeyEvent.KEYCODE_EQUALS), k("BACKSPACE", KeyEvent.KEYCODE_DEL, 2), s(),
        k("INS", KeyEvent.KEYCODE_INSERT), k("HOME", KeyEvent.KEYCODE_MOVE_HOME),
        k("PGUP", KeyEvent.KEYCODE_PAGE_UP),
    ),
    listOf(
        k("TAB", KeyEvent.KEYCODE_TAB, 2), k("Q", KeyEvent.KEYCODE_Q),
        k("W", KeyEvent.KEYCODE_W), k("E", KeyEvent.KEYCODE_E), k("R", KeyEvent.KEYCODE_R),
        k("T", KeyEvent.KEYCODE_T), k("Y", KeyEvent.KEYCODE_Y), k("U", KeyEvent.KEYCODE_U),
        k("I", KeyEvent.KEYCODE_I), k("O", KeyEvent.KEYCODE_O), k("P", KeyEvent.KEYCODE_P),
        k("{\n[", KeyEvent.KEYCODE_LEFT_BRACKET), k("}\n]", KeyEvent.KEYCODE_RIGHT_BRACKET),
        k("|\n\\", KeyEvent.KEYCODE_BACKSLASH), s(),
        k("DEL", KeyEvent.KEYCODE_FORWARD_DEL), k("END", KeyEvent.KEYCODE_MOVE_END),
        k("PGDN", KeyEvent.KEYCODE_PAGE_DOWN),
    ),
    listOf(
        k("CAPS LOCK", KeyEvent.KEYCODE_CAPS_LOCK, 2), k("A", KeyEvent.KEYCODE_A),
        k("S", KeyEvent.KEYCODE_S), k("D", KeyEvent.KEYCODE_D), k("F", KeyEvent.KEYCODE_F),
        k("G", KeyEvent.KEYCODE_G), k("H", KeyEvent.KEYCODE_H), k("J", KeyEvent.KEYCODE_J),
        k("K", KeyEvent.KEYCODE_K), k("L", KeyEvent.KEYCODE_L),
        k(":\n;", KeyEvent.KEYCODE_SEMICOLON), k("\"\n'", KeyEvent.KEYCODE_APOSTROPHE),
        k("ENTER", KeyEvent.KEYCODE_ENTER, 3),
    ),
    listOf(
        k("SHIFT", KeyEvent.KEYCODE_SHIFT_LEFT, 3), k("Z", KeyEvent.KEYCODE_Z),
        k("X", KeyEvent.KEYCODE_X), k("C", KeyEvent.KEYCODE_C), k("V", KeyEvent.KEYCODE_V),
        k("B", KeyEvent.KEYCODE_B), k("N", KeyEvent.KEYCODE_N), k("M", KeyEvent.KEYCODE_M),
        k("<\n,", KeyEvent.KEYCODE_COMMA), k(">\n.", KeyEvent.KEYCODE_PERIOD),
        k("?\n/", KeyEvent.KEYCODE_SLASH), k("SHIFT", KeyEvent.KEYCODE_SHIFT_RIGHT, 3),
    ),
    listOf(
        k("CTRL", KeyEvent.KEYCODE_CTRL_LEFT), k("META", KeyEvent.KEYCODE_META_LEFT, 2),
        k("ALT", KeyEvent.KEYCODE_ALT_LEFT), k("SPACE", KeyEvent.KEYCODE_SPACE, 7),
        k("ALT", KeyEvent.KEYCODE_ALT_RIGHT), k("META", KeyEvent.KEYCODE_META_RIGHT, 2),
        k("MENU", KeyEvent.KEYCODE_MENU), k("CTRL", KeyEvent.KEYCODE_CTRL_RIGHT),
    ),
)

private fun modifierBit(keyCode: Int): Int = when (keyCode) {
    KeyEvent.KEYCODE_CTRL_LEFT, KeyEvent.KEYCODE_CTRL_RIGHT -> KeyEvent.META_CTRL_ON
    KeyEvent.KEYCODE_ALT_LEFT, KeyEvent.KEYCODE_ALT_RIGHT -> KeyEvent.META_ALT_ON
    KeyEvent.KEYCODE_SHIFT_LEFT, KeyEvent.KEYCODE_SHIFT_RIGHT -> KeyEvent.META_SHIFT_ON
    KeyEvent.KEYCODE_META_LEFT, KeyEvent.KEYCODE_META_RIGHT -> KeyEvent.META_META_ON
    else -> 0
}

private const val SPACER = -10_000
private const val KEY_WIDTH = 48
private const val KEYBOARD_CANVAS_WIDTH = 1200
private const val DEFAULT_HOLD_MS = 40L
private const val DEFAULT_STEP_DELAY_MS = 50L
private const val MIN_TIMING_MS = 10L
private const val MAX_TIMING_MS = 10_000L
