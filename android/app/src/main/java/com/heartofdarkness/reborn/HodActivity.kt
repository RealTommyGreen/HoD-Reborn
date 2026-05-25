package com.heartofdarkness.reborn

import android.os.Bundle
import android.util.Log
import android.view.ViewGroup
import com.heartofdarkness.reborn.touch.ControllerConfig
import com.heartofdarkness.reborn.touch.ControllerConfigStore
import com.heartofdarkness.reborn.touch.TouchButtonStore
import com.heartofdarkness.reborn.touch.TouchOverlayController
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.libsdl.app.SDLActivity
import java.io.File

class HodActivity : SDLActivity() {

    companion object {
        private const val TAG = "HodActivity"
        private const val TOUCH_OVERLAY_ENABLED = true

        @JvmStatic
        external fun nativeSetCheat(cheatId: Int, enabled: Boolean)

        @JvmStatic
        external fun nativeSetControllerConfig(enabled: Boolean, mapping: String, dpadDoubleTapRunEnabled: Boolean)

    }

    private var touchOverlayController: TouchOverlayController? = null
    private var controllerEnabled: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        controllerEnabled = intent.getBooleanExtra("controller_enabled", false)
        Log.i(TAG, "Controller enabled: $controllerEnabled")

        val controllerStore = ControllerConfigStore(filesDir)
        val controllerConfig: ControllerConfig = controllerStore.loadOrDefault()
        val mappingJson = Json.encodeToString(controllerConfig.mapping)
        val touchStore = TouchButtonStore(filesDir)
        val touchConfig = touchStore.loadOrDefault()
        Log.i(TAG, "Controller config loaded, mapping: $mappingJson, dpadRun=${touchConfig.dpadDoubleTapRunEnabled}")
        nativeSetControllerConfig(controllerEnabled, mappingJson, touchConfig.dpadDoubleTapRunEnabled)

        if (!TOUCH_OVERLAY_ENABLED) {
            Log.i(TAG, "Touch overlay disabled for startup crash isolation")
            return
        }

        val root = getContentView() as? ViewGroup
        if (root != null) {
            touchOverlayController = TouchOverlayController(filesDir, this, root, controllerEnabled, controllerConfig)
            touchOverlayController?.attach()
        } else {
            Log.w(TAG, "SDL content view is not available; touch overlay disabled")
        }
    }

    override fun onPause() {
        touchOverlayController?.releasePressedInputs()
        super.onPause()
    }

    override fun onDestroy() {
        touchOverlayController?.detach()
        touchOverlayController = null
        super.onDestroy()
    }

    override fun getLibraries(): Array<String> = arrayOf("hode")

    override fun getArguments(): Array<String> {
        val importedDir = File(filesDir, SafImporter.IMPORT_DIR).resolve(SafImporter.HOD_DIR)
        val savePath = File(filesDir, "saves")

        savePath.mkdirs()

        Log.i(TAG, "datapath=${importedDir.absolutePath}")
        Log.i(TAG, "savepath=${savePath.absolutePath}")

        return arrayOf(
            "--datapath=${importedDir.absolutePath}",
            "--savepath=${savePath.absolutePath}",
            "--debug=0"
        )
    }
}
