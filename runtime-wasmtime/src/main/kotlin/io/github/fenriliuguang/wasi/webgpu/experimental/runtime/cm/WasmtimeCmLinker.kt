package io.github.fenriliuguang.wasi.webgpu.experimental.runtime.cm

import ai.tegmentum.wasmtime4j.Engine
import ai.tegmentum.wasmtime4j.RuntimeType
import ai.tegmentum.wasmtime4j.Store
import ai.tegmentum.wasmtime4j.component.Component
import ai.tegmentum.wasmtime4j.component.ComponentEngine
import ai.tegmentum.wasmtime4j.component.ComponentEngineConfig
import ai.tegmentum.wasmtime4j.component.ComponentHostFunction
import ai.tegmentum.wasmtime4j.component.ComponentInstance
import ai.tegmentum.wasmtime4j.component.ComponentLinker
import ai.tegmentum.wasmtime4j.component.ComponentResourceDefinition
import ai.tegmentum.wasmtime4j.component.ComponentVal
import ai.tegmentum.wasmtime4j.factory.WasmRuntimeFactory
import io.github.fenriliuguang.wasi.webgpu.experimental.abicm.AbiCm
import io.github.fenriliuguang.wasi.webgpu.experimental.abicm.AbiCmHostBindings
import io.github.fenriliuguang.wasi.webgpu.experimental.abiwasi.AbiWasi
import io.github.fenriliuguang.wasi.webgpu.experimental.host.HostException
import io.github.fenriliuguang.wasi.webgpu.experimental.host.VertexAttribute
import io.github.fenriliuguang.wasi.webgpu.experimental.host.VertexBufferLayout
import io.github.fenriliuguang.wasi.webgpu.experimental.host.WasiWebGpuHost

/**
 * L1 Wasmtime Component Model adapter: registers experimental CM host imports → [WasiWebGpuHost],
 * and dual-track wasi:webgpu@0.3.0-rc.2 resources/stubs (compliant-world slice B).
 *
 * WIT resources are registered via [ComponentLinker.defineResource]. Host callbacks exchange
 * resource reps as u32 (L2 [GpuHandle.raw]); a patched wasmtime4j native maps those to
 * ResourceAny for the Component Model ABI, and replays resources when instantiation rebuilds
 * a fresh linker from the process-global host registry.
 *
 * Note: wasmtime4j 47.0.2-1.5.0 `createComponentEngine()` does not attach the runtime to
 * [ComponentEngine.getEngine]; we create a CM-enabled [Engine] separately for linker/store.
 * Desktop CM: run `scripts/build-wasmtime4j-desktop-cm.ps1` to populate
 * `runtime-wasmtime/desktop-natives/`.
 */
