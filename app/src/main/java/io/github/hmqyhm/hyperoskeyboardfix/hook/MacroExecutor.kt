package io.github.hmqyhm.hyperoskeyboardfix.hook

import android.os.Handler
import android.os.HandlerThread
import android.os.SystemClock
import android.content.res.Resources
import android.view.InputDevice
import android.view.InputEvent
import android.view.KeyCharacterMap
import android.view.KeyEvent
import android.view.MotionEvent
import io.github.hmqyhm.hyperoskeyboardfix.model.KeyboardMacro
import io.github.hmqyhm.hyperoskeyboardfix.model.MacroActionStepType
import io.github.hmqyhm.hyperoskeyboardfix.model.MacroKeyStep
import io.github.hmqyhm.hyperoskeyboardfix.model.MacroMouseAction
import io.github.hmqyhm.hyperoskeyboardfix.model.MacroMouseStep
import io.github.hmqyhm.hyperoskeyboardfix.model.MacroRepeatMode
import io.github.hmqyhm.hyperoskeyboardfix.utils.HookLog
import de.robv.android.xposed.XposedHelpers
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicBoolean

object MacroExecutor {
    private val worker = Handler(
        HandlerThread("HyperOSKeyboardFix-Macro").apply { start() }.looper,
    )
    private val running = ConcurrentHashMap<String, AtomicBoolean>()
    private val pressedTriggers = ConcurrentHashMap.newKeySet<String>()
    private val pendingInjectedEvents = ConcurrentHashMap<InjectedEventKey, Long>()

    fun handle(
        event: KeyEvent,
        foregroundPackage: String,
        macros: List<KeyboardMacro>,
    ): Boolean {
        val candidates = macros.filter {
            it.enabled &&
                it.appliesTo(foregroundPackage) &&
                it.triggerKeyCode == event.keyCode
        }

        if (event.action == KeyEvent.ACTION_UP) {
            var consumed = false
            candidates.forEach { macro ->
                if (pressedTriggers.remove(macro.id)) {
                    consumed = true
                    if (macro.repeatMode == MacroRepeatMode.WHILE_HELD) stop(macro.id)
                }
            }
            return consumed
        }

        val macro = candidates.firstOrNull {
            normalizeMeta(it.triggerMetaState) == normalizeMeta(event.metaState)
        } ?: return false

        if (event.action != KeyEvent.ACTION_DOWN || event.repeatCount != 0) return true
        if (!pressedTriggers.add(macro.id)) return true

        if (macro.repeatMode == MacroRepeatMode.TOGGLE && running.containsKey(macro.id)) {
            stop(macro.id)
            HookLog.i("MACRO_STOPPED name=${macro.name}")
            return true
        }
        start(macro, InjectionTarget.from(event))
        HookLog.i("MACRO_TRIGGERED name=${macro.name} package=$foregroundPackage")
        val snapshot = ConfigProviderClient.snapshot()
        if (snapshot.notificationsEnabled && snapshot.notifyMacro) {
            ConfigProviderClient.notifyEvent(
                io.github.hmqyhm.hyperoskeyboardfix.config.ConfigContract.EVENT_MACRO,
            )
        }
        return true
    }

    private fun start(macro: KeyboardMacro, target: InjectionTarget) {
        stop(macro.id)
        val active = AtomicBoolean(true)
        running[macro.id] = active
        worker.post {
            try {
                val count = when (macro.repeatMode) {
                    MacroRepeatMode.ONCE -> 1
                    MacroRepeatMode.FIXED -> macro.repeatCount.coerceIn(1, 100)
                    MacroRepeatMode.WHILE_HELD, MacroRepeatMode.TOGGLE -> Int.MAX_VALUE
                }
                var iteration = 0
                while (active.get() && iteration < count) {
                    executeOnce(macro, active, target)
                    iteration++
                    if (active.get() && iteration < count) {
                        pause(macro.repeatIntervalMs, active)
                    }
                }
            } catch (error: Throwable) {
                HookLog.i(
                    "ERROR macro=${macro.name} exception=${error.javaClass.simpleName}: " +
                        error.message,
                )
            } finally {
                running.remove(macro.id, active)
            }
        }
    }

