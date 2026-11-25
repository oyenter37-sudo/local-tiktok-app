package com.local.tiktok

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
private fun Header(title: String, pop: () -> Unit) {
    Row(Modifier.fillMaxWidth().padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
        Text("‹", color = Color.White, fontSize = 34.sp, modifier = Modifier.clickable(onClick = pop).padding(horizontal = 10.dp))
        Text(title, color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Black)
    }
}

@Composable
private fun BigCard(emoji: String, value: String, label: String, modifier: Modifier) {
    Column(
        modifier.clip(RoundedCornerShape(16.dp)).background(Color(0xFF161616)).padding(14.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(emoji, fontSize = 26.sp)
        Text(value, color = Color.White, fontWeight = FontWeight.Black, fontSize = 20.sp)
        Text(label, color = Color.White.copy(.55f), fontSize = 12.sp)
    }
}

// ---------------- Мои итоги ----------------

@Composable
fun StatsScreen(app: AppState, push: (Screen) -> Unit, pop: () -> Unit) {
    val byId = remember(app.posts) { app.postsById }
    val total = app.watch["total"] ?: 0L
    val todayMs = app.watch["d:" + today()] ?: 0L
    val streak = app.streak()
    val days = app.watchedDays().size
    val topAlbums = app.albums.map { it.title to (app.watch["a:" + it.title] ?: 0L) }
        .filter { it.second > 0 }.sortedByDescending { it.second }.take(5)
    val maxAlbum = topAlbums.firstOrNull()?.second ?: 1L
    val rewatched = app.views.entries.filter { it.value > 1 && it.key in byId }
        .sortedByDescending { it.value }.take(12).mapNotNull { byId[it.key] }

    LazyVerticalGrid(GridCells.Fixed(3), Modifier.fillMaxSize().background(Color.Black).statusBarsPadding()) {
        item(span = { GridItemSpan(3) }) {
            Column(Modifier.fillMaxWidth().padding(horizontal = 12.dp)) {
                Header("📊 Мои итоги", pop)
                Box(
                    Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp))
                        .background(Brush.linearGradient(listOf(Pink, Color(0xFF8E2DE2), Cyan))).padding(20.dp)
                ) {
                    Column {
                        Text("Всего смотрел", color = Color.White.copy(.85f))
                        Text(formatDuration(total), color = Color.White, fontSize = 34.sp, fontWeight = FontWeight.Black)
                        Text("Сегодня: ${formatDuration(todayMs)}", color = Color.White)
                    }
                    Text("🔥 $streak", color = Color.White, fontSize = 30.sp, fontWeight = FontWeight.Black,
                        modifier = Modifier.align(Alignment.TopEnd))
                }
                Spacer(Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    BigCard("🔥", "$streak", "серия дней", Modifier.weight(1f))
                    BigCard("📅", "$days", "дней с тт", Modifier.weight(1f))
                    BigCard("👁", formatCount(app.totalViews), "просмотров", Modifier.weight(1f))
                }
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    BigCard("🌝", "${app.likes.size}", "лайков", Modifier.weight(1f))
                    BigCard("✅", "${app.eligibleCount - app.unseenCount}/${app.eligibleCount}", "просмотрено", Modifier.weight(1f))
                    BigCard("⭐", "${app.saved.size}", "сохранено", Modifier.weight(1f))
                }

                Text("🏆 Топ альбомов", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp,
                    modifier = Modifier.padding(top = 20.dp, bottom = 8.dp))
                if (topAlbums.isEmpty()) Text("Пока пусто — посмотри что-нибудь 🌝", color = Color.White.copy(.5f))
                topAlbums.forEachIndexed { i, (name, ms) ->
                    Row(
                        Modifier.fillMaxWidth().padding(vertical = 4.dp).clickable { push(Screen.AlbumProfile(name)) },
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(listOf("🥇", "🥈", "🥉", "4", "5")[i], color = Color.White, fontSize = 18.sp, modifier = Modifier.width(30.dp))
                        Avatar(name, 36)
                        Spacer(Modifier.width(10.dp))
                        Column(Modifier.weight(1f)) {
                            Text("@$name", color = Color.White, fontWeight = FontWeight.SemiBold)
                            Box(Modifier.fillMaxWidth().height(6.dp).clip(CircleShape).background(Color.White.copy(.1f))) {
                                Box(Modifier.fillMaxHeight().fillMaxWidth(ms.toFloat() / maxAlbum).background(avatarBrush(name)))
                            }
                        }
                        Text(formatDuration(ms), color = Color.White.copy(.7f), fontSize = 12.sp, modifier = Modifier.padding(start = 8.dp))
                    }
                }

                Text("🔁 Самые пересматриваемые", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp,
                    modifier = Modifier.padding(top = 20.dp, bottom = 8.dp))
                if (rewatched.isEmpty()) Text("Ещё ничего не пересматривал", color = Color.White.copy(.5f),
                    modifier = Modifier.padding(bottom = 24.dp))
            }
        }
        items(rewatched, key = { "r" + it.id }) { post ->
            PostThumb(app, post) { push(Screen.FeedOf("🔁 Пересматриваемые", rewatched.map { it.id }, rewatched.indexOf(post))) }
        }
        item(span = { GridItemSpan(3) }) { Spacer(Modifier.navigationBarsPadding().height(24.dp)) }
    }
}

