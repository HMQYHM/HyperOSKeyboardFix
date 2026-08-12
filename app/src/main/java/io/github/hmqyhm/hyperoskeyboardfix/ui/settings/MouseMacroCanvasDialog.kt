package io.github.hmqyhm.hyperoskeyboardfix.ui.settings

import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.graphics.Matrix
import android.graphics.Paint
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.annotation.DrawableRes
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChanged
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import io.github.hmqyhm.hyperoskeyboardfix.R
import io.github.hmqyhm.hyperoskeyboardfix.model.MacroMouseAction
import io.github.hmqyhm.hyperoskeyboardfix.model.MacroMouseStep
import java.io.File
import java.util.UUID
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.hypot
import kotlin.math.roundToInt

@Composable
internal fun MouseMacroCanvasDialog(
    initialSteps: List<MacroMouseStep>,
    initialBackgroundFile: String?,
    onDismiss: () -> Unit,
    onConfirm: (List<MacroMouseStep>, String?) -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var steps by remember { mutableStateOf(initialSteps) }
    var selectedTool by remember { mutableStateOf(MacroMouseAction.LEFT_CLICK) }
    var sidebarCollapsed by remember { mutableStateOf(false) }
    var editingIndex by remember { mutableIntStateOf(-1) }
    var draggingIndex by remember { mutableIntStateOf(-1) }
    var scale by remember { mutableFloatStateOf(1f) }
    var pan by remember { mutableStateOf(Offset.Zero) }
    var backgroundBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var backgroundChanged by remember { mutableStateOf(false) }
    var saving by remember { mutableStateOf(false) }
    var rootSize by remember { mutableStateOf(androidx.compose.ui.unit.IntSize.Zero) }
    var sidebarSize by remember { mutableStateOf(androidx.compose.ui.unit.IntSize.Zero) }
    var sidebarDockedRight by remember { mutableStateOf(false) }
    val density = LocalDensity.current
    var sidebarPosition by remember {
        mutableStateOf(Offset(with(density) { 14.dp.toPx() }, with(density) { 36.dp.toPx() }))
    }

    LaunchedEffect(initialBackgroundFile) {
        backgroundBitmap = withContext(Dispatchers.IO) {
            initialBackgroundFile?.let { fileName ->
                val file = File(mouseBackgroundDirectory(context.filesDir), fileName)
                runCatching { android.graphics.BitmapFactory.decodeFile(file.absolutePath) }.getOrNull()
            }
        }
    }

    val importBackground = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent(),
    ) { uri: Uri? ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            val decoded = withContext(Dispatchers.IO) {
                runCatching {
                    ImageDecoder.decodeBitmap(ImageDecoder.createSource(context.contentResolver, uri)) {
                        decoder, info, _ ->
                        decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
                        val largestSide = maxOf(info.size.width, info.size.height)
                        if (largestSide > MAX_BACKGROUND_SIDE) {
                            decoder.setTargetSampleSize(
                                ((largestSide + MAX_BACKGROUND_SIDE - 1) /
                                    MAX_BACKGROUND_SIDE).coerceAtLeast(1),
                            )
                        }
                    }
                }.getOrNull()
            }
            if (decoded != null) {
                backgroundBitmap = decoded
                backgroundChanged = true
                scale = 1f
                pan = Offset.Zero
            }
        }
    }

    val latestSteps by rememberUpdatedState(steps)
    val latestScale by rememberUpdatedState(scale)
    val latestPan by rememberUpdatedState(pan)
    val latestSelectedTool by rememberUpdatedState(selectedTool)
    val sidebarWidth by animateDpAsState(
        targetValue = if (sidebarCollapsed) 56.dp else 176.dp,
        animationSpec = spring(dampingRatio = 0.86f),
        label = "mouse_macro_sidebar_width",
    )
    val edgeMarginPx = with(density) { 10.dp.toPx() }

    LaunchedEffect(rootSize, sidebarSize, sidebarCollapsed, sidebarDockedRight) {
        if (rootSize.width <= 0 || sidebarSize.width <= 0) return@LaunchedEffect
        val maxX = (rootSize.width - sidebarSize.width).coerceAtLeast(0).toFloat()
        sidebarPosition = Offset(
            x = if (sidebarDockedRight) {
                (maxX - edgeMarginPx).coerceAtLeast(0f)
            } else {
                edgeMarginPx.coerceAtMost(maxX)
            },
            y = sidebarPosition.y.coerceIn(
                edgeMarginPx,
                (rootSize.height - sidebarSize.height - edgeMarginPx).coerceAtLeast(
                    edgeMarginPx,
                ),
            ),
        )
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false,
        ),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
                .onSizeChanged { rootSize = it },
        ) {
            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.surfaceContainerHighest)
                    .pointerInput(Unit) {
                        detectDragGesturesAfterLongPress(
                            onDragStart = { position ->
                                val center = Offset(size.width / 2f, size.height / 2f)
                                val contentPoint = Offset(
                                    x = (position.x - center.x - latestPan.x) /
                                        latestScale + center.x,
                                    y = (position.y - center.y - latestPan.y) /
                                        latestScale + center.y,
                                )
                                draggingIndex = findMarkerIndex(
                                    steps = latestSteps,
                                    xFraction = (contentPoint.x / size.width).coerceIn(0f, 1f),
                                    yFraction = (contentPoint.y / size.height).coerceIn(0f, 1f),
                                    scale = latestScale,
                                )
                            },
                            onDrag = { change, dragAmount ->
                                val index = draggingIndex
                                val currentSteps = latestSteps
                                if (index !in currentSteps.indices) {
                                    return@detectDragGesturesAfterLongPress
                                }
                                change.consume()
                                val current = currentSteps[index]
                                steps = currentSteps.toMutableList().also { updated ->
                                    updated[index] = current.copy(
                                        xFraction = (current.xFraction +
                                            dragAmount.x / latestScale / size.width)
                                            .coerceIn(0f, 1f),
                                        yFraction = (current.yFraction +
                                            dragAmount.y / latestScale / size.height)
                                            .coerceIn(0f, 1f),
                                    )
                                }
                            },
                            onDragEnd = { draggingIndex = -1 },
                            onDragCancel = { draggingIndex = -1 },
                        )
                    }
                    .pointerInput(Unit) {
                        detectTapGestures { tap ->
                            val center = Offset(size.width / 2f, size.height / 2f)
                            val contentPoint = Offset(
                                x = (tap.x - center.x - latestPan.x) /
                                    latestScale + center.x,
                                y = (tap.y - center.y - latestPan.y) /
                                    latestScale + center.y,
                            )
                            val normalizedX = (contentPoint.x / size.width).coerceIn(0f, 1f)
                            val normalizedY = (contentPoint.y / size.height).coerceIn(0f, 1f)
                            val hitIndex = findMarkerIndex(
                                latestSteps,
                                normalizedX,
                                normalizedY,
                                latestScale,
                            )
                            if (hitIndex >= 0) {
                                editingIndex = hitIndex
                            } else {
                                steps = latestSteps + MacroMouseStep(
                                    action = latestSelectedTool,
                                    xFraction = normalizedX,
                                    yFraction = normalizedY,
                                )
                            }
                        }
                    }
                    .pointerInput(Unit) {
                        awaitEachGesture {
                            var multiTouch = false
                            do {
                                val event = awaitPointerEvent()
                                if (event.changes.count { it.pressed } >= 2) {
                                    multiTouch = true
                                }
                                if (multiTouch) {
                                    val zoomChange = event.calculateZoom()
                                    val panChange = event.calculatePan()
                                    val updatedScale = (latestScale * zoomChange)
                                        .coerceIn(1f, 4f)
                                    scale = updatedScale
                                    pan = if (updatedScale <= 1.01f) {
                                        Offset.Zero
                                    } else {
                                        latestPan + panChange
                                    }
                                    event.changes.forEach { change ->
                                        if (change.positionChanged()) change.consume()
                                    }
                                }
                            } while (event.changes.any { it.pressed })
                        }
                    },
            ) {
                Box(
                    modifier = Modifier.fillMaxSize().graphicsLayer {
                        scaleX = scale
                        scaleY = scale
                        translationX = pan.x
                        translationY = pan.y
                    },
                ) {
                    val bitmap = backgroundBitmap
                    if (bitmap == null) {
                        Image(
                            painter = painterResource(R.drawable.macro_mouse_canvas_default),
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.FillBounds,
                        )
                    } else {
                        Image(
                            bitmap = bitmap.asImageBitmap(),
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Fit,
                        )
                    }
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        steps.forEachIndexed { index, step ->
                            val point = Offset(
                                step.xFraction * size.width,
                                step.yFraction * size.height,
                            )
                            val color = mouseActionColor(step.action)
                            drawCircle(color, radius = 18.dp.toPx(), center = point)
                            if (step.action == MacroMouseAction.SCROLL_UP ||
                                step.action == MacroMouseAction.SCROLL_DOWN
                            ) {
                                val direction = if (
                                    step.action == MacroMouseAction.SCROLL_UP
                                ) -1f else 1f
                                drawLine(
                                    color = color,
                                    start = point,
                                    end = point + Offset(
                                        0f,
                                        direction * step.distanceFraction * size.height,
                                    ),
                                    strokeWidth = 5.dp.toPx(),
                                )
                            }
                            drawIntoCanvas { canvas ->
                                val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                                    this.color = android.graphics.Color.WHITE
                                    textSize = 13.sp.toPx()
                                    textAlign = Paint.Align.CENTER
                                    typeface = android.graphics.Typeface.DEFAULT_BOLD
                                }
                                canvas.nativeCanvas.drawText(
                                    (index + 1).toString(),
                                    point.x,
                                    point.y - (paint.ascent() + paint.descent()) / 2f,
                                    paint,
                                )
                            }
                        }
                    }
                }
            }

            Surface(
                modifier = Modifier
                    .offset {
                        IntOffset(
                            sidebarPosition.x.roundToInt(),
                            sidebarPosition.y.roundToInt(),
                        )
                    }
                    .width(sidebarWidth)
                    .onSizeChanged { sidebarSize = it }
                    .then(
                        if (sidebarCollapsed) {
                            Modifier.pointerInput(rootSize, sidebarSize) {
                                detectDragGesturesAfterLongPress(
                                    onDrag = { change, dragAmount ->
                                        change.consume()
                                        val maxX = (rootSize.width - sidebarSize.width)
                                            .coerceAtLeast(0).toFloat()
                                        val maxY = (rootSize.height - sidebarSize.height)
                                            .coerceAtLeast(0).toFloat()
                                        sidebarPosition = Offset(
                                            (sidebarPosition.x + dragAmount.x).coerceIn(0f, maxX),
                                            (sidebarPosition.y + dragAmount.y).coerceIn(0f, maxY),
                                        )
                                    },
                                    onDragEnd = {
                                        val maxX = (rootSize.width - sidebarSize.width)
                                            .coerceAtLeast(0).toFloat()
                                        sidebarDockedRight = sidebarPosition.x +
                                            sidebarSize.width / 2f > rootSize.width / 2f
                                        sidebarPosition = sidebarPosition.copy(
                                            x = if (sidebarDockedRight) {
                                                (maxX - edgeMarginPx).coerceAtLeast(0f)
                                            } else {
                                                edgeMarginPx.coerceAtMost(maxX)
                                            },
                                        )
                                    },
                                )
                            }
                        } else {
                            Modifier
                        }
                    ),
                shape = if (sidebarCollapsed) CircleShape else RoundedCornerShape(22.dp),
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.94f),
                tonalElevation = 6.dp,
                shadowElevation = 8.dp,
            ) {
                if (sidebarCollapsed) {
                    IconButton(
                        onClick = { sidebarCollapsed = false },
                        modifier = Modifier.size(56.dp),
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.ic_mouse_toolbar),
                            contentDescription = stringResource(R.string.macro_mouse_canvas_title),
                            tint = MaterialTheme.colorScheme.primary,
                        )
                    }
                } else {
                    Column(
                        modifier = Modifier
                            .padding(8.dp)
                            .verticalScroll(rememberScrollState()),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        IconButton(onClick = { sidebarCollapsed = true }) {
                            Icon(
                                painter = painterResource(
                                    if (sidebarDockedRight) {
                                        R.drawable.ic_chevron_right
                                    } else {
                                        R.drawable.ic_chevron_left
                                    },
                                ),
                                contentDescription = stringResource(R.string.macro_mouse_collapse),
                            )
                        }
                        Text(
                            stringResource(R.string.macro_mouse_canvas_title),
                            fontWeight = FontWeight.Bold,
                        )
                        Text(
                            stringResource(R.string.macro_mouse_sidebar_drag_hint),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        MacroMouseAction.entries.forEach { action ->
                            MouseToolButton(
                                action = action,
                                selected = selectedTool == action,
                                onClick = { selectedTool = action },
                            )
                        }
                        SidebarActionButton(
                        iconRes = R.drawable.ic_image_import,
                        label = stringResource(R.string.macro_mouse_import_background),
                        onClick = { importBackground.launch("image/*") },
                    )
                    SidebarActionButton(
                        iconRes = R.drawable.ic_rotate_90,
                        label = stringResource(R.string.macro_mouse_rotate_90),
                        enabled = backgroundBitmap != null,
                        onClick = {
                            backgroundBitmap?.let { bitmap ->
                                backgroundBitmap = rotateBitmapClockwise(bitmap)
                                steps = steps.map(::rotateStepClockwise)
                                backgroundChanged = true
                                scale = 1f
                                pan = Offset.Zero
                            }
                        },
                    )
                    SidebarActionButton(
                        iconRes = R.drawable.ic_image_reset,
                        label = stringResource(R.string.macro_mouse_default_background),
                        enabled = backgroundBitmap != null,
                        onClick = {
                            backgroundBitmap = null
                            backgroundChanged = true
                            scale = 1f
                            pan = Offset.Zero
                        },
                    )
                    SidebarActionButton(
                        iconRes = R.drawable.ic_close,
                        label = stringResource(R.string.cancel),
                        onClick = onDismiss,
                    )
                    SidebarActionButton(
                        iconRes = R.drawable.ic_check,
                        label = stringResource(R.string.confirm),
                        enabled = steps.isNotEmpty() && !saving,
                        onClick = {
                            saving = true
                            scope.launch {
                                val fileName = if (backgroundBitmap != null && backgroundChanged) {
                                    withContext(Dispatchers.IO) {
                                        saveBackground(context.filesDir, backgroundBitmap!!)
                                    }
                                } else if (backgroundBitmap != null) {
                                    initialBackgroundFile
                                } else {
                                    null
                                }
                                onConfirm(steps, fileName)
                                saving = false
                            }
                        },
                        )
                    }
                }
            }

            if (!sidebarCollapsed && steps.isNotEmpty()) {
                Surface(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(12.dp),
                    shape = RoundedCornerShape(18.dp),
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f),
                ) {
                    Row(
                        modifier = Modifier
                            .horizontalScroll(rememberScrollState())
                            .padding(6.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        steps.forEachIndexed { index, step ->
                            FilledTonalButton(onClick = { editingIndex = index }) {
                                Text("${index + 1}. ${mouseStepDisplayName(step)}")
                            }
                        }
                    }
                }
            }
        }
    }

    if (editingIndex in steps.indices) {
        MouseStepEditorDialog(
            index = editingIndex,
            step = steps[editingIndex],
            canMovePrevious = editingIndex > 0,
            canMoveNext = editingIndex < steps.lastIndex,
            onDismiss = { editingIndex = -1 },
            onUpdate = { updated ->
                steps = steps.toMutableList().also { it[editingIndex] = updated }
                editingIndex = -1
            },
            onDelete = {
                steps = steps.toMutableList().also { it.removeAt(editingIndex) }
                editingIndex = -1
            },
            onDuplicate = {
                steps = steps.toMutableList().also {
                    it.add(
                        editingIndex + 1,
                        offsetDuplicatedStep(steps[editingIndex], steps),
                    )
                }
                editingIndex += 1
            },
            onMove = { offset ->
                val target = (editingIndex + offset).coerceIn(0, steps.lastIndex)
                if (target != editingIndex) {
                    steps = steps.toMutableList().also {
                        val item = it.removeAt(editingIndex)
                        it.add(target, item)
                    }
                    editingIndex = target
                }
            },
        )
    }
}

