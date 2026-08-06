package io.github.fenriliuguang.wasi.webgpu.demo.onscreen

import android.content.Context
import android.os.Handler
import android.os.HandlerThread
import android.util.Log
import android.view.Surface
import io.github.fenriliuguang.wasi.webgpu.demo.WasmtimeCmTriangleAndroid

/**
 * One-shot CM Guest red triangle on an Android [Surface].
 *
 * Does not own a frame loop — call [drawOnce] after pausing any L2 [TriangleRenderer]
 * that shares the same Surface. All Host/CM work runs on `webgpu-triangle-cm`.
 */
class TriangleCmOneShot(
    private val appContext: Context,
    private val onStatus: (String) -> Unit,
) {
    private val thread = HandlerThread("webgpu-triangle-cm").also { it.start() }
    private val handler = Handler(thread.looper)
    private var closed = false

    fun drawOnce(surface: Surface, width: Int, height: Int) {
        handler.post {
            if (closed) return@post
            runCatching {
                WasmtimeCmTriangleAndroid.runOnce(appContext, surface, width, height)
            }.onSuccess {
                postStatus("CM Guest triangle OK (one-shot)")
            }.onFailure {
                Log.e(TAG, "CM triangle failed", it)
                postStatus("CM triangle FAILED: ${it.message}")
            }
        }
    }

    fun release() {
        handler.post { closed = true }
        thread.quitSafely()
    }

    private fun postStatus(message: String) {
        onStatus(message)
    }

    companion object {
        private const val TAG = "TriangleCmOneShot"
    }
}