    private fun executeOnce(
        macro: KeyboardMacro,
        active: AtomicBoolean,
        target: InjectionTarget,
    ) {
        macro.effectiveActions().forEach { action ->
            if (!active.get()) return
            when (action.type) {
                MacroActionStepType.TEXT -> {
                    injectText(action.text, active, target)
                    pause(DEFAULT_ACTION_DELAY_MS, active)
                }
                MacroActionStepType.KEY -> action.keyStep?.let { step ->
                    injectStep(step, target, active)
                    pause(step.delayAfterMs.coerceAtLeast(MIN_STEP_TIMING_MS), active)
                }
                MacroActionStepType.MOUSE -> action.mouseStep?.let { step ->
                    injectMouseStep(step, target, active)
                    pause(step.delayAfterMs.coerceAtLeast(MIN_STEP_TIMING_MS), active)
                }
            }
        }
    }

    private fun injectMouseStep(
        step: MacroMouseStep,
        target: InjectionTarget,
        active: AtomicBoolean,
    ) {
        val size = resolveDisplaySize(target.displayId)
        val x = step.xFraction.coerceIn(0f, 1f) * (size.first - 1).coerceAtLeast(1)
        val y = step.yFraction.coerceIn(0f, 1f) * (size.second - 1).coerceAtLeast(1)
        injectMotion(mouseMotionEvent(MotionEvent.ACTION_HOVER_MOVE, x, y, 0, target))
        when (step.action) {
            MacroMouseAction.LEFT_CLICK -> injectMouseClick(
                x = x,
                y = y,
                button = MotionEvent.BUTTON_PRIMARY,
                holdDurationMs = step.holdDurationMs,
                target = target,
                active = active,
            )
            MacroMouseAction.RIGHT_CLICK -> injectMouseClick(
                x = x,
                y = y,
                button = MotionEvent.BUTTON_SECONDARY,
                holdDurationMs = step.holdDurationMs,
                target = target,
                active = active,
            )
            MacroMouseAction.SCROLL_UP, MacroMouseAction.SCROLL_DOWN -> {
                val ticks = (step.distanceFraction.coerceIn(0.01f, 1f) * 20f)
                    .toInt().coerceIn(1, 20)
                val axisValue = if (step.action == MacroMouseAction.SCROLL_UP) 1f else -1f
                val interval = (step.gestureDurationMs / ticks).coerceAtLeast(MIN_STEP_TIMING_MS)
                repeat(ticks) {
                    if (!active.get()) return
                    injectMotion(
                        mouseMotionEvent(
                            action = MotionEvent.ACTION_SCROLL,
                            x = x,
                            y = y,
                            buttonState = 0,
                            target = target,
                            verticalScroll = axisValue,
                        ),
                    )
                    pause(interval, active)
                }
            }
        }
    }

    private fun injectMouseClick(
        x: Float,
        y: Float,
        button: Int,
        holdDurationMs: Long,
        target: InjectionTarget,
        active: AtomicBoolean,
    ) {
        val downTime = SystemClock.uptimeMillis()
        injectMotion(mouseMotionEvent(MotionEvent.ACTION_DOWN, x, y, button, target, downTime))
        try {
            injectMotion(
                mouseMotionEvent(
                    MotionEvent.ACTION_BUTTON_PRESS,
                    x,
                    y,
                    button,
                    target,
                    downTime,
                    actionButton = button,
                ),
            )
            pause(holdDurationMs.coerceAtLeast(MIN_STEP_TIMING_MS), active)
        } finally {
            runCatching {
                injectMotion(
                    mouseMotionEvent(
                        MotionEvent.ACTION_BUTTON_RELEASE,
                        x,
                        y,
                        0,
                        target,
                        downTime,
                        actionButton = button,
                    ),
                )
            }
            runCatching {
                injectMotion(mouseMotionEvent(MotionEvent.ACTION_UP, x, y, 0, target, downTime))
            }
        }
    }

