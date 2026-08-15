package io.github.fenriliuguang.wasi.webgpu.experimental.abi

import io.github.fenriliuguang.wasi.webgpu.experimental.host.BindGroupDescriptor
import io.github.fenriliuguang.wasi.webgpu.experimental.host.BindGroupEntry
import io.github.fenriliuguang.wasi.webgpu.experimental.host.BindGroupLayoutDescriptor
import io.github.fenriliuguang.wasi.webgpu.experimental.host.BindGroupLayoutEntry
import io.github.fenriliuguang.wasi.webgpu.experimental.host.BufferBinding
import io.github.fenriliuguang.wasi.webgpu.experimental.host.BufferBindingLayout
import io.github.fenriliuguang.wasi.webgpu.experimental.host.BufferBindingType
import io.github.fenriliuguang.wasi.webgpu.experimental.host.BufferDescriptor
import io.github.fenriliuguang.wasi.webgpu.experimental.host.Color
import io.github.fenriliuguang.wasi.webgpu.experimental.host.ComputePipelineDescriptor
import io.github.fenriliuguang.wasi.webgpu.experimental.host.Extent3D
import io.github.fenriliuguang.wasi.webgpu.experimental.host.GpuHandle
import io.github.fenriliuguang.wasi.webgpu.experimental.host.GpuMapMode
import io.github.fenriliuguang.wasi.webgpu.experimental.host.GpuShaderStage
import io.github.fenriliuguang.wasi.webgpu.experimental.host.HostException
import io.github.fenriliuguang.wasi.webgpu.experimental.host.PipelineLayoutDescriptor
import io.github.fenriliuguang.wasi.webgpu.experimental.host.ProgrammableStage
import io.github.fenriliuguang.wasi.webgpu.experimental.host.RenderPassColorAttachment
import io.github.fenriliuguang.wasi.webgpu.experimental.host.RenderPassDepthStencilAttachment
import io.github.fenriliuguang.wasi.webgpu.experimental.host.RenderPassDescriptor
import io.github.fenriliuguang.wasi.webgpu.experimental.host.ShaderModuleDescriptor
import io.github.fenriliuguang.wasi.webgpu.experimental.host.SurfaceTextureStatus
import io.github.fenriliuguang.wasi.webgpu.experimental.host.TextureDescriptor
import io.github.fenriliuguang.wasi.webgpu.experimental.host.WasiWebGpuHost

/**
 * Thin L1→L2 adapter: flattened core-wasm imports forward to [WasiWebGpuHost].
 *
 * Descriptor packing is intentionally minimal (vector-add / cube-shaped helpers).
 * Surface/render helpers mirror the CM cube L2 path; primary device acceptance stays CM cube.
 */
