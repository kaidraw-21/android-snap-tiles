package com.snap.tiles.data.remote

import com.google.gson.annotations.SerializedName

/**
 * Root JSON structure fetched from GitHub.
 */
data class RemoteActionManifest(
    val version: Int = 0,
    val actions: List<RemoteAction>? = null,
    val categories: List<RemoteCategory>? = null
)

data class RemoteCategory(
    val id: String,
    val label: String,
    val order: Int = 0
)

data class RemoteAction(
    val id: String,
    val label: String,
    val description: String? = null,
    val icon: String? = null,
    val emoji: String? = null,
    val category: String? = null,
    val settingType: SettingType? = null,
    val settingKey: String? = null,
    val onValue: String? = null,
    val offValue: String? = null,
    val valueType: ValueType? = null,
    val stateCheck: StateCheck? = null,
    val dependencies: List<String>? = null,
    val sideEffects: SideEffects? = null,
    val linkedSettings: List<LinkedSetting>? = null,
    val cacheKey: String? = null,
    val customHandler: String? = null,
    val children: List<RemoteAction>? = null
) {
    // Safe accessors with defaults — Gson ignores Kotlin default values
    val safeDescription get() = description.orEmpty()
    val safeIcon get() = icon.orEmpty()
    val safeEmoji get() = emoji.orEmpty()
    val safeCategory get() = category.orEmpty()
    val safeSettingType get() = settingType ?: SettingType.GLOBAL
    val safeSettingKey get() = settingKey.orEmpty()
    val safeOnValue get() = onValue ?: "1"
    val safeOffValue get() = offValue ?: "0"
    val safeValueType get() = valueType ?: ValueType.INT
    val safeStateCheck get() = stateCheck ?: StateCheck.DEFAULT
    val safeDependencies get() = dependencies.orEmpty()
    val safeLinkedSettings get() = linkedSettings.orEmpty()
    val safeChildren get() = children.orEmpty()

    /** Flat list: this action + all descendants */
    fun flatten(): List<RemoteAction> = listOf(this) + safeChildren.flatMap { it.flatten() }
}

data class SideEffects(
    val onEnable: List<SettingWrite>? = null,
    val onDisable: List<SettingWrite>? = null
) {
    val safeOnEnable get() = onEnable.orEmpty()
    val safeOnDisable get() = onDisable.orEmpty()
}

data class SettingWrite(
    val settingType: SettingType = SettingType.GLOBAL,
    val key: String,
    val value: String,
    val valueType: ValueType = ValueType.INT
)

data class LinkedSetting(
    val key: String,
    val onValue: String = "1",
    val offValue: String = "0",
    val valueType: ValueType = ValueType.INT
)

enum class SettingType {
    @SerializedName("global") GLOBAL,
    @SerializedName("secure") SECURE
}

enum class ValueType {
    @SerializedName("int") INT,
    @SerializedName("float") FLOAT,
    @SerializedName("string") STRING
}

enum class StateCheck {
    @SerializedName("default") DEFAULT,
    @SerializedName("notEmpty") NOT_EMPTY,
    @SerializedName("allLinkedOn") ALL_LINKED_ON
}