    private fun mouseMotionEvent(
        action: Int,
        x: Float,
        y: Float,
        buttonState: Int,
        target: InjectionTarget,
        downTime: Long = SystemClock.uptimeMillis(),
        verticalScroll: Float = 0f,
        actionButton: Int = 0,
    ): MotionEvent {
        val properties = MotionEvent.PointerProperties().apply {
            id = 0
            toolType = MotionEvent.TOOL_TYPE_MOUSE
        }
        val coordinates = MotionEvent.PointerCoords().apply {
            this.x = x
            this.y = y
            pressure = if (action == MotionEvent.ACTION_DOWN) 1f else 0f
            size = 1f
            if (verticalScroll != 0f) setAxisValue(MotionEvent.AXIS_VSCROLL, verticalScroll)
        }
        return MotionEvent.obtain(
            downTime,
            SystemClock.uptimeMillis(),
            action,
            1,
            arrayOf(properties),
            arrayOf(coordinates),
            0,
            buttonState,
            1f,
            1f,
            KeyCharacterMap.VIRTUAL_KEYBOARD,
            0,
            InputDevice.SOURCE_MOUSE,
            0,
        ).also { event ->
            if (actionButton != 0) {
                try {
                    XposedHelpers.callMethod(event, "setActionButton", actionButton)
                } catch (_: Throwable) {
                    // ACTION_DOWN/UP with buttonState remains a safe compatibility fallback.
                }
            }
            markInputDisplayId(event, target.displayId)
        }
    }

    private fun resolveDisplaySize(displayId: Int): Pair<Int, Int> = try {
        val globalClass = XposedHelpers.findClass(
            "android.hardware.display.DisplayManagerGlobal",
            null,
        )
        val global = XposedHelpers.callStaticMethod(globalClass, "getInstance")
        val info = XposedHelpers.callMethod(global, "getDisplayInfo", displayId.coerceAtLeast(0))
        val width = XposedHelpers.getIntField(info, "logicalWidth")
        val height = XposedHelpers.getIntField(info, "logicalHeight")
        require(width > 0 && height > 0)
        width to height
    } catch (_: Throwable) {
        val metrics = Resources.getSystem().displayMetrics
        metrics.widthPixels.coerceAtLeast(1) to metrics.heightPixels.coerceAtLeast(1)
    }

    private fun injectText(
        text: String,
        active: AtomicBoolean,
        target: InjectionTarget,
    ) {
        val events = KeyCharacterMap.load(KeyCharacterMap.VIRTUAL_KEYBOARD)
            .getEvents(text.toCharArray())
        if (events == null) {
            HookLog.i("ERROR macro_text_unavailable characters=${text.length}")
            return
        }
        events.forEach { event ->
            if (!active.get()) return
            inject(markInjected(event, target))
            pause(TEXT_EVENT_INTERVAL_MS, active)
        }
    }

    private fun injectStep(
        step: MacroKeyStep,
        target: InjectionTarget,
        active: AtomicBoolean,
    ) {
        val modifiers = modifierKeyCodes(step.metaState)
        modifiers.forEach { injectKey(it, KeyEvent.ACTION_DOWN, target = target) }
        injectKey(step.keyCode, KeyEvent.ACTION_DOWN, step.metaState, target)
        pause(step.holdDurationMs.coerceAtLeast(MIN_STEP_TIMING_MS), active)
        injectKey(step.keyCode, KeyEvent.ACTION_UP, step.metaState, target)
        modifiers.asReversed().forEach {
            injectKey(it, KeyEvent.ACTION_UP, target = target)
        }
    }

    private fun injectKey(
        keyCode: Int,
        action: Int,
        metaState: Int = 0,
        target: InjectionTarget,
    ) {
        val now = SystemClock.uptimeMillis()
        inject(markDisplayId(
            KeyEvent(
                now,
                now,
                action,
                keyCode,
                0,
                metaState,
                target.deviceId,
                0,
                KeyEvent.FLAG_FROM_SYSTEM,
                target.source,
            ),
            target.displayId,
        ))
    }

    private fun inject(event: KeyEvent) {
        markPendingInjected(event)
        injectInputEvent(event)
    }

    private fun injectMotion(event: MotionEvent) {
        injectInputEvent(event)
    }

