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
 *
 * Prefer [Session] for repeated Demo draws — wasmtime4j CM host callbacks are process-global
 * and back-to-back linker recreate can trap (`invalid handle` / missing destructor).
 */
object WasmtimeCmTriangle {

    /**
     * Long-lived CM linker + instance for repeated [runTriangle] calls on one Host.
     * Aligns with [WasmtimeCmVectorAdd.runAll]: do not recreate the linker between draws.
     */
    class Session internal constructor(
        private val linker: WasmtimeCmLinker,
        private val instance: ComponentInstance,
        private val host: WasiWebGpuHost,
        private val ownedHost: Boolean,
    ) : AutoCloseable {
        private var closed = false

        fun runTriangle(windowHandle: Long, width: Int, height: Int) {
            check(!closed) { "CM triangle session is closed" }
            require(windowHandle != 0L) { "window-handle is null" }
            require(width > 0 && height > 0) { "invalid surface size ${width}x$height" }
            invokeRun(instance, windowHandle, width, height)
        }

        override fun close() {
            if (closed) return
            closed = true
            runCatching { linker.close() }
            if (ownedHost) {
                // Let Dawn finish present / GPU work before tearing down the Host.
                Thread.sleep(100)
                host.close()
            }
        }
    }

    fun openSession(
        componentBytes: ByteArray,
        host: WasiWebGpuHost? = null,
    ): Session {
        val ownedHost = host == null
        val h = host ?: CpuWasiWebGpuHost()
        val linker = WasmtimeCmLinker(h)
        return try {
            val instance = linker.instantiate(componentBytes)
            Session(linker, instance, h, ownedHost)
        } catch (t: Throwable) {
            runCatching { linker.close() }
            if (ownedHost) {
                runCatching { h.close() }
            }
            throw t
        }
    }

    fun run(
        componentBytes: ByteArray,
        windowHandle: Long,
        width: Int,
        height: Int,
        host: WasiWebGpuHost? = null,
    ) {
        openSession(componentBytes, host).use { session ->
            session.runTriangle(windowHandle, width, height)
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
