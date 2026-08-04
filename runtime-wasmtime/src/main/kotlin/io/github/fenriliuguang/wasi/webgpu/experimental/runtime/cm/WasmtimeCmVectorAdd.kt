package io.github.fenriliuguang.wasi.webgpu.experimental.runtime.cm

import ai.tegmentum.wasmtime4j.component.ComponentInstance
import ai.tegmentum.wasmtime4j.component.ComponentVal
import ai.tegmentum.wasmtime4j.wit.WitFloat32
import ai.tegmentum.wasmtime4j.wit.WitList
import ai.tegmentum.wasmtime4j.wit.WitValue
import io.github.fenriliuguang.wasi.webgpu.experimental.abicm.AbiCm
import io.github.fenriliuguang.wasi.webgpu.experimental.host.CpuWasiWebGpuHost
import io.github.fenriliuguang.wasi.webgpu.experimental.host.WasiWebGpuHost
import java.nio.file.Files
import java.nio.file.Path

object WasmtimeCmVectorAdd {

    fun run(
        componentBytes: ByteArray,
        a: FloatArray,
        b: FloatArray,
        host: WasiWebGpuHost? = null,
    ): FloatArray {
        require(a.size == b.size && a.isNotEmpty())
        val ownedHost = host == null
        val h = host ?: CpuWasiWebGpuHost()
        try {
            WasmtimeCmLinker(h).use { linker ->
                val instance = linker.instantiate(componentBytes)
                return invokeRun(instance, a, b)
            }
        } finally {
            if (ownedHost) {
                h.close()
            }
        }
    }

    /**
     * Run multiple vector-add invocations on one CM linker/instance.
     * Prefer this over repeated [run] calls — wasmtime4j CM host callbacks are process-global
     * and back-to-back linker recreate can trap.
     */
    fun runAll(
        componentBytes: ByteArray,
        cases: List<Pair<FloatArray, FloatArray>>,
        host: WasiWebGpuHost? = null,
    ): List<FloatArray> {
        require(cases.isNotEmpty())
        val ownedHost = host == null
        val h = host ?: CpuWasiWebGpuHost()
        try {
            WasmtimeCmLinker(h).use { linker ->
                val instance = linker.instantiate(componentBytes)
                return cases.map { (a, b) ->
                    require(a.size == b.size && a.isNotEmpty())
                    invokeRun(instance, a, b)
                }
            }
        } finally {
            if (ownedHost) {
                h.close()
            }
        }
    }

    fun loadGuestComponent(path: Path = defaultGuestPath()): ByteArray = Files.readAllBytes(path)

    fun defaultGuestPath(): Path {
        val prop = System.getProperty("wasi.webgpu.guest.vectorAddCm")
        if (!prop.isNullOrBlank()) {
            return Path.of(prop)
        }
        return Path.of("guest", "vector-add-cm", "vector_add_cm.wasm")
    }

    private fun invokeRun(instance: ComponentInstance, a: FloatArray, b: FloatArray): FloatArray {
        val fn = instance.getFunc(AbiCm.EXPORT_RUN_VECTOR_ADD).orElseThrow {
            IllegalStateException("missing export ${AbiCm.EXPORT_RUN_VECTOR_ADD}")
        }
        val result = fn.call(toWitF32List(a), toWitF32List(b))
        return parseResultListF32(result)
    }

    private fun toWitF32List(values: FloatArray): WitList {
        val elements = ArrayList<WitValue>(values.size)
        for (v in values) {
            elements.add(WitFloat32.of(v))
        }
        return WitList.of(elements)
    }

    private fun parseResultListF32(result: Any?): FloatArray {
        requireNotNull(result) { "guest returned null" }

        if (result is ComponentVal) {
            if (result.isResult) {
                val r = result.asResult()
                if (r.isErr) {
                    val err = r.err.orElse(null)
                    error("guest returned error: ${err?.asString() ?: err}")
                }
                val ok = r.ok.orElseThrow { IllegalStateException("ok result missing payload") }
                return listToFloats(ok)
            }
            return listToFloats(result)
        }

        if (result is Map<*, *>) {
            val isOk = result["isOk"] as? Boolean
                ?: (result["ok"] != null)
            if (isOk == false) {
                val err = result["err"] ?: result["error"]
                error("guest returned error: $err")
            }
            val ok = result["ok"] ?: result["Ok"] ?: result["value"]
            return listToFloats(ok)
        }

        if (result is WitValue) {
            return parseResultListF32(result.toJava())
        }

        return listToFloats(result)
    }

    private fun listToFloats(value: Any?): FloatArray {
        when (value) {
            is FloatArray -> return value
            is ComponentVal -> {
                require(value.isList) { "expected list ComponentVal, got ${value.type}" }
                val elems = value.asList()
                val out = FloatArray(elems.size)
                for (i in elems.indices) {
                    out[i] = elems[i].asF32()
                }
                return out
            }
            is List<*> -> {
                val out = FloatArray(value.size)
                for (i in value.indices) {
                    val el = value[i]
                    out[i] = when (el) {
                        is ComponentVal -> el.asF32()
                        is Number -> el.toFloat()
                        else -> error("unexpected list element: ${el?.javaClass?.name}")
                    }
                }
                return out
            }
            is WitList -> {
                val elems = value.getElements()
                val out = FloatArray(elems.size)
                for (i in elems.indices) {
                    out[i] = (elems[i].toJava() as Number).toFloat()
                }
                return out
            }
            else -> error("unexpected list payload: ${value?.javaClass?.name}")
        }
    }
}
