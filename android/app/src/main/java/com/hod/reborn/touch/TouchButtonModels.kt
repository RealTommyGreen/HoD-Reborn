package com.hod.reborn.touch

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

const val TOUCH_OVERLAY_CONFIG_VERSION = 12
const val CONTROLLER_CONFIG_VERSION = 1

@Serializable
data class TouchOverlayConfig(
    @SerialName("schema_version") val schemaVersion: Int = TOUCH_OVERLAY_CONFIG_VERSION,
    val enabled: Boolean = true,
    @SerialName("layout_locked") val layoutLocked: Boolean = true,
    val buttons: List<TouchButtonConfig> = emptyList(),
    @SerialName("dpad_double_tap_run_enabled") val dpadDoubleTapRunEnabled: Boolean = true,
    @SerialName("cheat_spectre_fireball_no_hit") val cheatSpectreFireballNoHit: Boolean = false,
    @SerialName("cheat_one_hit_plasma_cannon") val cheatOneHitPlasmaCannon: Boolean = false,
    @SerialName("cheat_walk_on_lava") val cheatWalkOnLava: Boolean = false,
    @SerialName("video_filter") val videoFilter: String = VIDEO_FILTER_NEAREST
)

@Serializable
data class TouchButtonConfig(
    val id: String,
    val label: String = "",
    val icon: String? = null,
    val shape: String = BUTTON_SHAPE_CIRCLE,
    val x: Float,
    val y: Float,
    val size: Float,
    val alpha: Float = 0.45f,
    @SerialName("icon_fill") val iconFill: Float = -1f,
    val visible: Boolean = true,
    @SerialName("anchor_x") val anchorX: String? = null,
    @SerialName("anchor_y") val anchorY: String? = null,
    @SerialName("offset_x") val offsetX: Float? = null,
    @SerialName("offset_y") val offsetY: Float? = null,
    @SerialName("dpad_double_tap_run") val dpadDoubleTapRun: Boolean = false,
    val actions: List<TouchButtonAction> = emptyList()
)

@Serializable
data class TouchButtonAction(
    val type: String,
    val button: String? = null,
    val mode: String = "hold",
    @SerialName("key_code") val keyCode: Int? = null,
    @SerialName("key_name") val keyName: String? = null,
    @SerialName("key_codes") val keyCodes: List<Int> = emptyList(),
    @SerialName("key_names") val keyNames: List<String> = emptyList(),
    @SerialName("text") val text: String? = null,
    @SerialName("modifiers") val modifiers: List<String> = emptyList()
)

@Serializable
data class ControllerConfig(
    @SerialName("schema_version") val schemaVersion: Int = CONTROLLER_CONFIG_VERSION,
    val mapping: Map<String, String> = defaultControllerMapping()
) {
    companion object {
        // Matches hardcoded SDL joystick mapping in system_sdl2.cpp:557-584
        fun defaultControllerMapping(): Map<String, String> = mapOf(
            "A" to "run",
            "B" to "jump",
            "X" to "shoot",
            "Y" to "shoot_run",
            "START" to "menu"
        )

        val actions = listOf("run", "jump", "shoot", "shoot_run", "menu")
        val buttons = listOf("A", "B", "X", "Y", "START")
        val actionLabels = mapOf(
            "run" to "Run",
            "jump" to "Jump",
            "shoot" to "Shoot",
            "shoot_run" to "Shoot+Run",
            "menu" to "Menu"
        )
    }
}

const val BUTTON_SHAPE_CIRCLE = "circle"
const val BUTTON_SHAPE_SQUARE = "square"
const val BUTTON_SHAPE_RECTANGLE = "rectangle"
const val BUTTON_ANCHOR_START = "start"
const val BUTTON_ANCHOR_END = "end"
const val BUTTON_ANCHOR_TOP = "top"
const val BUTTON_ANCHOR_BOTTOM = "bottom"
const val VIDEO_FILTER_NEAREST = "nearest"
const val VIDEO_FILTER_LINEAR = "linear"
const val VIDEO_FILTER_XBR = "xbr"

// Default touch overlay layout — from hod_touch_preset_default.json
fun defaultButtons(): List<TouchButtonConfig> = listOf(
    TouchButtonConfig(id = "btn_menu", label = "Menu", icon = "menu", shape = BUTTON_SHAPE_CIRCLE,
        x = 0.017916666f, y = 0.039814815f, size = 0.18f, alpha = 0.34f, visible = true,
        actions = listOf(TouchButtonAction(type = "native_menu", mode = "tap"))),

    TouchButtonConfig(id = "dpad", label = "", icon = "dpad_map", shape = BUTTON_SHAPE_SQUARE,
        x = 0.0f, y = 0.35f, size = 0.500f, alpha = 0.30f, visible = true, dpadDoubleTapRun = true,
        actions = listOf(TouchButtonAction(type = "dpad", mode = "hold"))),

    TouchButtonConfig(id = "btn_run", label = "Run", icon = "HoD_Run", shape = BUTTON_SHAPE_CIRCLE,
        x = 0.7875f, y = 0.42777777f, size = 0.26f, alpha = 0.34f, visible = true,
        actions = listOf(TouchButtonAction(type = "key", mode = "hold", keyName = "CTRL"))),
    TouchButtonConfig(id = "btn_jump", label = "Jump", icon = "HoD_Jump", shape = BUTTON_SHAPE_CIRCLE,
        x = 0.8225f, y = 0.5833333f, size = 0.26f, alpha = 0.34f, visible = true,
        actions = listOf(TouchButtonAction(type = "key", mode = "hold", keyName = "ENTER"))),
    TouchButtonConfig(id = "btn_shoot", label = "Shoot", icon = "HoD_Shoot", shape = BUTTON_SHAPE_CIRCLE,
        x = 0.875f, y = 0.42777777f, size = 0.26f, alpha = 0.34f, visible = true,
        actions = listOf(TouchButtonAction(type = "key", mode = "hold", keyName = "SHIFT")))
)
