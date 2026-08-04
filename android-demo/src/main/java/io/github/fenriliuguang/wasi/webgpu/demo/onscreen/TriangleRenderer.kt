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
            postStatus("Surface destroyed")
        }
    }

    fun release() {
        handler.post {
            closed = true
            stopLoop()
            releaseSurface()
            pipeline?.let { runCatching { host?.drop(it) } }
            pipeline = null
            queue?.let { runCatching { host?.drop(it) } }
            queue = null
            device?.let { runCatching { host?.drop(it) } }
            device = null
            adapter?.let { runCatching { host?.drop(it) } }
            adapter = null
            runCatching { host?.close() }
            host = null
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
        handler.post(frameRunnable)
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
        h.renderPassSetPipeline(pass, pipe)
        h.renderPassDraw(pass, vertexCount = 3)
        h.renderPassEnd(pass)
        val cmd = h.commandEncoderFinish(encoder)
        h.queueSubmit(q, listOf(cmd))
        h.surfacePresent(surf)
        runCatching { h.drop(view) }
        runCatching { h.drop(texture) }
        runCatching { h.drop(cmd) }
    }

    private fun postStatus(message: String) {
        onStatus(message)
    }

    companion object {
        private const val TAG = "TriangleRenderer"
        private const val FRAME_MS = 16L

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