@Composable
private fun MouseToolButton(
    action: MacroMouseAction,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val label = mouseActionLabel(action)
    val iconRes = when (action) {
        MacroMouseAction.LEFT_CLICK -> R.drawable.ic_mouse_left
        MacroMouseAction.RIGHT_CLICK -> R.drawable.ic_mouse_right
        MacroMouseAction.SCROLL_UP -> R.drawable.ic_swipe_up
        MacroMouseAction.SCROLL_DOWN -> R.drawable.ic_swipe_down
    }
    FilledTonalButton(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth().height(52.dp),
        shape = RoundedCornerShape(16.dp),
    ) {
        Icon(
            painter = painterResource(iconRes),
            contentDescription = null,
            tint = if (selected) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            },
            modifier = Modifier.size(22.dp),
        )
        Spacer(Modifier.width(10.dp))
        Text(label, maxLines = 1)
    }
}

@Composable
private fun SidebarActionButton(
    @DrawableRes iconRes: Int,
    label: String,
    enabled: Boolean = true,
    onClick: () -> Unit,
) {
    OutlinedButton(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier.fillMaxWidth().height(48.dp),
        shape = RoundedCornerShape(15.dp),
    ) {
        Icon(
            painter = painterResource(iconRes),
            contentDescription = null,
            modifier = Modifier.size(22.dp),
        )
        Spacer(Modifier.width(10.dp))
        Text(label, maxLines = 1)
    }
}

