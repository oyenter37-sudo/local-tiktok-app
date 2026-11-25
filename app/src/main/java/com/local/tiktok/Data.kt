package com.local.tiktok

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import kotlin.random.Random

data class Album(val path: String, val name: String, val enabled: Boolean = true, val weight: Int = 50) {
    val title: String get() = name.ifBlank { File(path).name.ifBlank { path } }
}

enum class MediaType { VIDEO, IMAGE }

data class MediaFile(val path: String, val type: MediaType, val album: String, val modified: Long) {
    val title: String get() = File(path).nameWithoutExtension
}

data class Post(val id: String, val album: String, val items: List<MediaFile>) {
    val isVideo: Boolean get() = items.first().type == MediaType.VIDEO
    val modified: Long get() = items.maxOf { it.modified }
}

private const val ROOT = "/storage/emulated/0"

/** No albums by default — the user adds their own folders. */
val DEFAULT_ALBUMS = emptyList<Album>()

/** Common folders offered as quick-add suggestions. */
val SUGGESTED_ALBUMS = listOf(
    Album("$ROOT/DCIM/Camera", "Camera"),
    Album("$ROOT/Movies", "Movies"),
    Album("$ROOT/Pictures", "Pictures"),
    Album("$ROOT/Download", "Download"),
    Album("$ROOT/Pictures/Screenshots", "Screenshots"),
    Album("$ROOT/Pictures/Telegram", "Telegram"),
)

object Scanner {
    private val videoExt = setOf("mp4", "webm", "mkv", "3gp", "mov", "m4v")
    private val imageExt = setOf("jpg", "jpeg", "png", "webp", "gif", "bmp", "heic")

    fun scan(albums: List<Album>): List<MediaFile> = albums.filter { it.enabled }.flatMap { album ->
        val dir = File(album.path)
        (dir.listFiles() ?: emptyArray()).mapNotNull { f ->
            if (!f.isFile || f.name.startsWith(".")) return@mapNotNull null
            val ext = f.extension.lowercase()
            val type = when (ext) {
                in videoExt -> MediaType.VIDEO
                in imageExt -> MediaType.IMAGE
                else -> return@mapNotNull null
            }
            MediaFile(f.absolutePath, type, album.title, f.lastModified())
        }
    }

    /** Videos are single posts; pictures are grouped randomly into 1..maxImages per post. */
    fun buildPosts(files: List<MediaFile>, maxImages: Int, rnd: Random): List<Post> {
        val out = ArrayList<Post>()
        files.groupBy { it.album }.forEach { (album, list) ->
            list.filter { it.type == MediaType.VIDEO }.forEach { out += Post(it.path, album, listOf(it)) }
            val imgs = list.filter { it.type == MediaType.IMAGE }.sortedBy { it.modified }
            var i = 0
            while (i < imgs.size) {
                val n = rnd.nextInt(1, maxImages.coerceIn(1, 3) + 1).coerceAtMost(imgs.size - i)
                val group = imgs.subList(i, i + n)
                out += Post("g:" + group.first().path, album, group.toList())
                i += n
            }
        }
        return out
    }
}

class Store(ctx: Context) {
    private val p = ctx.getSharedPreferences("ltt", Context.MODE_PRIVATE)
    private val v = ctx.getSharedPreferences("views", Context.MODE_PRIVATE)
    private val t = ctx.getSharedPreferences("time", Context.MODE_PRIVATE)

    fun allTime(): Map<String, Long> = t.all.mapNotNull { (k, x) -> (x as? Long)?.let { k to it } }.toMap()
    fun setTime(key: String, ms: Long) = t.edit().putLong(key, ms).apply()
    fun clearTime() = t.edit().clear().apply()

    var albums: List<Album>
        get() {
            val s = p.getString("albums", null) ?: return DEFAULT_ALBUMS
            return runCatching {
                val a = JSONArray(s)
                (0 until a.length()).map {
                    val o = a.getJSONObject(it)
                    Album(o.getString("path"), o.optString("name"), o.optBoolean("on", true), o.optInt("w", 50))
                }
            }.getOrDefault(DEFAULT_ALBUMS)
        }
        set(value) {
            val a = JSONArray()
            value.forEach { a.put(JSONObject().put("path", it.path).put("name", it.name).put("on", it.enabled).put("w", it.weight)) }
            p.edit().putString("albums", a.toString()).apply()
        }

