package io.github.fenriliuguang.wasi.webgpu.experimental.host

import org.junit.Assert.assertArrayEquals
import org.junit.Test

class CpuWasiWebGpuHostTest {

    @Test
    fun vectorAddMatchesCpu() {
        val a = floatArrayOf(1f, 2f, 3f, 4f)
        val b = floatArrayOf(10f, 20f, 30f, 40f)
        val expected = floatArrayOf(11f, 22f, 33f, 44f)
        CpuWasiWebGpuHost().use { host ->
            val actual = VectorAddScenario.runOn(host, a, b)
            assertArrayEquals(expected, actual, 1e-5f)
        }
    }
}
