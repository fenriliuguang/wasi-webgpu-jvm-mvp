package io.github.fenriliuguang.wasi.webgpu.demo

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import io.github.fenriliuguang.wasi.webgpu.experimental.dawn.DawnWasiWebGpuHost
import io.github.fenriliuguang.wasi.webgpu.experimental.host.VectorAddScenario
import org.junit.Assert.assertArrayEquals
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Android acceptance: Guest → Wasmtime + abi-mvp → Dawn vector-add readback.
 *
 * Run via Android Studio: right-click this class → Run.
 * Requires a device/emulator with a usable WebGPU/Vulkan backend and packaged
 * `libwasmtime4j.so` under jniLibs.
 */
@RunWith(AndroidJUnit4::class)
class WasmtimeVectorAddInstrumentedTest {

    @Test
    fun guestViaWasmtimeMatchesDawnDirect() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val a = floatArrayOf(1f, 2f, 3f, 4f, 5f, 6f, 7f, 8f)
        val b = floatArrayOf(8f, 7f, 6f, 5f, 4f, 3f, 2f, 1f)
        val expected = FloatArray(a.size) { i -> a[i] + b[i] }

        val viaGuest = WasmtimeVectorAddAndroid.run(context, a, b)
        assertArrayEquals(expected, viaGuest, 1e-5f)

        DawnWasiWebGpuHost.create().use { host ->
            val direct = VectorAddScenario.runOn(host, a, b)
            assertArrayEquals(expected, direct, 1e-5f)
        }
    }
}
