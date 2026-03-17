package com.snap.tiles.data

import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import android.service.quicksettings.TileService
import android.util.Log
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import com.snap.tiles.R
import com.snap.tiles.data.remote.ActionRegistry

data class FixedTileInfo(
    val action: Action,
    @StringRes val labelRes: Int,
    @DrawableRes val iconRes: Int,
    val className: String
)

data class CustomSlotInfo(
    val slotIndex: Int,
    @StringRes val labelRes: Int,
    @DrawableRes val iconRes: Int,
    val className: String
)

object TileConfigRepo {

    private const val TAG = "TileConfigRepo"
    const val SLOT_COUNT = 5

    val fixedTiles = listOf(
        FixedTileInfo(Action.USB_DEBUGGING, R.string.tile_usb_debugging, R.drawable.ic_tile_usb_debug, "com.snap.tiles.tile.UsbDebugTile"),
        FixedTileInfo(Action.DEVELOPER_MODE, R.string.tile_developer_mode, R.drawable.ic_tile_dev_mode, "com.snap.tiles.tile.DevModeTile"),
        FixedTileInfo(Action.ACCESSIBILITY, R.string.tile_accessibility, R.drawable.ic_tile_accessibility, "com.snap.tiles.tile.AccessibilityTile")
    )

    val customSlots = listOf(
        CustomSlotInfo(1, R.string.slot_metal, R.drawable.ic_tile_metal, "com.snap.tiles.tile.TileKim"),
        CustomSlotInfo(2, R.string.slot_wood, R.drawable.ic_tile_wood, "com.snap.tiles.tile.TileMoc"),
        CustomSlotInfo(3, R.string.slot_water, R.drawable.ic_tile_water, "com.snap.tiles.tile.TileThuy"),
        CustomSlotInfo(4, R.string.slot_fire, R.drawable.ic_tile_fire, "com.snap.tiles.tile.TileHoa"),
        CustomSlotInfo(5, R.string.slot_earth, R.drawable.ic_tile_earth, "com.snap.tiles.tile.TileTho")
    )

    private val slotClassNames = customSlots.associate { it.slotIndex to it.className }
    val fixedTileClasses = fixedTiles.map { it.className }

    fun get(slotIndex: Int): TileConfig {
        Log.d(TAG, "get(slot=$slotIndex)")
        // Filter: only keep actions that exist in the current ActionRegistry
        val registryIds = ActionRegistry.getActions().map { it.id }.toSet()
        val allActions = PrefsManager.getSlotActions(slotIndex)
        val filtered = allActions.filter { it.name in registryIds }
        if (filtered.size != allActions.size) {
            Log.d(TAG, "get(slot=$slotIndex): filtered ${allActions.size} -> ${filtered.size} actions by registry")
        }
        return TileConfig(
            slotIndex = slotIndex,
            label = PrefsManager.getSlotLabel(slotIndex),
            actions = filtered,
            enabled = PrefsManager.isSlotEnabled(slotIndex)
        )
    }

    fun save(config: TileConfig) {
        Log.d(TAG, "save(slot=${config.slotIndex})")
        PrefsManager.saveSlot(config)
    }

    fun setEnabled(context: Context, slotIndex: Int, enabled: Boolean) {
        Log.d(TAG, "setEnabled(slot=$slotIndex, enabled=$enabled)")
        val config = get(slotIndex)
        save(config.copy(enabled = enabled))
        updateComponentState(context, slotIndex, enabled)
    }

    fun updateComponentState(context: Context, slotIndex: Int, enabled: Boolean) {
        val className = slotClassNames[slotIndex] ?: return
        val component = ComponentName(context, className)
        val newState = if (enabled)
            PackageManager.COMPONENT_ENABLED_STATE_ENABLED
        else
            PackageManager.COMPONENT_ENABLED_STATE_DISABLED
        context.packageManager.setComponentEnabledSetting(component, newState, PackageManager.DONT_KILL_APP)
    }

    fun isFixedTileEnabled(action: Action): Boolean =
        PrefsManager.isFixedTileEnabled(action.name)

    fun setFixedTileEnabled(context: Context, action: Action, enabled: Boolean) {
        PrefsManager.setFixedTileEnabled(action.name, enabled)
        val info = fixedTiles.firstOrNull { it.action == action } ?: return
        val component = ComponentName(context, info.className)
        val newState = if (enabled)
            PackageManager.COMPONENT_ENABLED_STATE_ENABLED
        else
            PackageManager.COMPONENT_ENABLED_STATE_DISABLED
        context.packageManager.setComponentEnabledSetting(component, newState, PackageManager.DONT_KILL_APP)
        Log.d(TAG, "setFixedTileEnabled(action=${action.name}, enabled=$enabled)")
    }

    fun initAllSlots(context: Context) {
        for (i in 1..SLOT_COUNT) updateComponentState(context, i, get(i).enabled)
        // Fixed tiles are always enabled
        fixedTiles.forEach { info ->
            val component = ComponentName(context, info.className)
            context.packageManager.setComponentEnabledSetting(
                component,
                PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
                PackageManager.DONT_KILL_APP
            )
        }
    }

    /**
     * Refresh only tiles whose actions overlap with [changedActionIds].
     * Falls back to refreshing all tiles when [changedActionIds] is null.
     */
    fun requestRefreshAffectedTiles(context: Context, changedActionIds: Set<String>? = null) {
        if (changedActionIds == null) {
            requestRefreshAllTiles(context)
            return
        }
        // Custom slots: only refresh if any of its actions is in the changed set
        for (i in 1..SLOT_COUNT) {
            val cn = slotClassNames[i] ?: continue
            if (!PrefsManager.isSlotEnabled(i)) continue
            val slotActionIds = PrefsManager.getSlotActions(i).map { it.name }.toSet()
            if (slotActionIds.any { it in changedActionIds }) {
                requestRefresh(context, cn)
            }
        }
        // Fixed tiles: only refresh if their action is in the changed set
        fixedTiles.forEach { info ->
            if (info.action.name in changedActionIds) {
                requestRefresh(context, info.className)
            }
        }
    }

    fun requestRefreshAllTiles(context: Context) {
        // Read enabled state directly from prefs — skip full get() to avoid repeated registry lookups
        for (i in 1..SLOT_COUNT) {
            val cn = slotClassNames[i] ?: continue
            if (!PrefsManager.isSlotEnabled(i)) continue
            requestRefresh(context, cn)
        }
        fixedTiles.forEach { info ->
            requestRefresh(context, info.className)
        }
    }

    private fun requestRefresh(context: Context, className: String) {
        runCatching {
            TileService.requestListeningState(context, ComponentName(context, className))
        }
    }

    fun defaultLabelRes(slot: Int): Int = customSlots.firstOrNull { it.slotIndex == slot }?.labelRes ?: R.string.app_name
}
