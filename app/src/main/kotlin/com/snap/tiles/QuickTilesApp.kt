package com.snap.tiles

import android.app.Application
import android.util.Log
import com.snap.tiles.data.PrefsManager
import com.snap.tiles.data.remote.ActionRegistry
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class QuickTilesApp : Application() {

    companion object {
        private const val TAG = "QuickTilesApp"
    }

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "onCreate()")
        PrefsManager.init(this)
        ActionRegistry.init(this)

        // Background refresh from remote
        appScope.launch {
            ActionRegistry.refresh()
        }
    }
}
