package com.snap.tiles.data

import androidx.annotation.StringRes
import com.snap.tiles.R

enum class ActionCategory(@StringRes val labelRes: Int) {
    SYSTEM_CONTROLS(R.string.category_system_controls),
    ADVANCED_DEBUGGING(R.string.category_advanced_debugging)
}

enum class Action(
    @StringRes val labelRes: Int,
    @StringRes val descriptionRes: Int,
    val icon: String,
    val category: ActionCategory
) {
    USB_DEBUGGING(
        R.string.action_usb_debugging,
        R.string.action_desc_usb_debugging,
        "usb",
        ActionCategory.SYSTEM_CONTROLS
    ),
    DEVELOPER_MODE(
        R.string.action_developer_mode,
        R.string.action_desc_developer_mode,
        "code",
        ActionCategory.SYSTEM_CONTROLS
    ),
    ACCESSIBILITY(
        R.string.action_accessibility,
        R.string.action_desc_accessibility,
        "accessibility",
        ActionCategory.SYSTEM_CONTROLS
    ),
    STAY_AWAKE(
        R.string.action_stay_awake,
        R.string.action_desc_stay_awake,
        "light_mode",
        ActionCategory.SYSTEM_CONTROLS
    ),
    RUNNING_SERVICES(
        R.string.action_running_services,
        R.string.action_desc_running_services,
        "memory",
        ActionCategory.SYSTEM_CONTROLS
    ),
    FORCE_RTL(
        R.string.action_force_rtl,
        R.string.action_desc_force_rtl,
        "format_textdirection_r_to_l",
        ActionCategory.SYSTEM_CONTROLS
    ),
    PROFILE_GPU(
        R.string.action_profile_gpu,
        R.string.action_desc_profile_gpu,
        "bar_chart",
        ActionCategory.ADVANCED_DEBUGGING
    ),
    DEMO_MODE(
        R.string.action_demo_mode,
        R.string.action_desc_demo_mode,
        "phone_android",
        ActionCategory.ADVANCED_DEBUGGING
    ),
    ANIMATOR_SCALE(
        R.string.action_animator_scale,
        R.string.action_desc_animator_scale,
        "speed",
        ActionCategory.ADVANCED_DEBUGGING
    );

    companion object {
        fun byCategory(): Map<ActionCategory, List<Action>> =
            entries.groupBy { it.category }
    }
}
