package com.local.tiktok

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat

sealed class Screen {
    data object Main : Screen()
    data class AlbumProfile(val album: String) : Screen()
    data class FeedOf(val title: String, val ids: List<String>, val start: Int) : Screen()
    data object Settings : Screen()
    data object Stats : Screen()
    data object Search : Screen()
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val app = AppState(applicationContext)
        setContent {
            MaterialTheme(
                colorScheme = darkColorScheme(
                    primary = Pink, secondary = Cyan, background = Color.Black, surface = Color(0xFF121212)
                )
            ) { Root(app) }
        }
    }
}

private fun mediaPermissions(): Array<String> =
    if (Build.VERSION.SDK_INT >= 33) arrayOf(Manifest.permission.READ_MEDIA_VIDEO, Manifest.permission.READ_MEDIA_IMAGES)
    else arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)

@Composable
fun Root(app: AppState) {
    val ctx = LocalContext.current
    var splashDone by remember { mutableStateOf(!app.showSplash) }
    fun granted() = mediaPermissions().all { ContextCompat.checkSelfPermission(ctx, it) == PackageManager.PERMISSION_GRANTED }
    var hasPerm by remember { mutableStateOf(granted()) }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
        hasPerm = granted()
    }
    LaunchedEffect(Unit) { if (!hasPerm) launcher.launch(mediaPermissions()) }
    LaunchedEffect(hasPerm) { if (hasPerm) app.reload() }

    Box(Modifier.fillMaxSize().background(Color.Black)) {
        when {
            !splashDone -> SplashScreen { splashDone = true }
            !hasPerm -> PermissionScreen { launcher.launch(mediaPermissions()) }
            else -> Navigator(app)
        }
    }
}

@Composable
fun Navigator(app: AppState) {
    val stack = remember { mutableStateListOf<Screen>(Screen.Main) }
    val top = stack.last()
    BackHandler(enabled = stack.size > 1) { stack.removeAt(stack.lastIndex) }
    val push: (Screen) -> Unit = { stack.add(it) }
    val pop: () -> Unit = { if (stack.size > 1) stack.removeAt(stack.lastIndex) }

    AnimatedContent(
        targetState = top,
        transitionSpec = {
            if (targetState is Screen.Main) fadeIn() togetherWith fadeOut()
            else (slideInHorizontally { it } + fadeIn()) togetherWith fadeOut()
        },
        label = "nav"
    ) { screen ->
        when (screen) {
            is Screen.Main -> MainScreen(app, push)
            is Screen.AlbumProfile -> AlbumScreen(app, screen.album, push, pop)
            is Screen.FeedOf -> {
                val map = app.postsById
                val posts = remember(screen) { screen.ids.mapNotNull { map[it] } }
                StandaloneFeed(app, screen.title, posts, screen.start, push, pop)
            }
            is Screen.Settings -> SettingsScreen(app, pop)
            is Screen.Stats -> StatsScreen(app, push, pop)
            is Screen.Search -> SearchScreen(app, push, pop)
        }
    }
}
