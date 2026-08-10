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
import io.github.fenriliuguang.wasi.webgpu.experimental.abiwasi.AbiWasiResults
import io.github.fenriliuguang.wasi.webgpu.experimental.host.HostException
import io.github.fenriliuguang.wasi.webgpu.experimental.host.SamplerDescriptor
import io.github.fenriliuguang.wasi.webgpu.experimental.host.WasiWebGpuHost

/**
 * L1 Wasmtime Component Model adapter: registers experimental CM host imports → [WasiWebGpuHost],
 * and dual-track wasi:webgpu@0.3.0-rc.2 (primary-path wiring + stubs).
 *
 * WIT resources are registered via [ComponentLinker.defineResource]. Host callbacks exchange
 * resource reps as u32 (L2 [io.github.fenriliuguang.wasi.webgpu.experimental.host.GpuHandle.raw]);
 * a patched wasmtime4j native maps those to ResourceAny for the Component Model ABI.
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
    private val bindings = AbiCmHostBindings(host)

    /** Same bindings registered into the Component linker (lifetime safety nets / diagnostics). */
    fun abiBindings(): AbiCmHostBindings = bindings

    fun instantiate(componentBytes: ByteArray): ComponentInstance {
        require(runtime.supportsComponentModel()) {
            "wasmtime4j runtime does not report Component Model support"
        }
        val linker: ComponentLinker<Any> = runtime.createComponentLinker(engine)
        registerExperimentalResources(linker)
        registerWasiResources(linker)
        registerExperimentalImports(linker, bindings)
        registerWasiImports(linker, bindings)
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
        val ns = AbiCm.PACKAGE
        val iface = "${AbiCm.INTERFACE}@${AbiCm.VERSION}"
        for (name in AbiCm.Resource.ALL) {
            val definition = ComponentResourceDefinition.builder<Any>(name).build()
            linker.defineResource(ns, iface, name, definition)
        }
    }

    private fun registerWasiResources(linker: ComponentLinker<Any>) {
        val ns = AbiWasi.PACKAGE
        val iface = "${AbiWasi.INTERFACE}@${AbiWasi.VERSION}"
        for (name in AbiWasi.Resource.ALL) {
            val definition = ComponentResourceDefinition.builder<Any>(name).build()
            linker.defineResource(ns, iface, name, definition)
        }
    }

    /**
     * Stub remaining wasi:webgpu imports. Skips [PRIMARY_PATH] (already wired).
     * Non-result methods throw [HostException.Unsupported]; result methods return
     * `ComponentVal.err` with a mapped error record.
     */
    private fun registerWasiImportStubs(linker: ComponentLinker<Any>) {
        val throwStub = ComponentHostFunction.singleValue { _ ->
            throw HostException.Unsupported(
                "wasi:webgpu@${AbiWasi.VERSION} import not wired yet " +
                    "(compliant-world stub; wire in later slices)",
            )
        }
        for (func in AbiWasi.Func.ALL) {
            if (func in PRIMARY_PATH) continue
            val shape = AbiWasiResults.BY_FUNC[func]
            val stub = if (shape != null) {
                ComponentHostFunction.singleValue { _ ->
                    WasiResultCodec.unsupportedResult(func, shape)
                }
            } else {
                throwStub
            }
            linker.defineFunction("${AbiWasi.IMPORT_INTERFACE}#$func", stub)
        }
    }

    /**
     * guest-descriptor-cube slice C: wire existing L2 primary path onto wasi:webgpu.
     * Same [AbiCmHostBindings] / GpuHandle space as experimental.
     */
    private fun registerWasiImports(linker: ComponentLinker<Any>, bindings: AbiCmHostBindings) {
        fun path(func: String): String = "${AbiWasi.IMPORT_INTERFACE}#$func"

        fun define(name: String, impl: ComponentHostFunction) {
            linker.defineFunction(path(name), impl)
        }

        fun u32(v: Int): ComponentVal = ComponentVal.u32(Integer.toUnsignedLong(v))

        fun paramU32(params: List<ComponentVal>, i: Int): Int =
            CmDescriptorParsers.asU32Compat(params[i])

        fun paramU64(params: List<ComponentVal>, i: Int): Long = params[i].asU64()

        fun resultOk(): ComponentVal = WasiResultCodec.ok()

        fun resultOkU32(v: Int): ComponentVal = WasiResultCodec.ok(u32(v))

        fun resultOkBytes(data: ByteArray): ComponentVal =
            WasiResultCodec.ok(ComponentVal.listU8(data))

        fun catchResult(
            func: String,
            shape: AbiWasiResults.ErrorShape,
            block: () -> ComponentVal,
        ): ComponentVal =
            try {
                block()
            } catch (ex: HostException) {
                WasiResultCodec.errFromHostException(func, shape, ex)
            }

        define(AbiWasi.Func.GPU_REQUEST_ADAPTER, ComponentHostFunction.singleValue {
            ComponentVal.some(u32(bindings.requestAdapter()))
        })
        define(AbiWasi.Func.GPU_ADAPTER_REQUEST_DEVICE, ComponentHostFunction.singleValue { params ->
            catchResult(
                AbiWasi.Func.GPU_ADAPTER_REQUEST_DEVICE,
                AbiWasiResults.ErrorShape.RequestDevice,
            ) {
                resultOkU32(bindings.adapterRequestDevice(paramU32(params, 0)))
            }
        })
        define(AbiWasi.Func.GPU_DEVICE_QUEUE, ComponentHostFunction.singleValue { params ->
            u32(bindings.deviceGetQueue(paramU32(params, 0)))
        })
        define(AbiWasi.Func.GPU_DEVICE_CREATE_BUFFER, ComponentHostFunction.singleValue { params ->
            val desc = CmDescriptorParsers.parseBufferDescriptor(params[1])
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
        define(AbiWasi.Func.GPU_DEVICE_CREATE_SHADER_MODULE, ComponentHostFunction.singleValue { params ->
            u32(
                bindings.deviceCreateShaderModule(
                    paramU32(params, 0),
                    CmDescriptorParsers.parseShaderModuleCode(params[1]),
                ),
            )
        })
        define(
            AbiWasi.Func.GPU_DEVICE_CREATE_BIND_GROUP_LAYOUT,
            ComponentHostFunction.singleValue { params ->
                u32(
                    bindings.deviceCreateBindGroupLayout(
                        paramU32(params, 0),
                        CmDescriptorParsers.parseBindGroupLayoutDescriptor(params[1]),
                    ),
                )
            },
        )
        define(AbiWasi.Func.GPU_DEVICE_CREATE_BIND_GROUP, ComponentHostFunction.singleValue { params ->
            u32(
                bindings.deviceCreateBindGroup(
                    paramU32(params, 0),
                    CmDescriptorParsers.parseBindGroupDescriptor(params[1]),
                ),
            )
        })
        define(
            AbiWasi.Func.GPU_DEVICE_CREATE_PIPELINE_LAYOUT,
            ComponentHostFunction.singleValue { params ->
                u32(
                    bindings.deviceCreatePipelineLayout(
                        paramU32(params, 0),
                        CmDescriptorParsers.parsePipelineLayoutDescriptor(params[1]),
                    ),
                )
            },
        )
        define(
            AbiWasi.Func.GPU_DEVICE_CREATE_COMPUTE_PIPELINE,
            ComponentHostFunction.singleValue { params ->
                u32(
                    bindings.deviceCreateComputePipeline(
                        paramU32(params, 0),
                        CmDescriptorParsers.parseComputePipelineDescriptor(params[1]),
                    ),
                )
            },
        )
        define(
            AbiWasi.Func.GPU_DEVICE_CREATE_RENDER_PIPELINE,
            ComponentHostFunction.singleValue { params ->
                u32(
                    bindings.deviceCreateRenderPipeline(
                        paramU32(params, 0),
                        CmDescriptorParsers.parseRenderPipelineDescriptor(params[1]),
                    ),
                )
            },
        )
        define(AbiWasi.Func.GPU_DEVICE_CREATE_TEXTURE, ComponentHostFunction.singleValue { params ->
            u32(
                bindings.deviceCreateTexture(
                    paramU32(params, 0),
                    CmDescriptorParsers.parseTextureDescriptor(params[1]),
                ),
            )
        })
        define(AbiWasi.Func.GPU_DEVICE_CREATE_SAMPLER, ComponentHostFunction.singleValue { params ->
            val descVal = params[1]
            val desc = if (descVal.isOption) {
                descVal.asSome().map { CmDescriptorParsers.parseSamplerDescriptor(it) }
                    .orElse(SamplerDescriptor())
            } else {
                CmDescriptorParsers.parseSamplerDescriptor(descVal)
            }
            u32(bindings.deviceCreateSampler(paramU32(params, 0), desc))
        })
        define(
            AbiWasi.Func.GPU_DEVICE_CREATE_COMMAND_ENCODER,
            ComponentHostFunction.singleValue { params ->
                u32(bindings.deviceCreateCommandEncoder(paramU32(params, 0)))
            },
        )
        define(AbiWasi.Func.GPU_TEXTURE_CREATE_VIEW, ComponentHostFunction.singleValue { params ->
            u32(bindings.textureCreateView(paramU32(params, 0)))
        })
        define(
            AbiWasi.Func.GPU_QUEUE_SUBMIT,
            ComponentHostFunction.voidFunctionWithParams { params ->
                bindings.queueSubmit(
                    paramU32(params, 0),
                    CmDescriptorParsers.parseCommandBufferList(params[1]),
                )
            },
        )
        define(
            AbiWasi.Func.GPU_QUEUE_WRITE_BUFFER_WITH_COPY,
            ComponentHostFunction.singleValue { params ->
                catchResult(
                    AbiWasi.Func.GPU_QUEUE_WRITE_BUFFER_WITH_COPY,
                    AbiWasiResults.ErrorShape.WriteBuffer,
                ) {
                    val raw = params[3].asByteArray()
                    val dataOffset = CmDescriptorParsers.optionalU64(params[4], 0L)
                    val size = CmDescriptorParsers.optionalU64(
                        params[5],
                        (raw.size - dataOffset.toInt()).toLong().coerceAtLeast(0L),
                    )
                    bindings.queueWriteBuffer(
                        paramU32(params, 0),
                        paramU32(params, 1),
                        paramU64(params, 2),
                        CmDescriptorParsers.sliceBytes(raw, dataOffset, size),
                    )
                    resultOk()
                }
            },
        )
        define(
            AbiWasi.Func.GPU_QUEUE_WRITE_TEXTURE_WITH_COPY,
            ComponentHostFunction.voidFunctionWithParams { params ->
                val fields = CmDescriptorParsers.parseWriteTexture(
                    params[1],
                    params[2],
                    params[3],
                    params[4],
                )
                bindings.queueWriteTexture(
                    paramU32(params, 0),
                    fields.texture,
                    fields.data,
                    fields.width,
                    fields.height,
                    fields.bytesPerRow,
                )
            },
        )
        define(
            AbiWasi.Func.GPU_COMMAND_ENCODER_BEGIN_COMPUTE_PASS,
            ComponentHostFunction.singleValue { params ->
                u32(bindings.commandEncoderBeginComputePass(paramU32(params, 0)))
            },
        )
        define(
            AbiWasi.Func.GPU_COMMAND_ENCODER_BEGIN_RENDER_PASS,
            ComponentHostFunction.singleValue { params ->
                u32(
                    bindings.commandEncoderBeginRenderPass(
                        paramU32(params, 0),
                        CmDescriptorParsers.parseRenderPassDescriptor(params[1]),
                    ),
                )
            },
        )
        define(
            AbiWasi.Func.GPU_COMMAND_ENCODER_COPY_BUFFER_TO_BUFFER,
            ComponentHostFunction.voidFunctionWithParams { params ->
                bindings.commandEncoderCopyBufferToBuffer(
                    paramU32(params, 0),
                    paramU32(params, 1),
                    CmDescriptorParsers.optionalU64(params[2], 0L),
                    paramU32(params, 3),
                    CmDescriptorParsers.optionalU64(params[4], 0L),
                    CmDescriptorParsers.optionalU64(params[5], 0L),
                )
            },
        )
        define(AbiWasi.Func.GPU_COMMAND_ENCODER_FINISH, ComponentHostFunction.singleValue { params ->
            u32(bindings.commandEncoderFinish(paramU32(params, 0)))
        })
        define(
            AbiWasi.Func.GPU_COMPUTE_PASS_ENCODER_SET_PIPELINE,
            ComponentHostFunction.voidFunctionWithParams { params ->
                bindings.computePassSetPipeline(paramU32(params, 0), paramU32(params, 1))
            },
        )
        define(
            AbiWasi.Func.GPU_COMPUTE_PASS_ENCODER_SET_BIND_GROUP,
            ComponentHostFunction.singleValue { params ->
                catchResult(
                    AbiWasi.Func.GPU_COMPUTE_PASS_ENCODER_SET_BIND_GROUP,
                    AbiWasiResults.ErrorShape.SetBindGroup,
                ) {
                    val bindGroup = CmDescriptorParsers.optionalHandle(params[2])
                        ?: throw HostException.Validation("set-bind-group: bind-group is none")
                    bindings.computePassSetBindGroup(
                        paramU32(params, 0),
                        paramU32(params, 1),
                        bindGroup,
                    )
                    resultOk()
                }
            },
        )
        define(
            AbiWasi.Func.GPU_COMPUTE_PASS_ENCODER_DISPATCH_WORKGROUPS,
            ComponentHostFunction.voidFunctionWithParams { params ->
                bindings.computePassDispatchWorkgroups(
                    paramU32(params, 0),
                    paramU32(params, 1),
                    CmDescriptorParsers.optionalU32(params[2], 1),
                    CmDescriptorParsers.optionalU32(params[3], 1),
                )
            },
        )
        define(
            AbiWasi.Func.GPU_COMPUTE_PASS_ENCODER_END,
            ComponentHostFunction.voidFunctionWithParams { params ->
                bindings.computePassEnd(paramU32(params, 0))
            },
        )
        define(
            AbiWasi.Func.GPU_RENDER_PASS_ENCODER_SET_PIPELINE,
            ComponentHostFunction.voidFunctionWithParams { params ->
                bindings.renderPassSetPipeline(paramU32(params, 0), paramU32(params, 1))
            },
        )
        define(
            AbiWasi.Func.GPU_RENDER_PASS_ENCODER_SET_BIND_GROUP,
            ComponentHostFunction.singleValue { params ->
                catchResult(
                    AbiWasi.Func.GPU_RENDER_PASS_ENCODER_SET_BIND_GROUP,
                    AbiWasiResults.ErrorShape.SetBindGroup,
                ) {
                    val bindGroup = CmDescriptorParsers.optionalHandle(params[2])
                        ?: throw HostException.Validation("set-bind-group: bind-group is none")
                    bindings.renderPassSetBindGroup(
                        paramU32(params, 0),
                        paramU32(params, 1),
                        bindGroup,
                    )
                    resultOk()
                }
            },
        )
        define(
            AbiWasi.Func.GPU_RENDER_PASS_ENCODER_SET_VERTEX_BUFFER,
            ComponentHostFunction.voidFunctionWithParams { params ->
                val buffer = CmDescriptorParsers.optionalHandle(params[2])
                    ?: throw HostException.Validation("set-vertex-buffer: buffer is none")
                bindings.renderPassSetVertexBuffer(
                    paramU32(params, 0),
                    paramU32(params, 1),
                    buffer,
                    CmDescriptorParsers.optionalU64(params[3], 0L),
                    CmDescriptorParsers.optionalU64(params[4], 0L),
                )
            },
        )
        define(
            AbiWasi.Func.GPU_RENDER_PASS_ENCODER_DRAW,
            ComponentHostFunction.voidFunctionWithParams { params ->
                bindings.renderPassDraw(paramU32(params, 0), paramU32(params, 1))
            },
        )
        define(
            AbiWasi.Func.GPU_RENDER_PASS_ENCODER_END,
            ComponentHostFunction.voidFunctionWithParams { params ->
                bindings.renderPassEnd(paramU32(params, 0))
            },
        )
        define(AbiWasi.Func.GPU_BUFFER_MAP_ASYNC, ComponentHostFunction.singleValue { params ->
            catchResult(
                AbiWasi.Func.GPU_BUFFER_MAP_ASYNC,
                AbiWasiResults.ErrorShape.MapAsync,
            ) {
                bindings.bufferMapAsync(
                    paramU32(params, 0),
                    paramU32(params, 1),
                    CmDescriptorParsers.optionalU64(params[2], 0L),
                    CmDescriptorParsers.optionalU64(params[3], 0L),
                )
                resultOk()
            }
        })
        define(
            AbiWasi.Func.GPU_BUFFER_GET_MAPPED_RANGE_GET_WITH_COPY,
            ComponentHostFunction.singleValue { params ->
                catchResult(
                    AbiWasi.Func.GPU_BUFFER_GET_MAPPED_RANGE_GET_WITH_COPY,
                    AbiWasiResults.ErrorShape.GetMappedRange,
                ) {
                    val data = bindings.bufferGetMappedRange(
                        paramU32(params, 0),
                        CmDescriptorParsers.optionalU64(params[1], 0L),
                        CmDescriptorParsers.optionalU64(params[2], 0L),
                    )
                    resultOkBytes(data)
                }
            },
        )
        define(AbiWasi.Func.GPU_BUFFER_UNMAP, ComponentHostFunction.singleValue { params ->
            catchResult(
                AbiWasi.Func.GPU_BUFFER_UNMAP,
                AbiWasiResults.ErrorShape.Unmap,
            ) {
                bindings.bufferUnmap(paramU32(params, 0))
                resultOk()
            }
        })
    }

    private fun registerExperimentalImports(linker: ComponentLinker<Any>, bindings: AbiCmHostBindings) {
        fun path(func: String): String = "${AbiCm.IMPORT_INTERFACE}#$func"

        fun define(name: String, impl: ComponentHostFunction) {
            linker.defineFunction(path(name), impl)
        }

        fun u32(v: Int): ComponentVal = ComponentVal.u32(Integer.toUnsignedLong(v))

        fun paramU32(params: List<ComponentVal>, i: Int): Int = params[i].asU32().toInt()

        fun paramU64(params: List<ComponentVal>, i: Int): Long = params[i].asU64()

        define(AbiCm.Func.REQUEST_ADAPTER, ComponentHostFunction.singleValue {
            u32(bindings.requestAdapter())
        })
        define(
            AbiCm.Func.CREATE_SURFACE_FROM_NATIVE_WINDOW,
            ComponentHostFunction.singleValue { params ->
                require(params.isNotEmpty()) { "create-surface-from-native-window: missing window-handle" }
                val handleVal = params[0]
                val windowHandle = paramU64(params, 0)
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
            val desc = CmDescriptorParsers.parseBufferDescriptor(params[1])
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
        define(
            AbiCm.Func.QUEUE_WRITE_TEXTURE,
            ComponentHostFunction.voidFunctionWithParams { params ->
                bindings.queueWriteTexture(
                    paramU32(params, 0),
                    paramU32(params, 1),
                    params[2].asByteArray(),
                    paramU32(params, 3),
                    paramU32(params, 4),
                    paramU32(params, 5),
                )
            },
        )
        define(AbiCm.Func.DEVICE_CREATE_SHADER_MODULE, ComponentHostFunction.singleValue { params ->
            u32(bindings.deviceCreateShaderModule(paramU32(params, 0), params[1].asString()))
        })
        define(
            AbiCm.Func.DEVICE_CREATE_BIND_GROUP_LAYOUT,
            ComponentHostFunction.singleValue { params ->
                u32(
                    bindings.deviceCreateBindGroupLayout(
                        paramU32(params, 0),
                        CmDescriptorParsers.parseBindGroupLayoutDescriptor(params[1]),
                    ),
                )
            },
        )
        define(AbiCm.Func.DEVICE_CREATE_BIND_GROUP, ComponentHostFunction.singleValue { params ->
            u32(
                bindings.deviceCreateBindGroup(
                    paramU32(params, 0),
                    CmDescriptorParsers.parseBindGroupDescriptor(params[1]),
                ),
            )
        })
        define(AbiCm.Func.DEVICE_CREATE_TEXTURE, ComponentHostFunction.singleValue { params ->
            u32(
                bindings.deviceCreateTexture(
                    paramU32(params, 0),
                    CmDescriptorParsers.parseTextureDescriptor(params[1]),
                ),
            )
        })
        define(AbiCm.Func.DEVICE_CREATE_SAMPLER, ComponentHostFunction.singleValue { params ->
            val descVal = params[1]
            val desc = if (descVal.isOption) {
                descVal.asSome().map { CmDescriptorParsers.parseSamplerDescriptor(it) }
                    .orElse(SamplerDescriptor())
            } else {
                CmDescriptorParsers.parseSamplerDescriptor(descVal)
            }
            u32(bindings.deviceCreateSampler(paramU32(params, 0), desc))
        })
        define(
            AbiCm.Func.DEVICE_CREATE_PIPELINE_LAYOUT,
            ComponentHostFunction.singleValue { params ->
                u32(
                    bindings.deviceCreatePipelineLayout(
                        paramU32(params, 0),
                        CmDescriptorParsers.parsePipelineLayoutDescriptor(params[1]),
                    ),
                )
            },
        )
        define(
            AbiCm.Func.DEVICE_CREATE_COMPUTE_PIPELINE,
            ComponentHostFunction.singleValue { params ->
                u32(
                    bindings.deviceCreateComputePipeline(
                        paramU32(params, 0),
                        CmDescriptorParsers.parseComputePipelineDescriptor(params[1]),
                    ),
                )
            },
        )
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
            AbiCm.Func.DEVICE_CREATE_COMPUTE_PIPELINE_BGL,
            ComponentHostFunction.singleValue { params ->
                u32(
                    bindings.deviceCreateComputePipelineBgl(
                        paramU32(params, 0),
                        paramU32(params, 1),
                        paramU32(params, 2),
                        params[3].asString(),
                    ),
                )
            },
        )
        define(
            AbiCm.Func.DEVICE_CREATE_RENDER_PIPELINE,
            ComponentHostFunction.singleValue { params ->
                u32(
                    bindings.deviceCreateRenderPipeline(
                        paramU32(params, 0),
                        CmDescriptorParsers.parseRenderPipelineDescriptor(params[1]),
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
                        CmDescriptorParsers.parseVertexBufferLayouts(params[3]),
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
        define(AbiCm.Func.TEXTURE_CREATE_VIEW, ComponentHostFunction.singleValue { params ->
            u32(bindings.textureCreateView(paramU32(params, 0)))
        })
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
            AbiCm.Func.COMMAND_ENCODER_BEGIN_RENDER_PASS,
            ComponentHostFunction.singleValue { params ->
                u32(
                    bindings.commandEncoderBeginRenderPass(
                        paramU32(params, 0),
                        CmDescriptorParsers.parseRenderPassDescriptor(params[1]),
                    ),
                )
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
            AbiCm.Func.RENDER_PASS_SET_BIND_GROUP,
            ComponentHostFunction.voidFunctionWithParams { params ->
                bindings.renderPassSetBindGroup(
                    paramU32(params, 0),
                    paramU32(params, 1),
                    paramU32(params, 2),
                )
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
            AbiCm.Func.QUEUE_SUBMIT,
            ComponentHostFunction.voidFunctionWithParams { params ->
                bindings.queueSubmit(
                    paramU32(params, 0),
                    CmDescriptorParsers.parseCommandBufferList(params[1]),
                )
            },
        )
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

    companion object {
        /**
         * wasi:webgpu primary-path funcs wired in guest-descriptor-cube slice C.
         * [registerWasiImportStubs] skips these names.
         */
        val PRIMARY_PATH: Set<String> = setOf(
            AbiWasi.Func.GPU_REQUEST_ADAPTER,
            AbiWasi.Func.GPU_ADAPTER_REQUEST_DEVICE,
            AbiWasi.Func.GPU_DEVICE_QUEUE,
            AbiWasi.Func.GPU_DEVICE_CREATE_BUFFER,
            AbiWasi.Func.GPU_DEVICE_CREATE_SHADER_MODULE,
            AbiWasi.Func.GPU_DEVICE_CREATE_BIND_GROUP_LAYOUT,
            AbiWasi.Func.GPU_DEVICE_CREATE_BIND_GROUP,
            AbiWasi.Func.GPU_DEVICE_CREATE_PIPELINE_LAYOUT,
            AbiWasi.Func.GPU_DEVICE_CREATE_COMPUTE_PIPELINE,
            AbiWasi.Func.GPU_DEVICE_CREATE_RENDER_PIPELINE,
            AbiWasi.Func.GPU_DEVICE_CREATE_TEXTURE,
            AbiWasi.Func.GPU_DEVICE_CREATE_SAMPLER,
            AbiWasi.Func.GPU_DEVICE_CREATE_COMMAND_ENCODER,
            AbiWasi.Func.GPU_TEXTURE_CREATE_VIEW,
            AbiWasi.Func.GPU_QUEUE_SUBMIT,
            AbiWasi.Func.GPU_QUEUE_WRITE_BUFFER_WITH_COPY,
            AbiWasi.Func.GPU_QUEUE_WRITE_TEXTURE_WITH_COPY,
            AbiWasi.Func.GPU_COMMAND_ENCODER_BEGIN_COMPUTE_PASS,
            AbiWasi.Func.GPU_COMMAND_ENCODER_BEGIN_RENDER_PASS,
            AbiWasi.Func.GPU_COMMAND_ENCODER_COPY_BUFFER_TO_BUFFER,
            AbiWasi.Func.GPU_COMMAND_ENCODER_FINISH,
            AbiWasi.Func.GPU_COMPUTE_PASS_ENCODER_SET_PIPELINE,
            AbiWasi.Func.GPU_COMPUTE_PASS_ENCODER_SET_BIND_GROUP,
            AbiWasi.Func.GPU_COMPUTE_PASS_ENCODER_DISPATCH_WORKGROUPS,
            AbiWasi.Func.GPU_COMPUTE_PASS_ENCODER_END,
            AbiWasi.Func.GPU_RENDER_PASS_ENCODER_SET_PIPELINE,
            AbiWasi.Func.GPU_RENDER_PASS_ENCODER_SET_BIND_GROUP,
            AbiWasi.Func.GPU_RENDER_PASS_ENCODER_SET_VERTEX_BUFFER,
            AbiWasi.Func.GPU_RENDER_PASS_ENCODER_DRAW,
            AbiWasi.Func.GPU_RENDER_PASS_ENCODER_END,
            AbiWasi.Func.GPU_BUFFER_MAP_ASYNC,
            AbiWasi.Func.GPU_BUFFER_GET_MAPPED_RANGE_GET_WITH_COPY,
            AbiWasi.Func.GPU_BUFFER_UNMAP,
        )
    }
}