@Composable
private fun MouseStepEditorDialog(
    index: Int,
    step: MacroMouseStep,
    canMovePrevious: Boolean,
    canMoveNext: Boolean,
    onDismiss: () -> Unit,
    onUpdate: (MacroMouseStep) -> Unit,
    onDelete: () -> Unit,
    onDuplicate: () -> Unit,
    onMove: (Int) -> Unit,
) {
    var action by remember(step) { mutableStateOf(step.action) }
    var hold by remember(step) { mutableStateOf(step.holdDurationMs.toString()) }
    var duration by remember(step) { mutableStateOf(step.gestureDurationMs.toString()) }
    var distance by remember(step) {
        mutableStateOf((step.distanceFraction * 100f).toInt().toString())
    }
    var delay by remember(step) { mutableStateOf(step.delayAfterMs.toString()) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.macro_mouse_edit_marker, index + 1)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    MacroMouseAction.entries.forEach { candidate ->
                        FilledTonalButton(onClick = { action = candidate }) {
                            Text(mouseActionLabel(candidate))
                        }
                    }
                }
                if (action == MacroMouseAction.LEFT_CLICK ||
                    action == MacroMouseAction.RIGHT_CLICK
                ) {
                    NumberField(
                        value = hold,
                        onValueChange = { hold = it },
                        label = stringResource(R.string.macro_mouse_hold_duration),
                    )
                } else {
                    NumberField(
                        value = duration,
                        onValueChange = { duration = it },
                        label = stringResource(R.string.macro_mouse_gesture_duration),
                    )
                    NumberField(
                        value = distance,
                        onValueChange = { distance = it },
                        label = stringResource(R.string.macro_mouse_scroll_distance),
                    )
                }
                NumberField(
                    value = delay,
                    onValueChange = { delay = it },
                    label = stringResource(R.string.macro_delay_short),
                )
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    OutlinedButton(enabled = canMovePrevious, onClick = { onMove(-1) }) {
                        Icon(
                            painterResource(R.drawable.ic_chevron_left),
                            contentDescription = null,
                        )
                    }
                    OutlinedButton(enabled = canMoveNext, onClick = { onMove(1) }) {
                        Icon(
                            painterResource(R.drawable.ic_chevron_right),
                            contentDescription = null,
                        )
                    }
                    OutlinedButton(onClick = onDuplicate) {
                        Text(stringResource(R.string.macro_mouse_duplicate))
                    }
                    OutlinedButton(onClick = onDelete) {
                        Text(stringResource(R.string.macro_delete))
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                onUpdate(
                    step.copy(
                        action = action,
                        holdDurationMs = hold.toLongOrNull()?.coerceIn(10L, 10_000L) ?: 40L,
                        gestureDurationMs = duration.toLongOrNull()
                            ?.coerceIn(10L, 10_000L) ?: 300L,
                        distanceFraction = ((distance.toFloatOrNull() ?: 18f) / 100f)
                            .coerceIn(0.01f, 1f),
                        delayAfterMs = delay.toLongOrNull()?.coerceIn(0L, 10_000L) ?: 50L,
                    ),
                )
            }) { Text(stringResource(R.string.confirm)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
        },
    )
}

