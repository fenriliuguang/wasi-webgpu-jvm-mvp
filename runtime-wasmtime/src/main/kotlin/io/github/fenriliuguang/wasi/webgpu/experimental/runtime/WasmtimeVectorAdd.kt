package io.github.fenriliuguang.wasi.webgpu.experimental.runtime

import ai.tegmentum.wasmtime4j.WasmValue
import io.github.fenriliuguang.wasi.webgpu.experimental.abi.AbiMvp
import io.github.fenriliuguang.wasi.webgpu.experimental.host.CpuWasiWebGpuHost
import io.github.fenriliuguang.wasi.webgpu.experimental.host.VectorAddScenario
import io.github.fenriliuguang.wasi.webgpu.experimental.host.WasiWebGpuHost
import java.nio.file.Files
import java.nio.file.Path

object WasmtimeVectorAdd {

    /** Scratch region in guest memory (after shader/entry data). */
    private const val SCRATCH = 1024

    fun run(
        wasmBytes: ByteArray,
        a: FloatArray,
        b: FloatArray,
        host: WasiWebGpuHost? = null,
    ): FloatArray {
        require(a.size == b.size && a.isNotEmpty())
        val ownedHost = host == null
        val h = host ?: CpuWasiWebGpuHost()
        try {
            WasmtimeAbiLinker(h).use { linker ->
                val instance = linker.instantiate(wasmBytes)
                val memory = instance.getMemory("memory").orElseThrow()
                val n = a.size
                val bytes = n * 4
                val ptrA = SCRATCH
                val ptrB = SCRATCH + bytes
                val ptrOut = SCRATCH + bytes * 2
                val aBytes = VectorAddScenario.floatsToBytes(a)
                val bBytes = VectorAddScenario.floatsToBytes(b)
                memory.writeBytes(ptrA, aBytes, 0, aBytes.size)
                memory.writeBytes(ptrB, bBytes, 0, bBytes.size)

                val fn = instance.getFunction(AbiMvp.EXPORT_RUN_VECTOR_ADD).orElseThrow {
                    IllegalStateException("missing export ${AbiMvp.EXPORT_RUN_VECTOR_ADD}")
                }
                val status = fn.call(
                    WasmValue.i32(ptrA),
                    WasmValue.i32(ptrB),
                    WasmValue.i32(ptrOut),
                    WasmValue.i32(n),
                )
                require(status.isNotEmpty() && status[0].asInt() == 0) {
                    "guest returned failure: ${status.firstOrNull()?.asInt()}"
                }
                val out = ByteArray(bytes)
                memory.readBytes(ptrOut, out, 0, bytes)
                return VectorAddScenario.bytesToFloats(out)
            }
        } finally {
            if (ownedHost) {
                h.close()
            }
        }
    }

    fun loadGuestWasm(path: Path = defaultGuestPath()): ByteArray = Files.readAllBytes(path)

    fun defaultGuestPath(): Path {
        val prop = System.getProperty("wasi.webgpu.guest.vectorAdd")
        if (!prop.isNullOrBlank()) {
            return Path.of(prop)
        }
        return Path.of("guest", "vector-add", "vector_add.wasm")
    }
}
