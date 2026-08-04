package io.github.fenriliuguang.wasi.webgpu.experimental.runtime.cm

import org.junit.Assume

/**
 * Desktop CM tests need a CM-resources–patched wasmtime4j native under
 * `runtime-wasmtime/desktop-natives/` (see `scripts/build-wasmtime4j-desktop-cm.ps1`).
 *
 * Gradle sets `wasi.webgpu.cm.natives=true` when that file exists. CI without a local
 * rebuild skips CM and still runs abi-mvp.
 */
object CmNativesGate {
    fun assumePatchedNativesPresent() {
        val flag = System.getProperty("wasi.webgpu.cm.natives", "false")
        val forced = flag.equals("true", ignoreCase = true)
        val path = System.getProperty("wasi.webgpu.cm.natives.path")
        val fileOk = path != null && java.io.File(path).isFile
        Assume.assumeTrue(
            "CM-patched desktop natives missing; run ./scripts/build-wasmtime4j-desktop-cm.ps1",
            forced && (path == null || fileOk),
        )
    }
}
