package io.github.fenriliuguang.wasi.webgpu.experimental.abi

import io.github.fenriliuguang.wasi.webgpu.experimental.host.BindGroupDescriptor
import io.github.fenriliuguang.wasi.webgpu.experimental.host.BindGroupEntry
import io.github.fenriliuguang.wasi.webgpu.experimental.host.BindGroupLayoutDescriptor
import io.github.fenriliuguang.wasi.webgpu.experimental.host.BindGroupLayoutEntry
import io.github.fenriliuguang.wasi.webgpu.experimental.host.BufferBinding
import io.github.fenriliuguang.wasi.webgpu.experimental.host.BufferBindingLayout
import io.github.fenriliuguang.wasi.webgpu.experimental.host.BufferBindingType
import io.github.fenriliuguang.wasi.webgpu.experimental.host.BufferDescriptor
import io.github.fenriliuguang.wasi.webgpu.experimental.host.ComputePipelineDescriptor
import io.github.fenriliuguang.wasi.webgpu.experimental.host.GpuHandle
import io.github.fenriliuguang.wasi.webgpu.experimental.host.GpuMapMode
import io.github.fenriliuguang.wasi.webgpu.experimental.host.GpuShaderStage
import io.github.fenriliuguang.wasi.webgpu.experimental.host.ProgrammableStage
import io.github.fenriliuguang.wasi.webgpu.experimental.host.ShaderModuleDescriptor
import io.github.fenriliuguang.wasi.webgpu.experimental.host.WasiWebGpuHost

/**
 * Thin L1→L2 adapter: flattened core-wasm imports forward to [WasiWebGpuHost].
 *
 * Descriptor packing is intentionally minimal (vector-add shaped helpers).
 */
class AbiMvpHostBindings(
    private val host: WasiWebGpuHost,
    private val memory: () -> GuestMemory,
) {

    fun requestAdapter(): Int = host.requestAdapter().raw

    fun adapterRequestDevice(adapter: Int): Int =
        host.adapterRequestDevice(GpuHandle(adapter)).raw

    fun deviceGetQueue(device: Int): Int = host.deviceGetQueue(GpuHandle(device)).raw

    fun deviceCreateBuffer(device: Int, size: Int, usage: Int): Int =
        host.deviceCreateBuffer(
            GpuHandle(device),
            BufferDescriptor(size = size.toLong(), usage = usage),
        ).raw

    fun queueWriteBuffer(queue: Int, buffer: Int, offset: Int, ptr: Int, len: Int) {
        val data = memory().readBytes(ptr, len)
        host.queueWriteBuffer(GpuHandle(queue), GpuHandle(buffer), offset.toLong(), data)
    }

    fun deviceCreateShaderModule(device: Int, ptr: Int, len: Int): Int {
        val code = memory().readUtf8(ptr, len)
        return host.deviceCreateShaderModule(
            GpuHandle(device),
            ShaderModuleDescriptor(code = code),
        ).raw
    }

    fun deviceCreateBindGroupLayoutStorage3(device: Int): Int {
        val layout = BindGroupLayoutDescriptor(
            entries = listOf(
                storageEntry(0, BufferBindingType.ReadOnlyStorage),
                storageEntry(1, BufferBindingType.ReadOnlyStorage),
                storageEntry(2, BufferBindingType.Storage),
            ),
        )
        return host.deviceCreateBindGroupLayout(GpuHandle(device), layout).raw
    }

    fun deviceCreateBindGroup3(device: Int, layout: Int, b0: Int, b1: Int, b2: Int): Int =
        host.deviceCreateBindGroup(
            GpuHandle(device),
            BindGroupDescriptor(
                layout = GpuHandle(layout),
                entries = listOf(
                    BindGroupEntry(0, BufferBinding(GpuHandle(b0))),
                    BindGroupEntry(1, BufferBinding(GpuHandle(b1))),
                    BindGroupEntry(2, BufferBinding(GpuHandle(b2))),
                ),
            ),
        ).raw

    fun deviceCreateComputePipeline(
        device: Int,
        layout: Int,
        shader: Int,
        entryPtr: Int,
        entryLen: Int,
    ): Int {
        val entry = memory().readUtf8(entryPtr, entryLen)
        return host.deviceCreateComputePipeline(
            GpuHandle(device),
            ComputePipelineDescriptor(
                layout = GpuHandle(layout),
                compute = ProgrammableStage(module = GpuHandle(shader), entryPoint = entry),
            ),
        ).raw
    }

    fun deviceCreateCommandEncoder(device: Int): Int =
        host.deviceCreateCommandEncoder(GpuHandle(device)).raw

    fun commandEncoderBeginComputePass(encoder: Int): Int =
        host.commandEncoderBeginComputePass(GpuHandle(encoder)).raw

    fun computePassSetPipeline(pass: Int, pipeline: Int) {
        host.computePassSetPipeline(GpuHandle(pass), GpuHandle(pipeline))
    }

    fun computePassSetBindGroup(pass: Int, index: Int, bindGroup: Int) {
        host.computePassSetBindGroup(GpuHandle(pass), index, GpuHandle(bindGroup))
    }

    fun computePassDispatch(pass: Int, x: Int, y: Int, z: Int) {
        host.computePassDispatchWorkgroups(GpuHandle(pass), x, y, z)
    }

    fun computePassEnd(pass: Int) {
        host.computePassEnd(GpuHandle(pass))
    }

    fun commandEncoderCopyBufferToBuffer(
        encoder: Int,
        source: Int,
        sourceOffset: Int,
        destination: Int,
        destinationOffset: Int,
        size: Int,
    ) {
        host.commandEncoderCopyBufferToBuffer(
            GpuHandle(encoder),
            GpuHandle(source),
            sourceOffset.toLong(),
            GpuHandle(destination),
            destinationOffset.toLong(),
            size.toLong(),
        )
    }

    fun commandEncoderFinish(encoder: Int): Int =
        host.commandEncoderFinish(GpuHandle(encoder)).raw

    fun queueSubmit1(queue: Int, commandBuffer: Int) {
        host.queueSubmit(GpuHandle(queue), listOf(GpuHandle(commandBuffer)))
    }

    fun bufferMapRead(buffer: Int, offset: Int, size: Int) {
        host.bufferMapAsync(GpuHandle(buffer), GpuMapMode.READ, offset.toLong(), size.toLong())
    }

    fun bufferGetMappedRange(buffer: Int, offset: Int, size: Int, dstPtr: Int) {
        val data = host.bufferGetMappedRange(GpuHandle(buffer), offset.toLong(), size.toLong())
        memory().writeBytes(dstPtr, data)
    }

    fun bufferUnmap(buffer: Int) {
        host.bufferUnmap(GpuHandle(buffer))
    }

    private fun storageEntry(binding: Int, type: BufferBindingType) = BindGroupLayoutEntry(
        binding = binding,
        visibility = GpuShaderStage.COMPUTE,
        buffer = BufferBindingLayout(type = type, minBindingSize = 4),
    )
}
