package com.local.tiktok

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File

// ---------------- Splash ----------------

@Composable
fun SplashScreen(onDone: () -> Unit) {
    val moonScale = remember { Animatable(0f) }
    val textAlpha = remember { Animatable(0f) }
    val subAlpha = remember { Animatable(0f) }
    val inf = rememberInfiniteTransition(label = "splash")
    val ring by inf.animateFloat(0f, 360f, infiniteRepeatable(tween(2200, easing = LinearEasing)), label = "ring")
    val glow by inf.animateFloat(.6f, 1f, infiniteRepeatable(tween(900), RepeatMode.Reverse), label = "glow")
    LaunchedEffect(Unit) {
        launch { moonScale.animateTo(1f, spring(dampingRatio = .4f, stiffness = 120f)) }
        delay(350); launch { textAlpha.animateTo(1f, tween(500)) }
        delay(350); subAlpha.animateTo(1f, tween(500))
        delay(900)
        onDone()
    }
    Box(
        Modifier.fillMaxSize().background(
            Brush.radialGradient(listOf(Color(0xFF1B1030), Color.Black), radius = 1400f)
        ),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(contentAlignment = Alignment.Center) {
                Box(
                    Modifier.size(170.dp).rotate(ring).clip(CircleShape)
                        .background(Brush.sweepGradient(listOf(Cyan, Color.Transparent, Pink, Color.Transparent, Cyan)))
                        .alpha(glow)
                )
                Box(Modifier.size(158.dp).clip(CircleShape).background(Color.Black))
                Text("🌝", fontSize = 96.sp, modifier = Modifier.scale(moonScale.value))
            }
            Spacer(Modifier.height(28.dp))
            Row(Modifier.alpha(textAlpha.value)) {
                Text("Local ", color = Cyan, fontSize = 38.sp, fontWeight = FontWeight.Black)
                Text("TikTok", color = Pink, fontSize = 38.sp, fontWeight = FontWeight.Black)
            }
            Text(
                "Смотри тт без интернета", color = Color.White.copy(.75f), fontSize = 16.sp,
                modifier = Modifier.alpha(subAlpha.value).padding(top = 6.dp)
            )
        }
        Text(
            "offline • 🌝 • только твоё", color = Color.White.copy(.35f), fontSize = 12.sp,
            modifier = Modifier.align(Alignment.BottomCenter).navigationBarsPadding().padding(bottom = 24.dp).alpha(subAlpha.value)
        )
    }
}

