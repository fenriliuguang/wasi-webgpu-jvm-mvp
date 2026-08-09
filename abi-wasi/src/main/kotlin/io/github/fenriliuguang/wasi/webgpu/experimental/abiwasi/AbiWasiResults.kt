package io.github.fenriliuguang.wasi.webgpu.experimental.abiwasi

/**
 * wasi:webgpu@0.3.0-rc.2 host methods whose WIT return type is `result<…>` (slice F).
 *
 * Hand-maintained (not generated): keeps [AbiWasi] regen free of result metadata.
 * Stubs for these return `ComponentVal.err` instead of throwing.
 */
object AbiWasiResults {

    /** Import function name → WIT error-record shape used for `result` Err payload. */
    enum class ErrorShape {
        RequestDevice,
        MapAsync,
        GetMappedRange,
        Unmap,
        SetBindGroup,
        CreatePipeline,
        CreateQuerySet,
        PopErrorScope,
        WriteBuffer,
    }

    val BY_FUNC: Map<String, ErrorShape> = mapOf(
        AbiWasi.Func.GPU_ADAPTER_REQUEST_DEVICE to ErrorShape.RequestDevice,
        AbiWasi.Func.GPU_BUFFER_MAP_ASYNC to ErrorShape.MapAsync,
        AbiWasi.Func.GPU_BUFFER_GET_MAPPED_RANGE_GET_WITH_COPY to ErrorShape.GetMappedRange,
        AbiWasi.Func.GPU_BUFFER_GET_MAPPED_RANGE_SET_WITH_COPY to ErrorShape.GetMappedRange,
        AbiWasi.Func.GPU_BUFFER_UNMAP to ErrorShape.Unmap,
        AbiWasi.Func.GPU_COMPUTE_PASS_ENCODER_SET_BIND_GROUP to ErrorShape.SetBindGroup,
        AbiWasi.Func.GPU_RENDER_PASS_ENCODER_SET_BIND_GROUP to ErrorShape.SetBindGroup,
        AbiWasi.Func.GPU_RENDER_BUNDLE_ENCODER_SET_BIND_GROUP to ErrorShape.SetBindGroup,
        AbiWasi.Func.GPU_DEVICE_CREATE_COMPUTE_PIPELINE_ASYNC to ErrorShape.CreatePipeline,
        AbiWasi.Func.GPU_DEVICE_CREATE_RENDER_PIPELINE_ASYNC to ErrorShape.CreatePipeline,
        AbiWasi.Func.GPU_DEVICE_CREATE_QUERY_SET to ErrorShape.CreateQuerySet,
        AbiWasi.Func.GPU_DEVICE_POP_ERROR_SCOPE to ErrorShape.PopErrorScope,
        AbiWasi.Func.GPU_QUEUE_WRITE_BUFFER_WITH_COPY to ErrorShape.WriteBuffer,
    )
}
