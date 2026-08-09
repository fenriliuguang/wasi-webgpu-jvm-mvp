package io.github.fenriliuguang.wasi.webgpu.demo.onscreen

import android.content.Context
import android.os.Handler
import android.os.HandlerThread
import android.util.Log
import android.view.Surface
import androidx.webgpu.helper.Util
import io.github.fenriliuguang.wasi.webgpu.demo.WasmtimeCmCubeAndroid
import io.github.fenriliuguang.wasi.webgpu.demo.WasmtimeVectorAddAndroid
import io.github.fenriliuguang.wasi.webgpu.experimental.dawn.DawnWasiWebGpuHost
import io.github.fenriliuguang.wasi.webgpu.experimental.host.WasiWebGpuHost
import io.github.fenriliuguang.wasi.webgpu.experimental.runtime.cm.WasmtimeCmCube
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

/**
 * CM Guest rotating textured cube on an Android [Surface] (host-driven frame loop).
 *
 * Keeps one [DawnWasiWebGpuHost] + [WasmtimeCmCube.Session] for the Activity.
 * Do not run back-to-back with triangle CM in the same process without force-stop
 * (wasmtime4j process-global CM registry).
 */
class CubeCmOneShot(
    private val appContext: Context,
    private val onStatus: (String) -> Unit,
) {
    private val thread = HandlerThread("webgpu-cube-cm").also { it.start() }
    private val handler = Handler(thread.looper)
    private var host: WasiWebGpuHost? = null
    private var session: WasmtimeCmCube.Session? = null
    private var closed = false

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
                    releaseGpuOwnership()
                    val windowHandle = Util.windowFromSurface(surface)
                    postStatus("CM Guest cube frame loop ($frameCount frames)…")
                    ensureSession().runFrameLoop(windowHandle, width, height, frameCount)
                }.recoverCatching { first ->
                    Log.w(TAG, "CM cube frame loop failed; recreating Session", first)
                    recreateSession()
                    val windowHandle = Util.windowFromSurface(surface)
                    ensureSession().runFrameLoop(windowHandle, width, height, frameCount)
                }.onSuccess {
                    postStatus("CM Guest cube OK (frame loop)")
                }.onFailure {
                    Log.e(TAG, "CM cube frame loop failed", it)
                    postStatus("CM cube FAILED: ${it.message}")
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

    private fun ensureSession(): WasmtimeCmCube.Session {
        val h = host ?: DawnWasiWebGpuHost.create().also { host = it }
        session?.let { return it }
        WasmtimeVectorAddAndroid.ensureNativeLoaded()
        return WasmtimeCmCubeAndroid.openSession(appContext, h).also { session = it }
    }

    private fun recreateSession() {
        runCatching { session?.close() }
        session = null
        Thread.sleep(SESSION_RECREATE_SETTLE_MS)
        releaseGpuOwnership()
        val h = host ?: DawnWasiWebGpuHost.create().also { host = it }
        WasmtimeVectorAddAndroid.ensureNativeLoaded()
        session = WasmtimeCmCubeAndroid.openSession(appContext, h)
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
        private const val TAG = "CubeCmOneShot"
        private const val DEFAULT_FRAME_COUNT = 60
        private const val SURFACE_RELEASE_SETTLE_MS = 400L
        private const val SESSION_RECREATE_SETTLE_MS = 250L
    }
}
