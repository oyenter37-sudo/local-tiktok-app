package com.local.tiktok

import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TextButton
import kotlinx.coroutines.awaitCancellation
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import coil.compose.AsyncImage
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt
import kotlin.random.Random

val Pink = Color(0xFFFE2C55)
val Cyan = Color(0xFF25F4EE)

fun avatarBrush(name: String): Brush {
    val palettes = listOf(
        listOf(Color(0xFFFE2C55), Color(0xFFFF8A00)),
        listOf(Color(0xFF25F4EE), Color(0xFF3A7BFF)),
        listOf(Color(0xFF8E2DE2), Color(0xFFFE2C55)),
        listOf(Color(0xFF00C853), Color(0xFF25F4EE)),
        listOf(Color(0xFFFFD54F), Color(0xFFFF6F00)),
        listOf(Color(0xFF7F00FF), Color(0xFF00C6FF)),
    )
    return Brush.linearGradient(palettes[(name.hashCode() and 0x7fffffff) % palettes.size])
}

@Composable
fun Avatar(name: String, size: Int, modifier: Modifier = Modifier, emoji: String? = null) {
    Box(
        modifier.size(size.dp).clip(CircleShape).background(avatarBrush(name)),
        contentAlignment = Alignment.Center
    ) {
        Text(
            emoji ?: name.take(1).uppercase(),
            color = Color.White, fontWeight = FontWeight.Black, fontSize = (size * 0.42f).sp
        )
    }
}

/** Vertical feed of posts. */
@Composable
fun Feed(
    app: AppState,
    posts: List<Post>,
    start: Int,
    active: Boolean,
    onOpenAlbum: (String) -> Unit,
    resetKey: Any = Unit,
    onNearEnd: (() -> Unit)? = null,
) {
    if (posts.isEmpty()) {
        EmptyFeed(app); return
    }
    val pager = key(resetKey) { rememberPagerState(initialPage = start.coerceIn(0, posts.lastIndex)) { posts.size } }
    LaunchedEffect(pager.settledPage, resetKey) {
        posts.getOrNull(pager.settledPage)?.let { app.addView(it.id) }
        if (pager.settledPage >= posts.lastIndex - 1) onNearEnd?.invoke()
    }
    VerticalPager(
        state = pager,
        modifier = Modifier.fillMaxSize(),
        key = { "$it:" + posts[it].id },
        beyondViewportPageCount = 1,
    ) { page ->
        PostPage(app, posts[page], isActive = active && pager.settledPage == page, onOpenAlbum)
    }
}

@Composable
private fun EmptyFeed(app: AppState) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(if (app.loading) "⏳" else "🌚", fontSize = 64.sp)
            Spacer(Modifier.height(12.dp))
            Text(
                if (app.loading) "Ищу видео…" else "Тут пусто",
                color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold
            )
            if (!app.loading) Text(
                if (app.albums.isEmpty()) "Добавь папки с видео/фото: Профиль → ⚙" else "Проверь пути альбомов в Профиль → ⚙",
                color = Color.White.copy(alpha = .6f), fontSize = 14.sp
            )
        }
    }
}

