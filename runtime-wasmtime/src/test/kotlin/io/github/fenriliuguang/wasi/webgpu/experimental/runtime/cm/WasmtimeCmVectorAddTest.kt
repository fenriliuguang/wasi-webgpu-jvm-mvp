package io.github.fenriliuguang.wasi.webgpu.experimental.runtime.cm

import io.github.fenriliuguang.wasi.webgpu.experimental.host.CpuWasiWebGpuHost
import io.github.fenriliuguang.wasi.webgpu.experimental.host.VectorAddScenario
import org.junit.Assert.assertArrayEquals
import org.junit.Test

/**
 * Desktop CM Guest → Wasmtime ComponentLinker → L2 CpuHost.
 *
 * Multiple vector sizes share one CM linker/instance ([WasmtimeCmVectorAdd.runAll]) because
 * wasmtime4j's process-wide host-callback registry can trap on back-to-back linker recreate.
 */
class WasmtimeCmVectorAddTest {

    @Test
    fun cmGuestViaWasmtimeMatchesCpuHostDirect() {
        val component = WasmtimeCmVectorAdd.loadGuestComponent()

        val a = floatArrayOf(1f, 2f, 3f, 4f, 5f, 6f, 7f, 8f)
        val b = floatArrayOf(8f, 7f, 6f, 5f, 4f, 3f, 2f, 1f)
        val expected = FloatArray(a.size) { i -> a[i] + b[i] }

        val a2 = FloatArray(128) { it.toFloat() }
        val b2 = FloatArray(128) { (127 - it).toFloat() }
        val expected2 = FloatArray(a2.size) { i -> a2[i] + b2[i] }

        val results = WasmtimeCmVectorAdd.runAll(
            component,
            listOf(a to b, a2 to b2),
        )
        assertArrayEquals(expected, results[0], 1e-5f)
        assertArrayEquals(expected2, results[1], 1e-5f)

        CpuWasiWebGpuHost().use { host ->
            val direct = VectorAddScenario.runOn(host, a, b)
            assertArrayEquals(expected, direct, 1e-5f)
        }
    }
}
