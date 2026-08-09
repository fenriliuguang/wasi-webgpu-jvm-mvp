package io.github.fenriliuguang.wasi.webgpu.demo

import android.app.Activity
import android.os.Bundle
import android.view.SurfaceView
import android.widget.FrameLayout

/**
 * Minimal Activity that only hosts a [SurfaceView] for CM Guest instrumented tests.
 * Does **not** wire Demo [MainActivity] cube autorun.
 */
class CmSurfaceTestActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        android.util.Log.i(TAG, "onCreate — Surface-only Activity (no Demo autorun)")
        val surfaceView = SurfaceView(this).apply {
            id = SURFACE_VIEW_ID
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT,
            )
        }
        setContentView(
            FrameLayout(this).apply {
                addView(surfaceView)
            },
        )
    }

    companion object {
        private const val TAG = "CmSurfaceTest"
        const val SURFACE_VIEW_ID: Int = 0x6C6F6361 // 'loca'
    }
}