private data class Burst(val id: Long, val pos: Offset, val rot: Float)

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun PostPage(app: AppState, post: Post, isActive: Boolean, onOpenAlbum: (String) -> Unit) {
    var paused by remember(post.id) { mutableStateOf(false) }
    var progress by remember(post.id) { mutableFloatStateOf(0f) }
    val bursts = remember(post.id) { mutableStateListOf<Burst>() }
    val liked = post.id in app.likes
    val isSaved = post.id in app.saved
    var speedUp by remember(post.id) { mutableStateOf(false) }
    var menu by remember(post.id) { mutableStateOf(false) }
    LaunchedEffect(isActive) { if (!isActive) { paused = false; speedUp = false } }

    // real watch time
    LaunchedEffect(isActive, paused) {
        if (isActive && !paused) {
            val s = System.currentTimeMillis()
            try { awaitCancellation() } finally { app.addWatch(post, System.currentTimeMillis() - s) }
        }
    }

    val onDouble: (Offset) -> Unit = { o ->
        bursts.add(Burst(System.nanoTime(), o, Random.nextInt(-25, 25).toFloat()))
        app.like(post.id)
    }
    val tapMod = Modifier.pointerInput(post.id) {
        detectTapGestures(
            onTap = { if (post.isVideo) paused = !paused },
            onDoubleTap = onDouble,
            onLongPress = { o ->
                val edge = o.x < size.width * 0.3f || o.x > size.width * 0.7f
                if (post.isVideo && edge) speedUp = true else menu = true
            },
            onPress = { tryAwaitRelease(); speedUp = false },
        )
    }
    // swipe left -> album profile
    val swipeMod = Modifier.pointerInput(post.id) {
        var total = 0f
        detectHorizontalDragGestures(
            onDragStart = { total = 0f },
            onDragEnd = { if (total < -110.dp.toPx()) onOpenAlbum(post.album) },
        ) { change, d -> total += d; change.consume() }
    }

    Box(Modifier.fillMaxSize().background(Color.Black)) {
        var page by remember(post.id) { mutableIntStateOf(0) }
        if (post.isVideo) {
            VideoPlayer(post.items.first().path, isActive && !paused, app.soundOn, if (speedUp) 2f else 1f) { progress = it }
            Box(Modifier.fillMaxSize().then(swipeMod).then(tapMod))
        } else if (post.items.size == 1) {
            AsyncImage(
                model = File(post.items.first().path),
                contentDescription = null,
                contentScale = ContentScale.Fit,
                modifier = Modifier.fillMaxSize().then(swipeMod).then(tapMod)
            )
        } else {
            val hp = rememberPagerState { post.items.size }
            LaunchedEffect(hp.currentPage) { page = hp.currentPage }
            HorizontalPager(state = hp, modifier = Modifier.fillMaxSize()) { i ->
                AsyncImage(
                    model = File(post.items[i].path),
                    contentDescription = null,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.fillMaxSize().then(tapMod)
                )
            }
        }

        // shades for readability
        Box(
            Modifier.fillMaxWidth().height(160.dp).align(Alignment.TopCenter)
                .background(Brush.verticalGradient(listOf(Color.Black.copy(.45f), Color.Transparent)))
        )
        Box(
            Modifier.fillMaxWidth().height(260.dp).align(Alignment.BottomCenter)
                .background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(.65f))))
        )

        // paused indicator
        AnimatedVisibility(
            visible = paused, modifier = Modifier.align(Alignment.Center),
            enter = scaleIn(initialScale = 1.6f) + fadeIn(), exit = scaleOut() + fadeOut()
        ) {
            Text("▶", color = Color.White.copy(alpha = .7f), fontSize = 72.sp)
        }

        // x2 indicator
        AnimatedVisibility(
            visible = speedUp, modifier = Modifier.align(Alignment.TopCenter).statusBarsPadding().padding(top = 56.dp),
            enter = fadeIn() + scaleIn(), exit = fadeOut() + scaleOut()
        ) {
            Text(
                "⏩ 2x", color = Color.White, fontWeight = FontWeight.Bold,
                modifier = Modifier.clip(RoundedCornerShape(14.dp)).background(Color.Black.copy(.5f))
                    .padding(horizontal = 14.dp, vertical = 6.dp)
            )
        }

        // counter chip for multi-image posts
        if (post.items.size > 1) {
            Text(
                "${page + 1}/${post.items.size}",
                color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold,
                modifier = Modifier.align(Alignment.TopEnd).statusBarsPadding().padding(top = 56.dp, end = 12.dp)
                    .clip(RoundedCornerShape(10.dp)).background(Color.Black.copy(.45f))
                    .padding(horizontal = 8.dp, vertical = 3.dp)
            )
        }

        // right action column
        Column(
            Modifier.align(Alignment.BottomEnd).padding(end = 8.dp, bottom = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            Box(Modifier.padding(bottom = 6.dp)) {
                Avatar(
                    post.album, 48,
                    Modifier.border(2.dp, Color.White, CircleShape).clickable { onOpenAlbum(post.album) }
                )
                Box(
                    Modifier.align(Alignment.BottomCenter).offset(y = 9.dp).size(20.dp)
                        .clip(CircleShape).background(Pink),
                    contentAlignment = Alignment.Center
                ) { Text("+", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold) }
            }
            MoonLikeButton(liked, if (liked) 1 else 0) { app.toggleLike(post.id) }
            ActionButton("👁", formatCount(app.views[post.id] ?: 0))
            ActionButton(if (isSaved) "⭐" else "☆", if (isSaved) "Сохр." else "Сохранить") { app.toggleSaved(post.id) }
            ActionButton(if (app.soundOn) "🔊" else "🔇", if (app.soundOn) "Звук" else "Без звука") {
                app.soundOn = !app.soundOn; app.persistSettings()
            }
            SpinningDisc(post.album, isActive && !paused && post.isVideo)
        }

        // bottom info
        Column(
            Modifier.align(Alignment.BottomStart).fillMaxWidth(.78f)
                .padding(start = 12.dp, bottom = if (post.items.size > 1) 34.dp else 18.dp)
        ) {
            Text(
                "@${post.album}", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 17.sp,
                modifier = Modifier.clickable { onOpenAlbum(post.album) }
            )
            Spacer(Modifier.height(6.dp))
            val item = post.items.getOrNull(page) ?: post.items.first()
            Text(
                item.title, color = Color.White, fontSize = 14.sp, maxLines = 2, overflow = TextOverflow.Ellipsis
            )
            Text(
                remember(item.modified) { SimpleDateFormat("d MMM yyyy", Locale("ru")).format(Date(item.modified)) } +
                    if (post.isVideo) "" else " · 🖼 ${post.items.size}",
                color = Color.White.copy(.6f), fontSize = 12.sp
            )
            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("♫ ", color = Color.White, fontSize = 13.sp)
                Text(
                    "оригинальный звук — ${post.album}   •   Local TikTok   •   ",
                    color = Color.White, fontSize = 13.sp, maxLines = 1,
                    modifier = Modifier.basicMarquee(iterations = Int.MAX_VALUE)
                )
            }
        }

        // dots for multi-image posts
        if (post.items.size > 1) {
            Row(
                Modifier.align(Alignment.BottomCenter).padding(bottom = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                repeat(post.items.size) { i ->
                    val w by animateDpAsState(if (i == page) 18.dp else 6.dp, label = "dot")
                    Box(
                        Modifier.height(6.dp).width(w).clip(CircleShape)
                            .background(if (i == page) Color.White else Color.White.copy(.4f))
                    )
                }
            }
        }

        // video progress
        if (post.isVideo) {
            Box(Modifier.align(Alignment.BottomCenter).fillMaxWidth().height(2.dp).background(Color.White.copy(.2f))) {
                Box(Modifier.fillMaxHeight().fillMaxWidth(progress).background(Color.White.copy(.9f)))
            }
        }

        // double-tap 🌝 bursts
        bursts.forEach { b ->
            key(b.id) { MoonBurst(b) { bursts.remove(b) } }
        }
    }

    if (menu) PostMenu(app, post, onOpenAlbum) { menu = false }
}

