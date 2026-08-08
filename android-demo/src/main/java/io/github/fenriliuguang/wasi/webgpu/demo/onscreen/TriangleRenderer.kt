package io.github.fenriliuguang.wasi.webgpu.demo.onscreen

import android.os.Handler
import android.os.HandlerThread
import android.util.Log
import android.view.Surface
import androidx.webgpu.helper.Util
import io.github.fenriliuguang.wasi.webgpu.experimental.dawn.DawnWasiWebGpuHost
import io.github.fenriliuguang.wasi.webgpu.experimental.host.GpuHandle
import io.github.fenriliuguang.wasi.webgpu.experimental.host.PowerPreference
import io.github.fenriliuguang.wasi.webgpu.experimental.host.RequestAdapterOptions
import io.github.fenriliuguang.wasi.webgpu.experimental.host.ShaderModuleDescriptor
import io.github.fenriliuguang.wasi.webgpu.experimental.host.SurfaceTextureStatus
import io.github.fenriliuguang.wasi.webgpu.experimental.host.WasiWebGpuHost
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

/**
 * On-screen red triangle via [WasiWebGpuHost] → Dawn (L2).
 *
 * Does **not** go through Guest/CM/wasi-gfx. Demo-only; not compliant wasi:webgpu.
 * All Host calls run on [HandlerThread] `webgpu-triangle` (see docs/mapping/threading.md).
 */
