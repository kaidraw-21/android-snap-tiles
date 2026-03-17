package com.snap.tiles.data

data class TileConfig(
    val slotIndex: Int,
    val label: String,
    val actions: List<Action>,
    val enabled: Boolean
) {
    /** Action IDs for use with DynamicActionExecutor / ActionRegistry */
    val actionIds: List<String> get() = actions.map { it.name }
}
