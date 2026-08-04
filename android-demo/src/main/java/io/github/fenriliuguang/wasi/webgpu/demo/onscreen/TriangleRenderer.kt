package io.github.fenriliuguang.wasi.webgpu.demo.onscreen

import android.os.Handler
import android.os.HandlerThread
import android.util.Log
import android.view.Surface
import androidx.webgpu.BackendType
import androidx.webgpu.CompositeAlphaMode
import androidx.webgpu.DeviceLostCallback
import androidx.webgpu.GPU
import androidx.webgpu.GPUAdapter
import androidx.webgpu.GPUColor
import androidx.webgpu.GPUColorTargetState
import androidx.webgpu.GPUDevice
import androidx.webgpu.GPUDeviceDescriptor
import androidx.webgpu.GPUFragmentState
import androidx.webgpu.GPUInstance
import androidx.webgpu.GPUPipelineLayoutDescriptor
import androidx.webgpu.GPUPrimitiveState
import androidx.webgpu.GPURenderPassColorAttachment
import androidx.webgpu.GPURenderPassDescriptor
import androidx.webgpu.GPURenderPipeline
import androidx.webgpu.GPURenderPipelineDescriptor
import androidx.webgpu.GPURequestAdapterOptions
import androidx.webgpu.GPURequestCallback
import androidx.webgpu.GPUShaderModuleDescriptor
import androidx.webgpu.GPUShaderSourceWGSL
import androidx.webgpu.GPUSurface
import androidx.webgpu.GPUSurfaceConfiguration
import androidx.webgpu.GPUSurfaceDescriptor
import androidx.webgpu.GPUSurfaceSourceAndroidNativeWindow
import androidx.webgpu.GPUVertexState
import androidx.webgpu.LoadOp
import androidx.webgpu.PowerPreference
import androidx.webgpu.PresentMode
import androidx.webgpu.PrimitiveTopology
import androidx.webgpu.StoreOp
import androidx.webgpu.SurfaceGetCurrentTextureStatus
import androidx.webgpu.TextureUsage
import androidx.webgpu.UncapturedErrorCallback
import androidx.webgpu.helper.Util
import androidx.webgpu.helper.initLibrary
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executor
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference

/**
 * Minimal Kotlin→androidx.webgpu on-screen path (red triangle).
 *
 * Does **not** go through [io.github.fenriliuguang.wasi.webgpu.experimental.host.WasiWebGpuHost]
 * or Guest/CM. Demo-only; not wasi-gfx / compliant wasi:webgpu.
 */
