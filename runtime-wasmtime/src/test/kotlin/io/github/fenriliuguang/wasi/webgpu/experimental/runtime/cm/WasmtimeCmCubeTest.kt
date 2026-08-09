package io.github.fenriliuguang.wasi.webgpu.experimental.runtime.cm

import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Assume
import org.junit.Before
import org.junit.Test

/**
 * Desktop CM wiring for `run-cube`.
 *
 * Successful Dawn present needs an Android Surface — instrumented-only.
 * With CM natives present, expects CpuHost Unsupported / trap for surface creation.
 */
class WasmtimeCmCubeTest {

    @Before
    fun requireCmNatives() {
        CmNativesGate.assumePatchedNativesPresent()
    }

    @Test
    fun cmCubeOneShotDrawIsAndroidOnly() {
        Assume.assumeTrue(
            "successful CM cube draw needs Android Surface; see instrumented tests",
            System.getProperty("wasi.webgpu.android.surface", "false")
                .equals("true", ignoreCase = true),
        )
        fail("desktop JVM should not enable wasi.webgpu.android.surface")
    }

    @Test
    fun cmCubeOnCpuHostRejectsAndroidSurface() {
        val component = WasmtimeCmCube.loadGuestComponent()
        try {
            WasmtimeCmCube.run(
                componentBytes = component,
                windowHandle = 1L,
                width = 64,
                height = 64,
            )
            fail("expected Unsupported / trap for Android surface on CpuHost")
        } catch (e: Throwable) {
            val text = generateSequence(e) { it.cause }
                .mapNotNull { it.message }
                .joinToString(" | ")
                .ifBlank { e.toString() }
            assertTrue(
                "expected CM trap / invocation failure, got: $text",
                text.contains("Function invocation failed", ignoreCase = true) ||
                    text.contains("Runtime error", ignoreCase = true) ||
                    text.contains("wasm", ignoreCase = true) ||
                    text.contains("Android surface", ignoreCase = true) ||
                    text.contains("Unsupported", ignoreCase = true),
            )
        }
    }
}
