package io.github.fenriliuguang.wasi.webgpu.experimental.runtime

import ai.tegmentum.wasmtime4j.Engine
import ai.tegmentum.wasmtime4j.Instance
import ai.tegmentum.wasmtime4j.Linker
import ai.tegmentum.wasmtime4j.Module
import ai.tegmentum.wasmtime4j.RuntimeType
import ai.tegmentum.wasmtime4j.Store
import ai.tegmentum.wasmtime4j.WasmMemory
import ai.tegmentum.wasmtime4j.WasmRuntime
import ai.tegmentum.wasmtime4j.WasmValue
import ai.tegmentum.wasmtime4j.WasmValueType
import ai.tegmentum.wasmtime4j.factory.WasmRuntimeFactory
import ai.tegmentum.wasmtime4j.func.HostFunction
import ai.tegmentum.wasmtime4j.type.FunctionType
import io.github.fenriliuguang.wasi.webgpu.experimental.abi.AbiMvp
import io.github.fenriliuguang.wasi.webgpu.experimental.abi.AbiMvpHostBindings
import io.github.fenriliuguang.wasi.webgpu.experimental.host.WasiWebGpuHost
import java.util.concurrent.atomic.AtomicReference

/**
 * L1 Wasmtime adapter: registers abi-mvp imports that forward to [WasiWebGpuHost].
 */
