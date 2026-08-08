package io.github.fenriliuguang.wasi.webgpu.experimental.abicm

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
import io.github.fenriliuguang.wasi.webgpu.experimental.host.GpuShaderStage
import io.github.fenriliuguang.wasi.webgpu.experimental.host.HostException
import io.github.fenriliuguang.wasi.webgpu.experimental.host.ProgrammableStage
import io.github.fenriliuguang.wasi.webgpu.experimental.host.ShaderModuleDescriptor
import io.github.fenriliuguang.wasi.webgpu.experimental.host.SurfaceTextureStatus
import io.github.fenriliuguang.wasi.webgpu.experimental.host.WasiWebGpuHost

/**
 * L1→L2 adapter for experimental CM host imports (typed lists/strings).
 *
 * WIT resource reps arrive as u32 and map 1:1 to L2 [GpuHandle.raw].
 * No Guest linear-memory dependency — buffer bytes arrive as [ByteArray].
 */
class AbiCmHostBindings(
    private val host: WasiWebGpuHost,
) {

    fun requestAdapter(): Int = host.requestAdapter().raw

    fun createSurfaceFromNativeWindow(windowHandle: Long): Int =
        host.instanceCreateSurfaceFromAndroidNativeWindow(windowHandle).raw

    fun adapterRequestDevice(adapter: Int): Int =
        host.adapterRequestDevice(GpuHandle(adapter)).raw

    fun deviceGetQueue(device: Int): Int = host.deviceGetQueue(GpuHandle(device)).raw

    fun deviceCreateBuffer(
        device: Int,
        size: Long,
        usage: Int,
        mappedAtCreation: Boolean = false,
        label: String? = null,
    ): Int =
        host.deviceCreateBuffer(
            GpuHandle(device),
            BufferDescriptor(
                size = size,
                usage = usage,
                mappedAtCreation = mappedAtCreation,
                label = label,
            ),
        ).raw

    fun queueWriteBuffer(queue: Int, buffer: Int, offset: Long, data: ByteArray) {
        host.queueWriteBuffer(GpuHandle(queue), GpuHandle(buffer), offset, data)
    }

    fun deviceCreateShaderModule(device: Int, code: String): Int =
        host.deviceCreateShaderModule(
            GpuHandle(device),
            ShaderModuleDescriptor(code = code),
        ).raw

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
        entryPoint: String,
    ): Int =
        host.deviceCreateComputePipeline(
            GpuHandle(device),
            ComputePipelineDescriptor(
                layout = GpuHandle(layout),
                compute = ProgrammableStage(module = GpuHandle(shader), entryPoint = entryPoint),
            ),
        ).raw

    fun deviceCreateRenderPipelineTriangle(device: Int, shader: Int, format: Int): Int =
        host.deviceCreateRenderPipelineTriangle(
            GpuHandle(device),
            GpuHandle(shader),
            format,
        ).raw

    fun deviceCreateCommandEncoder(device: Int): Int =
        host.deviceCreateCommandEncoder(GpuHandle(device)).raw

    fun surfaceConfigure(surface: Int, device: Int, adapter: Int, width: Int, height: Int): Int =
        host.surfaceConfigure(
            GpuHandle(surface),
            GpuHandle(device),
            GpuHandle(adapter),
            width,
            height,
        )

    fun surfaceGetCurrentTextureView(surface: Int): Int {
        // Previous frame leftovers (Guest WIT drop is not wired) pin BLAST buffers.
        host.releaseFrameResources()
        val result = host.surfaceGetCurrentTexture(GpuHandle(surface))
        if (
            result.status != SurfaceTextureStatus.SuccessOptimal &&
            result.status != SurfaceTextureStatus.SuccessSuboptimal
        ) {
            throw HostException.Validation("surface get-current-texture status=${result.status}")
        }
        val texture = result.texture
            ?: throw HostException.Validation("surface get-current-texture returned null texture")
        // Texture stays in the Host table until present/releaseFrameResources — Guest only
        // receives the view rep and never sees the texture handle.
        return host.textureCreateView(texture).raw
    }

    fun surfacePresent(surface: Int) {
        host.surfacePresent(GpuHandle(surface))
        // Return swapchain buffers to BLAST (D5); destructors are not wired from Guest.
        host.releaseFrameResources()
    }

    fun surfaceUnconfigure(surface: Int) {
        host.surfaceUnconfigure(GpuHandle(surface))
    }

    fun commandEncoderBeginComputePass(encoder: Int): Int =
        host.commandEncoderBeginComputePass(GpuHandle(encoder)).raw

    fun commandEncoderBeginRenderPassClear(
        encoder: Int,
        view: Int,
        r: Float,
        g: Float,
        b: Float,
        a: Float,
    ): Int =
        host.commandEncoderBeginRenderPassClear(
            GpuHandle(encoder),
            GpuHandle(view),
            r,
            g,
            b,
            a,
        ).raw

    fun computePassSetPipeline(pass: Int, pipeline: Int) {
        host.computePassSetPipeline(GpuHandle(pass), GpuHandle(pipeline))
    }

    fun computePassSetBindGroup(pass: Int, index: Int, bindGroup: Int) {
        host.computePassSetBindGroup(GpuHandle(pass), index, GpuHandle(bindGroup))
    }

    fun computePassDispatchWorkgroups(pass: Int, x: Int, y: Int, z: Int) {
        host.computePassDispatchWorkgroups(GpuHandle(pass), x, y, z)
    }

    fun computePassEnd(pass: Int) {
        host.computePassEnd(GpuHandle(pass))
    }

    fun renderPassSetPipeline(pass: Int, pipeline: Int) {
        host.renderPassSetPipeline(GpuHandle(pass), GpuHandle(pipeline))
    }

    fun renderPassDraw(pass: Int, vertexCount: Int) {
        host.renderPassDraw(GpuHandle(pass), vertexCount)
    }

    fun renderPassEnd(pass: Int) {
        host.renderPassEnd(GpuHandle(pass))
    }

    fun commandEncoderCopyBufferToBuffer(
        encoder: Int,
        source: Int,
        sourceOffset: Long,
        destination: Int,
        destinationOffset: Long,
        size: Long,
    ) {
        host.commandEncoderCopyBufferToBuffer(
            GpuHandle(encoder),
            GpuHandle(source),
            sourceOffset,
            GpuHandle(destination),
            destinationOffset,
            size,
        )
    }

    fun commandEncoderFinish(encoder: Int): Int =
        host.commandEncoderFinish(GpuHandle(encoder)).raw

    fun queueSubmit1(queue: Int, commandBuffer: Int) {
        host.queueSubmit(GpuHandle(queue), listOf(GpuHandle(commandBuffer)))
    }

    fun bufferMapAsync(buffer: Int, mode: Int, offset: Long, size: Long) {
        host.bufferMapAsync(GpuHandle(buffer), mode, offset, size)
    }

    fun bufferGetMappedRange(buffer: Int, offset: Long, size: Long): ByteArray =
        host.bufferGetMappedRange(GpuHandle(buffer), offset, size)

    fun bufferUnmap(buffer: Int) {
        host.bufferUnmap(GpuHandle(buffer))
    }

    private fun storageEntry(binding: Int, type: BufferBindingType) = BindGroupLayoutEntry(
        binding = binding,
        visibility = GpuShaderStage.COMPUTE,
        buffer = BufferBindingLayout(type = type, minBindingSize = 4),
    )
}
