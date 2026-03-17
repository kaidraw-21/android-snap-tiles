package com.snap.tiles.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.snap.tiles.data.TileConfigRepo

class BootReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "BootReceiver"
    }

    override fun onReceive(context: Context, intent: Intent?) {
        Log.d(TAG, "onReceive(action=${intent?.action})")
        if (intent?.action != Intent.ACTION_BOOT_COMPLETED) return
        TileConfigRepo.initAllSlots(context)
    }
}