@Composable
private fun NumberField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
) {
    OutlinedTextField(
        value = value,
        onValueChange = { onValueChange(it.filter(Char::isDigit)) },
        label = { Text(label) },
        singleLine = true,
        modifier = Modifier.fillMaxWidth(),
    )
}

@Composable
private fun mouseActionLabel(action: MacroMouseAction): String = stringResource(
    when (action) {
        MacroMouseAction.LEFT_CLICK -> R.string.macro_mouse_left_click
        MacroMouseAction.RIGHT_CLICK -> R.string.macro_mouse_right_click
        MacroMouseAction.SCROLL_UP -> R.string.macro_mouse_scroll_up
        MacroMouseAction.SCROLL_DOWN -> R.string.macro_mouse_scroll_down
    },
)

@Composable
internal fun mouseStepDisplayName(step: MacroMouseStep): String = mouseActionLabel(step.action)

private fun mouseActionColor(action: MacroMouseAction): Color = when (action) {
    MacroMouseAction.LEFT_CLICK -> Color(0xFF2563EB)
    MacroMouseAction.RIGHT_CLICK -> Color(0xFF7C3AED)
    MacroMouseAction.SCROLL_UP -> Color(0xFF059669)
    MacroMouseAction.SCROLL_DOWN -> Color(0xFFEA580C)
}

