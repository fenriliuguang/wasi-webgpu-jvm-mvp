package io.github.fenriliuguang.wasi.webgpu.experimental.dawn

import androidx.webgpu.BackendType
import androidx.webgpu.BufferBindingType as DawnBufferBindingType
import androidx.webgpu.CompositeAlphaMode
import androidx.webgpu.DeviceLostCallback
import androidx.webgpu.GPU
import androidx.webgpu.GPUAdapter
import androidx.webgpu.GPUBindGroup
import androidx.webgpu.GPUBindGroupDescriptor
import androidx.webgpu.GPUBindGroupEntry
import androidx.webgpu.GPUBindGroupLayout
import androidx.webgpu.GPUBindGroupLayoutDescriptor
import androidx.webgpu.GPUBindGroupLayoutEntry
import androidx.webgpu.GPUBuffer
import androidx.webgpu.GPUBufferBindingLayout
import androidx.webgpu.GPUBufferDescriptor
import androidx.webgpu.GPUColor
import androidx.webgpu.GPUColorTargetState
import androidx.webgpu.GPUCommandBuffer
import androidx.webgpu.GPUCommandEncoder
import androidx.webgpu.GPUCommandEncoderDescriptor
import androidx.webgpu.GPUComputePassDescriptor
import androidx.webgpu.GPUComputePassEncoder
import androidx.webgpu.GPUComputePipeline
import androidx.webgpu.GPUComputePipelineDescriptor
import androidx.webgpu.GPUComputeState
import androidx.webgpu.GPUDevice
import androidx.webgpu.GPUDeviceDescriptor
import androidx.webgpu.GPUFragmentState
import androidx.webgpu.GPUInstance
import androidx.webgpu.GPUPipelineLayout
import androidx.webgpu.GPUPipelineLayoutDescriptor
import androidx.webgpu.GPUPrimitiveState
import androidx.webgpu.GPUQueue
import androidx.webgpu.GPURenderPassColorAttachment
import androidx.webgpu.GPURenderPassDescriptor
import androidx.webgpu.GPURenderPassEncoder
import androidx.webgpu.GPURenderPipeline
import androidx.webgpu.GPURenderPipelineDescriptor
import androidx.webgpu.GPURequestAdapterOptions
import androidx.webgpu.GPURequestCallback
import androidx.webgpu.GPUShaderModule
import androidx.webgpu.GPUShaderModuleDescriptor
import androidx.webgpu.GPUShaderSourceWGSL
import androidx.webgpu.GPUSurface
import androidx.webgpu.GPUSurfaceConfiguration
import androidx.webgpu.GPUSurfaceDescriptor
import androidx.webgpu.GPUSurfaceSourceAndroidNativeWindow
import androidx.webgpu.GPUTexture
import androidx.webgpu.GPUTextureView
import androidx.webgpu.GPUVertexAttribute
import androidx.webgpu.GPUVertexBufferLayout
import androidx.webgpu.GPUVertexState
import androidx.webgpu.LoadOp
import androidx.webgpu.PowerPreference as DawnPowerPreference
import androidx.webgpu.PresentMode
import androidx.webgpu.PrimitiveTopology
import androidx.webgpu.StoreOp
import androidx.webgpu.SurfaceGetCurrentTextureStatus
import androidx.webgpu.TextureUsage
import androidx.webgpu.UncapturedErrorCallback
import androidx.webgpu.VertexFormat
import androidx.webgpu.VertexStepMode
import androidx.webgpu.helper.initLibrary
import io.github.fenriliuguang.wasi.webgpu.experimental.host.BindGroupDescriptor
import io.github.fenriliuguang.wasi.webgpu.experimental.host.BindGroupLayoutDescriptor
import io.github.fenriliuguang.wasi.webgpu.experimental.host.BufferBindingType
import io.github.fenriliuguang.wasi.webgpu.experimental.host.BufferDescriptor
import io.github.fenriliuguang.wasi.webgpu.experimental.host.CommandEncoderDescriptor
import io.github.fenriliuguang.wasi.webgpu.experimental.host.ComputePassDescriptor
import io.github.fenriliuguang.wasi.webgpu.experimental.host.ComputePipelineDescriptor
import io.github.fenriliuguang.wasi.webgpu.experimental.host.GpuHandle
import io.github.fenriliuguang.wasi.webgpu.experimental.host.GpuVertexFormat
import io.github.fenriliuguang.wasi.webgpu.experimental.host.GpuVertexStepMode
import io.github.fenriliuguang.wasi.webgpu.experimental.host.HandleTable
import io.github.fenriliuguang.wasi.webgpu.experimental.host.HostException
import io.github.fenriliuguang.wasi.webgpu.experimental.host.PowerPreference
import io.github.fenriliuguang.wasi.webgpu.experimental.host.RequestAdapterOptions
import io.github.fenriliuguang.wasi.webgpu.experimental.host.ResourceKind
import io.github.fenriliuguang.wasi.webgpu.experimental.host.ShaderModuleDescriptor
import io.github.fenriliuguang.wasi.webgpu.experimental.host.SurfaceTextureResult
import io.github.fenriliuguang.wasi.webgpu.experimental.host.SurfaceTextureStatus
import io.github.fenriliuguang.wasi.webgpu.experimental.host.VertexBufferLayout
import io.github.fenriliuguang.wasi.webgpu.experimental.host.WasiWebGpuHost
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executor
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference

