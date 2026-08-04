package io.github.fenriliuguang.wasi.webgpu.experimental.host

/**
 * L2 Host API — experimental Dawn host mapping for wasi:webgpu.
 *
 * Scope (P0): compute subset only. No Wasm runtime / ABI / Component Model dependency.
 * Callers may be Kotlin unit tests or a thin Android demo.
 *
 * Handles are opaque u32-style ids managed by the host implementation.
 */
interface WasiWebGpuHost : AutoCloseable {

    // --- Instance / Adapter / Device ---

    fun requestAdapter(options: RequestAdapterOptions = RequestAdapterOptions()): GpuHandle

    fun adapterRequestDevice(adapter: GpuHandle): GpuHandle

    fun deviceGetQueue(device: GpuHandle): GpuHandle

    // --- Resources ---

    fun deviceCreateBuffer(device: GpuHandle, descriptor: BufferDescriptor): GpuHandle

    fun deviceCreateShaderModule(device: GpuHandle, descriptor: ShaderModuleDescriptor): GpuHandle

    fun deviceCreateBindGroupLayout(
        device: GpuHandle,
        descriptor: BindGroupLayoutDescriptor,
    ): GpuHandle

    fun deviceCreateBindGroup(device: GpuHandle, descriptor: BindGroupDescriptor): GpuHandle

    fun deviceCreateComputePipeline(
        device: GpuHandle,
        descriptor: ComputePipelineDescriptor,
    ): GpuHandle

    fun deviceCreateCommandEncoder(
        device: GpuHandle,
        descriptor: CommandEncoderDescriptor = CommandEncoderDescriptor(),
    ): GpuHandle

    // --- Command encoding (compute) ---

    fun commandEncoderBeginComputePass(
        encoder: GpuHandle,
        descriptor: ComputePassDescriptor = ComputePassDescriptor(),
    ): GpuHandle

    fun computePassSetPipeline(pass: GpuHandle, pipeline: GpuHandle)

    fun computePassSetBindGroup(
        pass: GpuHandle,
        index: Int,
        bindGroup: GpuHandle,
        dynamicOffsets: IntArray = intArrayOf(),
    )

    fun computePassDispatchWorkgroups(
        pass: GpuHandle,
        workgroupCountX: Int,
        workgroupCountY: Int = 1,
        workgroupCountZ: Int = 1,
    )

    fun computePassEnd(pass: GpuHandle)

    fun commandEncoderCopyBufferToBuffer(
        encoder: GpuHandle,
        source: GpuHandle,
        sourceOffset: Long,
        destination: GpuHandle,
        destinationOffset: Long,
        size: Long,
    )

    fun commandEncoderFinish(encoder: GpuHandle): GpuHandle

    // --- Queue / buffer IO ---

    fun queueWriteBuffer(
        queue: GpuHandle,
        buffer: GpuHandle,
        bufferOffset: Long,
        data: ByteArray,
    )

    fun queueSubmit(queue: GpuHandle, commandBuffers: List<GpuHandle>)

    fun bufferMapAsync(buffer: GpuHandle, mode: Int, offset: Long, size: Long)

    fun bufferGetMappedRange(buffer: GpuHandle, offset: Long, size: Long): ByteArray

    fun bufferUnmap(buffer: GpuHandle)

    // --- Lifetime ---

    fun drop(handle: GpuHandle)

    override fun close()
}
