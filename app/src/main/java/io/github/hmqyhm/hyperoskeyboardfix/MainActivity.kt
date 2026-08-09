package io.github.hmqyhm.hyperoskeyboardfix

import android.app.LocaleManager
import android.os.Bundle
import android.os.LocaleList
import androidx.activity.ComponentActivity
import androidx.activity.compose.PredictiveBackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.clickable
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import io.github.hmqyhm.hyperoskeyboardfix.config.ModulePreferences
import io.github.hmqyhm.hyperoskeyboardfix.config.ProjectLinks
import io.github.hmqyhm.hyperoskeyboardfix.ui.home.HomeScreen
import io.github.hmqyhm.hyperoskeyboardfix.ui.settings.MacroSettingsScreen
import io.github.hmqyhm.hyperoskeyboardfix.ui.settings.MainSettingsScreen
import io.github.hmqyhm.hyperoskeyboardfix.ui.settings.WhitelistScreen
import io.github.hmqyhm.hyperoskeyboardfix.ui.theme.HyperoskeyboardfixTheme
import java.util.concurrent.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

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
                KeyboardFixApp(
                    preferences = remember { ModulePreferences(applicationContext) },
                    showStarPromptOnLaunch = showStarPromptOnLaunch,
                    onLanguageSelected = ::applyAppLanguage,
                )
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
private fun KeyboardFixApp(
    preferences: ModulePreferences,
    showStarPromptOnLaunch: Boolean,
    onLanguageSelected: (String) -> Unit,
) {
    var page by rememberSaveable { mutableStateOf(AppPage.HOME) }
    var railCollapsed by rememberSaveable { mutableStateOf(false) }
    var showStarPrompt by rememberSaveable { mutableStateOf(showStarPromptOnLaunch) }
    var autoImeFocusRequest by rememberSaveable { mutableIntStateOf(0) }
    var predictiveBackCompleting by remember { mutableStateOf(false) }
    val backProgress = remember { Animatable(0f) }

    fun parentOf(current: AppPage): AppPage = when (current) {
        AppPage.WHITELIST -> AppPage.APPLICATIONS
        AppPage.MACROS -> AppPage.SHORTCUTS
        AppPage.APPLICATIONS, AppPage.SHORTCUTS, AppPage.OTHER -> AppPage.HOME
        AppPage.HOME -> AppPage.HOME
    }

    PredictiveBackHandler(enabled = page != AppPage.HOME) { events ->
        try {
            events.collect { event -> backProgress.snapTo(event.progress) }
            backProgress.animateTo(
                targetValue = 1f,
                animationSpec = tween(90),
            )
            predictiveBackCompleting = true
            page = parentOf(page)
            backProgress.snapTo(0f)
            delay(80L)
            predictiveBackCompleting = false
        } catch (_: CancellationException) {
            backProgress.animateTo(
                targetValue = 0f,
                animationSpec = spring(
                    dampingRatio = 0.82f,
                    stiffness = Spring.StiffnessLow,
                ),
            )
        }
    }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding(),
    ) {
        val portraitCompact = maxHeight > maxWidth
        LaunchedEffect(portraitCompact) { railCollapsed = portraitCompact }
        val railWidth by animateDpAsState(
            targetValue = if (railCollapsed) 58.dp else 150.dp,
            animationSpec = spring(
                dampingRatio = 0.84f,
                stiffness = Spring.StiffnessMediumLow,
            ),
            label = "navigation_rail_width",
        )

        Box(modifier = Modifier.fillMaxSize()) {
            Row(modifier = Modifier.fillMaxSize()) {
                if (!portraitCompact) {
                    AppNavigationRail(
                        width = railWidth,
                        collapsed = railCollapsed,
                        selectedPage = page.topLevelPage(),
                        onToggleCollapsed = { railCollapsed = !railCollapsed },
                        onSelect = { page = it },
                    )
                }
                Surface(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .padding(bottom = if (portraitCompact) 76.dp else 0.dp),
                color = MaterialTheme.colorScheme.background,
                ) {
                    AnimatedContent(
                    targetState = page,
                    modifier = Modifier,
                    transitionSpec = {
                        if (predictiveBackCompleting) {
                            return@AnimatedContent fadeIn(tween(1)) togetherWith fadeOut(tween(1))
                        }
                        val forward = targetState.order >= initialState.order
                        val direction = if (forward) 1 else -1
                        (
                            slideInHorizontally(
                                animationSpec = spring(
                                    dampingRatio = 0.86f,
                                    stiffness = Spring.StiffnessMediumLow,
                                ),
                            ) { width -> direction * width / 10 } +
                                scaleIn(
                                    initialScale = 0.975f,
                                    animationSpec = spring(
                                        dampingRatio = 0.88f,
                                        stiffness = Spring.StiffnessMediumLow,
                                    ),
                                ) + fadeIn(tween(180))
                            ) togetherWith (
                            slideOutHorizontally(tween(250)) { width -> -direction * width / 14 } +
                                scaleOut(targetScale = 0.99f, animationSpec = tween(250)) +
                                fadeOut(tween(180))
                            )
                    },
                    label = "global_page_transition",
                    ) { targetPage ->
                        FloatingPageSurface(
                            animateEntry = !predictiveBackCompleting,
                            predictiveBackProgress = backProgress.value,
                        ) {
                            when (targetPage) {
                        AppPage.HOME -> HomeScreen(
                            preferences = preferences,
                            contentPadding = androidx.compose.foundation.layout.PaddingValues(),
                            onOpenWhitelist = { page = AppPage.WHITELIST },
                            onOpenAutoIme = {
                                autoImeFocusRequest += 1
                                page = AppPage.OTHER
                            },
                        )
                        AppPage.APPLICATIONS -> WhitelistScreen(
                            preferences = preferences,
                            contentPadding = androidx.compose.foundation.layout.PaddingValues(),
                            onBack = { page = AppPage.HOME },
                            onSaved = { page = AppPage.APPLICATIONS },
                        )
                        AppPage.SHORTCUTS -> MacroSettingsScreen(
                            preferences = preferences,
                            contentPadding = androidx.compose.foundation.layout.PaddingValues(),
                            onBack = { page = AppPage.HOME },
                        )
                        AppPage.OTHER -> MainSettingsScreen(
                            preferences = preferences,
                            contentPadding = androidx.compose.foundation.layout.PaddingValues(),
                            onLanguageSelected = onLanguageSelected,
                            autoImeFocusRequest = autoImeFocusRequest,
                        )
                        AppPage.WHITELIST -> WhitelistScreen(
                            preferences = preferences,
                            contentPadding = androidx.compose.foundation.layout.PaddingValues(),
                            onBack = { page = AppPage.APPLICATIONS },
                            onSaved = { page = AppPage.APPLICATIONS },
                        )
                        AppPage.MACROS -> MacroSettingsScreen(
                            preferences = preferences,
                            contentPadding = androidx.compose.foundation.layout.PaddingValues(),
                            onBack = { page = AppPage.SHORTCUTS },
                        )
                            }
                        }
                    }
                }
            }
            if (portraitCompact) {
                AppBottomNavigation(
                    selectedPage = page.topLevelPage(),
                    onSelect = { page = it },
                    modifier = Modifier.align(Alignment.BottomCenter),
                )
            }
        }
    }

    if (showStarPrompt) {
        AnimatedStarPromptDialog(
            preferences = preferences,
            onDismiss = { showStarPrompt = false },
        )
    }
}

