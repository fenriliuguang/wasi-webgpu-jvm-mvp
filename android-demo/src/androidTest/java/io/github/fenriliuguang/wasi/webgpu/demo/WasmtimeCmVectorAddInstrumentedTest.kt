package io.github.fenriliuguang.wasi.webgpu.demo

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import io.github.fenriliuguang.wasi.webgpu.experimental.dawn.DawnWasiWebGpuHost
import io.github.fenriliuguang.wasi.webgpu.experimental.host.VectorAddScenario
import org.junit.Assert.assertArrayEquals
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Android acceptance: CM Guest → Wasmtime ComponentLinker + abi-cm → Dawn vector-add readback.
 *
 * Run via Android Studio: right-click this class → Run.
 * Requires a device/emulator with WebGPU/Vulkan and a Bionic `libwasmtime4j.so`
 * built with the CM resources patch (`scripts/build-wasmtime4j-android.ps1`).
 */
@RunWith(AndroidJUnit4::class)
class WasmtimeCmVectorAddInstrumentedTest {

    @Test
    fun cmGuestViaWasmtimeMatchesDawnDirect() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val a = floatArrayOf(1f, 2f, 3f, 4f, 5f, 6f, 7f, 8f)
        val b = floatArrayOf(8f, 7f, 6f, 5f, 4f, 3f, 2f, 1f)
        val expected = FloatArray(a.size) { i -> a[i] + b[i] }

        val a2 = FloatArray(128) { it.toFloat() }
        val b2 = FloatArray(128) { (127 - it).toFloat() }
        val expected2 = FloatArray(a2.size) { i -> a2[i] + b2[i] }

        // Prefer one linker/instance for multiple cases (wasmtime4j CM host registry).
        val viaGuest = WasmtimeCmVectorAddAndroid.runAll(
            context,
            listOf(a to b, a2 to b2),
        )
        assertArrayEquals(expected, viaGuest[0], 1e-5f)
        assertArrayEquals(expected2, viaGuest[1], 1e-5f)

        DawnWasiWebGpuHost.create().use { host ->
            val direct = VectorAddScenario.runOn(host, a, b)
            assertArrayEquals(expected, direct, 1e-5f)
        }
    }
}
