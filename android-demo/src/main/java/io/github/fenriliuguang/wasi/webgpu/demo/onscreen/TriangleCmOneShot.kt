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
 *
 * Keeps one [DawnWasiWebGpuHost] + [WasmtimeCmTriangle.Session] for the Activity (D6). After each
 * loop calls [WasiWebGpuHost.releaseAllGpuObjects] so ANativeWindow disconnects without tearing
 * down the CM linker / GPUInstance (D2).
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
                    // Drop leftover Device/Surface from a prior press before Guest re-inits.
                    releaseGpuOwnership()
                    val windowHandle = Util.windowFromSurface(surface)
                    postStatus("CM Guest triangle frame loop ($frameCount frames)…")
                    ensureSession().runFrameLoop(windowHandle, width, height, frameCount)
                }.recoverCatching { first ->
                    Log.w(TAG, "CM frame loop failed; recreating Session", first)
                    recreateSession()
                    val windowHandle = Util.windowFromSurface(surface)
                    ensureSession().runFrameLoop(windowHandle, width, height, frameCount)
                }.onSuccess {
                    postStatus("CM Guest triangle OK (frame loop)")
                }.onFailure {
                    Log.e(TAG, "CM triangle frame loop failed", it)
                    postStatus("CM triangle FAILED: ${it.message}")
                }.also {
                    releaseGpuOwnership()
                    Thread.sleep(SURFACE_RELEASE_SETTLE_MS)
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
                val h = host
                host = null
                if (h != null) {
                    runCatching { h.releaseAllGpuObjects() }
                    (h as? DawnWasiWebGpuHost)?.flushEvents()
                    runCatching { h.close() }
                }
            } finally {
                latch.countDown()
            }
        }
        latch.await(5_000L, TimeUnit.MILLISECONDS)
        thread.quitSafely()
    }

    private fun ensureSession(): WasmtimeCmTriangle.Session {
        val h = host ?: DawnWasiWebGpuHost.create().also { host = it }
        session?.let { return it }
        WasmtimeVectorAddAndroid.ensureNativeLoaded()
        return WasmtimeCmTriangleAndroid.openSession(appContext, h).also { session = it }
    }

    private fun recreateSession() {
        runCatching { session?.close() }
        session = null
        Thread.sleep(SESSION_RECREATE_SETTLE_MS)
        releaseGpuOwnership()
        val h = host ?: DawnWasiWebGpuHost.create().also { host = it }
        WasmtimeVectorAddAndroid.ensureNativeLoaded()
        session = WasmtimeCmTriangleAndroid.openSession(appContext, h)
    }

    private fun releaseGpuOwnership() {
        val h = host ?: return
        runCatching { h.releaseAllGpuObjects() }
        (h as? DawnWasiWebGpuHost)?.flushEvents()
    }

    private fun postStatus(message: String) {
        onStatus(message)
    }

    companion object {
        private const val TAG = "TriangleCmOneShot"
        private const val DEFAULT_FRAME_COUNT = 30
        private const val SURFACE_RELEASE_SETTLE_MS = 400L
        private const val SESSION_RECREATE_SETTLE_MS = 250L
    }
}
