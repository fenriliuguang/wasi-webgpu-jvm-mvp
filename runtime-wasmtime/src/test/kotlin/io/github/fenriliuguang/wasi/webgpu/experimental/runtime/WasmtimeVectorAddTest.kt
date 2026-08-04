package io.github.fenriliuguang.wasi.webgpu.experimental.runtime

import io.github.fenriliuguang.wasi.webgpu.experimental.host.CpuWasiWebGpuHost
import io.github.fenriliuguang.wasi.webgpu.experimental.host.VectorAddScenario
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.system.measureNanoTime

class WasmtimeVectorAddTest {

    @Test
    fun guestViaWasmtimeMatchesCpuHostDirect() {
        val a = floatArrayOf(1f, 2f, 3f, 4f, 5f, 6f, 7f, 8f)
        val b = floatArrayOf(8f, 7f, 6f, 5f, 4f, 3f, 2f, 1f)
        val expected = FloatArray(a.size) { i -> a[i] + b[i] }

        val wasm = WasmtimeVectorAdd.loadGuestWasm()
        val viaGuest = WasmtimeVectorAdd.run(wasm, a, b)
        assertArrayEquals(expected, viaGuest, 1e-5f)

        CpuWasiWebGpuHost().use { host ->
            val direct = VectorAddScenario.runOn(host, a, b)
            assertArrayEquals(expected, direct, 1e-5f)
        }
    }

    @Test
    fun boundaryNoteTimingSmoke() {
        val a = FloatArray(256) { it.toFloat() }
        val b = FloatArray(256) { (255 - it).toFloat() }
        val wasm = WasmtimeVectorAdd.loadGuestWasm()

        // Warmup
        repeat(2) {
            WasmtimeVectorAdd.run(wasm, a, b)
            CpuWasiWebGpuHost().use { VectorAddScenario.runOn(it, a, b) }
        }

        val guestNs = measureNanoTime {
            repeat(5) { WasmtimeVectorAdd.run(wasm, a, b) }
        }
        val directNs = measureNanoTime {
            repeat(5) {
                CpuWasiWebGpuHost().use { VectorAddScenario.runOn(it, a, b) }
            }
        }
        assertTrue("guest path should complete", guestNs > 0)
        assertTrue("direct path should complete", directNs > 0)
        println(
            "P1 boundary smoke: guest=${guestNs / 5 / 1_000_000.0}ms avg, " +
                "directKotlinL2=${directNs / 5 / 1_000_000.0}ms avg (n=256, 5 iters)",
        )
    }
}
