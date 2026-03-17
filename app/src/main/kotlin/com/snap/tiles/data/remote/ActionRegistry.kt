package com.snap.tiles.data.remote

import android.content.Context
import android.util.Log
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.util.concurrent.TimeUnit

/**
 * Fetches action definitions from a remote JSON on GitHub,
 * caches locally, and falls back to bundled assets/actions.json.
 *
 * Call [init] once from Application.onCreate, then use [getActions] / [getCategories].
 */
object ActionRegistry {

    private const val TAG = "ActionRegistry"

    private const val REMOTE_URL =
        "https://raw.githubusercontent.com/kaidraw-21/android-snap-tiles/main/config/actions.json"

    private const val CACHE_FILE = "actions_cache.json"
    private const val CACHE_MAX_AGE_MS = 6 * 60 * 60 * 1000L // 6 hours

    private val gson: Gson = GsonBuilder().create()
    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .build()

    private lateinit var appContext: Context
    private var manifest: RemoteActionManifest = RemoteActionManifest()
    private val mutex = Mutex()

    private val actionIndex: MutableMap<String, RemoteAction> = mutableMapOf()
    /** parentId -> action. Populated during applyManifest for tree traversal. */
    private val parentIndex: MutableMap<String, String> = mutableMapOf()

    fun init(context: Context) {
        appContext = context.applicationContext
        // Load synchronously from cache or bundled so tiles work immediately
        val cached = readCache()
        val loaded = cached ?: readBundled()
        if (loaded != null) {
            applyManifest(loaded)
        }
        Log.d(TAG, "init: loaded ${manifest.actions?.size ?: 0} actions (cached=${cached != null})")
    }

    /** Fetch remote JSON in background. Call from a coroutine. */
    suspend fun refresh() = mutex.withLock {
        val remote = fetchRemote()
        if (remote != null && remote.version >= manifest.version) {
            applyManifest(remote)
            writeCache(remote)
            Log.d(TAG, "refresh: updated to v${remote.version} with ${remote.actions?.size ?: 0} actions")
        } else {
            Log.d(TAG, "refresh: no update (remote=${remote?.version}, current=${manifest.version})")
        }
    }

    /** Returns all actions flattened — use for ID lookups and compatibility */
    fun getActions(): List<RemoteAction> = getAllActionsFlat()

    fun getCategories(): List<RemoteCategory> =
        (manifest.categories ?: emptyList()).sortedBy { it.order }

    fun getAction(id: String): RemoteAction? = actionIndex[id]

    /** Get the parent action ID, or null if this is a root action. */
    fun getParentId(id: String): String? = parentIndex[id]

    /** Get all descendant IDs (children, grandchildren, etc.) */
    fun getDescendantIds(id: String): List<String> {
        val action = actionIndex[id] ?: return emptyList()
        return action.safeChildren.flatMap { listOf(it.id) + getDescendantIds(it.id) }
    }

    /** Get all actions flattened (root + all descendants) */
    fun getAllActionsFlat(): List<RemoteAction> =
        (manifest.actions ?: emptyList()).flatMap { it.flatten() }

    fun getActionsByCategory(): Map<RemoteCategory, List<RemoteAction>> {
        val catMap = (manifest.categories ?: emptyList()).associateBy { it.id }
        val flat = getAllActionsFlat()
        return flat
            .groupBy { catMap[it.safeCategory] ?: RemoteCategory(it.safeCategory, it.safeCategory) }
            .toSortedMap(compareBy { it.order })
    }

    /** Top-level actions (with children intact) grouped by category — for tree UI */
    fun getTopLevelByCategory(): Map<RemoteCategory, List<RemoteAction>> {
        val catMap = (manifest.categories ?: emptyList()).associateBy { it.id }
        return (manifest.actions ?: emptyList())
            .groupBy { catMap[it.safeCategory] ?: RemoteCategory(it.safeCategory, it.safeCategory) }
            .toSortedMap(compareBy { it.order })
    }

    // --- internal ---

    private fun applyManifest(m: RemoteActionManifest) {
        manifest = m
        actionIndex.clear()
        parentIndex.clear()
        fun indexRecursive(actions: List<RemoteAction>, parentId: String?) {
            actions.forEach { action ->
                actionIndex[action.id] = action
                if (parentId != null) parentIndex[action.id] = parentId
                indexRecursive(action.children ?: emptyList(), action.id)
            }
        }
        indexRecursive(m.actions ?: emptyList(), null)
    }

    private fun fetchRemote(): RemoteActionManifest? = runCatching {
        val request = Request.Builder().url(REMOTE_URL).build()
        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) return@runCatching null
            val body = response.body?.string() ?: return@runCatching null
            gson.fromJson(body, RemoteActionManifest::class.java)
        }
    }.onFailure { Log.w(TAG, "fetchRemote failed", it) }.getOrNull()

    private suspend fun fetchRemoteSuspend(): RemoteActionManifest? =
        withContext(Dispatchers.IO) { fetchRemote() }

    private fun readCache(): RemoteActionManifest? = runCatching {
        val file = File(appContext.filesDir, CACHE_FILE)
        if (!file.exists()) return null
        // Skip stale cache only for version check; still load it
        val json = file.readText()
        gson.fromJson(json, RemoteActionManifest::class.java)
    }.onFailure { Log.w(TAG, "readCache failed", it) }.getOrNull()

    private fun writeCache(m: RemoteActionManifest) = runCatching {
        val file = File(appContext.filesDir, CACHE_FILE)
        file.writeText(gson.toJson(m))
    }.onFailure { Log.w(TAG, "writeCache failed", it) }

    private fun readBundled(): RemoteActionManifest? = runCatching {
        val json = appContext.assets.open("actions.json").bufferedReader().readText()
        gson.fromJson(json, RemoteActionManifest::class.java)
    }.onFailure { Log.w(TAG, "readBundled failed", it) }.getOrNull()
}