    private fun injectInputEvent(event: InputEvent) {
        val globalClass = XposedHelpers.findClassIfExists(
            "android.hardware.input.InputManagerGlobal",
            null,
        )
        if (globalClass != null) {
            val global = XposedHelpers.callStaticMethod(globalClass, "getInstance")
            val accepted = XposedHelpers.callMethod(
                global,
                "injectInputEvent",
                event,
                0,
            ) as? Boolean
            if (accepted == false) {
                throw IllegalStateException("InputManager rejected injected event")
            }
            return
        }
        val managerClass = XposedHelpers.findClass("android.hardware.input.InputManager", null)
        val manager = XposedHelpers.callStaticMethod(managerClass, "getInstance")
        val accepted = XposedHelpers.callMethod(
            manager,
            "injectInputEvent",
            event,
            0,
        ) as? Boolean
        if (accepted == false) {
            throw IllegalStateException("InputManager rejected injected event")
        }
    }

    private fun markInjected(event: KeyEvent, target: InjectionTarget): KeyEvent =
        markDisplayId(
            KeyEvent(
                event.downTime,
                event.eventTime,
                event.action,
                event.keyCode,
                event.repeatCount,
                event.metaState,
                target.deviceId,
                event.scanCode,
                event.flags or KeyEvent.FLAG_FROM_SYSTEM,
                target.source,
            ),
            target.displayId,
        )

    fun consumeInjectedEvent(event: KeyEvent): Boolean {
        val now = SystemClock.uptimeMillis()
        pendingInjectedEvents.entries.removeIf { it.value < now }
        return pendingInjectedEvents.containsKey(injectedEventKey(event))
    }

    private fun markPendingInjected(event: KeyEvent) {
        pendingInjectedEvents[injectedEventKey(event)] =
            SystemClock.uptimeMillis() + INJECTED_EVENT_TIMEOUT_MS
    }

    private fun injectedEventKey(event: KeyEvent) = InjectedEventKey(
        deviceId = event.deviceId,
        keyCode = event.keyCode,
        action = event.action,
        downTime = event.downTime,
        eventTime = event.eventTime,
    )

    private fun markDisplayId(event: KeyEvent, displayId: Int): KeyEvent {
        markInputDisplayId(event, displayId)
        return event
    }

    private fun markInputDisplayId(event: InputEvent, displayId: Int) {
        if (displayId >= 0) {
            try {
                XposedHelpers.callMethod(event, "setDisplayId", displayId)
            } catch (_: Throwable) {
                // The hidden setter is ROM-dependent; the default display is a safe fallback.
            }
        }
    }

    private fun stop(id: String) {
        running.remove(id)?.set(false)
    }

    private fun pause(durationMs: Long, active: AtomicBoolean) {
        var remaining = durationMs.coerceIn(0L, 10_000L)
        while (active.get() && remaining > 0L) {
            val slice = minOf(remaining, 25L)
            Thread.sleep(slice)
            remaining -= slice
        }
    }

    private fun modifierKeyCodes(metaState: Int) = buildList {
        if (metaState and KeyEvent.META_CTRL_ON != 0) add(KeyEvent.KEYCODE_CTRL_LEFT)
        if (metaState and KeyEvent.META_ALT_ON != 0) add(KeyEvent.KEYCODE_ALT_LEFT)
        if (metaState and KeyEvent.META_SHIFT_ON != 0) add(KeyEvent.KEYCODE_SHIFT_LEFT)
        if (metaState and KeyEvent.META_META_ON != 0) add(KeyEvent.KEYCODE_META_LEFT)
    }

    private fun normalizeMeta(metaState: Int): Int = metaState and META_MASK

    private data class InjectionTarget(
        val deviceId: Int,
        val source: Int,
        val displayId: Int,
    ) {
        companion object {
            fun from(event: KeyEvent) = InjectionTarget(
                deviceId = event.deviceId,
                source = event.source,
                displayId = try {
                    XposedHelpers.callMethod(event, "getDisplayId") as? Int ?: -1
                } catch (_: Throwable) {
                    -1
                },
            )
        }
    }

    private data class InjectedEventKey(
        val deviceId: Int,
        val keyCode: Int,
        val action: Int,
        val downTime: Long,
        val eventTime: Long,
    )

    private const val META_MASK = KeyEvent.META_CTRL_ON or KeyEvent.META_ALT_ON or
        KeyEvent.META_SHIFT_ON or KeyEvent.META_META_ON
    private const val TEXT_EVENT_INTERVAL_MS = 8L
    private const val DEFAULT_ACTION_DELAY_MS = 50L
    private const val INJECTED_EVENT_TIMEOUT_MS = 2_000L
    private const val MIN_STEP_TIMING_MS = 10L
}