class WasmtimeAbiLinker(
    private val host: WasiWebGpuHost,
) : AutoCloseable {

    private val runtime: WasmRuntime = WasmRuntimeFactory.create(RuntimeType.JNI)
    private val engine: Engine = runtime.createEngine()
    private val store: Store = runtime.createStore(engine)
    private val memoryRef = AtomicReference<WasmMemory?>()

    fun instantiate(wasmBytes: ByteArray): Instance {
        val bindings = AbiMvpHostBindings(host) {
            val mem = memoryRef.get() ?: error("guest memory not ready")
            WasmtimeGuestMemory(mem)
        }
        val linker: Linker<Any> = runtime.createLinker(engine)
        registerImports(linker, bindings)
        val module: Module = runtime.compileModule(engine, wasmBytes)
        val instance = linker.instantiate(store, module)
        val memory = instance.getMemory("memory").orElseThrow {
            IllegalStateException("guest must export memory")
        }
        memoryRef.set(memory)
        return instance
    }

    override fun close() {
        runCatching { store.close() }
        runCatching { engine.close() }
        runCatching { runtime.close() }
    }

    private fun registerImports(linker: Linker<Any>, bindings: AbiMvpHostBindings) {
        val m = AbiMvp.MODULE
        val i32 = WasmValueType.I32

        fun ft(params: Array<WasmValueType>, results: Array<WasmValueType>) =
            FunctionType.of(params, results)

        fun defineVoid(name: String, params: Array<WasmValueType>, body: (Array<WasmValue>) -> Unit) {
            linker.defineHostFunction(
                m,
                name,
                ft(params, emptyArray()),
                HostFunction.voidFunction { args -> body(args) },
            )
        }

        fun defineI32(name: String, params: Array<WasmValueType>, body: (Array<WasmValue>) -> Int) {
            linker.defineHostFunction(
                m,
                name,
                ft(params, arrayOf(i32)),
                HostFunction.singleValue { args -> WasmValue.i32(body(args)) },
            )
        }

        defineI32(AbiMvp.Func.REQUEST_ADAPTER, emptyArray()) {
            bindings.requestAdapter()
        }
        defineI32(AbiMvp.Func.ADAPTER_REQUEST_DEVICE, arrayOf(i32)) { args ->
            bindings.adapterRequestDevice(args[0].asInt())
        }
        defineI32(AbiMvp.Func.DEVICE_GET_QUEUE, arrayOf(i32)) { args ->
            bindings.deviceGetQueue(args[0].asInt())
        }
        defineI32(AbiMvp.Func.DEVICE_CREATE_BUFFER, arrayOf(i32, i32, i32)) { args ->
            bindings.deviceCreateBuffer(args[0].asInt(), args[1].asInt(), args[2].asInt())
        }
        defineVoid(AbiMvp.Func.QUEUE_WRITE_BUFFER, arrayOf(i32, i32, i32, i32, i32)) { args ->
            bindings.queueWriteBuffer(
                args[0].asInt(), args[1].asInt(), args[2].asInt(),
                args[3].asInt(), args[4].asInt(),
            )
        }
        defineI32(AbiMvp.Func.DEVICE_CREATE_SHADER_MODULE, arrayOf(i32, i32, i32)) { args ->
            bindings.deviceCreateShaderModule(args[0].asInt(), args[1].asInt(), args[2].asInt())
        }
        defineI32(AbiMvp.Func.DEVICE_CREATE_BIND_GROUP_LAYOUT_STORAGE3, arrayOf(i32)) { args ->
            bindings.deviceCreateBindGroupLayoutStorage3(args[0].asInt())
        }
        defineI32(AbiMvp.Func.DEVICE_CREATE_BIND_GROUP3, arrayOf(i32, i32, i32, i32, i32)) { args ->
            bindings.deviceCreateBindGroup3(
                args[0].asInt(), args[1].asInt(), args[2].asInt(),
                args[3].asInt(), args[4].asInt(),
            )
        }
        defineI32(AbiMvp.Func.DEVICE_CREATE_COMPUTE_PIPELINE, arrayOf(i32, i32, i32, i32, i32)) { args ->
            bindings.deviceCreateComputePipeline(
                args[0].asInt(), args[1].asInt(), args[2].asInt(),
                args[3].asInt(), args[4].asInt(),
            )
        }
        defineI32(AbiMvp.Func.DEVICE_CREATE_COMMAND_ENCODER, arrayOf(i32)) { args ->
            bindings.deviceCreateCommandEncoder(args[0].asInt())
        }
        defineI32(AbiMvp.Func.COMMAND_ENCODER_BEGIN_COMPUTE_PASS, arrayOf(i32)) { args ->
            bindings.commandEncoderBeginComputePass(args[0].asInt())
        }
        defineVoid(AbiMvp.Func.COMPUTE_PASS_SET_PIPELINE, arrayOf(i32, i32)) { args ->
            bindings.computePassSetPipeline(args[0].asInt(), args[1].asInt())
        }
        defineVoid(AbiMvp.Func.COMPUTE_PASS_SET_BIND_GROUP, arrayOf(i32, i32, i32)) { args ->
            bindings.computePassSetBindGroup(args[0].asInt(), args[1].asInt(), args[2].asInt())
        }
        defineVoid(AbiMvp.Func.COMPUTE_PASS_DISPATCH, arrayOf(i32, i32, i32, i32)) { args ->
            bindings.computePassDispatch(
                args[0].asInt(), args[1].asInt(), args[2].asInt(), args[3].asInt(),
            )
        }
        defineVoid(AbiMvp.Func.COMPUTE_PASS_END, arrayOf(i32)) { args ->
            bindings.computePassEnd(args[0].asInt())
        }
        defineVoid(
            AbiMvp.Func.COMMAND_ENCODER_COPY_BUFFER_TO_BUFFER,
            arrayOf(i32, i32, i32, i32, i32, i32),
        ) { args ->
            bindings.commandEncoderCopyBufferToBuffer(
                args[0].asInt(), args[1].asInt(), args[2].asInt(),
                args[3].asInt(), args[4].asInt(), args[5].asInt(),
            )
        }
        defineI32(AbiMvp.Func.COMMAND_ENCODER_FINISH, arrayOf(i32)) { args ->
            bindings.commandEncoderFinish(args[0].asInt())
        }
        defineVoid(AbiMvp.Func.QUEUE_SUBMIT1, arrayOf(i32, i32)) { args ->
            bindings.queueSubmit1(args[0].asInt(), args[1].asInt())
        }
        defineVoid(AbiMvp.Func.BUFFER_MAP_READ, arrayOf(i32, i32, i32)) { args ->
            bindings.bufferMapRead(args[0].asInt(), args[1].asInt(), args[2].asInt())
        }
        defineVoid(AbiMvp.Func.BUFFER_GET_MAPPED_RANGE, arrayOf(i32, i32, i32, i32)) { args ->
            bindings.bufferGetMappedRange(
                args[0].asInt(), args[1].asInt(), args[2].asInt(), args[3].asInt(),
            )
        }
        defineVoid(AbiMvp.Func.BUFFER_UNMAP, arrayOf(i32)) { args ->
            bindings.bufferUnmap(args[0].asInt())
        }
    }
}
