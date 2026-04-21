package me.shirobyte42.glosso.presentation

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.*
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.*
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.material3.windowsizeclass.calculateWindowSizeClass
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import org.koin.android.ext.android.inject
import me.shirobyte42.glosso.data.prefs.AndroidPreferenceRepository
import me.shirobyte42.glosso.domain.repository.PreferenceRepository
import me.shirobyte42.glosso.presentation.theme.GlossoTheme
import me.shirobyte42.glosso.presentation.home.HomeScreen
import me.shirobyte42.glosso.presentation.studio.StudioScreen
import me.shirobyte42.glosso.presentation.topic.TopicSelectionScreen
import me.shirobyte42.glosso.presentation.about.AboutScreen
import me.shirobyte42.glosso.presentation.settings.SettingsScreen
import me.shirobyte42.glosso.presentation.language.LanguageSelectionScreen

val LocalWindowWidthClass = compositionLocalOf { WindowWidthSizeClass.Compact }

private const val TRANSITION_DURATION = 280

class MainActivity : ComponentActivity() {

    private val prefs: PreferenceRepository by inject()

    @OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

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
                    CompositionLocalProvider(LocalWindowWidthClass provides windowSizeClass.widthSizeClass) {
                        val navController = rememberNavController()
                        val background = MaterialTheme.colorScheme.background
                        NavHost(
                            navController = navController,
                            startDestination = "home",
                    modifier = Modifier
                        .fillMaxSize()
                        .background(background),
                    enterTransition = {
                        fadeIn(animationSpec = tween(TRANSITION_DURATION, easing = LinearEasing))
                    },
                    exitTransition = {
                        fadeOut(animationSpec = tween(TRANSITION_DURATION, easing = LinearEasing))
                    },
                    popEnterTransition = {
                        fadeIn(animationSpec = tween(TRANSITION_DURATION, easing = LinearEasing))
                    },
                    popExitTransition = {
                        fadeOut(animationSpec = tween(TRANSITION_DURATION, easing = LinearEasing))
                    }
                ) {
                    composable("home") {
                        HomeScreen(
                            onNavigateToStudio = { category ->
                                navController.navigate("topics/$category")
                            },
                            onNavigateToSettings = {
                                navController.navigate("settings")
                            }
                        )
                    }
                    composable("about") {
                        AboutScreen(onNavigateBack = { navController.popBackStack() })
                    }
                    composable("settings") {
                        SettingsScreen(
                            onNavigateBack = { navController.popBackStack() },
                            onNavigateToAbout = { navController.navigate("about") }
                        )
                    }
                    composable(
                        route = "topics/{levelIndex}",
                        arguments = listOf(navArgument("levelIndex") { type = NavType.IntType })
                    ) { backStackEntry ->
                        val levelIndex = backStackEntry.arguments?.getInt("levelIndex") ?: 0
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
                    ) { backStackEntry ->
                        val levelIndex = backStackEntry.arguments?.getInt("levelIndex") ?: 0
                        val topics = backStackEntry.arguments?.getString("topics")?.split(",")?.filter { it.isNotBlank() }
                        StudioScreen(
                            category = levelIndex,
                            topics = topics,
                            onNavigateBack = { navController.popBackStack() },
                            onNavigateToSettings = { navController.navigate("settings") }
                        )
                    }
                    composable(
                        route = "studio_resume/{levelIndex}",
                        arguments = listOf(navArgument("levelIndex") { type = NavType.IntType })
                    ) { backStackEntry ->
                        val levelIndex = backStackEntry.arguments?.getInt("levelIndex") ?: 0
                        StudioScreen(
                            category = levelIndex,
                            resume = true,
                            onNavigateBack = { navController.popBackStack() },
                            onNavigateToSettings = { navController.navigate("settings") }
                        )
                    }
                }
                } // CompositionLocalProvider
            }
            } // GlossoTheme
        }
    }
}