@Composable
private fun FloatingPageSurface(
    animateEntry: Boolean,
    predictiveBackProgress: Float,
    content: @Composable () -> Unit,
) {
    var settled by remember { mutableStateOf(!animateEntry) }
    LaunchedEffect(animateEntry) {
        if (animateEntry) {
            settled = false
            delay(16L)
        }
        settled = true
    }
    val progress by animateFloatAsState(
        targetValue = if (settled) 1f else 0f,
        animationSpec = spring(
            dampingRatio = 0.78f,
            stiffness = Spring.StiffnessMediumLow,
        ),
        label = "page_float_settle",
    )
    val boundedProgress = progress.coerceIn(0f, 1f)
    val backProgress = predictiveBackProgress.coerceIn(0f, 1f)
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(8.dp),
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    translationX = backProgress * 64.dp.toPx()
                    translationY = ((1f - boundedProgress) * 22.dp.toPx()) +
                        (backProgress * 22.dp.toPx())
                    val entryScale = 0.965f + boundedProgress * 0.035f
                    val backScale = 1f - backProgress * 0.035f
                    scaleX = entryScale * backScale
                    scaleY = entryScale * backScale
                    alpha = 1f - backProgress * 0.08f
                },
            shape = RoundedCornerShape(26.dp),
            color = MaterialTheme.colorScheme.surface,
        ) {
            content()
        }
    }
}

