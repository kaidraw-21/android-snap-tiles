package com.usb.tiledebug

import android.provider.Settings
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService

class UsbDebuggingTileService : TileService() {

    private val adbEnabled: Boolean
        get() = Settings.Global.getInt(contentResolver, "adb_enabled", 0) == 1

    override fun onStartListening() {
        updateTile()
    }

    override fun onClick() {
        Settings.Global.putInt(contentResolver, "adb_enabled", if (adbEnabled) 0 else 1)
        updateTile()
    }

    private fun updateTile() {
        qsTile?.apply {
            state = if (adbEnabled) Tile.STATE_ACTIVE else Tile.STATE_INACTIVE
            label = if (adbEnabled) "USB Debug: On" else "USB Debug: Off"
            updateTile()
        }
    }
}