/**
 * L3 Dawn backend for [WasiWebGpuHost].
 *
 * Depends on `androidx.webgpu`. Must not be referenced by L1 runtime adapters.
 * Methods are synchronous wrappers around Dawn async entry points and poll
 * [GPUInstance.processEvents] until callbacks fire.
 *
 * Surface/render: caller must invoke configure / getCurrentTexture / present /
 * submit on the same thread for a given host instance (see docs/mapping/threading.md).
 */
class DawnWasiWebGpuHost private constructor(
    private val instance: GPUInstance,
) : WasiWebGpuHost {

    private val handles = HandleTable()
    private val callbackExecutor: Executor = Executor(Runnable::run)
    private val eventPoller = Executors.newSingleThreadExecutor()
    private val pipelineLayouts = HashMap<Int, GPUPipelineLayout>()
    /** Serializes Dawn GPU work with [GPUInstance.processEvents] (Mali SIGSEGV under races). */
    private val gpuLock = Any()
    @Volatile private var closed = false

    init {
        // Keep L2 GpuVertex* constants aligned with androidx.webgpu for CM Guest u32 flags.
        check(GpuVertexFormat.FLOAT32X2 == VertexFormat.Float32x2) {
            "GpuVertexFormat.FLOAT32X2=${GpuVertexFormat.FLOAT32X2} != VertexFormat.Float32x2=${VertexFormat.Float32x2}"
        }
        check(GpuVertexStepMode.VERTEX == VertexStepMode.Vertex) {
            "GpuVertexStepMode.VERTEX=${GpuVertexStepMode.VERTEX} != VertexStepMode.Vertex=${VertexStepMode.Vertex}"
        }
        eventPoller.execute {
            while (!closed) {
                synchronized(gpuLock) {
                    runCatching { instance.processEvents() }
                }
                try {
                    Thread.sleep(POLL_MS)
                } catch (_: InterruptedException) {
                    Thread.currentThread().interrupt()
                    break
                }
            }
        }
    }

    override fun requestAdapter(options: RequestAdapterOptions): GpuHandle {
        val dawnOptions = GPURequestAdapterOptions(
            powerPreference = when (options.powerPreference) {
                PowerPreference.Undefined -> DawnPowerPreference.Undefined
                PowerPreference.LowPower -> DawnPowerPreference.LowPower
                PowerPreference.HighPerformance -> DawnPowerPreference.HighPerformance
            },
            forceFallbackAdapter = options.forceFallbackAdapter,
            // Android Surface path needs Vulkan; Undefined may pick GLES and leave the
            // native window connected, so CM Vulkan createSurface hits WINDOW_IN_USE.
            backendType = BackendType.Vulkan,
        )
        val adapter = awaitRequest<GPUAdapter>("requestAdapter") { callback ->
            instance.requestAdapter(callbackExecutor, dawnOptions, callback)
        }
        return handles.insert(ResourceKind.Adapter, adapter)
    }

    override fun adapterRequestDevice(adapter: GpuHandle): GpuHandle {
        val gpuAdapter = handles.get<GPUAdapter>(adapter, ResourceKind.Adapter)
        val descriptor = GPUDeviceDescriptor(
            deviceLostCallbackExecutor = callbackExecutor,
            uncapturedErrorCallbackExecutor = callbackExecutor,
            deviceLostCallback = DeviceLostCallback { _, reason, message ->
                throw HostException.Backend("device lost reason=$reason: $message")
            },
            uncapturedErrorCallback = UncapturedErrorCallback { _, type, message ->
                throw HostException.Backend("uncaptured error type=$type: $message")
            },
        )
        val device = awaitRequest<GPUDevice>("requestDevice") { callback ->
            gpuAdapter.requestDevice(callbackExecutor, descriptor, callback)
        }
        return handles.insert(ResourceKind.Device, device)
    }

    override fun deviceGetQueue(device: GpuHandle): GpuHandle {
        val gpuDevice = handles.get<GPUDevice>(device, ResourceKind.Device)
        return handles.insert(ResourceKind.Queue, gpuDevice.queue)
    }

    override fun deviceCreateBuffer(device: GpuHandle, descriptor: BufferDescriptor): GpuHandle {
        val gpuDevice = handles.get<GPUDevice>(device, ResourceKind.Device)
        val buffer = gpuDevice.createBuffer(
            GPUBufferDescriptor(
                usage = descriptor.usage,
                size = descriptor.size,
                mappedAtCreation = descriptor.mappedAtCreation,
                label = descriptor.label,
            ),
        )
        return handles.insert(ResourceKind.Buffer, buffer)
    }

    override fun deviceCreateShaderModule(
        device: GpuHandle,
        descriptor: ShaderModuleDescriptor,
    ): GpuHandle {
        val gpuDevice = handles.get<GPUDevice>(device, ResourceKind.Device)
        val module = gpuDevice.createShaderModule(
            GPUShaderModuleDescriptor(
                label = descriptor.label,
                shaderSourceWGSL = GPUShaderSourceWGSL(descriptor.code),
            ),
        )
        return handles.insert(ResourceKind.ShaderModule, module)
    }

    override fun deviceCreateBindGroupLayout(
        device: GpuHandle,
        descriptor: BindGroupLayoutDescriptor,
    ): GpuHandle {
        val gpuDevice = handles.get<GPUDevice>(device, ResourceKind.Device)
        val entries = descriptor.entries.map { entry ->
            val bufferLayout = entry.buffer
                ?: throw HostException.Validation("P0 bind-group-layout entry requires buffer layout")
            GPUBindGroupLayoutEntry(
                binding = entry.binding,
                visibility = entry.visibility,
                buffer = GPUBufferBindingLayout(
                    type = when (bufferLayout.type) {
                        BufferBindingType.Uniform -> DawnBufferBindingType.Uniform
                        BufferBindingType.Storage -> DawnBufferBindingType.Storage
                        BufferBindingType.ReadOnlyStorage -> DawnBufferBindingType.ReadOnlyStorage
                    },
                    hasDynamicOffset = bufferLayout.hasDynamicOffset,
                    minBindingSize = bufferLayout.minBindingSize,
                ),
            )
        }.toTypedArray()
        val layout = gpuDevice.createBindGroupLayout(
            GPUBindGroupLayoutDescriptor(
                entries = entries,
                label = descriptor.label,
            ),
        )
        return handles.insert(ResourceKind.BindGroupLayout, layout)
    }

    override fun deviceCreateBindGroup(device: GpuHandle, descriptor: BindGroupDescriptor): GpuHandle {
        val gpuDevice = handles.get<GPUDevice>(device, ResourceKind.Device)
        val layout = handles.get<GPUBindGroupLayout>(descriptor.layout, ResourceKind.BindGroupLayout)
        val entries = descriptor.entries.map { entry ->
            val buffer = handles.get<GPUBuffer>(entry.resource.buffer, ResourceKind.Buffer)
            GPUBindGroupEntry(
                binding = entry.binding,
                buffer = buffer,
                offset = entry.resource.offset,
                size = entry.resource.size ?: (buffer.size - entry.resource.offset),
            )
        }.toTypedArray()
        val bindGroup = gpuDevice.createBindGroup(
            GPUBindGroupDescriptor(
                layout = layout,
                entries = entries,
                label = descriptor.label,
            ),
        )
        return handles.insert(ResourceKind.BindGroup, bindGroup)
    }

    override fun deviceCreateComputePipeline(
        device: GpuHandle,
        descriptor: ComputePipelineDescriptor,
    ): GpuHandle {
        val gpuDevice = handles.get<GPUDevice>(device, ResourceKind.Device)
        val module = handles.get<GPUShaderModule>(descriptor.compute.module, ResourceKind.ShaderModule)
        val layoutHandle = descriptor.layout
            ?: throw HostException.Unsupported("auto pipeline layout; pass an explicit bind-group layout handle")
        val bindGroupLayout = handles.get<GPUBindGroupLayout>(layoutHandle, ResourceKind.BindGroupLayout)
        val pipelineLayout = gpuDevice.createPipelineLayout(
            GPUPipelineLayoutDescriptor(bindGroupLayouts = arrayOf(bindGroupLayout)),
        )
        val pipeline = gpuDevice.createComputePipeline(
            GPUComputePipelineDescriptor(
                layout = pipelineLayout,
                compute = GPUComputeState(
                    module = module,
                    entryPoint = descriptor.compute.entryPoint ?: "main",
                ),
                label = descriptor.label,
            ),
        )
        val handle = handles.insert(ResourceKind.ComputePipeline, pipeline)
        pipelineLayouts[handle.raw] = pipelineLayout
        return handle
    }

    override fun deviceCreateCommandEncoder(
        device: GpuHandle,
        descriptor: CommandEncoderDescriptor,
    ): GpuHandle {
        synchronized(gpuLock) {
            val gpuDevice = handles.get<GPUDevice>(device, ResourceKind.Device)
            val encoder = gpuDevice.createCommandEncoder(
                GPUCommandEncoderDescriptor(label = descriptor.label),
            )
            return handles.insert(ResourceKind.CommandEncoder, encoder)
        }
    }

    override fun instanceCreateSurfaceFromAndroidNativeWindow(nativeWindowHandle: Long): GpuHandle {
        synchronized(gpuLock) {
            val surface = instance.createSurface(
                GPUSurfaceDescriptor(
                    surfaceSourceAndroidNativeWindow =
                        GPUSurfaceSourceAndroidNativeWindow(nativeWindowHandle),
                ),
            )
            return handles.insert(ResourceKind.Surface, surface)
        }
    }

    override fun surfaceConfigure(
        surface: GpuHandle,
        device: GpuHandle,
        adapter: GpuHandle,
        width: Int,
        height: Int,
    ): Int {
        require(width > 0 && height > 0) { "invalid surface size ${width}x$height" }
        synchronized(gpuLock) {
            val gpuSurface = handles.get<GPUSurface>(surface, ResourceKind.Surface)
            val gpuDevice = handles.get<GPUDevice>(device, ResourceKind.Device)
            val gpuAdapter = handles.get<GPUAdapter>(adapter, ResourceKind.Adapter)
            val caps = gpuSurface.getCapabilities(gpuAdapter)
            val format = caps.formats.firstOrNull()
                ?: throw HostException.Backend("surface has no texture formats")
            val presentMode = PresentMode.Fifo
            val alphaMode = caps.alphaModes.firstOrNull() ?: CompositeAlphaMode.Opaque
            gpuSurface.configure(
                GPUSurfaceConfiguration(
                    device = gpuDevice,
                    width = width,
                    height = height,
                    format = format,
                    usage = TextureUsage.RenderAttachment,
                    viewFormats = intArrayOf(),
                    alphaMode = alphaMode,
                    presentMode = presentMode,
                ),
            )
            return format
        }
    }

    override fun surfaceUnconfigure(surface: GpuHandle) {
        synchronized(gpuLock) {
            val gpuSurface = handles.get<GPUSurface>(surface, ResourceKind.Surface)
            gpuSurface.unconfigure()
        }
    }

    override fun surfaceGetCurrentTexture(surface: GpuHandle): SurfaceTextureResult {
        synchronized(gpuLock) {
            val gpuSurface = handles.get<GPUSurface>(surface, ResourceKind.Surface)
            val surfaceTexture = gpuSurface.getCurrentTexture()
            val status = when (surfaceTexture.status) {
                SurfaceGetCurrentTextureStatus.SuccessOptimal -> SurfaceTextureStatus.SuccessOptimal
                SurfaceGetCurrentTextureStatus.SuccessSuboptimal -> SurfaceTextureStatus.SuccessSuboptimal
                SurfaceGetCurrentTextureStatus.Timeout -> SurfaceTextureStatus.Timeout
                SurfaceGetCurrentTextureStatus.Outdated -> SurfaceTextureStatus.Outdated
                SurfaceGetCurrentTextureStatus.Lost -> SurfaceTextureStatus.Lost
                else -> SurfaceTextureStatus.Error
            }
            val texture = surfaceTexture.texture
            return if (
                texture != null &&
                (status == SurfaceTextureStatus.SuccessOptimal ||
                    status == SurfaceTextureStatus.SuccessSuboptimal)
            ) {
                SurfaceTextureResult(status, handles.insert(ResourceKind.Texture, texture))
            } else {
                SurfaceTextureResult(status, null)
            }
        }
    }

    override fun surfacePresent(surface: GpuHandle) {
        synchronized(gpuLock) {
            val gpuSurface = handles.get<GPUSurface>(surface, ResourceKind.Surface)
            gpuSurface.present()
        }
    }

    override fun deviceCreateRenderPipelineTriangle(
        device: GpuHandle,
        shader: GpuHandle,
        format: Int,
    ): GpuHandle = createRenderPipelineTriangle(device, shader, format, vertexBuffers = emptyList())

    override fun deviceCreateRenderPipelineTriangleBuffers(
        device: GpuHandle,
        shader: GpuHandle,
        format: Int,
        vertexBuffers: List<VertexBufferLayout>,
    ): GpuHandle = createRenderPipelineTriangle(device, shader, format, vertexBuffers)

    private fun createRenderPipelineTriangle(
        device: GpuHandle,
        shader: GpuHandle,
        format: Int,
        vertexBuffers: List<VertexBufferLayout>,
    ): GpuHandle {
        synchronized(gpuLock) {
            val gpuDevice = handles.get<GPUDevice>(device, ResourceKind.Device)
            val module = handles.get<GPUShaderModule>(shader, ResourceKind.ShaderModule)
            val pipelineLayout = gpuDevice.createPipelineLayout(
                GPUPipelineLayoutDescriptor(bindGroupLayouts = emptyArray()),
            )
            val dawnBuffers = vertexBuffers.map { layout ->
                GPUVertexBufferLayout(
                    arrayStride = layout.arrayStride,
                    stepMode = layout.stepMode,
                    attributes = layout.attributes.map { attr ->
                        GPUVertexAttribute(
                            format = attr.format,
                            offset = attr.offset,
                            shaderLocation = attr.shaderLocation,
                        )
                    }.toTypedArray(),
                )
            }.toTypedArray()
            val pipeline = gpuDevice.createRenderPipeline(
                GPURenderPipelineDescriptor(
                    vertex = GPUVertexState(
                        module = module,
                        entryPoint = "vs_main",
                        buffers = dawnBuffers,
                    ),
                    layout = pipelineLayout,
                    primitive = GPUPrimitiveState(topology = PrimitiveTopology.TriangleList),
                    fragment = GPUFragmentState(
                        module = module,
                        entryPoint = "fs_main",
                        targets = arrayOf(GPUColorTargetState(format = format)),
                    ),
                ),
            )
            val handle = handles.insert(ResourceKind.RenderPipeline, pipeline)
            pipelineLayouts[handle.raw] = pipelineLayout
            return handle
        }
    }

    override fun textureCreateView(texture: GpuHandle): GpuHandle {
        synchronized(gpuLock) {
            val gpuTexture = handles.get<GPUTexture>(texture, ResourceKind.Texture)
            return handles.insert(ResourceKind.TextureView, gpuTexture.createView())
        }
    }

    override fun commandEncoderBeginRenderPassClear(
        encoder: GpuHandle,
        view: GpuHandle,
        clearR: Float,
        clearG: Float,
        clearB: Float,
        clearA: Float,
    ): GpuHandle {
        synchronized(gpuLock) {
            val commandEncoder = handles.get<GPUCommandEncoder>(encoder, ResourceKind.CommandEncoder)
            val textureView = handles.get<GPUTextureView>(view, ResourceKind.TextureView)
            val pass = commandEncoder.beginRenderPass(
                GPURenderPassDescriptor(
                    colorAttachments = arrayOf(
                        GPURenderPassColorAttachment(
                            clearValue = GPUColor(
                                clearR.toDouble(),
                                clearG.toDouble(),
                                clearB.toDouble(),
                                clearA.toDouble(),
                            ),
                            view = textureView,
                            loadOp = LoadOp.Clear,
                            storeOp = StoreOp.Store,
                        ),
                    ),
                ),
            )
            return handles.insert(ResourceKind.RenderPassEncoder, pass)
        }
    }

    override fun renderPassSetPipeline(pass: GpuHandle, pipeline: GpuHandle) {
        synchronized(gpuLock) {
            val renderPass = handles.get<GPURenderPassEncoder>(pass, ResourceKind.RenderPassEncoder)
            val renderPipeline = handles.get<GPURenderPipeline>(pipeline, ResourceKind.RenderPipeline)
            renderPass.setPipeline(renderPipeline)
        }
    }

    override fun renderPassSetVertexBuffer(
        pass: GpuHandle,
        slot: Int,
        buffer: GpuHandle,
        offset: Long,
        size: Long,
    ) {
        synchronized(gpuLock) {
            val renderPass = handles.get<GPURenderPassEncoder>(pass, ResourceKind.RenderPassEncoder)
            val gpuBuffer = handles.get<GPUBuffer>(buffer, ResourceKind.Buffer)
            renderPass.setVertexBuffer(slot, gpuBuffer, offset, size)
        }
    }

    override fun renderPassDraw(
        pass: GpuHandle,
        vertexCount: Int,
        instanceCount: Int,
        firstVertex: Int,
        firstInstance: Int,
    ) {
        synchronized(gpuLock) {
            val renderPass = handles.get<GPURenderPassEncoder>(pass, ResourceKind.RenderPassEncoder)
            renderPass.draw(vertexCount, instanceCount, firstVertex, firstInstance)
        }
    }

    override fun renderPassEnd(pass: GpuHandle) {
        synchronized(gpuLock) {
            val renderPass = handles.get<GPURenderPassEncoder>(pass, ResourceKind.RenderPassEncoder)
            renderPass.end()
            dropLocked(pass, closeResource = true)
        }
    }

    override fun commandEncoderBeginComputePass(
        encoder: GpuHandle,
        descriptor: ComputePassDescriptor,
    ): GpuHandle {
        val commandEncoder = handles.get<GPUCommandEncoder>(encoder, ResourceKind.CommandEncoder)
        val pass = commandEncoder.beginComputePass(
            GPUComputePassDescriptor(label = descriptor.label),
        )
        return handles.insert(ResourceKind.ComputePassEncoder, pass)
    }

    override fun computePassSetPipeline(pass: GpuHandle, pipeline: GpuHandle) {
        val computePass = handles.get<GPUComputePassEncoder>(pass, ResourceKind.ComputePassEncoder)
        val computePipeline = handles.get<GPUComputePipeline>(pipeline, ResourceKind.ComputePipeline)
        computePass.setPipeline(computePipeline)
    }

    override fun computePassSetBindGroup(
        pass: GpuHandle,
        index: Int,
        bindGroup: GpuHandle,
        dynamicOffsets: IntArray,
    ) {
        val computePass = handles.get<GPUComputePassEncoder>(pass, ResourceKind.ComputePassEncoder)
        val group = handles.get<GPUBindGroup>(bindGroup, ResourceKind.BindGroup)
        computePass.setBindGroup(index, group, dynamicOffsets)
    }

    override fun computePassDispatchWorkgroups(
        pass: GpuHandle,
        workgroupCountX: Int,
        workgroupCountY: Int,
        workgroupCountZ: Int,
    ) {
        val computePass = handles.get<GPUComputePassEncoder>(pass, ResourceKind.ComputePassEncoder)
        computePass.dispatchWorkgroups(workgroupCountX, workgroupCountY, workgroupCountZ)
    }

    override fun computePassEnd(pass: GpuHandle) {
        val computePass = handles.get<GPUComputePassEncoder>(pass, ResourceKind.ComputePassEncoder)
        computePass.end()
        handles.drop(pass)
    }

    override fun commandEncoderCopyBufferToBuffer(
        encoder: GpuHandle,
        source: GpuHandle,
        sourceOffset: Long,
        destination: GpuHandle,
        destinationOffset: Long,
        size: Long,
    ) {
        val commandEncoder = handles.get<GPUCommandEncoder>(encoder, ResourceKind.CommandEncoder)
        val src = handles.get<GPUBuffer>(source, ResourceKind.Buffer)
        val dst = handles.get<GPUBuffer>(destination, ResourceKind.Buffer)
        commandEncoder.copyBufferToBuffer(src, sourceOffset, dst, destinationOffset, size)
    }

    override fun commandEncoderFinish(encoder: GpuHandle): GpuHandle {
        synchronized(gpuLock) {
            val commandEncoder = handles.get<GPUCommandEncoder>(encoder, ResourceKind.CommandEncoder)
            val commandBuffer = commandEncoder.finish()
            dropLocked(encoder, closeResource = true)
            return handles.insert(ResourceKind.CommandBuffer, commandBuffer)
        }
    }

    override fun queueWriteBuffer(
        queue: GpuHandle,
        buffer: GpuHandle,
        bufferOffset: Long,
        data: ByteArray,
    ) {
        val gpuQueue = handles.get<GPUQueue>(queue, ResourceKind.Queue)
        val gpuBuffer = handles.get<GPUBuffer>(buffer, ResourceKind.Buffer)
        val byteBuffer = ByteBuffer.allocateDirect(data.size).order(ByteOrder.nativeOrder())
        byteBuffer.put(data)
        byteBuffer.flip()
        gpuQueue.writeBuffer(gpuBuffer, bufferOffset, byteBuffer)
    }

    override fun queueSubmit(queue: GpuHandle, commandBuffers: List<GpuHandle>) {
        synchronized(gpuLock) {
            val gpuQueue = handles.get<GPUQueue>(queue, ResourceKind.Queue)
            val buffers = commandBuffers.map {
                handles.get<GPUCommandBuffer>(it, ResourceKind.CommandBuffer)
            }.toTypedArray()
            gpuQueue.submit(buffers)
        }
    }

    override fun bufferMapAsync(buffer: GpuHandle, mode: Int, offset: Long, size: Long) {
        val gpuBuffer = handles.get<GPUBuffer>(buffer, ResourceKind.Buffer)
        awaitRequest<Unit>("bufferMapAsync") { callback ->
            gpuBuffer.mapAsync(mode, offset, size, callbackExecutor, callback)
        }
    }

    override fun bufferGetMappedRange(buffer: GpuHandle, offset: Long, size: Long): ByteArray {
        val gpuBuffer = handles.get<GPUBuffer>(buffer, ResourceKind.Buffer)
        val mapped = gpuBuffer.getConstMappedRange(offset, size)
        val out = ByteArray(size.toInt())
        val duplicate = mapped.duplicate().order(ByteOrder.nativeOrder())
        duplicate.get(out)
        return out
    }

    override fun bufferUnmap(buffer: GpuHandle) {
        val gpuBuffer = handles.get<GPUBuffer>(buffer, ResourceKind.Buffer)
        gpuBuffer.unmap()
    }

    override fun drop(handle: GpuHandle) {
        drop(handle, closeResource = true)
    }

    /**
     * @param closeResource when false, only remove the handle table entry (abort paths before
     * present). After [surfacePresent], prefer [releaseFrameResources] / closeResource=true so
     * Dawn returns the BLAST buffer (D5).
     */
    fun drop(handle: GpuHandle, closeResource: Boolean) {
        synchronized(gpuLock) {
            dropLocked(handle, closeResource)
        }
    }

    /** Caller must hold [gpuLock]. */
    private fun dropLocked(handle: GpuHandle, closeResource: Boolean) {
        val entry = handles.drop(handle)
        pipelineLayouts.remove(handle.raw)?.let { layout ->
            runCatching { layout.close() }
        }
        if (closeResource) {
            closeGpuResource(entry.resource)
        }
    }

    private fun closeGpuResource(resource: Any) {
        when (resource) {
            is GPUDevice -> {
                runCatching { resource.destroy() }
                runCatching { resource.close() }
            }
            is AutoCloseable -> runCatching { resource.close() }
        }
    }

    /** Caller must hold [gpuLock]. */
    private fun releaseFrameResourcesLocked() {
        // Order: views → textures (return swapchain buffers) → encoders/buffers.
        for (
            kind in listOf(
                ResourceKind.TextureView,
                ResourceKind.Texture,
                ResourceKind.CommandBuffer,
                ResourceKind.RenderPassEncoder,
                ResourceKind.ComputePassEncoder,
                ResourceKind.CommandEncoder,
            )
        ) {
            for (handle in handles.handlesOfKind(kind)) {
                runCatching { dropLocked(handle, closeResource = true) }
            }
        }
        runCatching { instance.processEvents() }
    }

    override fun releaseFrameResources() {
        synchronized(gpuLock) {
            releaseFrameResourcesLocked()
        }
    }

    override fun releaseSurfaces() {
        synchronized(gpuLock) {
            // Guest WIT destructors are not wired — per-frame Texture/View stay in the table
            // and can pin the Android swapchain so GPUSurface.close() never api_disconnects
            // (VK_ERROR_NATIVE_WINDOW_IN_USE_KHR for the next owner).
            releaseFrameResourcesLocked()
            for (handle in handles.handlesOfKind(ResourceKind.Surface)) {
                runCatching {
                    handles.get<GPUSurface>(handle, ResourceKind.Surface).unconfigure()
                }
                runCatching { dropLocked(handle, closeResource = true) }
            }
            runCatching { instance.processEvents() }
        }
    }

    override fun releaseAllGpuObjects() {
        synchronized(gpuLock) {
            for (handle in handles.handlesOfKind(ResourceKind.Surface)) {
                runCatching {
                    handles.get<GPUSurface>(handle, ResourceKind.Surface).unconfigure()
                }
            }
            val closeOrder = listOf(
                ResourceKind.TextureView,
                ResourceKind.Texture,
                ResourceKind.CommandBuffer,
                ResourceKind.RenderPassEncoder,
                ResourceKind.ComputePassEncoder,
                ResourceKind.CommandEncoder,
                ResourceKind.RenderPipeline,
                ResourceKind.ComputePipeline,
                ResourceKind.BindGroup,
                ResourceKind.BindGroupLayout,
                ResourceKind.ShaderModule,
                ResourceKind.Buffer,
                ResourceKind.Queue,
                ResourceKind.Surface,
                ResourceKind.Device,
                ResourceKind.Adapter,
            )
            for (kind in closeOrder) {
                for (handle in handles.handlesOfKind(kind)) {
                    runCatching { dropLocked(handle, closeResource = true) }
                }
            }
            for (kind in ResourceKind.entries) {
                for (handle in handles.handlesOfKind(kind)) {
                    runCatching { dropLocked(handle, closeResource = true) }
                }
            }
            handles.clear()
            pipelineLayouts.values.forEach { runCatching { it.close() } }
            pipelineLayouts.clear()
            runCatching { instance.processEvents() }
        }
    }

    /** Pump Dawn events once (call after surface teardown before another API connects). */
    fun flushEvents() {
        synchronized(gpuLock) {
            runCatching { instance.processEvents() }
        }
    }

    override fun close() {
        if (closed) return
        closed = true
        // Stop the processEvents pump before tearing down the instance — shutdownNow()
        // during an in-flight processEvents races Mali/Dawn and can SIGABRT (Scudo).
        eventPoller.shutdown()
        try {
            if (!eventPoller.awaitTermination(2, TimeUnit.SECONDS)) {
                eventPoller.shutdownNow()
                eventPoller.awaitTermination(1, TimeUnit.SECONDS)
            }
        } catch (_: InterruptedException) {
            eventPoller.shutdownNow()
            Thread.currentThread().interrupt()
        }
        synchronized(gpuLock) {
            // Must close GPU objects (esp. GPUSurface) — clear() alone leaks the ANativeWindow
            // connection and causes VK_ERROR_NATIVE_WINDOW_IN_USE_KHR for the next owner.
            // Reentrant note: use dropLocked (not drop) while holding gpuLock.
            for (handle in handles.handlesOfKind(ResourceKind.Surface)) {
                runCatching {
                    handles.get<GPUSurface>(handle, ResourceKind.Surface).unconfigure()
                }
            }
            val closeOrder = listOf(
                ResourceKind.TextureView,
                ResourceKind.Texture,
                ResourceKind.CommandBuffer,
                ResourceKind.RenderPassEncoder,
                ResourceKind.ComputePassEncoder,
                ResourceKind.CommandEncoder,
                ResourceKind.RenderPipeline,
                ResourceKind.ComputePipeline,
                ResourceKind.BindGroup,
                ResourceKind.BindGroupLayout,
                ResourceKind.ShaderModule,
                ResourceKind.Buffer,
                ResourceKind.Queue,
                ResourceKind.Surface,
                ResourceKind.Device,
                ResourceKind.Adapter,
            )
            for (kind in closeOrder) {
                for (handle in handles.handlesOfKind(kind)) {
                    runCatching { dropLocked(handle, closeResource = true) }
                }
            }
            // Any leftover kinds / failed drops: still close natives before abandoning the table.
            for (kind in ResourceKind.entries) {
                for (handle in handles.handlesOfKind(kind)) {
                    runCatching { dropLocked(handle, closeResource = true) }
                }
            }
            handles.clear()
            pipelineLayouts.values.forEach { runCatching { it.close() } }
            pipelineLayouts.clear()
            runCatching { instance.processEvents() }
            runCatching { instance.close() }
        }
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
        if (!latch.await(TIMEOUT_SEC, TimeUnit.SECONDS)) {
            throw HostException.Backend("$op timed out")
        }
        error.get()?.let { throw HostException.Backend("$op failed: ${it.message}", it) }
        @Suppress("UNCHECKED_CAST")
        return resultRef.get() as T
    }

    companion object {
        private const val POLL_MS = 5L
        private const val TIMEOUT_SEC = 30L

        /** Create a host bound to a fresh Dawn [GPUInstance]. */
        fun create(): DawnWasiWebGpuHost {
            initLibrary()
            val instance = GPU.createInstance()
            return DawnWasiWebGpuHost(instance)
        }
    }
}