@Composable
private fun AppBottomNavigation(
    selectedPage: AppPage,
    onSelect: (AppPage) -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.fillMaxWidth().navigationBarsPadding(),
        color = MaterialTheme.colorScheme.surfaceContainer,
        tonalElevation = 5.dp,
        shadowElevation = 12.dp,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            BottomNavigationItem(R.drawable.ic_nav_home, R.string.navigation_home, AppPage.HOME, selectedPage, onSelect)
            BottomNavigationItem(R.drawable.ic_nav_apps, R.string.navigation_apps, AppPage.APPLICATIONS, selectedPage, onSelect)
            BottomNavigationItem(R.drawable.ic_nav_macro, R.string.navigation_shortcuts, AppPage.SHORTCUTS, selectedPage, onSelect)
            BottomNavigationItem(R.drawable.ic_nav_more, R.string.navigation_other, AppPage.OTHER, selectedPage, onSelect)
        }
    }
}

@Composable
private fun androidx.compose.foundation.layout.RowScope.BottomNavigationItem(
    iconRes: Int,
    labelRes: Int,
    page: AppPage,
    selectedPage: AppPage,
    onSelect: (AppPage) -> Unit,
) {
    val selected = page == selectedPage
    val selectionScale by animateFloatAsState(
        targetValue = if (selected) 1f else 0.94f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium,
        ),
        label = "bottom_navigation_scale",
    )
    val containerColor by animateColorAsState(
        targetValue = if (selected) {
            MaterialTheme.colorScheme.secondaryContainer
        } else {
            MaterialTheme.colorScheme.surfaceContainer
        },
        animationSpec = tween(240),
        label = "bottom_navigation_color",
    )
    Surface(
        modifier = Modifier
            .weight(1f)
            .graphicsLayer {
                scaleX = selectionScale
                scaleY = selectionScale
            }
            .clip(RoundedCornerShape(17.dp))
            .clickable { onSelect(page) },
        shape = RoundedCornerShape(17.dp),
        color = containerColor,
        contentColor = if (selected) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
    ) {
        Column(
            modifier = Modifier.padding(vertical = 7.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(1.dp),
        ) {
            Surface(
                modifier = Modifier.size(34.dp),
                shape = androidx.compose.foundation.shape.CircleShape,
                color = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerHigh,
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        painter = painterResource(iconRes),
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                    )
                }
            }
            Text(stringResource(labelRes), style = MaterialTheme.typography.labelMedium)
        }
    }
}

@Composable
private fun AppNavigationRail(
    width: androidx.compose.ui.unit.Dp,
    collapsed: Boolean,
    selectedPage: AppPage,
    onToggleCollapsed: () -> Unit,
    onSelect: (AppPage) -> Unit,
) {
    Surface(
        modifier = Modifier.width(width).fillMaxHeight(),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        tonalElevation = 2.dp,
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Box(
                modifier = Modifier.fillMaxWidth().size(48.dp),
                contentAlignment = Alignment.TopStart,
            ) {
                TextButton(
                    onClick = onToggleCollapsed,
                    modifier = Modifier.size(42.dp),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp),
                ) {
                    Text(
                        text = if (collapsed) "›" else "‹",
                        style = MaterialTheme.typography.headlineSmall,
                    )
                }
            }
            Spacer(Modifier.size(6.dp))
            TopNavigationItem(R.drawable.ic_nav_home, R.string.navigation_home, AppPage.HOME, selectedPage, collapsed, onSelect)
            TopNavigationItem(R.drawable.ic_nav_apps, R.string.navigation_apps, AppPage.APPLICATIONS, selectedPage, collapsed, onSelect)
            TopNavigationItem(R.drawable.ic_nav_macro, R.string.navigation_shortcuts, AppPage.SHORTCUTS, selectedPage, collapsed, onSelect)
            TopNavigationItem(R.drawable.ic_nav_more, R.string.navigation_other, AppPage.OTHER, selectedPage, collapsed, onSelect)
        }
    }
}