@Composable
private fun PostMenu(app: AppState, post: Post, onOpenAlbum: (String) -> Unit, onClose: () -> Unit) {
    val isSaved = post.id in app.saved
    val liked = post.id in app.likes
    val fmt = remember { SimpleDateFormat("d MMM yyyy, HH:mm", Locale("ru")) }
    AlertDialog(
        onDismissRequest = onClose,
        containerColor = Color(0xFF1C1C1C),
        title = { Text("@${post.album}", color = Color.White, fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                MenuRow(if (isSaved) "⭐  Убрать из сохранённого" else "☆  Сохранить") { app.toggleSaved(post.id) }
                MenuRow(if (liked) "🌚  Убрать 🌝" else "🌝  Поставить 🌝") { app.toggleLike(post.id) }
                MenuRow("👤  Профиль альбома") { onClose(); onOpenAlbum(post.album) }
                Box(Modifier.fillMaxWidth().height(1.dp).background(Color.White.copy(.1f)))
                Text("ℹ Инфо о файле", color = Color.White, fontWeight = FontWeight.Bold)
                post.items.forEach { f ->
                    val file = File(f.path)
                    Column {
                        Text(file.name, color = Color.White, fontSize = 13.sp)
                        Text(
                            "${String.format("%.1f", file.length() / 1048576.0)} МБ • ${fmt.format(Date(f.modified))}",
                            color = Color.White.copy(.6f), fontSize = 12.sp
                        )
                        Text(f.path, color = Color.White.copy(.4f), fontSize = 11.sp)
                    }
                }
                Text(
                    "👁 ${app.views[post.id] ?: 0} просмотров • ⏱ ${formatDuration(app.watch["p:" + post.id] ?: 0L)}",
                    color = Color.White.copy(.7f), fontSize = 12.sp
                )
            }
        },
        confirmButton = { TextButton(onClose) { Text("Закрыть", color = Pink) } }
    )
}

