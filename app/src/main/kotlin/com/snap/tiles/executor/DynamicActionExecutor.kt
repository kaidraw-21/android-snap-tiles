package com.snap.tiles.executor

import android.content.ContentResolver
import android.content.Context
import android.provider.Settings
import android.util.Log
import com.snap.tiles.data.PrefsManager
import com.snap.tiles.data.TileConfigRepo
import com.snap.tiles.data.remote.ActionRegistry
import com.snap.tiles.data.remote.LinkedSetting
import com.snap.tiles.data.remote.RemoteAction
import com.snap.tiles.data.remote.SettingType
import com.snap.tiles.data.remote.SettingWrite
import com.snap.tiles.data.remote.StateCheck
import com.snap.tiles.data.remote.ValueType

/**
 * Generic executor that reads/writes Android Settings based on [RemoteAction] JSON config.
 * Handles special cases (Accessibility cache, DevMode↔USB) via [RemoteAction.customHandler].
 */
object DynamicActionExecutor {

    private const val TAG = "DynExecutor"

    // ── Read state ──────────────────────────────────────────────

    fun getState(actionId: String, resolver: ContentResolver): Boolean {
        val action = ActionRegistry.getAction(actionId) ?: return false
        return getState(action, resolver)
    }

    fun getState(action: RemoteAction, resolver: ContentResolver): Boolean {
        val result = when (action.safeStateCheck) {
            StateCheck.NOT_EMPTY -> {
                val v = readString(action.safeSettingType, action.safeSettingKey, resolver)
                !v.isNullOrEmpty()
            }
            StateCheck.ALL_LINKED_ON -> {
                val main = readRaw(action, resolver) == action.safeOnValue
                val linked = action.safeLinkedSettings.all { ls ->
                    readRawLinked(action.safeSettingType, ls, resolver) == ls.onValue
                }
                main && linked
            }
            StateCheck.DEFAULT -> {
                readRaw(action, resolver) == action.safeOnValue
            }
        }
        Log.d(TAG, "getState(${action.id}) -> $result")
        return result
    }

    // ── Write state ─────────────────────────────────────────────

    fun setState(actionId: String, resolver: ContentResolver, targetOn: Boolean) {
        val action = ActionRegistry.getAction(actionId) ?: return
        setState(action, resolver, targetOn)
    }

    fun setState(action: RemoteAction, resolver: ContentResolver, targetOn: Boolean) {
        Log.d(TAG, "setState(${action.id}, on=$targetOn)")

        if (targetOn) {
            // Auto-ON parent chain: if this is a child, ensure parent is on first
            val parentId = ActionRegistry.getParentId(action.id)
            if (parentId != null) {
                val parent = ActionRegistry.getAction(parentId)
                if (parent != null && !getState(parent, resolver)) {
                    Log.d(TAG, "auto-enabling parent ${parent.id} for child ${action.id}")
                    setState(parent, resolver, true)
                }
            }
        }

        // Custom handlers for complex logic
        if (action.customHandler != null) {
            if (!targetOn) cacheAndCascadeOffChildren(action, resolver)
            handleCustom(action, resolver, targetOn)
            if (targetOn) restoreCachedChildren(action, resolver)
            return
        }

        // Side effects
        val effects = if (targetOn) action.sideEffects?.safeOnEnable else action.sideEffects?.safeOnDisable
        effects?.forEach { writeSetting(it, resolver) }

        // Dependencies
        if (targetOn) {
            action.safeDependencies.forEach { depId ->
                val dep = ActionRegistry.getAction(depId)
                if (dep != null && !getState(dep, resolver)) {
                    setState(dep, resolver, true)
                }
            }
        }

        // Cache/restore for actions with cacheKey (e.g. Accessibility)
        if (action.cacheKey != null) {
            if (!targetOn) cacheAndCascadeOffChildren(action, resolver)
            handleCacheAction(action, resolver, targetOn)
            if (targetOn) restoreCachedChildren(action, resolver)
            return
        }

        // Cascade OFF children BEFORE writing parent off (snapshot while still readable)
        if (!targetOn) cacheAndCascadeOffChildren(action, resolver)

        // Main setting
        writeValue(action.safeSettingType, action.safeSettingKey, action.safeValueType,
            if (targetOn) action.safeOnValue else action.safeOffValue, resolver)

        // Linked settings
        action.safeLinkedSettings.forEach { ls ->
            writeValue(action.safeSettingType, ls.key, ls.valueType,
                if (targetOn) ls.onValue else ls.offValue, resolver)
        }

        // Restore cached children AFTER parent is on
        if (targetOn) restoreCachedChildren(action, resolver)
    }