@Composable
fun PermissionScreen(onRequest: () -> Unit) {
    Column(
        Modifier.fillMaxSize().padding(32.dp),
        verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("🌚", fontSize = 80.sp)
        Spacer(Modifier.height(16.dp))
        Text("Нужен доступ к видео и фото", color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
        Text(
            "Чтобы показывать твои альбомы без интернета", color = Color.White.copy(.6f),
            textAlign = TextAlign.Center, modifier = Modifier.padding(top = 8.dp)
        )
        Spacer(Modifier.height(24.dp))
        Button(onClick = onRequest, colors = ButtonDefaults.buttonColors(containerColor = Pink)) {
            Text("Разрешить", fontWeight = FontWeight.Bold)
        }
    }
}

// ---------------- Main with bottom bar ----------------

private enum class Tab { HOME, DISCOVER, PROFILE }

@Composable
fun MainScreen(app: AppState, push: (Screen) -> Unit) {
    var tab by rememberSaveable { mutableStateOf(Tab.HOME) }
    var forYou by rememberSaveable { mutableStateOf(true) }
    val openAlbum: (String) -> Unit = { push(Screen.AlbumProfile(it)) }

    Column(Modifier.fillMaxSize().background(Color.Black)) {
        Box(Modifier.weight(1f).fillMaxWidth()) {
            when (tab) {
                Tab.HOME -> {
                    val list = if (forYou) app.forYou else remember(app.posts, app.feedVersion) { app.newest() }
                    Feed(
                        app, list, 0, active = true, onOpenAlbum = openAlbum, resetKey = "$forYou-${app.feedVersion}",
                        onNearEnd = if (forYou) ({ app.extendForYou() }) else null
                    )
                    Text(
                        "🔍", fontSize = 22.sp,
                        modifier = Modifier.align(Alignment.TopEnd).statusBarsPadding().padding(top = 6.dp, end = 12.dp)
                            .clickable { push(Screen.Search) }.padding(6.dp)
                    )
                    Row(
                        Modifier.align(Alignment.TopCenter).statusBarsPadding().padding(top = 10.dp),
                        horizontalArrangement = Arrangement.spacedBy(20.dp), verticalAlignment = Alignment.CenterVertically
                    ) {
                        TopTab("Новое", !forYou) { forYou = false }
                        TopTab("Для вас", forYou) { if (forYou) app.reshuffle() else forYou = true }
                    }
                    if (app.loading) LinearProgressIndicator(
                        Modifier.fillMaxWidth().align(Alignment.TopCenter).statusBarsPadding(), color = Pink
                    )
                }
                Tab.DISCOVER -> DiscoverScreen(app, openAlbum)
                Tab.PROFILE -> MyProfileScreen(app, push)
            }
        }
        BottomBar(tab, onTab = { tab = it }, onShuffle = { app.reshuffle(); tab = Tab.HOME; forYou = true })
    }
}

@Composable
private fun TopTab(text: String, selected: Boolean, onClick: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.clickable(onClick = onClick)) {
        Text(
            text, color = if (selected) Color.White else Color.White.copy(.6f),
            fontSize = 17.sp, fontWeight = if (selected) FontWeight.Bold else FontWeight.SemiBold
        )
        val w by animateDpAsState(if (selected) 24.dp else 0.dp, label = "tab")
        Box(Modifier.padding(top = 4.dp).height(3.dp).width(w).clip(CircleShape).background(Color.White))
    }
}

@Composable
private fun BottomBar(tab: Tab, onTab: (Tab) -> Unit, onShuffle: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().background(Color.Black).navigationBarsPadding().height(58.dp),
        horizontalArrangement = Arrangement.SpaceAround, verticalAlignment = Alignment.CenterVertically
    ) {
        NavItem("🏠", "Главная", tab == Tab.HOME) { onTab(Tab.HOME) }
        NavItem("🔍", "Профили", tab == Tab.DISCOVER) { onTab(Tab.DISCOVER) }
        Box(
            Modifier.width(48.dp).height(32.dp).clickable(onClick = onShuffle),
            contentAlignment = Alignment.Center
        ) {
            Box(Modifier.offset(x = (-4).dp).fillMaxSize().clip(RoundedCornerShape(9.dp)).background(Cyan))
            Box(Modifier.offset(x = 4.dp).fillMaxSize().clip(RoundedCornerShape(9.dp)).background(Pink))
            Box(Modifier.fillMaxSize().clip(RoundedCornerShape(9.dp)).background(Color.White), contentAlignment = Alignment.Center) {
                Text("🔀", fontSize = 16.sp)
            }
        }
        NavItem("🌝", "Лайки", false) { onTab(Tab.PROFILE) }
        NavItem("👤", "Профиль", tab == Tab.PROFILE) { onTab(Tab.PROFILE) }
    }
}

@Composable
private fun NavItem(icon: String, label: String, selected: Boolean, onClick: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clickable(onClick = onClick).padding(horizontal = 6.dp).alpha(if (selected) 1f else .6f)
    ) {
        Text(icon, fontSize = 20.sp)
        Text(label, color = Color.White, fontSize = 10.sp, fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal)
    }
}

// ---------------- Discover (album profiles) ----------------