@Composable
private fun MenuRow(text: String, onClick: () -> Unit) {
    Text(
        text, color = Color.White, fontSize = 15.sp,
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)).clickable(onClick = onClick).padding(vertical = 6.dp)
    )
}

@Composable
private fun MoonBurst(b: Burst, onDone: () -> Unit) {
    val scale = remember { Animatable(0f) }
    val alpha = remember { Animatable(1f) }
    val lift = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        launch { scale.animateTo(1.3f, spring(dampingRatio = .45f, stiffness = 500f)) }
        delay(450)
        launch { lift.animateTo(-160f, tween(450)) }
        alpha.animateTo(0f, tween(450))
        onDone()
    }
    val sizePx = with(LocalDensity.current) { 110.dp.toPx() }
    Text(
        "🌝", fontSize = 90.sp,
        modifier = Modifier
            .offset { IntOffset((b.pos.x - sizePx / 2).roundToInt(), (b.pos.y - sizePx / 2 + lift.value).roundToInt()) }
            .rotate(b.rot).scale(scale.value).alpha(alpha.value)
    )
}

@Composable
fun MoonLikeButton(liked: Boolean, count: Int, onClick: () -> Unit) {
    val scale = remember { Animatable(1f) }
    LaunchedEffect(liked) {
        if (liked) {
            scale.snapTo(0.5f)
            scale.animateTo(1f, spring(dampingRatio = .35f, stiffness = 600f))
        }
    }
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            "🌝", fontSize = 34.sp,
            modifier = Modifier
                .scale(scale.value)
                .graphicsLayer { this.alpha = if (liked) 1f else .45f }
                .clickable(remember { MutableInteractionSource() }, null, onClick = onClick)
        )
        Text(formatCount(count), color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
fun ActionButton(icon: String, label: String, onClick: (() -> Unit)? = null) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = if (onClick != null) Modifier.clickable(remember { MutableInteractionSource() }, null, onClick = onClick) else Modifier
    ) {
        Text(icon, fontSize = 28.sp, color = Color.White)
        Text(label, color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun SpinningDisc(name: String, spinning: Boolean) {
    val rot = remember { Animatable(0f) }
    LaunchedEffect(spinning) {
        while (spinning) {
            rot.animateTo(rot.value + 360f, tween(4000, easing = LinearEasing))
        }
    }
    Box(
        Modifier.size(44.dp).rotate(rot.value).clip(CircleShape)
            .background(Brush.radialGradient(listOf(Color(0xFF444444), Color(0xFF111111)))),
        contentAlignment = Alignment.Center
    ) { Avatar(name, 24) }
}

@Composable
fun VideoPlayer(path: String, playing: Boolean, sound: Boolean, speed: Float, onProgress: (Float) -> Unit) {
    val ctx = LocalContext.current
    val player = remember(path) {
        ExoPlayer.Builder(ctx).build().apply {
            setMediaItem(MediaItem.fromUri(Uri.fromFile(File(path))))
            repeatMode = Player.REPEAT_MODE_ONE
            prepare()
        }
    }
    DisposableEffect(player) { onDispose { player.release() } }
    LaunchedEffect(playing) { player.playWhenReady = playing }
    LaunchedEffect(sound) { player.volume = if (sound) 1f else 0f }
    LaunchedEffect(speed) { player.setPlaybackSpeed(speed) }
    LaunchedEffect(playing) {
        while (playing) {
            val d = player.duration
            if (d > 0) onProgress((player.currentPosition.toFloat() / d).coerceIn(0f, 1f))
            delay(100)
        }
    }
    AndroidView(
        factory = {
            PlayerView(it).apply {
                useController = false
                resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT
                setShutterBackgroundColor(android.graphics.Color.BLACK)
                this.player = player
            }
        },
        update = { it.player = player },
        modifier = Modifier.fillMaxSize()
    )
}