    // ── Children cascade helpers ────────────────────────────────

    private const val CHILDREN_STATE_PREFIX = "children_state_"

    /** Snapshot which children are ON, then turn them all OFF */
    private fun cacheAndCascadeOffChildren(parent: RemoteAction, resolver: ContentResolver) {
        if (parent.safeChildren.isEmpty()) return
        val onChildIds = parent.safeChildren
            .filter { getState(it, resolver) }
            .map { it.id }
        // Persist so it survives process death
        PrefsManager.tileRuntime.edit()
            .putStringSet("$CHILDREN_STATE_PREFIX${parent.id}", onChildIds.toSet())
            .apply()
        Log.d(TAG, "cached children state for ${parent.id}: $onChildIds")
        // Now cascade OFF
        parent.safeChildren.forEach { child ->
            if (child.id in onChildIds) {
                Log.d(TAG, "cascade OFF child ${child.id} (parent=${parent.id})")
                setState(child, resolver, false)
            }
        }
    }

    /** Restore only the children that were ON before parent was turned off */
    private fun restoreCachedChildren(parent: RemoteAction, resolver: ContentResolver) {
        if (parent.safeChildren.isEmpty()) return
        val key = "$CHILDREN_STATE_PREFIX${parent.id}"
        val cached = PrefsManager.tileRuntime.getStringSet(key, null)
        if (cached.isNullOrEmpty()) return
        Log.d(TAG, "restoring children for ${parent.id}: $cached")
        parent.safeChildren.forEach { child ->
            if (child.id in cached && !getState(child, resolver)) {
                Log.d(TAG, "restore ON child ${child.id} (parent=${parent.id})")
                setState(child, resolver, true)
            }
        }
        // Clear cache after restore
        PrefsManager.tileRuntime.edit().remove(key).apply()
    }

    fun toggleAll(actionIds: List<String>, resolver: ContentResolver, context: Context) {
        if (actionIds.isEmpty()) return
        val allOn = actionIds.all { getState(it, resolver) }
        val targetOn = !allOn
        Log.d(TAG, "toggleAll(ids=$actionIds, allOn=$allOn, target=$targetOn)")

        // Collect all affected action IDs (including dependencies & side effects)
        val affected = mutableSetOf<String>()
        affected.addAll(actionIds)

        actionIds.forEach { id ->
            val action = ActionRegistry.getAction(id) ?: return@forEach
            affected.addAll(action.safeDependencies)
            affected.addAll(ActionRegistry.getDescendantIds(id))
            // Also include parent chain
            var pid = ActionRegistry.getParentId(id)
            while (pid != null) {
                affected.add(pid)
                pid = ActionRegistry.getParentId(pid)
            }
            action.safeLinkedSettings.forEach { ls -> affected.add(ls.key) }
            action.sideEffects?.safeOnEnable?.forEach { affected.add(it.key) }
            action.sideEffects?.safeOnDisable?.forEach { affected.add(it.key) }
        }

        actionIds.forEach { setState(it, resolver, targetOn) }

        // Only refresh tiles that are affected by the changed actions
        TileConfigRepo.requestRefreshAffectedTiles(context, affected)
    }

    /** Keep a11y cache fresh — call on tile refresh when accessibility is ON */
    fun refreshA11yCache(resolver: ContentResolver) {
        val current = Settings.Secure.getString(resolver, "enabled_accessibility_services")
        if (!current.isNullOrEmpty()) {
            val saved = PrefsManager.getSavedA11y()
            if (saved != current) {
                Log.d(TAG, "refreshA11yCache: updating -> $current")
                PrefsManager.setSavedA11y(current)
            }
        }
    }

    // ── Internals ───────────────────────────────────────────────

    private fun handleCacheAction(action: RemoteAction, resolver: ContentResolver, targetOn: Boolean) {
        val key = action.safeSettingKey
        if (targetOn) {
            // Restore from cache
            val saved = PrefsManager.tileRuntime.getString(action.cacheKey, null)
            if (!saved.isNullOrEmpty() && saved != "null") {
                writeString(action.safeSettingType, key, saved, resolver)
            }
        } else {
            // Cache current value then clear
            val current = readString(action.safeSettingType, key, resolver)
            if (!current.isNullOrEmpty()) {
                PrefsManager.tileRuntime.edit().putString(action.cacheKey, current).apply()
                Log.d(TAG, "cached ${action.cacheKey} = $current")
            }
            writeString(action.safeSettingType, key, null, resolver)
        }
    }

