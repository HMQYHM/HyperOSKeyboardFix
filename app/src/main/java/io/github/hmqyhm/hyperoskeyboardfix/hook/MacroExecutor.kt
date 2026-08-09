package io.github.hmqyhm.hyperoskeyboardfix.hook

import android.os.Handler
import android.os.HandlerThread
import android.os.SystemClock
import android.view.InputDevice
import android.view.KeyCharacterMap
import android.view.KeyEvent
import io.github.hmqyhm.hyperoskeyboardfix.model.KeyboardMacro
import io.github.hmqyhm.hyperoskeyboardfix.model.MacroActionType
import io.github.hmqyhm.hyperoskeyboardfix.model.MacroKeyStep
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
        when (macro.actionType) {
            MacroActionType.TEXT -> injectText(macro.text, active, target)
            MacroActionType.SEQUENCE -> macro.steps.forEach { step ->
                if (!active.get()) return
                injectStep(step, target, active)
                pause(step.delayAfterMs.coerceAtLeast(MIN_STEP_TIMING_MS), active)
            }
        }
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
        if (displayId >= 0) {
            try {
                XposedHelpers.callMethod(event, "setDisplayId", displayId)
            } catch (_: Throwable) {
                // The hidden setter is ROM-dependent; the default display is a safe fallback.
            }
        }
        return event
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
    private const val INJECTED_EVENT_TIMEOUT_MS = 2_000L
    private const val MIN_STEP_TIMING_MS = 10L
}
