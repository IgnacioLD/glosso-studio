package me.shirobyte42.glosso.presentation

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Insights
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Insights
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.material3.windowsizeclass.calculateWindowSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import org.koin.android.ext.android.inject
import me.shirobyte42.glosso.R
import me.shirobyte42.glosso.data.prefs.AndroidPreferenceRepository
import me.shirobyte42.glosso.domain.repository.PreferenceRepository
import me.shirobyte42.glosso.presentation.about.AboutScreen
import me.shirobyte42.glosso.presentation.home.HomeScreen
import me.shirobyte42.glosso.presentation.language.LanguageSelectionScreen
import me.shirobyte42.glosso.presentation.settings.SettingsScreen
import me.shirobyte42.glosso.presentation.stats.StatsScreen
import me.shirobyte42.glosso.presentation.studio.StudioScreen
import me.shirobyte42.glosso.presentation.theme.GlossoTheme
import me.shirobyte42.glosso.presentation.topic.TopicSelectionScreen

val LocalWindowWidthClass = compositionLocalOf { WindowWidthSizeClass.Compact }

/** The three top-level destinations reachable from the bottom navigation bar. */
private enum class TopLevelDestination(
    val route: String,
    val labelRes: Int,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
) {
    HOME("home", R.string.nav_home, Icons.Filled.Home, Icons.Outlined.Home),
    PROGRESS("progress", R.string.nav_progress, Icons.Filled.Insights, Icons.Outlined.Insights),
    SETTINGS("settings", R.string.settings_title, Icons.Filled.Settings, Icons.Outlined.Settings)
}

class MainActivity : ComponentActivity() {

    private val prefs: PreferenceRepository by inject()

    @OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            val windowSizeClass = calculateWindowSizeClass(this)
            val themeMode by (prefs as? AndroidPreferenceRepository)
                ?.themeModeFlow
                ?.collectAsState()
                ?: remember { mutableStateOf(0) }
            val initialLang = remember { prefs.getTargetLanguage() }
            val targetLanguage by (prefs as? AndroidPreferenceRepository)
                ?.getTargetLanguageFlow()
                ?.collectAsState(initial = initialLang)
                ?: remember { mutableStateOf(initialLang) }

            GlossoTheme(themeMode = themeMode) {
                if (targetLanguage.isNullOrEmpty()) {
                    LanguageSelectionScreen { lang ->
                        prefs.setTargetLanguage(lang)
                    }
                } else {
                    CompositionLocalProvider(
                        LocalWindowWidthClass provides windowSizeClass.widthSizeClass
                    ) {
                        GlossoAppShell()
                    }
                }
            }
        }
    }
}