class WasmtimeCmLinker(
    private val host: WasiWebGpuHost,
) : AutoCloseable {

    private val runtime = WasmRuntimeFactory.create(RuntimeType.JNI)
    private val componentEngine: ComponentEngine = runtime.createComponentEngine()
    private val engine: Engine = runtime.createEngine(ComponentEngineConfig().toEngineConfig())
    private val store: Store = runtime.createStore(engine)

    fun instantiate(componentBytes: ByteArray): ComponentInstance {
        require(runtime.supportsComponentModel()) {
            "wasmtime4j runtime does not report Component Model support"
        }
        val bindings = AbiCmHostBindings(host)
        val linker: ComponentLinker<Any> = runtime.createComponentLinker(engine)
        registerExperimentalResources(linker)
        registerWasiResources(linker)
        registerExperimentalImports(linker, bindings)
        registerWasiImportStubs(linker)
        val component: Component = componentEngine.compileComponent(componentBytes)
        return linker.instantiate(store, component)
    }

    override fun close() {
        runCatching { store.close() }
        runCatching { engine.close() }
        runCatching { componentEngine.close() }
        runCatching { runtime.close() }
    }

    private fun registerExperimentalResources(linker: ComponentLinker<Any>) {
        // wasmtime4j 47.0.2 JNI builds the linker instance path as "{namespace}/{interfaceName}".
        // With PACKAGE "experimental:webgpu-cm" + "host@0.4.0" that yields
        // "experimental:webgpu-cm/host@0.4.0" — matching defineFunction / guest import.
        val ns = AbiCm.PACKAGE
        val iface = "${AbiCm.INTERFACE}@${AbiCm.VERSION}"
        for (name in AbiCm.Resource.ALL) {
            // Type registration only: wasmtime4j's Java destructor path keys a private
            // resourceTable filled by constructors, but constructors are not wired in native.
            // L2 handles are released when the host is closed after the CM run.
            val definition = ComponentResourceDefinition.builder<Any>(name).build()
            linker.defineResource(ns, iface, name, definition)
        }
    }

    /**
     * Dual-track (slice B): register standard-package resources on the same linker so
     * `wasi:webgpu/webgpu@0.3.0-rc.2` coexists with experimental. Function wiring is C+.
     */
    private fun registerWasiResources(linker: ComponentLinker<Any>) {
        val ns = AbiWasi.PACKAGE
        val iface = "${AbiWasi.INTERFACE}@${AbiWasi.VERSION}"
        for (name in AbiWasi.Resource.ALL) {
            val definition = ComponentResourceDefinition.builder<Any>(name).build()
            linker.defineResource(ns, iface, name, definition)
        }
    }

    /**
     * Stub every wasi:webgpu import so a Guest that resolves the standard package gets a clear
     * [HostException.Unsupported] instead of a missing-import link error. Real wiring is C+.
     */
    private fun registerWasiImportStubs(linker: ComponentLinker<Any>) {
        val stub = ComponentHostFunction.singleValue { _ ->
            throw HostException.Unsupported(
                "wasi:webgpu@${AbiWasi.VERSION} import not wired yet " +
                    "(compliant-world slice B stub; wire in C+)",
            )
        }
        for (func in AbiWasi.Func.ALL) {
            linker.defineFunction("${AbiWasi.IMPORT_INTERFACE}#$func", stub)
        }
    }

    private fun registerExperimentalImports(linker: ComponentLinker<Any>, bindings: AbiCmHostBindings) {
        fun path(func: String): String = "${AbiCm.IMPORT_INTERFACE}#$func"

        fun define(name: String, impl: ComponentHostFunction) {
            linker.defineFunction(path(name), impl)
        }

        fun u32(v: Int): ComponentVal = ComponentVal.u32(Integer.toUnsignedLong(v))

        fun paramU32(params: List<ComponentVal>, i: Int): Int = params[i].asU32().toInt()

        fun paramU64(params: List<ComponentVal>, i: Int): Long = params[i].asU64()

        fun parseBufferDescriptor(val_: ComponentVal): BufferDescriptorFields {
            require(val_.isRecord) { "expected buffer-descriptor record, got ${val_.type}" }
            val fields = val_.asRecord()
            val size = fields.getValue("size").asU64()
            val usage = fields.getValue("usage").asU32().toInt()
            val mapped = fields.getValue("mapped-at-creation").asBool()
            val labelVal = fields.getValue("label")
            val label = if (labelVal.isOption) {
                labelVal.asSome().map { it.asString() }.orElse(null)
            } else if (labelVal.isString) {
                labelVal.asString()
            } else {
                null
            }
            return BufferDescriptorFields(size, usage, mapped, label)
        }

        fun parseVertexAttribute(val_: ComponentVal): VertexAttribute {
            require(val_.isRecord) { "expected vertex-attribute record, got ${val_.type}" }
            val fields = val_.asRecord()
            return VertexAttribute(
                format = fields.getValue("format").asU32().toInt(),
                offset = fields.getValue("offset").asU64(),
                shaderLocation = fields.getValue("shader-location").asU32().toInt(),
            )
        }

        fun parseVertexBufferLayout(val_: ComponentVal): VertexBufferLayout {
            require(val_.isRecord) { "expected vertex-buffer-layout record, got ${val_.type}" }
            val fields = val_.asRecord()
            val attrsVal = fields.getValue("attributes")
            require(attrsVal.isList) { "expected attributes list, got ${attrsVal.type}" }
            val attributes = attrsVal.asList().map { el ->
                val attr = el as? ComponentVal
                    ?: error("expected ComponentVal attribute, got ${el?.javaClass}")
                parseVertexAttribute(attr)
            }
            return VertexBufferLayout(
                arrayStride = fields.getValue("array-stride").asU64(),
                stepMode = fields.getValue("step-mode").asU32().toInt(),
                attributes = attributes,
            )
        }

        fun parseVertexBufferLayouts(val_: ComponentVal): List<VertexBufferLayout> {
            require(val_.isList) { "expected list<vertex-buffer-layout>, got ${val_.type}" }
            return val_.asList().map { el ->
                val layout = el as? ComponentVal
                    ?: error("expected ComponentVal layout, got ${el?.javaClass}")
                parseVertexBufferLayout(layout)
            }
        }

        define(AbiCm.Func.REQUEST_ADAPTER, ComponentHostFunction.singleValue {
            u32(bindings.requestAdapter())
        })
        define(
            AbiCm.Func.CREATE_SURFACE_FROM_NATIVE_WINDOW,
            ComponentHostFunction.singleValue { params ->
                require(params.isNotEmpty()) { "create-surface-from-native-window: missing window-handle" }
                val handleVal = params[0]
                val windowHandle = paramU64(params, 0)
                // Diagnose u64 marshalling (Android TBI/PAC pointers often set the high bit).
                System.err.println(
                    "CREATE_SURFACE_FROM_NATIVE_WINDOW params=${params.size} " +
                        "type=${handleVal.type} handle=0x${java.lang.Long.toUnsignedString(windowHandle, 16)} " +
                        "unsignedDec=${java.lang.Long.toUnsignedString(windowHandle)}",
                )
                u32(bindings.createSurfaceFromNativeWindow(windowHandle))
            },
        )
        define(AbiCm.Func.ADAPTER_REQUEST_DEVICE, ComponentHostFunction.singleValue { params ->
            u32(bindings.adapterRequestDevice(paramU32(params, 0)))
        })
        define(AbiCm.Func.DEVICE_GET_QUEUE, ComponentHostFunction.singleValue { params ->
            u32(bindings.deviceGetQueue(paramU32(params, 0)))
        })
        define(AbiCm.Func.DEVICE_CREATE_BUFFER, ComponentHostFunction.singleValue { params ->
            val desc = parseBufferDescriptor(params[1])
            u32(
                bindings.deviceCreateBuffer(
                    paramU32(params, 0),
                    desc.size,
                    desc.usage,
                    desc.mappedAtCreation,
                    desc.label,
                ),
            )
        })
        define(
            AbiCm.Func.QUEUE_WRITE_BUFFER,
            ComponentHostFunction.voidFunctionWithParams { params ->
                bindings.queueWriteBuffer(
                    paramU32(params, 0),
                    paramU32(params, 1),
                    paramU64(params, 2),
                    params[3].asByteArray(),
                )
            },
        )
        define(AbiCm.Func.DEVICE_CREATE_SHADER_MODULE, ComponentHostFunction.singleValue { params ->
            u32(bindings.deviceCreateShaderModule(paramU32(params, 0), params[1].asString()))
        })
        define(
            AbiCm.Func.DEVICE_CREATE_BIND_GROUP_LAYOUT_STORAGE3,
            ComponentHostFunction.singleValue { params ->
                u32(bindings.deviceCreateBindGroupLayoutStorage3(paramU32(params, 0)))
            },
        )
        define(AbiCm.Func.DEVICE_CREATE_BIND_GROUP3, ComponentHostFunction.singleValue { params ->
            u32(
                bindings.deviceCreateBindGroup3(
                    paramU32(params, 0),
                    paramU32(params, 1),
                    paramU32(params, 2),
                    paramU32(params, 3),
                    paramU32(params, 4),
                ),
            )
        })
        define(
            AbiCm.Func.DEVICE_CREATE_COMPUTE_PIPELINE,
            ComponentHostFunction.singleValue { params ->
                u32(
                    bindings.deviceCreateComputePipeline(
                        paramU32(params, 0),
                        paramU32(params, 1),
                        paramU32(params, 2),
                        params[3].asString(),
                    ),
                )
            },
        )
        define(
            AbiCm.Func.DEVICE_CREATE_RENDER_PIPELINE_TRIANGLE,
            ComponentHostFunction.singleValue { params ->
                u32(
                    bindings.deviceCreateRenderPipelineTriangle(
                        paramU32(params, 0),
                        paramU32(params, 1),
                        paramU32(params, 2),
                    ),
                )
            },
        )
        define(
            AbiCm.Func.DEVICE_CREATE_RENDER_PIPELINE_TRIANGLE_BUFFERS,
            ComponentHostFunction.singleValue { params ->
                u32(
                    bindings.deviceCreateRenderPipelineTriangleBuffers(
                        paramU32(params, 0),
                        paramU32(params, 1),
                        paramU32(params, 2),
                        parseVertexBufferLayouts(params[3]),
                    ),
                )
            },
        )
        define(
            AbiCm.Func.DEVICE_CREATE_COMMAND_ENCODER,
            ComponentHostFunction.singleValue { params ->
                u32(bindings.deviceCreateCommandEncoder(paramU32(params, 0)))
            },
        )
        define(AbiCm.Func.SURFACE_CONFIGURE, ComponentHostFunction.singleValue { params ->
            u32(
                bindings.surfaceConfigure(
                    paramU32(params, 0),
                    paramU32(params, 1),
                    paramU32(params, 2),
                    paramU32(params, 3),
                    paramU32(params, 4),
                ),
            )
        })
        define(
            AbiCm.Func.SURFACE_GET_CURRENT_TEXTURE_VIEW,
            ComponentHostFunction.singleValue { params ->
                u32(bindings.surfaceGetCurrentTextureView(paramU32(params, 0)))
            },
        )
        define(
            AbiCm.Func.SURFACE_PRESENT,
            ComponentHostFunction.voidFunctionWithParams { params ->
                bindings.surfacePresent(paramU32(params, 0))
            },
        )
        define(
            AbiCm.Func.SURFACE_UNCONFIGURE,
            ComponentHostFunction.voidFunctionWithParams { params ->
                bindings.surfaceUnconfigure(paramU32(params, 0))
            },
        )
        define(
            AbiCm.Func.COMMAND_ENCODER_BEGIN_COMPUTE_PASS,
            ComponentHostFunction.singleValue { params ->
                u32(bindings.commandEncoderBeginComputePass(paramU32(params, 0)))
            },
        )
        define(
            AbiCm.Func.COMMAND_ENCODER_BEGIN_RENDER_PASS_CLEAR,
            ComponentHostFunction.singleValue { params ->
                u32(
                    bindings.commandEncoderBeginRenderPassClear(
                        paramU32(params, 0),
                        paramU32(params, 1),
                        params[2].asF32(),
                        params[3].asF32(),
                        params[4].asF32(),
                        params[5].asF32(),
                    ),
                )
            },
        )
        define(
            AbiCm.Func.COMPUTE_PASS_SET_PIPELINE,
            ComponentHostFunction.voidFunctionWithParams { params ->
                bindings.computePassSetPipeline(paramU32(params, 0), paramU32(params, 1))
            },
        )
        define(
            AbiCm.Func.COMPUTE_PASS_SET_BIND_GROUP,
            ComponentHostFunction.voidFunctionWithParams { params ->
                bindings.computePassSetBindGroup(
                    paramU32(params, 0),
                    paramU32(params, 1),
                    paramU32(params, 2),
                )
            },
        )
        define(
            AbiCm.Func.COMPUTE_PASS_DISPATCH_WORKGROUPS,
            ComponentHostFunction.voidFunctionWithParams { params ->
                bindings.computePassDispatchWorkgroups(
                    paramU32(params, 0),
                    paramU32(params, 1),
                    paramU32(params, 2),
                    paramU32(params, 3),
                )
            },
        )
        define(
            AbiCm.Func.COMPUTE_PASS_END,
            ComponentHostFunction.voidFunctionWithParams { params ->
                bindings.computePassEnd(paramU32(params, 0))
            },
        )
        define(
            AbiCm.Func.RENDER_PASS_SET_PIPELINE,
            ComponentHostFunction.voidFunctionWithParams { params ->
                bindings.renderPassSetPipeline(paramU32(params, 0), paramU32(params, 1))
            },
        )
        define(
            AbiCm.Func.RENDER_PASS_SET_VERTEX_BUFFER,
            ComponentHostFunction.voidFunctionWithParams { params ->
                bindings.renderPassSetVertexBuffer(
                    paramU32(params, 0),
                    paramU32(params, 1),
                    paramU32(params, 2),
                    paramU64(params, 3),
                    paramU64(params, 4),
                )
            },
        )
        define(
            AbiCm.Func.RENDER_PASS_DRAW,
            ComponentHostFunction.voidFunctionWithParams { params ->
                bindings.renderPassDraw(paramU32(params, 0), paramU32(params, 1))
            },
        )
        define(
            AbiCm.Func.RENDER_PASS_END,
            ComponentHostFunction.voidFunctionWithParams { params ->
                bindings.renderPassEnd(paramU32(params, 0))
            },
        )
        define(
            AbiCm.Func.COMMAND_ENCODER_COPY_BUFFER_TO_BUFFER,
            ComponentHostFunction.voidFunctionWithParams { params ->
                bindings.commandEncoderCopyBufferToBuffer(
                    paramU32(params, 0),
                    paramU32(params, 1),
                    paramU64(params, 2),
                    paramU32(params, 3),
                    paramU64(params, 4),
                    paramU64(params, 5),
                )
            },
        )
        define(AbiCm.Func.COMMAND_ENCODER_FINISH, ComponentHostFunction.singleValue { params ->
            u32(bindings.commandEncoderFinish(paramU32(params, 0)))
        })
        define(
            AbiCm.Func.QUEUE_SUBMIT1,
            ComponentHostFunction.voidFunctionWithParams { params ->
                bindings.queueSubmit1(paramU32(params, 0), paramU32(params, 1))
            },
        )
        define(
            AbiCm.Func.BUFFER_MAP_ASYNC,
            ComponentHostFunction.voidFunctionWithParams { params ->
                bindings.bufferMapAsync(
                    paramU32(params, 0),
                    paramU32(params, 1),
                    paramU64(params, 2),
                    paramU64(params, 3),
                )
            },
        )
        define(AbiCm.Func.BUFFER_GET_MAPPED_RANGE, ComponentHostFunction.singleValue { params ->
            val data = bindings.bufferGetMappedRange(
                paramU32(params, 0),
                paramU64(params, 1),
                paramU64(params, 2),
            )
            ComponentVal.listU8(data)
        })
        define(
            AbiCm.Func.BUFFER_UNMAP,
            ComponentHostFunction.voidFunctionWithParams { params ->
                bindings.bufferUnmap(paramU32(params, 0))
            },
        )
    }

    private data class BufferDescriptorFields(
        val size: Long,
        val usage: Int,
        val mappedAtCreation: Boolean,
        val label: String?,
    )
}