// ---------------- Поиск ----------------

@Composable
fun SearchScreen(app: AppState, push: (Screen) -> Unit, pop: () -> Unit) {
    var q by rememberSaveableQuery()
    val focus = remember { FocusRequester() }
    LaunchedEffect(Unit) { runCatching { focus.requestFocus() } }
    val query = q.trim()
    val albums = remember(query, app.albums) {
        if (query.isEmpty()) emptyList() else app.albums.filter { it.enabled && it.title.contains(query, true) }.map { it.title }
    }
    val results = remember(query, app.posts) {
        if (query.isEmpty()) emptyList()
        else app.posts.filter { p -> p.album.contains(query, true) || p.items.any { it.title.contains(query, true) } }
            .sortedByDescending { it.modified }
    }

    LazyVerticalGrid(GridCells.Fixed(3), Modifier.fillMaxSize().background(Color.Black).statusBarsPadding()) {
        item(span = { GridItemSpan(3) }) {
            Column(Modifier.fillMaxWidth().padding(horizontal = 12.dp)) {
                Header("🔍 Поиск", pop)
                OutlinedTextField(
                    q, { q = it }, singleLine = true, placeholder = { Text("Имя файла или альбома") },
                    modifier = Modifier.fillMaxWidth().focusRequester(focus)
                )
                albums.forEach { name ->
                    Row(
                        Modifier.fillMaxWidth().padding(top = 8.dp).clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFF161616)).clickable { push(Screen.AlbumProfile(name)) }.padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Avatar(name, 40)
                        Spacer(Modifier.width(10.dp))
                        Text("@$name", color = Color.White, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                        Text("профиль ›", color = Cyan, fontSize = 13.sp)
                    }
                }
                Text(
                    when {
                        query.isEmpty() -> "Начни вводить…"
                        results.isEmpty() -> "Ничего не нашлось 🌚"
                        else -> "Найдено: ${results.size}"
                    },
                    color = Color.White.copy(.5f), modifier = Modifier.padding(vertical = 10.dp)
                )
            }
        }
        items(results, key = { "s" + it.id }) { post ->
            PostThumb(app, post) { push(Screen.FeedOf("🔍 $query", results.map { it.id }, results.indexOf(post))) }
        }
    }
}

@Composable
private fun rememberSaveableQuery() = androidx.compose.runtime.saveable.rememberSaveable { mutableStateOf("") }