@Composable
private fun GlossoAppShell() {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = backStackEntry?.destination
    val currentRoute = currentDestination?.route

    val showBottomBar = TopLevelDestination.entries.any { it.route == currentRoute }

    fun navigateToTab(route: String) {
        navController.navigate(route) {
            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
            launchSingleTop = true
            restoreState = true
        }
    }

    val background = MaterialTheme.colorScheme.background

    // The navigation bar floats above the content so lists can scroll behind it
    // instead of being cut off by a reserved band.
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(background)
    ) {
        NavHost(
            navController = navController,
            startDestination = TopLevelDestination.HOME.route,
            modifier = Modifier.fillMaxSize()
        ) {
            composable(TopLevelDestination.HOME.route) {
                HomeScreen(
                    onNavigateToStudio = { category ->
                        navController.navigate("topics/$category")
                    }
                )
            }
            composable(TopLevelDestination.PROGRESS.route) {
                StatsScreen(onNavigateBack = null)
            }
            composable(TopLevelDestination.SETTINGS.route) {
                SettingsScreen(
                    onNavigateBack = null,
                    onNavigateToAbout = { navController.navigate("about") }
                )
            }
            composable("about") {
                AboutScreen(onNavigateBack = { navController.popBackStack() })
            }
            composable(
                route = "topics/{levelIndex}",
                arguments = listOf(navArgument("levelIndex") { type = NavType.IntType })
            ) { entry ->
                val levelIndex = entry.arguments?.getInt("levelIndex") ?: 0
                TopicSelectionScreen(
                    levelIndex = levelIndex,
                    onNavigateBack = { navController.popBackStack() },
                    onStartPractice = { level, topics ->
                        val topicsArg = if (topics.isNotEmpty()) "?topics=${topics.joinToString(",")}" else ""
                        navController.navigate("studio/$level$topicsArg")
                    },
                    onContinueBatch = { level ->
                        navController.navigate("studio_resume/$level")
                    }
                )
            }
            composable(
                route = "studio/{levelIndex}?topics={topics}",
                arguments = listOf(
                    navArgument("levelIndex") { type = NavType.IntType },
                    navArgument("topics") {
                        type = NavType.StringType
                        nullable = true
                        defaultValue = null
                    }
                )
            ) { entry ->
                val levelIndex = entry.arguments?.getInt("levelIndex") ?: 0
                val topics = entry.arguments?.getString("topics")?.split(",")?.filter { it.isNotBlank() }
                StudioScreen(
                    category = levelIndex,
                    topics = topics,
                    onNavigateBack = { navController.popBackStack() },
                    onNavigateToSettings = { navigateToTab(TopLevelDestination.SETTINGS.route) },
                    // popBackStack (not navigateToTab): navigating to the HOME tab would
                    // restore its saved back stack, landing right back on this Studio screen.
                    onNavigateHome = { navController.popBackStack(TopLevelDestination.HOME.route, inclusive = false) }
                )
            }
            composable(
                route = "studio_resume/{levelIndex}",
                arguments = listOf(navArgument("levelIndex") { type = NavType.IntType })
            ) { entry ->
                val levelIndex = entry.arguments?.getInt("levelIndex") ?: 0
                StudioScreen(
                    category = levelIndex,
                    resume = true,
                    onNavigateBack = { navController.popBackStack() },
                    onNavigateToSettings = { navigateToTab(TopLevelDestination.SETTINGS.route) },
                    // popBackStack (not navigateToTab): navigating to the HOME tab would
                    // restore its saved back stack, landing right back on this Studio screen.
                    onNavigateHome = { navController.popBackStack(TopLevelDestination.HOME.route, inclusive = false) }
                )
            }
        }

        if (showBottomBar) {
            GlossoBottomBar(
                currentRoute = currentRoute,
                onSelect = { route -> navigateToTab(route) },
                modifier = Modifier.align(Alignment.BottomCenter)
            )
        }
    }
}

/**
 * A floating capsule navigation bar. The active destination expands into a
 * filled pill that reveals its label, so the current section is unmistakable
 * while the bar stays compact and quiet.
 */
@Composable
private fun GlossoBottomBar(
    currentRoute: String?,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 24.dp, vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            shape = RoundedCornerShape(26.dp),
            color = MaterialTheme.colorScheme.surface,
            modifier = Modifier
                .wrapContentWidth()
                .animateContentSize()
        ) {
            Row(
                modifier = Modifier.padding(6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                TopLevelDestination.entries.forEach { destination ->
                    NavPill(
                        destination = destination,
                        selected = currentRoute == destination.route,
                        onClick = { if (currentRoute != destination.route) onSelect(destination.route) }
                    )
                }
            }
        }
    }
}

@Composable
private fun NavPill(
    destination: TopLevelDestination,
    selected: Boolean,
    onClick: () -> Unit
) {
    val containerColor by animateColorAsState(
        targetValue = if (selected) MaterialTheme.colorScheme.primary else Color.Transparent,
        label = "navPillContainer"
    )
    val contentColor by animateColorAsState(
        targetValue = if (selected) MaterialTheme.colorScheme.onPrimary
        else MaterialTheme.colorScheme.onSurfaceVariant,
        label = "navPillContent"
    )

    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(20.dp),
        color = containerColor,
        modifier = Modifier.height(44.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxHeight()
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = if (selected) destination.selectedIcon else destination.unselectedIcon,
                contentDescription = null,
                tint = contentColor,
                modifier = Modifier.size(22.dp)
            )
            AnimatedVisibility(
                visible = selected,
                enter = fadeIn() + expandHorizontally(),
                exit = fadeOut() + shrinkHorizontally()
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = stringResource(destination.labelRes),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = contentColor,
                        maxLines = 1
                    )
                }
            }
        }
    }
}
