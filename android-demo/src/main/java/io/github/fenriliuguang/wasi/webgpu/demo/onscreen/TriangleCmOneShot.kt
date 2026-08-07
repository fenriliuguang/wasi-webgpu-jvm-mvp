package io.github.fenriliuguang.wasi.webgpu.demo.onscreen

import android.content.Context
import android.os.Handler
import android.os.HandlerThread
import android.util.Log
import android.view.Surface
import androidx.webgpu.helper.Util
import io.github.fenriliuguang.wasi.webgpu.demo.WasmtimeCmTriangleAndroid
import io.github.fenriliuguang.wasi.webgpu.demo.WasmtimeVectorAddAndroid
import io.github.fenriliuguang.wasi.webgpu.experimental.dawn.DawnWasiWebGpuHost
import io.github.fenriliuguang.wasi.webgpu.experimental.host.WasiWebGpuHost
import io.github.fenriliuguang.wasi.webgpu.experimental.runtime.cm.WasmtimeCmTriangle
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

/**
 * CM Guest red triangle on an Android [Surface] (host-driven frame loop).
 *
 * Call [runFrameLoopAndAwait] after pausing any L2 [TriangleRenderer] that shares the same
 * Surface. All Host/CM work runs on `webgpu-triangle-cm`.
 * Reuses a single [DawnWasiWebGpuHost] and CM [WasmtimeCmTriangle.Session] across runs
 * (not shared with L2; avoids process-global linker recreate traps).
 */
class TriangleCmOneShot(
    private val appContext: Context,
    private val onStatus: (String) -> Unit,
) {
    private val thread = HandlerThread("webgpu-triangle-cm").also { it.start() }
    private val handler = Handler(thread.looper)
    private var host: WasiWebGpuHost? = null
    private var session: WasmtimeCmTriangle.Session? = null
    private var closed = false

    /**
     * init-triangle → [frameCount]× draw-frame → drop-triangle on the CM thread.
     * Returns false on timeout or if already released.
     */
    fun runFrameLoopAndAwait(
        surface: Surface,
        width: Int,
        height: Int,
        frameCount: Int = DEFAULT_FRAME_COUNT,
        timeoutMs: Long = 60_000L,
    ): Boolean {
        val latch = CountDownLatch(1)
        var posted = false
        handler.post {
            try {
                if (closed) return@post
                posted = true
                runCatching {
                    require(surface.isValid) { "Surface is not valid" }
                    require(width > 0 && height > 0) { "invalid surface size ${width}x$height" }
                    val h = host ?: DawnWasiWebGpuHost.create().also { host = it }
                    val s = session ?: run {
                        WasmtimeVectorAddAndroid.ensureNativeLoaded()
                        WasmtimeCmTriangleAndroid.openSession(appContext, h).also { session = it }
                    }
                    val windowHandle = Util.windowFromSurface(surface)
                    postStatus("CM Guest triangle frame loop ($frameCount frames)…")
                    s.runFrameLoop(windowHandle, width, height, frameCount)
                }.onSuccess {
                    postStatus("CM Guest triangle OK (frame loop, host+session reused)")
                }.onFailure {
                    Log.e(TAG, "CM triangle frame loop failed", it)
                    postStatus("CM triangle FAILED: ${it.message}")
                }
            } finally {
                latch.countDown()
            }
        }
        val done = latch.await(timeoutMs, TimeUnit.MILLISECONDS)
        return done && posted && !closed
    }

    fun release() {
        val latch = CountDownLatch(1)
        handler.post {
            try {
                closed = true
                runCatching { session?.close() }
                session = null
                runCatching { host?.close() }
                host = null
            } finally {
                latch.countDown()
            }
        }
        latch.await(5_000L, TimeUnit.MILLISECONDS)
        thread.quitSafely()
    }

    private fun postStatus(message: String) {
        onStatus(message)
    }

    companion object {
        private const val TAG = "TriangleCmOneShot"
        private const val DEFAULT_FRAME_COUNT = 60
    }
}
