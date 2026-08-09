package io.github.fenriliuguang.wasi.webgpu.experimental.runtime.cm

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import io.github.fenriliuguang.wasi.webgpu.experimental.abiwasi.AbiWasi
import io.github.fenriliuguang.wasi.webgpu.experimental.abiwasi.AbiWasiResults

class WasiResultCodecTest {

    @Test
    fun unsupportedRequestDeviceReturnsResultErrRecord() {
        val err = WasiResultCodec.unsupportedResult(
            AbiWasi.Func.GPU_ADAPTER_REQUEST_DEVICE,
            AbiWasiResults.ErrorShape.RequestDevice,
        )
        assertTrue(err.isResult)
        val result = err.asResult()
        assertTrue(result.isErr)
        val record = result.err.orElseThrow()
        assertTrue(record.isRecord)
        val fields = record.asRecord()
        assertTrue(fields.containsKey("kind"))
        assertTrue(fields.containsKey("message"))
        assertEquals("operation-error", fields.getValue("kind").asVariant().caseName)
        assertTrue(fields.getValue("message").asString().contains("not wired"))
    }

    @Test
    fun unsupportedPipelineAsyncUsesGpuPipelineErrorPayload() {
        val err = WasiResultCodec.unsupportedResult(
            AbiWasi.Func.GPU_DEVICE_CREATE_COMPUTE_PIPELINE_ASYNC,
            AbiWasiResults.ErrorShape.CreatePipeline,
        )
        val kind = err.asResult().err.orElseThrow().asRecord().getValue("kind").asVariant()
        assertEquals("gpu-pipeline-error", kind.caseName)
        assertTrue(kind.payload.isPresent)
        assertEquals("internal", kind.payload.get().asEnum())
    }

    @Test
    fun resultFuncTableCoversKnownAsyncAndCopyPaths() {
        assertEquals(
            AbiWasiResults.ErrorShape.MapAsync,
            AbiWasiResults.BY_FUNC[AbiWasi.Func.GPU_BUFFER_MAP_ASYNC],
        )
        assertEquals(
            AbiWasiResults.ErrorShape.CreatePipeline,
            AbiWasiResults.BY_FUNC[AbiWasi.Func.GPU_DEVICE_CREATE_RENDER_PIPELINE_ASYNC],
        )
        assertEquals(13, AbiWasiResults.BY_FUNC.size)
    }

    @Test
    fun okUnitAndPayloadAreResultOk() {
        val unit = WasiResultCodec.ok()
        assertTrue(unit.isResult)
        assertTrue(unit.asResult().isOk)

        val payload = WasiResultCodec.ok(ai.tegmentum.wasmtime4j.component.ComponentVal.u32(7))
        assertTrue(payload.isResult)
        val ok = payload.asResult()
        assertTrue(ok.isOk)
        assertEquals(7L, ok.ok.orElseThrow().asU32())
    }
}