class TriangleRenderer(
    private val onStatus: (String) -> Unit,
) {
    private val thread = HandlerThread("webgpu-triangle").also { it.start() }
    private val handler = Handler(thread.looper)
    private val callbackExecutor: Executor = Executor(Runnable::run)

    private var instance: GPUInstance? = null
    private var adapter: GPUAdapter? = null
    private var device: GPUDevice? = null
    private var gpuSurface: GPUSurface? = null
    private var pipeline: GPURenderPipeline? = null
    private var surfaceFormat: Int = 0
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
                postStatus("Triangle rendering (Kotlin→Dawn Surface)")
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
            runCatching { pipeline?.close() }
            pipeline = null
            runCatching { device?.close() }
            device = null
            runCatching { adapter?.close() }
            adapter = null
            runCatching { instance?.close() }
            instance = null
        }
        thread.quitSafely()
    }

    private fun ensureDevice() {
        if (device != null) return
        initLibrary()
        val inst = GPU.createInstance()
        instance = inst
        // Keep processEvents pumping while waiting for async callbacks.
        handler.post(object : Runnable {
            override fun run() {
                if (closed) return
                runCatching { instance?.processEvents() }
                if (!closed) handler.postDelayed(this, POLL_MS)
            }
        })
        val adapt = awaitRequest<GPUAdapter>("requestAdapter") { cb ->
            inst.requestAdapter(
                callbackExecutor,
                GPURequestAdapterOptions(
                    powerPreference = PowerPreference.HighPerformance,
                    forceFallbackAdapter = false,
                    backendType = BackendType.Vulkan,
                ),
                cb,
            )
        }
        adapter = adapt
        val dev = awaitRequest<GPUDevice>("requestDevice") { cb ->
            adapt.requestDevice(
                callbackExecutor,
                GPUDeviceDescriptor(
                    deviceLostCallbackExecutor = callbackExecutor,
                    uncapturedErrorCallbackExecutor = callbackExecutor,
                    deviceLostCallback = DeviceLostCallback { _, reason, message ->
                        Log.e(TAG, "device lost reason=$reason: $message")
                    },
                    uncapturedErrorCallback = UncapturedErrorCallback { _, type, message ->
                        Log.e(TAG, "uncaptured error type=$type: $message")
                    },
                ),
                cb,
            )
        }
        device = dev
    }

    private fun attachOrResize(surface: Surface, w: Int, h: Int) {
        require(w > 0 && h > 0) { "invalid surface size ${w}x$h" }
        val inst = instance ?: error("no instance")
        val adapt = adapter ?: error("no adapter")
        val dev = device ?: error("no device")

        if (gpuSurface == null) {
            val nativeWindow = Util.windowFromSurface(surface)
            gpuSurface = inst.createSurface(
                GPUSurfaceDescriptor(
                    surfaceSourceAndroidNativeWindow =
                        GPUSurfaceSourceAndroidNativeWindow(nativeWindow),
                ),
            )
        }

        val caps = gpuSurface!!.getCapabilities(adapt)
        val format = caps.formats.firstOrNull()
            ?: error("surface has no texture formats")
        val presentMode = caps.presentModes.firstOrNull() ?: PresentMode.Fifo
        val alphaMode = caps.alphaModes.firstOrNull() ?: CompositeAlphaMode.Opaque

        gpuSurface!!.configure(
            GPUSurfaceConfiguration(
                device = dev,
                width = w,
                height = h,
                format = format,
                usage = TextureUsage.RenderAttachment,
                viewFormats = intArrayOf(),
                alphaMode = alphaMode,
                presentMode = presentMode,
            ),
        )
        surfaceFormat = format
        width = w
        height = h
        ensurePipeline(dev, format)
    }

    private fun ensurePipeline(dev: GPUDevice, format: Int) {
        if (pipeline != null) {
            // Format may change after recreate; rebuild if needed.
            runCatching { pipeline?.close() }
            pipeline = null
        }
        val module = dev.createShaderModule(
            GPUShaderModuleDescriptor(
                shaderSourceWGSL = GPUShaderSourceWGSL(SHADER),
            ),
        )
        val layout = dev.createPipelineLayout(
            GPUPipelineLayoutDescriptor(bindGroupLayouts = emptyArray()),
        )
        pipeline = dev.createRenderPipeline(
            GPURenderPipelineDescriptor(
                vertex = GPUVertexState(module = module, entryPoint = "vs_main"),
                layout = layout,
                primitive = GPUPrimitiveState(topology = PrimitiveTopology.TriangleList),
                fragment = GPUFragmentState(
                    module = module,
                    entryPoint = "fs_main",
                    targets = arrayOf(GPUColorTargetState(format = format)),
                ),
            ),
        )
        runCatching { module.close() }
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
        runCatching { gpuSurface?.unconfigure() }
        runCatching { gpuSurface?.close() }
        gpuSurface = null
        width = 0
        height = 0
    }

    private fun drawFrame() {
        val surf = gpuSurface ?: return
        val dev = device ?: return
        val pipe = pipeline ?: return
        if (width <= 0 || height <= 0) return

        instance?.processEvents()
        val surfaceTexture = surf.getCurrentTexture()
        when (surfaceTexture.status) {
            SurfaceGetCurrentTextureStatus.SuccessOptimal,
            SurfaceGetCurrentTextureStatus.SuccessSuboptimal,
            -> Unit
            else -> {
                Log.w(TAG, "getCurrentTexture status=${surfaceTexture.status}")
                return
            }
        }
        val texture = surfaceTexture.texture ?: return
        val view = texture.createView()
        val encoder = dev.createCommandEncoder()
        val pass = encoder.beginRenderPass(
            GPURenderPassDescriptor(
                colorAttachments = arrayOf(
                    GPURenderPassColorAttachment(
                        clearValue = GPUColor(0.08, 0.09, 0.12, 1.0),
                        view = view,
                        loadOp = LoadOp.Clear,
                        storeOp = StoreOp.Store,
                    ),
                ),
            ),
        )
        pass.setPipeline(pipe)
        pass.draw(3)
        pass.end()
        val cmd = encoder.finish()
        dev.queue.submit(arrayOf(cmd))
        surf.present()
        runCatching { view.close() }
        runCatching { texture.close() }
        runCatching { cmd.close() }
        runCatching { encoder.close() }
        runCatching { pass.close() }
    }

    private fun postStatus(message: String) {
        onStatus(message)
    }

    private fun <T> awaitRequest(op: String, block: (GPURequestCallback<T>) -> Unit): T {
        val resultRef = AtomicReference<T?>()
        val error = AtomicReference<Exception?>()
        val latch = CountDownLatch(1)
        block(
            object : GPURequestCallback<T> {
                override fun onResult(result: T) {
                    resultRef.set(result)
                    latch.countDown()
                }

                override fun onError(exception: Exception) {
                    error.set(exception)
                    latch.countDown()
                }
            },
        )
        // Pump events on this (render) thread while waiting.
        val deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(TIMEOUT_SEC)
        while (latch.count > 0 && System.nanoTime() < deadline) {
            runCatching { instance?.processEvents() }
            latch.await(5, TimeUnit.MILLISECONDS)
        }
        if (latch.count > 0) {
            throw IllegalStateException("$op timed out")
        }
        error.get()?.let { throw IllegalStateException("$op failed: ${it.message}", it) }
        @Suppress("UNCHECKED_CAST")
        return resultRef.get() as T
    }

    companion object {
        private const val TAG = "TriangleRenderer"
        private const val POLL_MS = 5L
        private const val FRAME_MS = 16L
        private const val TIMEOUT_SEC = 30L

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
