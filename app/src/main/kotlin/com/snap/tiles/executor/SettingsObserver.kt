package com.snap.tiles.executor

import android.content.ContentResolver
import android.database.ContentObserver
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.util.Log

/**
 * Watches Settings.Global and Settings.Secure for changes.
 * Any registered listener gets notified so tiles/floating button can refresh.
 */
object SettingsObserver {

    private const val TAG = "SettingsObserver"

    fun interface OnSettingsChanged {
        fun onChanged()
    }

    private val listeners = mutableSetOf<OnSettingsChanged>()
    private var registered = false

    private val handler = Handler(Looper.getMainLooper())

    private val observer = object : ContentObserver(handler) {
        override fun onChange(selfChange: Boolean, uri: Uri?) {
            Log.d(TAG, "onChange(uri=$uri, listeners=${listeners.size})")
            listeners.forEach { it.onChanged() }
        }
    }

    fun register(resolver: ContentResolver) {
        if (registered) return
        Log.d(TAG, "register()")
        resolver.registerContentObserver(
            Settings.Global.CONTENT_URI, true, observer
        )
        resolver.registerContentObserver(
            Settings.Secure.CONTENT_URI, true, observer
        )
        registered = true
    }

    fun unregister(resolver: ContentResolver) {
        if (!registered) return
        Log.d(TAG, "unregister()")
        resolver.unregisterContentObserver(observer)
        registered = false
    }

    fun addListener(listener: OnSettingsChanged) {
        Log.d(TAG, "addListener()")
        listeners.add(listener)
    }

    fun removeListener(listener: OnSettingsChanged) {
        Log.d(TAG, "removeListener()")
        listeners.remove(listener)
    }
}
