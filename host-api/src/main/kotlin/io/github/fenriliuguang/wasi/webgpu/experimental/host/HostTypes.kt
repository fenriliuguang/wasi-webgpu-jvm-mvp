package io.github.fenriliuguang.wasi.webgpu.experimental.host

/**
 * L2 descriptor / flag types for the compute + minimal surface/render subset.
 *
 * Names follow wasi:webgpu WIT; values are chosen for Kotlin ergonomics.
 * This is NOT a full kotlin-webgpu client shim.
 */

object GpuBufferUsage {
    const val MAP_READ: Int = 1 shl 0
    const val MAP_WRITE: Int = 1 shl 1
    const val COPY_SRC: Int = 1 shl 2
    const val COPY_DST: Int = 1 shl 3
    const val INDEX: Int = 1 shl 4
    const val VERTEX: Int = 1 shl 5
    const val UNIFORM: Int = 1 shl 6
    const val STORAGE: Int = 1 shl 7
    const val INDIRECT: Int = 1 shl 8
    const val QUERY_RESOLVE: Int = 1 shl 9
}

object GpuShaderStage {
    const val VERTEX: Int = 1 shl 0
    const val FRAGMENT: Int = 1 shl 1
    const val COMPUTE: Int = 1 shl 2
}

object GpuMapMode {
    const val READ: Int = 1 shl 0
    const val WRITE: Int = 1 shl 1
}

data class RequestAdapterOptions(
    val powerPreference: PowerPreference = PowerPreference.Undefined,
    val forceFallbackAdapter: Boolean = false,
)

enum class PowerPreference {
    Undefined,
    LowPower,
    HighPerformance,
}

data class BufferDescriptor(
    val size: Long,
    val usage: Int,
    val mappedAtCreation: Boolean = false,
    val label: String? = null,
)

/**
 * WebGPU / Dawn GPUVertexFormat numeric values (`androidx.webgpu.VertexFormat`).
 * Only formats needed by the experimental render subset are listed.
 */
object GpuVertexFormat {
    /** `androidx.webgpu.VertexFormat.Float32x2` */
    const val FLOAT32X2: Int = 0x0000001d
}

/**
 * WebGPU / Dawn GPUVertexStepMode numeric values (`androidx.webgpu.VertexStepMode`).
 */
object GpuVertexStepMode {
    /** `androidx.webgpu.VertexStepMode.Vertex` */
    const val VERTEX: Int = 0x00000001
    /** `androidx.webgpu.VertexStepMode.Instance` */
    const val INSTANCE: Int = 0x00000002
}

data class VertexAttribute(
    val format: Int,
    val offset: Long,
    val shaderLocation: Int,
)

data class VertexBufferLayout(
    val arrayStride: Long,
    val stepMode: Int = GpuVertexStepMode.VERTEX,
    val attributes: List<VertexAttribute>,
)

data class ShaderModuleDescriptor(
    val code: String,
    val label: String? = null,
)

enum class BufferBindingType {
    Uniform,
    Storage,
    ReadOnlyStorage,
}

data class BufferBindingLayout(
    val type: BufferBindingType = BufferBindingType.Uniform,
    val hasDynamicOffset: Boolean = false,
    val minBindingSize: Long = 0,
)

data class BindGroupLayoutEntry(
    val binding: Int,
    val visibility: Int,
    val buffer: BufferBindingLayout? = null,
)

data class BindGroupLayoutDescriptor(
    val entries: List<BindGroupLayoutEntry>,
    val label: String? = null,
)

data class BufferBinding(
    val buffer: GpuHandle,
    val offset: Long = 0,
    val size: Long? = null,
)

data class BindGroupEntry(
    val binding: Int,
    val resource: BufferBinding,
)

data class BindGroupDescriptor(
    val layout: GpuHandle,
    val entries: List<BindGroupEntry>,
    val label: String? = null,
)

data class ProgrammableStage(
    val module: GpuHandle,
    val entryPoint: String? = null,
)

data class ComputePipelineDescriptor(
    val compute: ProgrammableStage,
    val layout: GpuHandle? = null,
    val label: String? = null,
)

data class CommandEncoderDescriptor(
    val label: String? = null,
)

data class ComputePassDescriptor(
    val label: String? = null,
)

/** Status from [WasiWebGpuHost.surfaceGetCurrentTexture] (Dawn surface acquire). */
enum class SurfaceTextureStatus {
    SuccessOptimal,
    SuccessSuboptimal,
    Timeout,
    Outdated,
    Lost,
    Error,
}

data class SurfaceTextureResult(
    val status: SurfaceTextureStatus,
    val texture: GpuHandle?,
)