class TriangleRenderer(
    private val onStatus: (String) -> Unit,
) {
    private val thread = HandlerThread("webgpu-triangle").also { it.start() }
    private val handler = Handler(thread.looper)

    private var host: WasiWebGpuHost? = null
    private var adapter: GpuHandle? = null
    private var device: GpuHandle? = null
    private var queue: GpuHandle? = null
    private var surfaceHandle: GpuHandle? = null
    private var pipeline: GpuHandle? = null
    private var width: Int = 0
    private var height: Int = 0
    private var rendering = false
    private var closed = false
    /** True while CM Guest owns the Android Surface — ignore SurfaceHolder resize/attach. */
    private var pausedForCm = false

    private val frameRunnable = object : Runnable {
        override fun run() {
            if (!rendering || closed) return
            runCatching { drawFrame() }
                .onFailure { Log.e(TAG, "drawFrame failed", it) }
            if (rendering && !closed) {
                handler.postDelayed(this, FRAME_MS)
            }
        }
    }

    fun start() {
        handler.post {
            runCatching { ensureDevice() }
                .onSuccess { postStatus("GPU ready — waiting for Surface") }
                .onFailure {
                    Log.e(TAG, "GPU init failed", it)
                    postStatus("GPU init FAILED: ${it.message}")
                }
        }
    }

    fun onSurfaceAvailable(surface: Surface, w: Int, h: Int) {
        handler.post {
            if (pausedForCm || closed) {
                Log.d(TAG, "ignore surfaceAvailable while pausedForCm=$pausedForCm closed=$closed")
                return@post
            }
            runCatching {
                ensureDevice()
                attachOrResize(surface, w, h)
                startLoop()
            }.onSuccess {
                postStatus("Triangle rendering (L2 Host→Dawn Surface)")
            }.onFailure {
                Log.e(TAG, "surface attach failed", it)
                postStatus("Surface FAILED: ${it.message}")
            }
        }
    }

    fun onSurfaceResized(surface: Surface, w: Int, h: Int) {
        handler.post {
            // CM owns the native window — attach/configure here races and fails getCapabilities.
            if (pausedForCm || closed) {
                Log.d(TAG, "ignore surfaceResized while pausedForCm=$pausedForCm closed=$closed")
                return@post
            }
            runCatching {
                if (device == null) return@runCatching
                attachOrResize(surface, w, h)
            }.onFailure {
                Log.e(TAG, "surface resize failed", it)
                postStatus("Resize FAILED: ${it.message}")
            }
        }
    }

    fun onSurfaceDestroyed() {
        handler.post {
            stopLoop()
            releaseSurface()
            // Surface gone — clear CM pause so a later available can attach again.
            pausedForCm = false
            postStatus("Surface destroyed")
        }
    }

    /**
     * Stop the L2 frame loop and fully tear down Dawn Surface + Host so CM can connect.
     * Call from a non-render caller (test thread / bg thread) before CM Guest owns the Surface.
     */
    fun pauseSurfaceAndAwait(timeoutMs: Long = 5_000L): Boolean {
        val latch = CountDownLatch(1)
        handler.post {
            try {
                pausedForCm = true
                stopLoop()
                // Full teardown: unconfigure alone leaves VK_ERROR_NATIVE_WINDOW_IN_USE on Mali.
                teardownGpu()
                postStatus("L2 Surface paused for CM")
            } finally {
                latch.countDown()
            }
        }
        val ok = latch.await(timeoutMs, TimeUnit.MILLISECONDS)
        if (ok) {
            Thread.sleep(PAUSE_SETTLE_MS)
        }
        return ok
    }

    /**
     * Recreate L2 Host, re-attach [surface], and restart the frame loop after CM finishes.
     */
    fun resumeSurfaceAndAwait(
        surface: Surface,
        w: Int,
        h: Int,
        timeoutMs: Long = 12_000L,
        attempts: Int = 5,
    ): Boolean {
        require(attempts > 0)
        val latch = CountDownLatch(1)
        var ok = false
        var lastError: Throwable? = null
        handler.post {
            try {
                if (closed) return@post
                require(surface.isValid) { "Surface is not valid" }
                repeat(attempts) { attempt ->
                    try {
                        teardownGpu()
                        ensureDevice()
                        attachOrResize(surface, w, h)
                        startLoop()
                        pausedForCm = false
                        ok = true
                        postStatus("Triangle rendering (L2 Host→Dawn Surface)")
                        return@post
                    } catch (t: Throwable) {
                        lastError = t
                        Log.w(TAG, "resume attempt ${attempt + 1}/$attempts failed", t)
                        teardownGpu()
                        if (attempt < attempts - 1) {
                            try {
                                Thread.sleep(RESUME_RETRY_DELAY_MS)
                            } catch (_: InterruptedException) {
                                Thread.currentThread().interrupt()
                                return@post
                            }
                        }
                    }
                }
                postStatus("L2 resume FAILED: ${lastError?.message}")
            } catch (t: Throwable) {
                Log.e(TAG, "resume surface failed", t)
                postStatus("L2 resume FAILED: ${t.message}")
            } finally {
                latch.countDown()
            }
        }
        return latch.await(timeoutMs, TimeUnit.MILLISECONDS) && ok
    }

    fun release() {
        handler.post {
            closed = true
            stopLoop()
            teardownGpu()
        }
        thread.quitSafely()
    }

    private fun ensureDevice() {
        if (device != null) return
        val h = DawnWasiWebGpuHost.create()
        host = h
        val adapt = h.requestAdapter(
            RequestAdapterOptions(powerPreference = PowerPreference.HighPerformance),
        )
        adapter = adapt
        val dev = h.adapterRequestDevice(adapt)
        device = dev
        queue = h.deviceGetQueue(dev)
    }

    private fun attachOrResize(surface: Surface, w: Int, h: Int) {
        require(w > 0 && h > 0) { "invalid surface size ${w}x$h" }
        val gpuHost = host ?: error("no host")
        val adapt = adapter ?: error("no adapter")
        val dev = device ?: error("no device")

        if (surfaceHandle == null) {
            val nativeWindow = Util.windowFromSurface(surface)
            surfaceHandle = gpuHost.instanceCreateSurfaceFromAndroidNativeWindow(nativeWindow)
        }

        val format = gpuHost.surfaceConfigure(surfaceHandle!!, dev, adapt, w, h)
        width = w
        height = h
        ensurePipeline(gpuHost, dev, format)
    }

    private fun ensurePipeline(gpuHost: WasiWebGpuHost, dev: GpuHandle, format: Int) {
        pipeline?.let { runCatching { gpuHost.drop(it) } }
        pipeline = null
        val module = gpuHost.deviceCreateShaderModule(
            dev,
            ShaderModuleDescriptor(code = SHADER),
        )
        pipeline = gpuHost.deviceCreateRenderPipelineTriangle(dev, module, format)
        runCatching { gpuHost.drop(module) }
    }

    private fun startLoop() {
        if (rendering) return
        rendering = true
        handler.removeCallbacks(frameRunnable)
        // Defer first present until after SurfaceView has composited (Mali cold-start race).
        handler.postDelayed(frameRunnable, FIRST_FRAME_DELAY_MS)
    }

    private fun stopLoop() {
        rendering = false
        handler.removeCallbacks(frameRunnable)
    }

    private fun releaseSurface() {
        val h = host
        val surf = surfaceHandle
        if (h != null && surf != null) {
            runCatching { h.surfaceUnconfigure(surf) }
            runCatching { h.drop(surf) }
        }
        surfaceHandle = null
        width = 0
        height = 0
    }

    private fun teardownGpu() {
        releaseSurface()
        val h = host
        pipeline?.let { runCatching { h?.drop(it) } }
        pipeline = null
        queue?.let { runCatching { h?.drop(it) } }
        queue = null
        device?.let { runCatching { h?.drop(it) } }
        device = null
        adapter?.let { runCatching { h?.drop(it) } }
        adapter = null
        runCatching { h?.close() }
        host = null
    }

    private fun drawFrame() {
        val h = host ?: return
        val surf = surfaceHandle ?: return
        val dev = device ?: return
        val q = queue ?: return
        val pipe = pipeline ?: return
        if (width <= 0 || height <= 0) return

        val acquired = h.surfaceGetCurrentTexture(surf)
        when (acquired.status) {
            SurfaceTextureStatus.SuccessOptimal,
            SurfaceTextureStatus.SuccessSuboptimal,
            -> Unit
            else -> {
                Log.w(TAG, "getCurrentTexture status=${acquired.status}")
                return
            }
        }
        val texture = acquired.texture ?: return
        val view = h.textureCreateView(texture)
        val encoder = h.deviceCreateCommandEncoder(dev)
        val pass = h.commandEncoderBeginRenderPassClear(
            encoder,
            view,
            clearR = 0.08f,
            clearG = 0.09f,
            clearB = 0.12f,
            clearA = 1.0f,
        )
        try {
            h.renderPassSetPipeline(pass, pipe)
            h.renderPassDraw(pass, vertexCount = 3)
            h.renderPassEnd(pass)
            val cmd = h.commandEncoderFinish(encoder)
            h.queueSubmit(q, listOf(cmd))
            h.surfacePresent(surf)
            runCatching { h.drop(cmd) }
        } finally {
            // Drop handle-table entries only — closing swapchain GPUTexture/View races Mali/Dawn
            // (D1). Present returns the buffer to BLAST (D5).
            runCatching { dropSwapchainRef(h, view) }
            runCatching { dropSwapchainRef(h, texture) }
            runCatching { h.drop(pass) }
            runCatching { h.drop(encoder) }
        }
    }

    /** Remove table entry without native close (swapchain textures are owned by the Surface). */
    private fun dropSwapchainRef(h: WasiWebGpuHost, handle: GpuHandle) {
        when (h) {
            is DawnWasiWebGpuHost -> h.drop(handle, closeResource = false)
            else -> h.drop(handle)
        }
    }

    private fun postStatus(message: String) {
        onStatus(message)
    }

    companion object {
        private const val TAG = "TriangleRenderer"
        private const val FRAME_MS = 16L
        private const val FIRST_FRAME_DELAY_MS = 250L
        private const val PAUSE_SETTLE_MS = 300L
        private const val RESUME_RETRY_DELAY_MS = 400L

        private val SHADER = """
            @vertex fn vs_main(@builtin(vertex_index) vertexIndex : u32) -> @builtin(position) vec4f {
              let pos = array(
                vec2f( 0.0,  0.6),
                vec2f(-0.6, -0.6),
                vec2f( 0.6, -0.6)
              );
              return vec4f(pos[vertexIndex], 0.0, 1.0);
            }
            @fragment fn fs_main() -> @location(0) vec4f {
              return vec4f(1.0, 0.15, 0.1, 1.0);
            }
        """.trimIndent()
    }
}