    private fun handleCustom(action: RemoteAction, resolver: ContentResolver, targetOn: Boolean) {
        when (action.customHandler) {
            "DEVELOPER_MODE" -> {
                if (!targetOn) {
                    val usbWasOn = Settings.Global.getInt(resolver, "adb_enabled", 0) == 1
                    PrefsManager.setCachedUsbBeforeDevOff(usbWasOn)
                    Settings.Global.putInt(resolver, "adb_enabled", 0)
                    Settings.Global.putInt(resolver, action.safeSettingKey, 0)
                } else {
                    Settings.Global.putInt(resolver, action.safeSettingKey, 1)
                    val restore = PrefsManager.getCachedUsbBeforeDevOff()
                    if (restore == true) {
                        Settings.Global.putInt(resolver, "adb_enabled", 1)
                    }
                    PrefsManager.setCachedUsbBeforeDevOff(null)
                }
            }
            else -> {
                Log.w(TAG, "Unknown customHandler: ${action.customHandler}")
                // Fallback to generic write
                writeValue(action.safeSettingType, action.safeSettingKey, action.safeValueType,
                    if (targetOn) action.safeOnValue else action.safeOffValue, resolver)
            }
        }
    }

    private fun readRaw(action: RemoteAction, resolver: ContentResolver): String? {
        return when (action.safeValueType) {
            ValueType.INT -> readInt(action.safeSettingType, action.safeSettingKey, resolver)?.toString()
            ValueType.FLOAT -> readFloat(action.safeSettingType, action.safeSettingKey, resolver)?.toString()
            ValueType.STRING -> readString(action.safeSettingType, action.safeSettingKey, resolver)
        }
    }

    private fun readRawLinked(type: SettingType, ls: LinkedSetting, resolver: ContentResolver): String? {
        return when (ls.valueType) {
            ValueType.INT -> readInt(type, ls.key, resolver)?.toString()
            ValueType.FLOAT -> readFloat(type, ls.key, resolver)?.toString()
            ValueType.STRING -> readString(type, ls.key, resolver)
        }
    }

    private fun writeSetting(sw: SettingWrite, resolver: ContentResolver) {
        writeValue(sw.settingType, sw.key, sw.valueType, sw.value, resolver)
    }

    private fun writeValue(type: SettingType, key: String, vt: ValueType, value: String, resolver: ContentResolver) {
        when (vt) {
            ValueType.INT -> {
                val v = value.toIntOrNull() ?: 0
                when (type) {
                    SettingType.GLOBAL -> Settings.Global.putInt(resolver, key, v)
                    SettingType.SECURE -> Settings.Secure.putInt(resolver, key, v)
                }
            }
            ValueType.FLOAT -> {
                val v = value.toFloatOrNull() ?: 0f
                when (type) {
                    SettingType.GLOBAL -> Settings.Global.putFloat(resolver, key, v)
                    SettingType.SECURE -> Settings.Secure.putFloat(resolver, key, v)
                }
            }
            ValueType.STRING -> writeString(type, key, value.ifEmpty { null }, resolver)
        }
    }

    private fun writeString(type: SettingType, key: String, value: String?, resolver: ContentResolver) {
        when (type) {
            SettingType.GLOBAL -> Settings.Global.putString(resolver, key, value)
            SettingType.SECURE -> Settings.Secure.putString(resolver, key, value)
        }
    }

    private fun readInt(type: SettingType, key: String, resolver: ContentResolver): Int? = runCatching {
        when (type) {
            SettingType.GLOBAL -> Settings.Global.getInt(resolver, key, 0)
            SettingType.SECURE -> Settings.Secure.getInt(resolver, key, 0)
        }
    }.getOrNull()

    private fun readFloat(type: SettingType, key: String, resolver: ContentResolver): Float? = runCatching {
        when (type) {
            SettingType.GLOBAL -> Settings.Global.getFloat(resolver, key, 0f)
            SettingType.SECURE -> Settings.Secure.getFloat(resolver, key, 0f)
        }
    }.getOrNull()

    private fun readString(type: SettingType, key: String, resolver: ContentResolver): String? = runCatching {
        when (type) {
            SettingType.GLOBAL -> Settings.Global.getString(resolver, key)
            SettingType.SECURE -> Settings.Secure.getString(resolver, key)
        }
    }.getOrNull()
}