@Composable
fun DiscoverScreen(app: AppState, openAlbum: (String) -> Unit) {
    val names = app.albums.filter { it.enabled }.map { it.title }
    LazyColumn(Modifier.fillMaxSize().statusBarsPadding(), contentPadding = PaddingValues(16.dp)) {
        item {
            Text("Профили", color = Color.White, fontSize = 26.sp, fontWeight = FontWeight.Black)
            Text("Каждый альбом — отдельный автор", color = Color.White.copy(.5f), fontSize = 13.sp)
            Spacer(Modifier.height(16.dp))
        }
        items(names) { name ->
            val posts = app.posts.filter { it.album == name }
            val videos = posts.count { it.isVideo }
            val views = posts.sumOf { app.views[it.id] ?: 0 }
            Row(
                Modifier.fillMaxWidth().padding(vertical = 6.dp).clip(RoundedCornerShape(16.dp))
                    .background(Color(0xFF161616)).clickable { openAlbum(name) }.padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Avatar(name, 56)
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text("@$name", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    Text(
                        "🎬 $videos  •  🖼 ${posts.size - videos}  •  👁 ${formatCount(views)}",
                        color = Color.White.copy(.6f), fontSize = 13.sp
                    )
                }
                posts.firstOrNull()?.let {
                    AsyncImage(
                        File(it.items.first().path), null, contentScale = ContentScale.Crop,
                        modifier = Modifier.size(48.dp, 64.dp).clip(RoundedCornerShape(8.dp))
                    )
                }
            }
        }
        if (names.isEmpty()) item {
            Text("Нет включённых альбомов. Добавь их в Профиль → ⚙", color = Color.White.copy(.6f))
        }
    }
}

// ---------------- Grids ----------------