class AbiMvpHostBindings(
    private val host: WasiWebGpuHost,
    private val memory: () -> GuestMemory,
) {
    /** view.raw → texture.raw for the current (or last) acquired swapchain frame. */
    private val frameTextureByView = LinkedHashMap<Int, Int>()

    /** Live View↔Texture pairs awaiting present / next acquire (test / diagnostics). */
    fun trackedFramePairCount(): Int = frameTextureByView.size

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

    /**
     * Flat abi-mvp still passes a **BindGroupLayout** handle as [layout] (vector-add Guest).
     * L2 [ComputePipelineDescriptor.layout] is a PipelineLayout (slice D), so wrap BGL → PL here.
     */
    fun deviceCreateComputePipeline(
        device: Int,
        layout: Int,
        shader: Int,
        entryPtr: Int,
        entryLen: Int,
    ): Int {
        val entry = memory().readUtf8(entryPtr, entryLen)
        val pipelineLayout = host.deviceCreatePipelineLayout(
            GpuHandle(device),
            PipelineLayoutDescriptor(bindGroupLayouts = listOf(GpuHandle(layout))),
        )
        return host.deviceCreateComputePipeline(
            GpuHandle(device),
            ComputePipelineDescriptor(
                layout = pipelineLayout,
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

    /** Formal list submit. Track B should use this instead of [queueSubmit1]. */
    fun queueSubmit(queue: Int, commandBuffers: List<Int>) {
        host.queueSubmit(GpuHandle(queue), commandBuffers.map { GpuHandle(it) })
    }

    /** @deprecated Prefer [queueSubmit] (Track B formal surface). */
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

    fun createSurfaceFromNativeWindow(windowHandle: Long): Int =
        host.instanceCreateSurfaceFromAndroidNativeWindow(windowHandle).raw

    fun surfaceConfigure(surface: Int, device: Int, adapter: Int, width: Int, height: Int): Int =
        host.surfaceConfigure(
            GpuHandle(surface),
            GpuHandle(device),
            GpuHandle(adapter),
            width,
            height,
        )

    fun surfaceGetCurrentTextureView(surface: Int): Int {
        releaseTrackedFrameTextures()
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
        val view = host.textureCreateView(texture)
        frameTextureByView[view.raw] = texture.raw
        return view.raw
    }

    fun surfacePresent(surface: Int) {
        host.surfacePresent(GpuHandle(surface))
        releaseTrackedFrameTextures()
        host.releaseFrameResources()
    }

    fun surfaceUnconfigure(surface: Int) {
        releaseTrackedFrameTextures()
        host.surfaceUnconfigure(GpuHandle(surface))
    }

    fun deviceCreateTexture2d(device: Int, width: Int, height: Int, format: Int, usage: Int): Int =
        host.deviceCreateTexture(
            GpuHandle(device),
            TextureDescriptor(
                size = Extent3D(width = width, height = height),
                format = format,
                usage = usage,
            ),
        ).raw

    fun textureCreateView(texture: Int): Int =
        host.textureCreateView(GpuHandle(texture)).raw

    /** Formal begin-render-pass. Track B should use this instead of [commandEncoderBeginRenderPassClear]. */
    fun commandEncoderBeginRenderPass(encoder: Int, descriptor: RenderPassDescriptor): Int =
        host.commandEncoderBeginRenderPass(GpuHandle(encoder), descriptor).raw

    fun queueWriteTexture(
        queue: Int,
        texture: Int,
        ptr: Int,
        len: Int,
        width: Int,
        height: Int,
        bytesPerRow: Int,
    ) {
        val data = memory().readBytes(ptr, len)
        host.queueWriteTexture(
            GpuHandle(queue),
            GpuHandle(texture),
            data,
            width,
            height,
            bytesPerRow,
        )
    }

    fun deviceCreateRenderPipelineTriangle(device: Int, shader: Int, format: Int): Int =
        host.deviceCreateRenderPipelineTriangle(
            GpuHandle(device),
            GpuHandle(shader),
            format,
        ).raw

    /** @deprecated Prefer [commandEncoderBeginRenderPass] (Track B formal surface). */
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

    fun commandEncoderBeginRenderPassColorDepth(
        encoder: Int,
        colorView: Int,
        depthView: Int,
        r: Float,
        g: Float,
        b: Float,
        a: Float,
    ): Int =
        host.commandEncoderBeginRenderPass(
            GpuHandle(encoder),
            RenderPassDescriptor(
                colorAttachments = listOf(
                    RenderPassColorAttachment(
                        view = GpuHandle(colorView),
                        clearValue = Color(r.toDouble(), g.toDouble(), b.toDouble(), a.toDouble()),
                    ),
                ),
                depthStencilAttachment = RenderPassDepthStencilAttachment(
                    view = GpuHandle(depthView),
                ),
            ),
        ).raw

    fun renderPassSetPipeline(pass: Int, pipeline: Int) {
        host.renderPassSetPipeline(GpuHandle(pass), GpuHandle(pipeline))
    }

    fun renderPassSetBindGroup(pass: Int, index: Int, bindGroup: Int) {
        host.renderPassSetBindGroup(GpuHandle(pass), index, GpuHandle(bindGroup))
    }

    fun renderPassSetVertexBuffer(pass: Int, slot: Int, buffer: Int, offset: Int, size: Int) {
        host.renderPassSetVertexBuffer(
            GpuHandle(pass),
            slot,
            GpuHandle(buffer),
            offset.toLong(),
            size.toLong(),
        )
    }

    fun renderPassDraw(pass: Int, vertexCount: Int) {
        host.renderPassDraw(GpuHandle(pass), vertexCount)
    }

    fun renderPassEnd(pass: Int) {
        host.renderPassEnd(GpuHandle(pass))
    }

    private fun releaseTrackedFrameTextures() {
        if (frameTextureByView.isEmpty()) return
        val pairs = frameTextureByView.entries.toList()
        frameTextureByView.clear()
        for ((viewRaw, textureRaw) in pairs) {
            host.tryDrop(GpuHandle(viewRaw))
            host.tryDrop(GpuHandle(textureRaw))
        }
    }

    private fun storageEntry(binding: Int, type: BufferBindingType) = BindGroupLayoutEntry(
        binding = binding,
        visibility = GpuShaderStage.COMPUTE,
        buffer = BufferBindingLayout(type = type, minBindingSize = 4),
    )
}
