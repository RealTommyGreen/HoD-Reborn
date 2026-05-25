package com.heartofdarkness.reborn.touch

import android.app.AlertDialog
import android.content.Context
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import com.heartofdarkness.reborn.ControllerDeviceDetector
import com.heartofdarkness.reborn.HodActivity

class TouchOverlaySettingsDialog(
    private val context: Context,
    private val config: TouchOverlayConfig,
    private val controllerEnabled: Boolean = false,
    private val controllerConfig: ControllerConfig? = null,
    private val onConfigChanged: (TouchOverlayConfig) -> Unit,
    private val onResetAll: () -> Unit,
    private val onControllerConfigChanged: ((ControllerConfig) -> Unit)? = null,
    private val onOpenControllerMapping: (() -> Unit)? = null
) {
    fun show() {
        var currentConfig = config

        val scrollView = ScrollView(context).apply { isFillViewport = false }
        val container = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(22.dp, 18.dp, 22.dp, 16.dp)
            background = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE; cornerRadius = 10.dp.toFloat()
                setColor(0xEE111820.toInt()); setStroke(1.dp, SURFACE_STROKE)
            }
        }

        container.addView(title("Touch Overlay Settings"))

        // --- Controller ---
        container.addView(sectionLabel("Controller"))
        val controllerButton = dialogButton(
            "Controller Mapping",
            0xFF1A3A24.toInt(),
            0xFF4A9A5A.toInt()
        ) {
            if (ControllerDeviceDetector.isControllerConnected() && onOpenControllerMapping != null) {
                onOpenControllerMapping()
            } else {
                Toast.makeText(context, "No controller detected", Toast.LENGTH_SHORT).show()
            }
        }
        container.addView(controllerButton)

        // --- D-Pad ---
        container.addView(separator())
        val dpadRunCheckBox = checkBox("Double tap left/right to run", currentConfig.dpadDoubleTapRunEnabled) {
            currentConfig = currentConfig.copy(dpadDoubleTapRunEnabled = it)
            onConfigChanged(currentConfig)
        }
        container.addView(sectionLabel("D-Pad"))
        container.addView(dpadRunCheckBox)

        // --- Cheats ---
        container.addView(separator())
        val spectreFireballCheckBox = checkBox("Spectre Fireball No-Hit", currentConfig.cheatSpectreFireballNoHit) {
            currentConfig = currentConfig.copy(cheatSpectreFireballNoHit = it)
            onConfigChanged(currentConfig)
            HodActivity.nativeSetCheat(0, it)
        }
        val oneHitPlasmaCheckBox = checkBox("One-Hit Plasma Cannon", currentConfig.cheatOneHitPlasmaCannon) {
            currentConfig = currentConfig.copy(cheatOneHitPlasmaCannon = it)
            onConfigChanged(currentConfig)
            HodActivity.nativeSetCheat(1, it)
        }
        val walkOnLavaCheckBox = checkBox("Walk on Lava", currentConfig.cheatWalkOnLava) {
            currentConfig = currentConfig.copy(cheatWalkOnLava = it)
            onConfigChanged(currentConfig)
            HodActivity.nativeSetCheat(2, it)
        }
        container.addView(sectionLabel("Cheats (v1)"))
        container.addView(spectreFireballCheckBox)
        container.addView(oneHitPlasmaCheckBox)
        container.addView(walkOnLavaCheckBox)

        // --- Layout ---
        container.addView(separator())
        container.addView(sectionLabel("Layout"))
        container.addView(dialogButton("Reset to Defaults", 0xFF1A3240.toInt(), 0xFF4A7A9A.toInt()) { onResetAll() })

        scrollView.addView(container)

        val dialog = AlertDialog.Builder(context)
            .setView(scrollView)
            .setPositiveButton("Close") { _, _ ->
                onConfigChanged(currentConfig)
            }
            .create()

        dialog.setOnShowListener {
            dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        }
        dialog.show()
    }

    private fun title(text: String) = TextView(context).apply {
        this.text = text; textSize = 18f
        setTypeface(Typeface.DEFAULT, Typeface.BOLD); setTextColor(TEXT)
        setPadding(0, 0, 0, 18.dp)
    }

    private fun sectionLabel(text: String) = TextView(context).apply {
        this.text = text; textSize = 12f
        setTypeface(Typeface.DEFAULT, Typeface.BOLD)
        setTextColor(ACCENT); setPadding(0, 14.dp, 0, 6.dp)
    }

    private fun checkBox(text: String, checked: Boolean, onChange: (Boolean) -> Unit) =
        CheckBox(context).apply {
            this.text = text; textSize = 14f
            setTextColor(TEXT)
            buttonTintList = tint(ACCENT)
            isChecked = checked
            setOnCheckedChangeListener { _, isChecked -> onChange(isChecked) }
        }

    private fun dialogButton(text: String, fill: Int, stroke: Int, onClick: () -> Unit): TextView {
        return TextView(context).apply {
            this.text = text; textSize = 14f; setTextColor(TEXT)
            setTypeface(Typeface.DEFAULT, Typeface.BOLD)
            gravity = Gravity.CENTER
            setPadding(16.dp, 12.dp, 16.dp, 12.dp)
            background = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE; cornerRadius = 8.dp.toFloat()
                setColor(fill); setStroke(1.dp, stroke)
            }
            setOnClickListener { onClick() }
        }.also {
            val lp = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
            lp.bottomMargin = 10.dp; it.layoutParams = lp
        }
    }

    private fun separator() = View(context).apply {
        layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 1.dp).apply {
            bottomMargin = 4.dp; topMargin = 8.dp
        }
        setBackgroundColor(SURFACE_STROKE)
    }

    private fun fieldBackground() = GradientDrawable().apply {
        shape = GradientDrawable.RECTANGLE; cornerRadius = 7.dp.toFloat()
        setColor(0xAA1D2A36.toInt()); setStroke(1.dp, SURFACE_STROKE)
    }

    private fun tint(color: Int) = android.content.res.ColorStateList.valueOf(color)
    private val Int.dp: Int get() = (this * context.resources.displayMetrics.density).toInt()

    companion object {
        private const val TEXT = 0xFFFFFFFF.toInt()
        private const val ACCENT = 0xFFFFC17A.toInt()
        private const val SURFACE_STROKE = 0x667D8DA0
    }
}