@Composable
fun PostThumb(app: AppState, post: Post, onClick: () -> Unit) {
    Box(Modifier.aspectRatio(3f / 4f).padding(1.dp).background(Color(0xFF1A1A1A)).clickable(onClick = onClick)) {
        AsyncImage(File(post.items.first().path), null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
        if (post.items.size > 1) Text("🖼${post.items.size}", fontSize = 11.sp, color = Color.White,
            modifier = Modifier.align(Alignment.TopEnd).padding(4.dp))
        Row(Modifier.align(Alignment.BottomStart).padding(4.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(if (post.isVideo) "▷ " else "👁 ", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            Text(formatCount(app.views[post.id] ?: 0), color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
        }
        if (post.id in app.likes) Text("🌝", fontSize = 12.sp, modifier = Modifier.align(Alignment.BottomEnd).padding(4.dp))
    }
}

@Composable
private fun Stat(value: String, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 17.sp)
        Text(label, color = Color.White.copy(.55f), fontSize = 12.sp)
    }
}

// ---------------- Album profile ----------------

@Composable
fun AlbumScreen(app: AppState, album: String, push: (Screen) -> Unit, pop: () -> Unit) {
    val posts = remember(app.posts, album) { app.postsOf(album) }
    val path = app.albums.firstOrNull { it.title == album }?.path ?: ""
    var filter by remember { mutableIntStateOf(0) }
    val shown = when (filter) { 1 -> posts.filter { it.isVideo }; 2 -> posts.filter { !it.isVideo }; else -> posts }
    LazyVerticalGrid(GridCells.Fixed(3), Modifier.fillMaxSize().background(Color.Black).statusBarsPadding()) {
        item(span = { GridItemSpan(3) }) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                Row(Modifier.fillMaxWidth().padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text("‹", color = Color.White, fontSize = 34.sp, modifier = Modifier.clickable(onClick = pop).padding(horizontal = 10.dp))
                    Text(album, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 17.sp, modifier = Modifier.weight(1f), textAlign = TextAlign.Center)
                    Spacer(Modifier.width(40.dp))
                }
                Avatar(album, 96)
                Text("@$album", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp, modifier = Modifier.padding(top = 10.dp))
                Text(path, color = Color.White.copy(.4f), fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(horizontal = 24.dp))
                Row(Modifier.padding(vertical = 16.dp), horizontalArrangement = Arrangement.spacedBy(28.dp)) {
                    Stat(posts.size.toString(), "Постов")
                    Stat(formatCount(posts.sumOf { app.views[it.id] ?: 0 }), "Просмотров")
                    Stat(posts.count { it.id in app.likes }.toString(), "🌝")
                }
                Button(
                    onClick = { if (posts.isNotEmpty()) push(Screen.FeedOf("@$album", posts.shuffled().map { it.id }, 0)) },
                    colors = ButtonDefaults.buttonColors(containerColor = Pink), modifier = Modifier.width(220.dp)
                ) { Text("▶  Смотреть подряд", fontWeight = FontWeight.Bold) }
                Row(Modifier.padding(vertical = 12.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    listOf("Все", "🎬 Видео", "🖼 Фото").forEachIndexed { i, t ->
                        FilterChip(selected = filter == i, onClick = { filter = i }, label = { Text(t) })
                    }
                }
            }
        }
        items(shown, key = { it.id }) { post ->
            PostThumb(app, post) { push(Screen.FeedOf("@$album", shown.map { it.id }, shown.indexOf(post))) }
        }
    }
}

// ---------------- Standalone feed ----------------

@Composable
fun StandaloneFeed(app: AppState, title: String, posts: List<Post>, start: Int, push: (Screen) -> Unit, pop: () -> Unit) {
    Box(Modifier.fillMaxSize().background(Color.Black)) {
        Feed(app, posts, start, active = true, onOpenAlbum = { push(Screen.AlbumProfile(it)) })
        Row(Modifier.statusBarsPadding().padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("‹", color = Color.White, fontSize = 36.sp, modifier = Modifier.clickable(onClick = pop).padding(horizontal = 10.dp))
            Text(title, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
        }
    }
}

// ---------------- My profile ----------------

@Composable
fun MyProfileScreen(app: AppState, push: (Screen) -> Unit) {
    var tab by rememberSaveable { mutableIntStateOf(0) }
    val byId = remember(app.posts) { app.postsById }
    val ids: List<String> = when (tab) {
        0 -> app.likes.toList()
        1 -> app.saved.toList()
        else -> app.history.toList()
    }
    val list = ids.mapNotNull { byId[it] }
    var editing by remember { mutableStateOf(false) }

    LazyVerticalGrid(GridCells.Fixed(3), Modifier.fillMaxSize().background(Color.Black).statusBarsPadding()) {
        item(span = { GridItemSpan(3) }) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Spacer(Modifier.width(36.dp))
                    Text(app.nickname, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 17.sp,
                        modifier = Modifier.weight(1f), textAlign = TextAlign.Center)
                    Text("⚙", fontSize = 24.sp, color = Color.White, modifier = Modifier.clickable { push(Screen.Settings) }.padding(6.dp))
                }
                Box(Modifier.size(100.dp).border(3.dp, Brush.linearGradient(listOf(Cyan, Pink)), CircleShape).padding(5.dp)) {
                    Avatar(app.nickname, 90, emoji = "🌝")
                }
                Text("@${app.nickname}", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp, modifier = Modifier.padding(top = 10.dp))
                Row(Modifier.padding(vertical = 14.dp), horizontalArrangement = Arrangement.spacedBy(26.dp)) {
                    Stat(formatCount(app.totalViews), "Просмотров")
                    Stat(app.likes.size.toString(), "🌝 Лайков")
                    Stat(app.saved.size.toString(), "Сохранено")
                    Stat(app.posts.size.toString(), "Постов")
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = { editing = true }) { Text("Изменить профиль", color = Color.White) }
                    OutlinedButton(onClick = { push(Screen.Settings) }) { Text("Альбомы и настройки", color = Color.White) }
                }
                Button(
                    onClick = { push(Screen.Stats) }, modifier = Modifier.padding(top = 8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Pink)
                ) { Text("📊 Мои итоги  •  🔥 ${app.streak()}", fontWeight = FontWeight.Bold) }
                Text(app.bio, color = Color.White.copy(.8f), fontSize = 14.sp, modifier = Modifier.padding(12.dp), textAlign = TextAlign.Center)
                Row(Modifier.fillMaxWidth()) {
                    listOf("🌝 Лайки", "⭐ Сохранённое", "🕘 История").forEachIndexed { i, t ->
                        Column(Modifier.weight(1f).clickable { tab = i }, horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(t, color = if (tab == i) Color.White else Color.White.copy(.5f), fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.padding(vertical = 10.dp))
                            Box(Modifier.fillMaxWidth(.6f).height(2.dp).background(if (tab == i) Color.White else Color.Transparent))
                        }
                    }
                }
                if (list.isEmpty()) Text(
                    when (tab) { 0 -> "Дважды тапни по видео, чтобы поставить 🌝"; 1 -> "Жми ☆ в ленте, чтобы сохранить"; else -> "Ты ещё ничего не смотрел" },
                    color = Color.White.copy(.5f), modifier = Modifier.padding(32.dp), textAlign = TextAlign.Center
                )
            }
        }
        items(list, key = { "p" + it.id }) { post ->
            PostThumb(app, post) { push(Screen.FeedOf(app.nickname, list.map { it.id }, list.indexOf(post))) }
        }
    }

    if (editing) {
        var nick by remember { mutableStateOf(app.nickname) }
        var bio by remember { mutableStateOf(app.bio) }
        AlertDialog(
            onDismissRequest = { editing = false },
            title = { Text("Профиль") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(nick, { nick = it }, label = { Text("Ник") }, singleLine = true)
                    OutlinedTextField(bio, { bio = it }, label = { Text("О себе") })
                }
            },
            confirmButton = {
                TextButton({ app.nickname = nick.ifBlank { "Я" }; app.bio = bio; app.persistSettings(); editing = false }) { Text("Сохранить") }
            },
            dismissButton = { TextButton({ editing = false }) { Text("Отмена") } }
        )
    }
}

