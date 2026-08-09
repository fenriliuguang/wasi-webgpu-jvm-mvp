package io.github.fenriliuguang.wasi.webgpu.experimental.runtime.cm

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import io.github.fenriliuguang.wasi.webgpu.experimental.abiwasi.AbiWasi
import io.github.fenriliuguang.wasi.webgpu.experimental.abiwasi.AbiWasiResults

/**
 * Smoke coverage for guest-descriptor-cube slice C wiring metadata (no wasi Guest required).
 */
class WasiPrimaryPathWiringTest {

    @Test
    fun primaryPathIsNonEmptySubsetOfAll() {
        assertTrue(WasmtimeCmLinker.PRIMARY_PATH.isNotEmpty())
        assertTrue(AbiWasi.Func.ALL.containsAll(WasmtimeCmLinker.PRIMARY_PATH))
        assertEquals(33, WasmtimeCmLinker.PRIMARY_PATH.size)
    }

    @Test
    fun primaryPathIncludesAdapterDeviceQueueBufferAndPasses() {
        assertTrue(AbiWasi.Func.GPU_REQUEST_ADAPTER in WasmtimeCmLinker.PRIMARY_PATH)
        assertTrue(AbiWasi.Func.GPU_ADAPTER_REQUEST_DEVICE in WasmtimeCmLinker.PRIMARY_PATH)
        assertTrue(AbiWasi.Func.GPU_DEVICE_CREATE_BUFFER in WasmtimeCmLinker.PRIMARY_PATH)
        assertTrue(AbiWasi.Func.GPU_QUEUE_WRITE_TEXTURE_WITH_COPY in WasmtimeCmLinker.PRIMARY_PATH)
        assertTrue(AbiWasi.Func.GPU_RENDER_PASS_ENCODER_SET_BIND_GROUP in WasmtimeCmLinker.PRIMARY_PATH)
        assertTrue(AbiWasi.Func.GPU_BUFFER_MAP_ASYNC in WasmtimeCmLinker.PRIMARY_PATH)
    }

    @Test
    fun stubsWouldSkipWiredResultFuncs() {
        val wiredResults = WasmtimeCmLinker.PRIMARY_PATH.filter { it in AbiWasiResults.BY_FUNC }
        assertTrue(wiredResults.contains(AbiWasi.Func.GPU_ADAPTER_REQUEST_DEVICE))
        assertTrue(wiredResults.contains(AbiWasi.Func.GPU_QUEUE_WRITE_BUFFER_WITH_COPY))
        assertTrue(wiredResults.contains(AbiWasi.Func.GPU_RENDER_PASS_ENCODER_SET_BIND_GROUP))
        // Async pipeline variants stay stubbed (not primary path).
        assertFalse(AbiWasi.Func.GPU_DEVICE_CREATE_COMPUTE_PIPELINE_ASYNC in WasmtimeCmLinker.PRIMARY_PATH)
    }
}