private fun rotateStepClockwise(step: MacroMouseStep): MacroMouseStep = step.copy(
    xFraction = 1f - step.yFraction,
    yFraction = step.xFraction,
)

private fun offsetDuplicatedStep(
    step: MacroMouseStep,
    existing: List<MacroMouseStep>,
): MacroMouseStep {
    val directions = listOf(
        1f to 1f,
        -1f to 1f,
        1f to -1f,
        -1f to -1f,
        1f to 0f,
        -1f to 0f,
        0f to 1f,
        0f to -1f,
    )
    repeat(4) { ring ->
        val distance = DUPLICATE_OFFSET * (ring + 1)
        directions.forEach { (xDirection, yDirection) ->
            val candidate = step.copy(
                xFraction = (step.xFraction + xDirection * distance).coerceIn(0f, 1f),
                yFraction = (step.yFraction + yDirection * distance).coerceIn(0f, 1f),
            )
            val clear = existing.none { other ->
                hypot(
                    (candidate.xFraction - other.xFraction).toDouble(),
                    (candidate.yFraction - other.yFraction).toDouble(),
                ) < DUPLICATE_OFFSET * 0.8f
            }
            if (clear) return candidate
        }
    }
    return step.copy(
        xFraction = (step.xFraction + DUPLICATE_OFFSET).mod(1f),
        yFraction = (step.yFraction + DUPLICATE_OFFSET * 2f).mod(1f),
    )
}

