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
 * L1 entry: Guest cube exports → Wasmtime CM + abi-cm → [WasiWebGpuHost].
 *
 * Host injects Android native window; Guest only holds `surface`.
 * Desktop CpuHost has no Android Surface — expect Unsupported / trap.
 * Prefer [Session] for Demo taps and instrumented same-process repeats.
 */
object WasmtimeCmCube {

    class Session internal constructor(
        private val linker: WasmtimeCmLinker,
        private val instance: ComponentInstance,
        private val host: WasiWebGpuHost,
        private val ownedHost: Boolean,
    ) : AutoCloseable {
        private var closed = false

        fun runCube(windowHandle: Long, width: Int, height: Int) {
            check(!closed) { "CM cube session is closed" }
            require(windowHandle != 0L) { "window-handle is null" }
            require(width > 0 && height > 0) { "invalid surface size ${width}x$height" }
            try {
                invokeNamed(
                    AbiCm.EXPORT_RUN_CUBE,
                    ComponentVal.u64(windowHandle),
                    ComponentVal.u32(Integer.toUnsignedLong(width)),
                    ComponentVal.u32(Integer.toUnsignedLong(height)),
                )
            } finally {
                runCatching { linker.abiBindings().releaseLifetimeSafetyNets() }
            }
        }

        fun initCube(windowHandle: Long, width: Int, height: Int) {
            check(!closed) { "CM cube session is closed" }
            require(windowHandle != 0L) { "window-handle is null" }
            require(width > 0 && height > 0) { "invalid surface size ${width}x$height" }
            invokeNamed(
                AbiCm.EXPORT_INIT_CUBE,
                ComponentVal.u64(windowHandle),
                ComponentVal.u32(Integer.toUnsignedLong(width)),
                ComponentVal.u32(Integer.toUnsignedLong(height)),
            )
        }

        fun drawFrame() {
            check(!closed) { "CM cube session is closed" }
            invokeNamed(AbiCm.EXPORT_DRAW_FRAME)
        }

        fun dropCube() {
            check(!closed) { "CM cube session is closed" }
            invokeNamed(AbiCm.EXPORT_DROP_CUBE)
        }

        fun runFrameLoop(
            windowHandle: Long,
            width: Int,
            height: Int,
            frameCount: Int,
            frameDelayMs: Long = 16L,
        ) {
            require(frameCount > 0) { "frameCount must be > 0" }
            initCube(windowHandle, width, height)
            var drawError: Throwable? = null
            try {
                repeat(frameCount) { i ->
                    drawFrame()
                    if (i < frameCount - 1 && frameDelayMs > 0L) {
                        Thread.sleep(frameDelayMs)
                    }
                }
            } catch (t: Throwable) {
                drawError = t
            } finally {
                val dropResult = runCatching { dropCube() }
                // guest-descriptor-cube D: clear View↔Texture map + encoder orphans before Demo
                // releaseAllGpuObjects handoff (still not true WIT dtor — see patches/UPSTREAM.md §4).
                runCatching { linker.abiBindings().releaseLifetimeSafetyNets() }
                drawError?.let { throw it }
                dropResult.getOrThrow()
            }
        }

        override fun close() {
            if (closed) return
            closed = true
            runCatching { linker.abiBindings().releaseLifetimeSafetyNets() }
            runCatching { linker.close() }
            if (ownedHost) {
                Thread.sleep(100)
                host.close()
            }
        }

        private fun invokeNamed(export: String, vararg args: ComponentVal) {
            val fn = instance.getFunc(export).orElseThrow {
                IllegalStateException("missing export $export")
            }
            val result = if (args.isEmpty()) fn.call() else fn.call(*args)
            parseResultUnitString(result)
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
            session.runCube(windowHandle, width, height)
        }
    }

    fun loadGuestComponent(path: Path = defaultGuestPath()): ByteArray = Files.readAllBytes(path)

    fun defaultGuestPath(): Path {
        val prop = System.getProperty("wasi.webgpu.guest.cubeCm")
        if (!prop.isNullOrBlank()) {
            return Path.of(prop)
        }
        return Path.of("guest", "cube-cm", "cube_cm.wasm")
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
