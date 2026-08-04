package io.github.fenriliuguang.wasi.webgpu.demo

import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertArrayEquals
import org.junit.Test
import org.junit.runner.RunWith

/**
 * P0 instrumented acceptance: Host → Dawn compute readback.
 * Requires a device/emulator with a usable WebGPU/Vulkan backend.
 *
 * Guest → Wasmtime → Dawn: see [WasmtimeVectorAddInstrumentedTest].
 */
@RunWith(AndroidJUnit4::class)
class VectorAddInstrumentedTest {

    @Test
    fun vectorAddMatchesCpu() {
        val a = floatArrayOf(1f, 2f, 3f, 4f, 5f, 6f, 7f, 8f)
        val b = floatArrayOf(8f, 7f, 6f, 5f, 4f, 3f, 2f, 1f)
        val expected = FloatArray(a.size) { i -> a[i] + b[i] }

        val actual = VectorAdd.run(a, b)
        assertArrayEquals(expected, actual, 1e-5f)
    }
}