private fun findMarkerIndex(
    steps: List<MacroMouseStep>,
    xFraction: Float,
    yFraction: Float,
    scale: Float,
): Int = steps.indexOfLast { step ->
    hypot(
        (step.xFraction - xFraction).toDouble(),
        (step.yFraction - yFraction).toDouble(),
    ) <= MARKER_HIT_FRACTION / scale
}

private fun rotateBitmapClockwise(bitmap: Bitmap): Bitmap = Bitmap.createBitmap(
    bitmap,
    0,
    0,
    bitmap.width,
    bitmap.height,
    Matrix().apply { postRotate(90f) },
    true,
)

private fun mouseBackgroundDirectory(filesDir: File): File =
    File(filesDir, "macro_mouse_backgrounds").apply { mkdirs() }

private fun saveBackground(filesDir: File, bitmap: Bitmap): String {
    val fileName = "${UUID.randomUUID()}.png"
    val target = File(mouseBackgroundDirectory(filesDir), fileName)
    target.outputStream().buffered().use { output ->
        check(bitmap.compress(Bitmap.CompressFormat.PNG, 95, output))
    }
    return fileName
}

private const val MAX_BACKGROUND_SIDE = 2048
private const val MARKER_HIT_FRACTION = 0.045
private const val DUPLICATE_OFFSET = 0.04f
