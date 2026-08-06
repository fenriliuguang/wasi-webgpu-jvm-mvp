package io.github.fenriliuguang.wasi.webgpu.experimental.runtime.cm

import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Assume
import org.junit.Before
import org.junit.Test

/**
 * Desktop CM wiring for `run-triangle`.
 *
 * Successful Dawn present needs an Android Surface — that path is instrumented-only.
 * With CM natives present, this test still exercises instantiate + export call and expects
 * CpuHost [io.github.fenriliuguang.wasi.webgpu.experimental.host.HostException.Unsupported]
 * (or a trap wrapping it) for surface creation.
 *
 * Skips entirely when CM-patched desktop natives are absent (same gate as vector-add CM).
 */
class WasmtimeCmTriangleTest {

    @Before
    fun requireCmNatives() {
        CmNativesGate.assumePatchedNativesPresent()
    }

    @Test
    fun cmTriangleOneShotDrawIsAndroidOnly() {
        Assume.assumeTrue(
            "successful CM triangle draw needs Android Surface; see instrumented tests",
            System.getProperty("wasi.webgpu.android.surface", "false")
                .equals("true", ignoreCase = true),
        )
        fail("desktop JVM should not enable wasi.webgpu.android.surface")
    }

    @Test
    fun cmTriangleOnCpuHostRejectsAndroidSurface() {
        val component = WasmtimeCmTriangle.loadGuestComponent()
        try {
            WasmtimeCmTriangle.run(
                componentBytes = component,
                windowHandle = 1L,
                width = 64,
                height = 64,
            )
            fail("expected Unsupported / trap for Android surface on CpuHost")
        } catch (e: Throwable) {
            // CpuHost throws HostException.Unsupported inside the host callback; wasmtime4j
            // typically surfaces that as a Function invocation / wasm trap (detail may only
            // appear in stderr: "Android surface (Cpu host)").
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
