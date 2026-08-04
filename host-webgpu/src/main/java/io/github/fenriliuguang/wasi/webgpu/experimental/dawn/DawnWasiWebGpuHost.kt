package io.github.fenriliuguang.wasi.webgpu.experimental.dawn

import androidx.webgpu.BackendType
import androidx.webgpu.BufferBindingType as DawnBufferBindingType
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
import androidx.webgpu.GPUInstance
import androidx.webgpu.GPUPipelineLayout
import androidx.webgpu.GPUPipelineLayoutDescriptor
import androidx.webgpu.GPUQueue
import androidx.webgpu.GPURequestAdapterOptions
import androidx.webgpu.GPURequestCallback
import androidx.webgpu.GPUShaderModule
import androidx.webgpu.GPUShaderModuleDescriptor
import androidx.webgpu.GPUShaderSourceWGSL
import androidx.webgpu.PowerPreference as DawnPowerPreference
import androidx.webgpu.UncapturedErrorCallback
import androidx.webgpu.helper.initLibrary
import io.github.fenriliuguang.wasi.webgpu.experimental.host.BindGroupDescriptor
import io.github.fenriliuguang.wasi.webgpu.experimental.host.BindGroupLayoutDescriptor
import io.github.fenriliuguang.wasi.webgpu.experimental.host.BufferBindingType
import io.github.fenriliuguang.wasi.webgpu.experimental.host.BufferDescriptor
import io.github.fenriliuguang.wasi.webgpu.experimental.host.CommandEncoderDescriptor
import io.github.fenriliuguang.wasi.webgpu.experimental.host.ComputePassDescriptor
import io.github.fenriliuguang.wasi.webgpu.experimental.host.ComputePipelineDescriptor
import io.github.fenriliuguang.wasi.webgpu.experimental.host.GpuHandle
import io.github.fenriliuguang.wasi.webgpu.experimental.host.HandleTable
import io.github.fenriliuguang.wasi.webgpu.experimental.host.HostException
import io.github.fenriliuguang.wasi.webgpu.experimental.host.PowerPreference
import io.github.fenriliuguang.wasi.webgpu.experimental.host.RequestAdapterOptions
import io.github.fenriliuguang.wasi.webgpu.experimental.host.ResourceKind
import io.github.fenriliuguang.wasi.webgpu.experimental.host.ShaderModuleDescriptor
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
 * P0 methods are synchronous wrappers around Dawn async entry points and poll
 * [GPUInstance.processEvents] until callbacks fire.
 */
class DawnWasiWebGpuHost private constructor(
    private val instance: GPUInstance,
) : WasiWebGpuHost {

    private val handles = HandleTable()
    private val callbackExecutor: Executor = Executor(Runnable::run)
    private val eventPoller = Executors.newSingleThreadExecutor()
    private val pipelineLayouts = HashMap<Int, GPUPipelineLayout>()
    @Volatile private var closed = false

    init {
        eventPoller.execute {
            while (!closed) {
                runCatching { instance.processEvents() }
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
                // P0: surface via Backend on next API call if needed; log-style throw avoided on callback thread.
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
        val gpuDevice = handles.get<GPUDevice>(device, ResourceKind.Device)
        val encoder = gpuDevice.createCommandEncoder(
            GPUCommandEncoderDescriptor(label = descriptor.label),
        )
        return handles.insert(ResourceKind.CommandEncoder, encoder)
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
        val commandEncoder = handles.get<GPUCommandEncoder>(encoder, ResourceKind.CommandEncoder)
        val commandBuffer = commandEncoder.finish()
        handles.drop(encoder)
        return handles.insert(ResourceKind.CommandBuffer, commandBuffer)
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
        val gpuQueue = handles.get<GPUQueue>(queue, ResourceKind.Queue)
        val buffers = commandBuffers.map {
            handles.get<GPUCommandBuffer>(it, ResourceKind.CommandBuffer)
        }.toTypedArray()
        gpuQueue.submit(buffers)
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
        val entry = handles.drop(handle)
        pipelineLayouts.remove(handle.raw)
        val resource = entry.resource
        if (resource is AutoCloseable) {
            runCatching { resource.close() }
        }
    }

    override fun close() {
        closed = true
        eventPoller.shutdownNow()
        handles.clear()
        pipelineLayouts.clear()
        runCatching { instance.close() }
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

        /** Create a host bound to a fresh Dawn [GPUInstance] (no Surface). */
        fun create(): DawnWasiWebGpuHost {
            initLibrary()
            val instance = GPU.createInstance()
            return DawnWasiWebGpuHost(instance)
        }
    }
}