    fun bool(key: String, def: Boolean) = p.getBoolean(key, def)
    fun setBool(key: String, value: Boolean) = p.edit().putBoolean(key, value).apply()
    fun int(key: String, def: Int) = p.getInt(key, def)
    fun setInt(key: String, value: Int) = p.edit().putInt(key, value).apply()
    fun str(key: String, def: String) = p.getString(key, def) ?: def
    fun setStr(key: String, value: String) = p.edit().putString(key, value).apply()
    fun set(key: String): Set<String> = p.getStringSet(key, emptySet())?.toSet() ?: emptySet()
    fun putSet(key: String, value: Set<String>) = p.edit().putStringSet(key, value).apply()
    fun list(key: String): List<String> = runCatching {
        val a = JSONArray(p.getString(key, "[]"))
        (0 until a.length()).map { a.getString(it) }
    }.getOrDefault(emptyList())
    fun putList(key: String, value: List<String>) = p.edit().putString(key, JSONArray(value).toString()).apply()

    fun allViews(): Map<String, Int> = v.all.mapNotNull { (k, x) -> (x as? Int)?.let { k to it } }.toMap()
    fun setViews(id: String, n: Int) = v.edit().putInt(id, n).apply()
    fun clearViews() = v.edit().clear().apply()
}

/** All observable app state. */
class AppState(ctx: Context) {
    val store = Store(ctx)

    var albums by mutableStateOf(store.albums)
        private set
    var showSplash by mutableStateOf(store.bool("splash", true))
    var soundOn by mutableStateOf(store.bool("sound", true))
    var showImages by mutableStateOf(store.bool("images", true))
    var showVideos by mutableStateOf(store.bool("videos", true))
    var maxImages by mutableStateOf(store.int("maxImages", 3))
    var imageChance by mutableStateOf(store.int("imgChance", 30))
    var seen by mutableStateOf(store.set("seen"))
        private set
    val watch = mutableStateMapOf<String, Long>().apply { putAll(store.allTime()) }
    var nickname by mutableStateOf(store.str("nick", "Я"))
    var bio by mutableStateOf(store.str("bio", "Смотрю тт без интернета 🌝"))

    var likes by mutableStateOf(store.set("likes"))
        private set
    var saved by mutableStateOf(store.set("saved"))
        private set
    val history = mutableStateListOf<String>().apply { addAll(store.list("history")) }
    val views = mutableStateMapOf<String, Int>().apply { putAll(store.allViews()) }

    var loading by mutableStateOf(true)
    var files by mutableStateOf<List<MediaFile>>(emptyList())
        private set
    var posts by mutableStateOf<List<Post>>(emptyList())
        private set
    var forYou by mutableStateOf<List<Post>>(emptyList())
        private set
    var feedVersion by mutableStateOf(0)
        private set

    val postsById: Map<String, Post> get() = posts.associateBy { it.id }

    suspend fun reload() {
        loading = true
        val al = albums
        val scanned = withContext(Dispatchers.IO) { Scanner.scan(al) }
        files = scanned
        rebuild()
        loading = false
    }

    fun rebuild() {
        val rnd = Random(System.nanoTime())
        val all = Scanner.buildPosts(files, maxImages, rnd)
        posts = all
        reshuffle()
    }

    /** Posts allowed in "For you" by type toggles, image chance and album weights. */
    private fun eligible(): List<Post> {
        val w = albums.associate { it.title to it.weight }
        val anyWeight = w.values.any { it > 0 }
        return posts.filter {
            val typeOk = if (it.isVideo) showVideos && imageChance < 100 else showImages && imageChance > 0
            typeOk && (!anyWeight || (w[it.album] ?: 50) > 0)
        }
    }

    /** Weighted random order: type by imageChance, then album by weight, then random post. */
    private fun weightedOrder(pool: List<Post>, rnd: Random): List<Post> {
        val w = albums.associate { it.title to it.weight }
        val buckets = pool.groupBy { it.isVideo }.mapValues { (_, l) ->
            l.groupBy { it.album }.mapValues { (_, a) -> ArrayDeque(a.shuffled(rnd)) }
        }
        val out = ArrayList<Post>(pool.size)
        while (out.size < pool.size) {
            val v = buckets[true]?.values?.any { it.isNotEmpty() } == true
            val i = buckets[false]?.values?.any { it.isNotEmpty() } == true
            val pickVideo = if (v && i) rnd.nextInt(100) >= imageChance else v
            val albumsMap = buckets[pickVideo] ?: break
            val cands = albumsMap.filter { it.value.isNotEmpty() }.keys.toList()
            if (cands.isEmpty()) break
            val ws = cands.map { (w[it] ?: 50).coerceAtLeast(0) }
            val total = ws.sum()
            val album = if (total <= 0) cands[rnd.nextInt(cands.size)] else {
                var x = rnd.nextInt(total)
                var k = 0
                while (x >= ws[k]) { x -= ws[k]; k++ }
                cands[k]
            }
            out += albumsMap.getValue(album).removeFirst()
        }
        return out
    }

