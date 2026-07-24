package io.github.hmqyhm.hyperoskeyboardfix

import android.app.LocaleManager
import android.os.Bundle
import android.os.LocaleList
import androidx.activity.ComponentActivity
import androidx.activity.compose.PredictiveBackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import io.github.hmqyhm.hyperoskeyboardfix.config.ModulePreferences
import io.github.hmqyhm.hyperoskeyboardfix.config.ProjectLinks
import io.github.hmqyhm.hyperoskeyboardfix.ui.home.HomeScreen
import io.github.hmqyhm.hyperoskeyboardfix.ui.settings.MainSettingsScreen
import io.github.hmqyhm.hyperoskeyboardfix.ui.settings.ShortcutSettingsScreen
import io.github.hmqyhm.hyperoskeyboardfix.ui.settings.WhitelistScreen
import io.github.hmqyhm.hyperoskeyboardfix.ui.theme.HyperoskeyboardfixTheme
import java.util.concurrent.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collect

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val initialPreferences = ModulePreferences(applicationContext)
        val showStarPromptOnLaunch = savedInstanceState == null &&
            initialPreferences.recordLaunchAndShouldShowStarPrompt()
        applyAppLanguage(initialPreferences.languageTag())
        enableEdgeToEdge()
        setContent {
            HyperoskeyboardfixTheme {
                val preferences = remember { ModulePreferences(applicationContext) }
                var page by rememberSaveable { mutableStateOf(AppPage.HOME) }
                var backProgress by remember { mutableFloatStateOf(0f) }
                var showStarPrompt by rememberSaveable {
                    mutableStateOf(showStarPromptOnLaunch)
                }

                PredictiveBackHandler(enabled = page != AppPage.HOME) { events ->
                    try {
                        events.collect { event ->
                            backProgress = event.progress
                        }
                        page = when (page) {
                            AppPage.WHITELIST -> AppPage.SETTINGS
                            AppPage.SHORTCUTS -> AppPage.SETTINGS
                            AppPage.SETTINGS -> AppPage.HOME
                            AppPage.HOME -> AppPage.HOME
                        }
                    } catch (_: CancellationException) {
                        // Gesture cancellation keeps the current page.
                    } finally {
                        backProgress = 0f
                    }
                }

                Scaffold(modifier = Modifier.fillMaxSize()) { contentPadding ->
                    AnimatedContent(
                        targetState = page,
                        modifier = Modifier.graphicsLayer {
                            val progress = backProgress.coerceIn(0f, 1f)
                            scaleX = 1f - progress * 0.035f
                            scaleY = 1f - progress * 0.035f
                            translationX = progress * 22f
                            alpha = 1f - progress * 0.12f
                        },
                        transitionSpec = {
                            fadeIn(tween(260)) togetherWith fadeOut(tween(180))
                        },
                        label = "app_page_transition",
                    ) { targetPage ->
                        when (targetPage) {
                            AppPage.HOME -> HomeScreen(
                                preferences = preferences,
                                contentPadding = contentPadding,
                                onOpenSettings = { page = AppPage.SETTINGS },
                            )

                            AppPage.SETTINGS -> MainSettingsScreen(
                                preferences = preferences,
                                contentPadding = contentPadding,
                                onBack = { page = AppPage.HOME },
                                onOpenWhitelist = { page = AppPage.WHITELIST },
                                onOpenShortcuts = { page = AppPage.SHORTCUTS },
                                onLanguageSelected = ::applyAppLanguage,
                            )

                            AppPage.SHORTCUTS -> ShortcutSettingsScreen(
                                preferences = preferences,
                                contentPadding = contentPadding,
                                onBack = { page = AppPage.SETTINGS },
                            )

                            AppPage.WHITELIST -> WhitelistScreen(
                                preferences = preferences,
                                contentPadding = contentPadding,
                                onBack = { page = AppPage.SETTINGS },
                                onSaved = { page = AppPage.SETTINGS },
                            )
                        }
                    }
                }

                if (showStarPrompt) {
                    StarPromptDialog(
                        preferences = preferences,
                        onDismiss = { showStarPrompt = false },
                    )
                }
            }
        }
    }

    private fun applyAppLanguage(languageTag: String) {
        val localeManager = getSystemService(LocaleManager::class.java)
        val requestedLocales = LocaleList.forLanguageTags(languageTag)
        if (localeManager.applicationLocales.toLanguageTags() != languageTag) {
            localeManager.applicationLocales = requestedLocales
        }
    }
}

@Composable
private fun StarPromptDialog(
    preferences: ModulePreferences,
    onDismiss: () -> Unit,
) {
    val uriHandler = LocalUriHandler.current
    var secondsRemaining by rememberSaveable {
        mutableStateOf(STAR_PROMPT_TIMEOUT_SECONDS)
    }

    LaunchedEffect(Unit) {
        preferences.markStarPromptShown()
        repeat(STAR_PROMPT_TIMEOUT_SECONDS) {
            delay(1_000L)
            secondsRemaining -= 1
        }
        onDismiss()
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(stringResource(R.string.star_prompt_title))
        },
        text = {
            Text(stringResource(R.string.star_prompt_message))
        },
        confirmButton = {
            TextButton(
                onClick = {
                    uriHandler.openUri(ProjectLinks.GITHUB_REPOSITORY)
                    onDismiss()
                },
            ) {
                Text(stringResource(R.string.star_prompt_github))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(
                    stringResource(
                        R.string.star_prompt_later,
                        secondsRemaining.coerceAtLeast(0),
                    ),
                )
            }
        },
    )
}

private enum class AppPage {
    HOME,
    SETTINGS,
    SHORTCUTS,
    WHITELIST,
}

private const val STAR_PROMPT_TIMEOUT_SECONDS = 5
