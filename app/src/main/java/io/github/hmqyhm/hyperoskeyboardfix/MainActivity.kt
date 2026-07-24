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
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import io.github.hmqyhm.hyperoskeyboardfix.config.ModulePreferences
import io.github.hmqyhm.hyperoskeyboardfix.ui.home.HomeScreen
import io.github.hmqyhm.hyperoskeyboardfix.ui.settings.MainSettingsScreen
import io.github.hmqyhm.hyperoskeyboardfix.ui.settings.WhitelistScreen
import io.github.hmqyhm.hyperoskeyboardfix.ui.theme.HyperoskeyboardfixTheme
import java.util.concurrent.CancellationException
import kotlinx.coroutines.flow.collect

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val initialPreferences = ModulePreferences(applicationContext)
        applyAppLanguage(initialPreferences.languageTag())
        enableEdgeToEdge()
        setContent {
            HyperoskeyboardfixTheme {
                val preferences = remember { ModulePreferences(applicationContext) }
                var page by rememberSaveable { mutableStateOf(AppPage.HOME) }
                var backProgress by remember { mutableFloatStateOf(0f) }

                PredictiveBackHandler(enabled = page != AppPage.HOME) { events ->
                    try {
                        events.collect { event ->
                            backProgress = event.progress
                        }
                        page = when (page) {
                            AppPage.WHITELIST -> AppPage.SETTINGS
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
                                onLanguageSelected = ::applyAppLanguage,
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

private enum class AppPage {
    HOME,
    SETTINGS,
    WHITELIST,
}
