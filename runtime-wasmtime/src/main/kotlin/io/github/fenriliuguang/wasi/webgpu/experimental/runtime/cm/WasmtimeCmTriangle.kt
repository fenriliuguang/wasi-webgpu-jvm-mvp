package io.github.fenriliuguang.wasi.webgpu.experimental.runtime.cm

import ai.tegmentum.wasmtime4j.component.ComponentInstance
import ai.tegmentum.wasmtime4j.component.ComponentVal
import ai.tegmentum.wasmtime4j.wit.WitValue
import io.github.fenriliuguang.wasi.webgpu.experimental.abicm.AbiCm
import io.github.fenriliuguang.wasi.webgpu.experimental.host.CpuWasiWebGpuHost
import io.github.fenriliuguang.wasi.webgpu.experimental.host.WasiWebGpuHost
import java.nio.file.Files
import java.nio.file.Path

/**
 * L1 entry: Guest `run-triangle` → Wasmtime CM + abi-cm → [WasiWebGpuHost].
 *
 * Host injects Android native window; Guest only holds `surface`.
 * Desktop CpuHost has no Android Surface — expect [io.github.fenriliuguang.wasi.webgpu.experimental.host.HostException.Unsupported]
 * (or a trap wrapping it). Successful draw is Android / Dawn only.
 */
object WasmtimeCmTriangle {

    fun run(
        componentBytes: ByteArray,
        windowHandle: Long,
        width: Int,
        height: Int,
        host: WasiWebGpuHost? = null,
    ) {
        require(windowHandle != 0L) { "window-handle is null" }
        require(width > 0 && height > 0) { "invalid surface size ${width}x$height" }
        val ownedHost = host == null
        val h = host ?: CpuWasiWebGpuHost()
        try {
            WasmtimeCmLinker(h).use { linker ->
                val instance = linker.instantiate(componentBytes)
                invokeRun(instance, windowHandle, width, height)
            }
            // Let Dawn finish present / GPU work before tearing down the Host.
            if (ownedHost) {
                Thread.sleep(100)
            }
        } finally {
            if (ownedHost) {
                h.close()
            }
        }
    }

    fun loadGuestComponent(path: Path = defaultGuestPath()): ByteArray = Files.readAllBytes(path)

    fun defaultGuestPath(): Path {
        val prop = System.getProperty("wasi.webgpu.guest.triangleCm")
        if (!prop.isNullOrBlank()) {
            return Path.of(prop)
        }
        return Path.of("guest", "triangle-cm", "triangle_cm.wasm")
    }

    private fun invokeRun(
        instance: ComponentInstance,
        windowHandle: Long,
        width: Int,
        height: Int,
    ) {
        val fn = instance.getFunc(AbiCm.EXPORT_RUN_TRIANGLE).orElseThrow {
            IllegalStateException("missing export ${AbiCm.EXPORT_RUN_TRIANGLE}")
        }
        // Bare Long/Int become s64/s32; WIT expects u64/u32.
        val result = fn.call(
            ComponentVal.u64(windowHandle),
            ComponentVal.u32(Integer.toUnsignedLong(width)),
            ComponentVal.u32(Integer.toUnsignedLong(height)),
        )
        parseResultUnitString(result)
    }

    private fun parseResultUnitString(result: Any?) {
        requireNotNull(result) { "guest returned null" }

        if (result is ComponentVal) {
            require(result.isResult) { "expected result, got ${result.type}" }
            val r = result.asResult()
            if (r.isErr) {
                val err = r.err.orElse(null)
                val msg = when {
                    err == null -> "err"
                    err.isString -> err.asString()
                    else -> err.toString()
                }
                error("guest returned error: $msg")
            }
            // unit ok: ignore ok payload (JNI may insert a bool placeholder)
            return
        }

        if (result is Map<*, *>) {
            val isOk = result["isOk"] as? Boolean
                ?: (result["err"] == null && result["error"] == null)
            if (!isOk) {
                error("guest returned error: ${result["err"] ?: result["error"]}")
            }
            return
        }

        if (result is WitValue) {
            parseResultUnitString(result.toJava())
            return
        }

        error("unexpected result type: ${result.javaClass.name}")
    }
}