// ---------------- Settings ----------------

@Composable
fun SettingsScreen(app: AppState, pop: () -> Unit) {
    val scope = rememberCoroutineScope()
    var albums by remember { mutableStateOf(app.albums) }
    var newPath by remember { mutableStateOf("/storage/emulated/0/") }
    var confirmReset by remember { mutableStateOf(false) }
    fun save(list: List<Album>) { albums = list; app.updateAlbums(list); scope.launch { app.reload() } }

    LazyColumn(Modifier.fillMaxSize().background(Color.Black).statusBarsPadding(), contentPadding = PaddingValues(16.dp)) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("‹", color = Color.White, fontSize = 34.sp, modifier = Modifier.clickable(onClick = pop).padding(end = 12.dp))
                Text("Настройки", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Black)
            }
            Section("📁 Альбомы")
            Text("Каждый путь — папка с видео/фото. Файлы ${app.files.size} шт.", color = Color.White.copy(.5f), fontSize = 12.sp)
        }
        items(albums, key = { it.path }) { a ->
            val count = app.files.count { it.album == a.title }
            Row(
                Modifier.fillMaxWidth().padding(vertical = 4.dp).clip(RoundedCornerShape(12.dp)).background(Color(0xFF161616)).padding(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Avatar(a.title, 36)
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Text("@${a.title}" + if (a.enabled) "  ($count)" else "", color = Color.White, fontWeight = FontWeight.SemiBold)
                    Text(a.path, color = Color.White.copy(.45f), fontSize = 11.sp, maxLines = 2)
                    if (a.enabled && count == 0 && !app.loading) Text("⚠ пусто или нет доступа", color = Pink, fontSize = 11.sp)
                    if (a.enabled) {
                        Text("Шанс в ленте: ${a.weight}%", color = Cyan, fontSize = 12.sp, modifier = Modifier.padding(top = 4.dp))
                        Slider(
                            value = a.weight.toFloat(),
                            onValueChange = { v -> albums = albums.map { if (it.path == a.path) it.copy(weight = v.toInt()) else it } },
                            onValueChangeFinished = { app.updateAlbums(albums); app.reshuffle() },
                            valueRange = 0f..100f,
                            colors = SliderDefaults.colors(thumbColor = Cyan, activeTrackColor = Cyan)
                        )
                    }
                }
                Switch(a.enabled, { on -> save(albums.map { if (it.path == a.path) it.copy(enabled = on) else it }) })
                Text("✕", color = Color.White.copy(.6f), fontSize = 18.sp,
                    modifier = Modifier.clickable { save(albums.filter { it.path != a.path }) }.padding(8.dp))
            }
        }
        item {
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(newPath, { newPath = it }, label = { Text("Путь к папке") }, singleLine = true, modifier = Modifier.fillMaxWidth())
            Row(Modifier.padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = {
                        val p = newPath.trim().trimEnd('/')
                        if (p.isNotEmpty() && albums.none { it.path == p }) save(albums + Album(p, File(p).name))
                        newPath = "/storage/emulated/0/"
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Pink)
                ) { Text("+ Добавить") }
            }
            val missing = SUGGESTED_ALBUMS.filter { d -> albums.none { it.path == d.path } }
            if (missing.isNotEmpty()) {
                Text("Быстро добавить:", color = Color.White.copy(.5f), fontSize = 12.sp, modifier = Modifier.padding(top = 10.dp))
                missing.forEach { d ->
                    Text("+ ${d.path}", color = Cyan, fontSize = 13.sp,
                        modifier = Modifier.clickable { save(albums + d) }.padding(vertical = 4.dp))
                }
            }

            Section("🎬 Лента")
            Toggle("Показывать видео", app.showVideos) { app.showVideos = it; app.persistSettings(); app.reshuffle() }
            Toggle("Показывать картинки", app.showImages) { app.showImages = it; app.persistSettings(); app.reshuffle() }
            Toggle("Звук по умолчанию", app.soundOn) { app.soundOn = it; app.persistSettings() }
            Text("Шанс картинок: ${app.imageChance}%  (видео ${100 - app.imageChance}%)", color = Color.White, modifier = Modifier.padding(top = 8.dp))
            Slider(
                value = app.imageChance.toFloat(), onValueChange = { app.imageChance = it.toInt() },
                onValueChangeFinished = { app.persistSettings(); app.reshuffle() },
                valueRange = 0f..100f
            )
            Text("Не просмотрено в ленте: ${app.unseenCount} из ${app.eligibleCount}", color = Color.White.copy(.5f), fontSize = 12.sp)
            Text("Картинок в посте: до ${app.maxImages}", color = Color.White, modifier = Modifier.padding(top = 8.dp))
            Slider(
                value = app.maxImages.toFloat(), onValueChange = { app.maxImages = it.toInt() },
                onValueChangeFinished = { app.persistSettings(); app.rebuild() },
                valueRange = 1f..3f, steps = 1
            )
            Button({ scope.launch { app.reload() } }, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2A2A2A))) {
                Text("🔄 Пересканировать альбомы")
            }

            Section("✨ Внешний вид")
            Toggle("Заставка при запуске", app.showSplash) { app.showSplash = it; app.persistSettings() }

            Section("📊 Статистика")
            Text("Всего просмотров: ${app.totalViews} • Лайков: ${app.likes.size}", color = Color.White.copy(.7f))
            TextButton({ confirmReset = true }) { Text("Сбросить просмотры, лайки и историю", color = Pink) }

            Spacer(Modifier.height(24.dp))
            Text("Local TikTok • смотри тт без интернета 🌝", color = Color.White.copy(.3f), fontSize = 12.sp,
                modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
            Spacer(Modifier.navigationBarsPadding().height(24.dp))
        }
    }
    if (confirmReset) AlertDialog(
        onDismissRequest = { confirmReset = false },
        title = { Text("Сбросить всё?") },
        text = { Text("Просмотры, 🌝, сохранённое и история будут удалены.") },
        confirmButton = { TextButton({ app.resetStats(); confirmReset = false }) { Text("Сбросить", color = Pink) } },
        dismissButton = { TextButton({ confirmReset = false }) { Text("Отмена") } }
    )
}

@Composable
private fun Section(title: String) {
    Text(title, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp, modifier = Modifier.padding(top = 22.dp, bottom = 6.dp))
}

@Composable
private fun Toggle(label: String, value: Boolean, onChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth().padding(vertical = 2.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(label, color = Color.White, modifier = Modifier.weight(1f))
        Switch(value, onChange, colors = SwitchDefaults.colors(checkedTrackColor = Pink))
    }
}
