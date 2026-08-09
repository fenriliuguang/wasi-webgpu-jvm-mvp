package io.github.fenriliuguang.wasi.webgpu.experimental.runtime.cm

import ai.tegmentum.wasmtime4j.component.ComponentVal
import io.github.fenriliuguang.wasi.webgpu.experimental.abiwasi.AbiWasi
import io.github.fenriliuguang.wasi.webgpu.experimental.abiwasi.AbiWasiResults
import io.github.fenriliuguang.wasi.webgpu.experimental.host.HostErrorMapping
import io.github.fenriliuguang.wasi.webgpu.experimental.host.HostException

/**
 * Encode wasi:webgpu `result` Err payloads (`record { kind, message }`) for CM host callbacks.
 */
object WasiResultCodec {

    /** Successful `result` with no Ok payload (`result<_, E>`). */
    fun ok(): ComponentVal = ComponentVal.ok()

    /** Successful `result` carrying an Ok payload. */
    fun ok(value: ComponentVal): ComponentVal = ComponentVal.ok(value)

    fun errFromHostException(func: String, shape: AbiWasiResults.ErrorShape, ex: HostException): ComponentVal {
        val message = HostErrorMapping.messageOf(ex)
        val kind = when (shape) {
            AbiWasiResults.ErrorShape.RequestDevice ->
                ComponentVal.variant(HostErrorMapping.witCase(HostErrorMapping.requestDevice(ex)))
            AbiWasiResults.ErrorShape.MapAsync ->
                ComponentVal.variant(HostErrorMapping.witCase(HostErrorMapping.mapAsync(ex)))
            AbiWasiResults.ErrorShape.GetMappedRange ->
                ComponentVal.variant(HostErrorMapping.witCase(HostErrorMapping.getMappedRange(ex)))
            AbiWasiResults.ErrorShape.Unmap ->
                ComponentVal.variant(HostErrorMapping.witCase(HostErrorMapping.unmap(ex)))
            AbiWasiResults.ErrorShape.SetBindGroup ->
                ComponentVal.variant(HostErrorMapping.witCase(HostErrorMapping.setBindGroup(ex)))
            AbiWasiResults.ErrorShape.WriteBuffer ->
                ComponentVal.variant(HostErrorMapping.witCase(HostErrorMapping.writeBuffer(ex)))
            AbiWasiResults.ErrorShape.CreateQuerySet ->
                ComponentVal.variant(HostErrorMapping.witCase(HostErrorMapping.createQuerySet(ex)))
            AbiWasiResults.ErrorShape.PopErrorScope ->
                ComponentVal.variant(HostErrorMapping.witCase(HostErrorMapping.popErrorScope(ex)))
            AbiWasiResults.ErrorShape.CreatePipeline -> {
                val reason = HostErrorMapping.pipelineReason(ex)
                ComponentVal.variant(
                    "gpu-pipeline-error",
                    ComponentVal.enum_(HostErrorMapping.witCase(reason)),
                )
            }
        }
        val record = ComponentVal.record(
            linkedMapOf(
                "kind" to kind,
                "message" to ComponentVal.string(message),
            ),
        )
        return ComponentVal.err(record)
    }

    /** Stub path: not-wired → Unsupported mapped into the method's result Err shape. */
    fun unsupportedResult(func: String, shape: AbiWasiResults.ErrorShape): ComponentVal {
        val ex = HostException.Unsupported(
            "wasi:webgpu@${AbiWasi.VERSION} import not wired yet " +
                "(compliant-world slice F result stub; wire in later slices): $func",
        )
        return errFromHostException(func, shape, ex)
    }
}