    private fun resetSeen() { seen = emptySet(); store.putSet("seen", seen) }

    /** Fresh "For you": only unseen posts; when everything is seen — reset and full random again. */
    fun reshuffle() {
        val rnd = Random(System.nanoTime())
        val el = eligible()
        var pool = el.filter { it.id !in seen }
        if (pool.isEmpty() && el.isNotEmpty()) { resetSeen(); pool = el }
        forYou = weightedOrder(pool, rnd)
        feedVersion++
    }

    /** Called when the feed reaches its end: append the next batch without jumping. */
    fun extendForYou() {
        val rnd = Random(System.nanoTime())
        val el = eligible()
        val inList = forYou.map { it.id }.toSet()
        var pool = el.filter { it.id !in seen && it.id !in inList }
        if (pool.isEmpty()) {
            resetSeen()
            val last = forYou.lastOrNull()?.id
            pool = el.filter { it.id != last }
        }
        if (pool.isNotEmpty()) forYou = forYou + weightedOrder(pool, rnd)
    }

    val unseenCount: Int get() = eligible().count { it.id !in seen }
    val eligibleCount: Int get() = eligible().size

    fun addWatch(post: Post, ms: Long) {
        if (ms < 300 || ms > 6 * 3600_000L) return
        listOf("p:" + post.id, "a:" + post.album, "d:" + today(), "total").forEach {
            val n = (watch[it] ?: 0L) + ms
            watch[it] = n
            store.setTime(it, n)
        }
    }

    fun watchedDays(): Set<String> = watch.keys.filter { it.startsWith("d:") }.map { it.removePrefix("d:") }.toSet()

    fun streak(): Int {
        val days = watchedDays()
        val cal = java.util.Calendar.getInstance()
        if (fmtDay(cal.time) !in days) cal.add(java.util.Calendar.DAY_OF_YEAR, -1)
        var n = 0
        while (fmtDay(cal.time) in days) { n++; cal.add(java.util.Calendar.DAY_OF_YEAR, -1) }
        return n
    }

    fun newest(): List<Post> =
        posts.filter { (it.isVideo && showVideos) || (!it.isVideo && showImages) }.sortedByDescending { it.modified }

    fun postsOf(album: String): List<Post> = posts.filter { it.album == album }.sortedByDescending { it.modified }

    fun updateAlbums(list: List<Album>) {
        albums = list
        store.albums = list
    }

    fun addView(id: String) {
        val n = (views[id] ?: 0) + 1
        views[id] = n
        store.setViews(id, n)
        if (id !in seen) { seen = seen + id; store.putSet("seen", seen) }
        history.remove(id)
        history.add(0, id)
        while (history.size > 300) history.removeAt(history.lastIndex)
        store.putList("history", history.toList())
    }

    fun toggleLike(id: String) {
        likes = if (id in likes) likes - id else likes + id
        store.putSet("likes", likes)
    }

    fun like(id: String) {
        if (id !in likes) toggleLike(id)
    }

    fun toggleSaved(id: String) {
        saved = if (id in saved) saved - id else saved + id
        store.putSet("saved", saved)
    }

    fun resetStats() {
        views.clear(); store.clearViews()
        history.clear(); store.putList("history", emptyList())
        likes = emptySet(); store.putSet("likes", likes)
        saved = emptySet(); store.putSet("saved", saved)
        watch.clear(); store.clearTime()
        resetSeen()
    }

    fun persistSettings() {
        store.setBool("splash", showSplash)
        store.setBool("sound", soundOn)
        store.setBool("images", showImages)
        store.setBool("videos", showVideos)
        store.setInt("maxImages", maxImages)
        store.setInt("imgChance", imageChance)
        store.setStr("nick", nickname)
        store.setStr("bio", bio)
    }

    val totalViews: Int get() = views.values.sum()
}

private fun fmtDay(d: java.util.Date): String = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US).format(d)
fun today(): String = fmtDay(java.util.Date())

fun formatDuration(ms: Long): String {
    val s = ms / 1000
    val h = s / 3600
    val m = (s % 3600) / 60
    return when { h > 0 -> "$h ч $m мин"; m > 0 -> "$m мин"; else -> "$s с" }
}

fun formatCount(n: Int): String = when {
    n >= 1_000_000 -> String.format("%.1fM", n / 1_000_000.0).replace(".0M", "M")
    n >= 1_000 -> String.format("%.1fK", n / 1_000.0).replace(".0K", "K")
    else -> n.toString()
}