@Composable
private fun TopNavigationItem(
    iconRes: Int,
    labelRes: Int,
    page: AppPage,
    selectedPage: AppPage,
    collapsed: Boolean,
    onSelect: (AppPage) -> Unit,
) {
    val selected = page == selectedPage
    val labelAlpha by animateFloatAsState(
        targetValue = if (collapsed) 0f else 1f,
        animationSpec = tween(180),
        label = "navigation_label_alpha",
    )
    Surface(
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).clickable { onSelect(page) },
        shape = RoundedCornerShape(18.dp),
        color = if (selected) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surfaceContainerLow,
        contentColor = if (selected) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 13.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Start,
        ) {
            Surface(
                modifier = Modifier.size(38.dp),
                shape = androidx.compose.foundation.shape.CircleShape,
                color = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerHigh,
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        painter = painterResource(iconRes),
                        contentDescription = null,
                        modifier = Modifier.size(22.dp),
                    )
                }
            }
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clipToBounds(),
            ) {
                Text(
                    text = stringResource(labelRes),
                    modifier = Modifier
                        .padding(start = 13.dp)
                        .graphicsLayer { alpha = labelAlpha },
                    style = MaterialTheme.typography.titleMedium,
                )
            }
        }
    }
}

@Composable
private fun AnimatedStarPromptDialog(
    preferences: ModulePreferences,
    onDismiss: () -> Unit,
) {
    val uriHandler = LocalUriHandler.current
    val scope = rememberCoroutineScope()
    var visible by remember { mutableStateOf(false) }
    var secondsRemaining by remember { mutableIntStateOf(STAR_PROMPT_TIMEOUT_SECONDS) }
    var closing by remember { mutableStateOf(false) }

    fun closeDialog(afterClose: (() -> Unit)? = null) {
        if (closing) return
        closing = true
        visible = false
        scope.launch {
            delay(STAR_PROMPT_EXIT_MS)
            afterClose?.invoke()
            onDismiss()
        }
    }

    LaunchedEffect(Unit) {
        preferences.markStarPromptShown()
        visible = true
        repeat(STAR_PROMPT_TIMEOUT_SECONDS) {
            delay(1_000L)
            secondsRemaining -= 1
        }
        closeDialog()
    }

    Dialog(
        onDismissRequest = { closeDialog() },
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Box(
            modifier = Modifier.fillMaxSize().padding(22.dp),
            contentAlignment = Alignment.Center,
        ) {
            AnimatedVisibility(
                visible = visible,
                enter = fadeIn(tween(180)) + scaleIn(
                    initialScale = 0.82f,
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioMediumBouncy,
                        stiffness = Spring.StiffnessMediumLow,
                    ),
                ),
                exit = fadeOut(tween(STAR_PROMPT_EXIT_MS.toInt())) + scaleOut(
                    targetScale = 0.9f,
                    animationSpec = tween(STAR_PROMPT_EXIT_MS.toInt()),
                ),
            ) {
                Surface(
                    modifier = Modifier.fillMaxWidth(0.88f),
                    shape = RoundedCornerShape(28.dp),
                    tonalElevation = 8.dp,
                    shadowElevation = 20.dp,
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Surface(
                            modifier = Modifier.size(52.dp),
                            shape = RoundedCornerShape(17.dp),
                            color = MaterialTheme.colorScheme.primaryContainer,
                        ) {
                            Box(contentAlignment = Alignment.Center) { Text("★") }
                        }
                        Text(stringResource(R.string.star_prompt_title), style = MaterialTheme.typography.headlineSmall)
                        Text(
                            stringResource(R.string.star_prompt_message),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End,
                        ) {
                            TextButton(onClick = { closeDialog() }) {
                                Text(stringResource(R.string.star_prompt_later, secondsRemaining.coerceAtLeast(0)))
                            }
                            TextButton(
                                onClick = {
                                    closeDialog { uriHandler.openUri(ProjectLinks.GITHUB_REPOSITORY) }
                                },
                            ) {
                                Text(stringResource(R.string.star_prompt_now))
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun AppPage.topLevelPage(): AppPage = when (this) {
    AppPage.WHITELIST -> AppPage.APPLICATIONS
    AppPage.MACROS -> AppPage.SHORTCUTS
    else -> this
}

private enum class AppPage(val order: Int) {
    HOME(0),
    APPLICATIONS(1),
    SHORTCUTS(2),
    OTHER(3),
    WHITELIST(4),
    MACROS(5),
}

private const val STAR_PROMPT_TIMEOUT_SECONDS = 6
private const val STAR_PROMPT_EXIT_MS = 280L
