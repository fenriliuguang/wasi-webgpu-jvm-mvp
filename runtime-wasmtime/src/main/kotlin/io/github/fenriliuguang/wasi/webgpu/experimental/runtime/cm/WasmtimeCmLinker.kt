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
import io.github.fenriliuguang.wasi.webgpu.experimental.host.BindGroupDescriptor
import io.github.fenriliuguang.wasi.webgpu.experimental.host.BindGroupEntry
import io.github.fenriliuguang.wasi.webgpu.experimental.host.BindGroupLayoutDescriptor
import io.github.fenriliuguang.wasi.webgpu.experimental.host.BindGroupLayoutEntry
import io.github.fenriliuguang.wasi.webgpu.experimental.host.BindingResource
import io.github.fenriliuguang.wasi.webgpu.experimental.host.BufferBinding
import io.github.fenriliuguang.wasi.webgpu.experimental.host.BufferBindingLayout
import io.github.fenriliuguang.wasi.webgpu.experimental.host.BufferBindingType
import io.github.fenriliuguang.wasi.webgpu.experimental.host.Color
import io.github.fenriliuguang.wasi.webgpu.experimental.host.ColorTargetState
import io.github.fenriliuguang.wasi.webgpu.experimental.host.ComputePipelineDescriptor
import io.github.fenriliuguang.wasi.webgpu.experimental.host.Extent3D
import io.github.fenriliuguang.wasi.webgpu.experimental.host.FragmentState
import io.github.fenriliuguang.wasi.webgpu.experimental.host.GpuHandle
import io.github.fenriliuguang.wasi.webgpu.experimental.host.HostException
import io.github.fenriliuguang.wasi.webgpu.experimental.host.PipelineLayoutDescriptor
import io.github.fenriliuguang.wasi.webgpu.experimental.host.PrimitiveState
import io.github.fenriliuguang.wasi.webgpu.experimental.host.ProgrammableStage
import io.github.fenriliuguang.wasi.webgpu.experimental.host.RenderPassColorAttachment
import io.github.fenriliuguang.wasi.webgpu.experimental.host.RenderPassDescriptor
import io.github.fenriliuguang.wasi.webgpu.experimental.host.RenderPipelineDescriptor
import io.github.fenriliuguang.wasi.webgpu.experimental.host.SamplerBindingLayout
import io.github.fenriliuguang.wasi.webgpu.experimental.host.SamplerDescriptor
import io.github.fenriliuguang.wasi.webgpu.experimental.host.TextureBindingLayout
import io.github.fenriliuguang.wasi.webgpu.experimental.host.TextureDescriptor
import io.github.fenriliuguang.wasi.webgpu.experimental.host.VertexAttribute
import io.github.fenriliuguang.wasi.webgpu.experimental.host.VertexBufferLayout
import io.github.fenriliuguang.wasi.webgpu.experimental.host.VertexState
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
        // With PACKAGE "experimental:webgpu-cm" + "host@0.6.0" that yields
        // "experimental:webgpu-cm/host@0.6.0" — matching defineFunction / guest import.
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
     * Stub wasi:webgpu imports. Non-result methods throw [HostException.Unsupported];
     * result-returning methods return `ComponentVal.err` with a mapped error record (slice F)
     * so Guests get WIT `result` Err instead of a trap.
     */
    private fun registerWasiImportStubs(linker: ComponentLinker<Any>) {
        val throwStub = ComponentHostFunction.singleValue { _ ->
            throw HostException.Unsupported(
                "wasi:webgpu@${AbiWasi.VERSION} import not wired yet " +
                    "(compliant-world stub; wire in later slices)",
            )
        }
        for (func in AbiWasi.Func.ALL) {
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

        fun parseOptionalString(val_: ComponentVal): String? = when {
            val_.isOption -> val_.asSome().map { it.asString() }.orElse(null)
            val_.isString -> val_.asString()
            else -> null
        }

        fun parseBufferBindingType(val_: ComponentVal): BufferBindingType {
            val ordinal = val_.asU32().toInt()
            return BufferBindingType.entries.getOrNull(ordinal)
                ?: error("unknown buffer-binding-type ordinal: $ordinal")
        }

        fun parseBufferBindingLayout(val_: ComponentVal): BufferBindingLayout {
            require(val_.isRecord) { "expected buffer-binding-layout record, got ${val_.type}" }
            val fields = val_.asRecord()
            return BufferBindingLayout(
                type = parseBufferBindingType(fields.getValue("type")),
                hasDynamicOffset = fields.getValue("has-dynamic-offset").asBool(),
                minBindingSize = fields.getValue("min-binding-size").asU64(),
            )
        }

        fun parseOptionalRecord(val_: ComponentVal): ComponentVal? =
            if (val_.isOption) {
                val_.asSome().orElse(null)
            } else if (val_.isRecord) {
                val_
            } else {
                null
            }

        fun parseSamplerBindingLayout(val_: ComponentVal): SamplerBindingLayout {
            require(val_.isRecord) { "expected sampler-binding-layout record, got ${val_.type}" }
            val fields = val_.asRecord()
            return SamplerBindingLayout(type = fields.getValue("type").asU32().toInt())
        }

        fun parseTextureBindingLayout(val_: ComponentVal): TextureBindingLayout {
            require(val_.isRecord) { "expected texture-binding-layout record, got ${val_.type}" }
            val fields = val_.asRecord()
            return TextureBindingLayout(
                sampleType = fields.getValue("sample-type").asU32().toInt(),
                viewDimension = fields.getValue("view-dimension").asU32().toInt(),
                multisampled = fields.getValue("multisampled").asBool(),
            )
        }

        fun parseBindGroupLayoutEntry(val_: ComponentVal): BindGroupLayoutEntry {
            require(val_.isRecord) { "expected bind-group-layout-entry record, got ${val_.type}" }
            val fields = val_.asRecord()
            val buffer = parseOptionalRecord(fields.getValue("buffer"))?.let { parseBufferBindingLayout(it) }
            val sampler = parseOptionalRecord(fields.getValue("sampler"))?.let { parseSamplerBindingLayout(it) }
            val texture = parseOptionalRecord(fields.getValue("texture"))?.let { parseTextureBindingLayout(it) }
            return BindGroupLayoutEntry(
                binding = fields.getValue("binding").asU32().toInt(),
                visibility = fields.getValue("visibility").asU32().toInt(),
                buffer = buffer,
                sampler = sampler,
                texture = texture,
            )
        }

        fun parseBindGroupLayoutDescriptor(val_: ComponentVal): BindGroupLayoutDescriptor {
            require(val_.isRecord) { "expected bind-group-layout-descriptor record, got ${val_.type}" }
            val fields = val_.asRecord()
            val entriesVal = fields.getValue("entries")
            require(entriesVal.isList) { "expected entries list, got ${entriesVal.type}" }
            val entries = entriesVal.asList().map { el ->
                val entry = el as? ComponentVal
                    ?: error("expected ComponentVal entry, got ${el?.javaClass}")
                parseBindGroupLayoutEntry(entry)
            }
            return BindGroupLayoutDescriptor(
                entries = entries,
                label = parseOptionalString(fields.getValue("label")),
            )
        }

        fun parseBufferBinding(val_: ComponentVal): BufferBinding {
            require(val_.isRecord) { "expected buffer-binding record, got ${val_.type}" }
            val fields = val_.asRecord()
            val sizeVal = fields.getValue("size")
            val size = if (sizeVal.isOption) {
                sizeVal.asSome().map { it.asU64() }.orElse(null)
            } else {
                runCatching { sizeVal.asU64() }.getOrNull()
            }
            return BufferBinding(
                buffer = GpuHandle(fields.getValue("buffer").asU32().toInt()),
                offset = fields.getValue("offset").asU64(),
                size = size,
            )
        }

        fun parseBindGroupEntry(val_: ComponentVal): BindGroupEntry {
            require(val_.isRecord) { "expected bind-group-entry record, got ${val_.type}" }
            val fields = val_.asRecord()
            val binding = fields.getValue("binding").asU32().toInt()
            val bufferOpt = parseOptionalRecord(fields.getValue("buffer"))
            val samplerVal = fields.getValue("sampler")
            val viewVal = fields.getValue("texture-view")
            val sampler = if (samplerVal.isOption) {
                samplerVal.asSome().map { GpuHandle(it.asU32().toInt()) }.orElse(null)
            } else {
                null
            }
            val textureView = if (viewVal.isOption) {
                viewVal.asSome().map { GpuHandle(it.asU32().toInt()) }.orElse(null)
            } else {
                null
            }
            val resource = when {
                bufferOpt != null -> BindingResource.Buffer(parseBufferBinding(bufferOpt))
                sampler != null -> BindingResource.Sampler(sampler)
                textureView != null -> BindingResource.TextureView(textureView)
                else -> error("bind-group-entry needs buffer, sampler, or texture-view")
            }
            return BindGroupEntry(binding = binding, resource = resource)
        }

        fun parseBindGroupDescriptor(val_: ComponentVal): BindGroupDescriptor {
            require(val_.isRecord) { "expected bind-group-descriptor record, got ${val_.type}" }
            val fields = val_.asRecord()
            val entriesVal = fields.getValue("entries")
            require(entriesVal.isList) { "expected entries list, got ${entriesVal.type}" }
            val entries = entriesVal.asList().map { el ->
                val entry = el as? ComponentVal
                    ?: error("expected ComponentVal entry, got ${el?.javaClass}")
                parseBindGroupEntry(entry)
            }
            return BindGroupDescriptor(
                layout = GpuHandle(fields.getValue("layout").asU32().toInt()),
                entries = entries,
                label = parseOptionalString(fields.getValue("label")),
            )
        }

        fun parseTextureDescriptor(val_: ComponentVal): TextureDescriptor {
            require(val_.isRecord) { "expected texture-descriptor record, got ${val_.type}" }
            val fields = val_.asRecord()
            val sizeVal = fields.getValue("size")
            require(sizeVal.isRecord) { "expected extent3-d record, got ${sizeVal.type}" }
            val sizeFields = sizeVal.asRecord()
            return TextureDescriptor(
                size = Extent3D(
                    width = sizeFields.getValue("width").asU32().toInt(),
                    height = sizeFields.getValue("height").asU32().toInt(),
                    depthOrArrayLayers = sizeFields.getValue("depth-or-array-layers").asU32().toInt(),
                ),
                format = fields.getValue("format").asU32().toInt(),
                usage = fields.getValue("usage").asU32().toInt(),
                mipLevelCount = fields.getValue("mip-level-count").asU32().toInt(),
                sampleCount = fields.getValue("sample-count").asU32().toInt(),
                dimension = fields.getValue("dimension").asU32().toInt(),
                label = parseOptionalString(fields.getValue("label")),
            )
        }

        fun parseSamplerDescriptor(val_: ComponentVal): SamplerDescriptor {
            require(val_.isRecord) { "expected sampler-descriptor record, got ${val_.type}" }
            val fields = val_.asRecord()
            return SamplerDescriptor(label = parseOptionalString(fields.getValue("label")))
        }

        fun parsePipelineLayoutDescriptor(val_: ComponentVal): PipelineLayoutDescriptor {
            require(val_.isRecord) { "expected pipeline-layout-descriptor record, got ${val_.type}" }
            val fields = val_.asRecord()
            val layoutsVal = fields.getValue("bind-group-layouts")
            require(layoutsVal.isList) { "expected bind-group-layouts list, got ${layoutsVal.type}" }
            val layouts = layoutsVal.asList().map { el ->
                val item = el as? ComponentVal
                    ?: error("expected ComponentVal layout, got ${el?.javaClass}")
                GpuHandle(item.asU32().toInt())
            }
            return PipelineLayoutDescriptor(
                bindGroupLayouts = layouts,
                label = parseOptionalString(fields.getValue("label")),
            )
        }

        fun parseComputePipelineDescriptor(val_: ComponentVal): ComputePipelineDescriptor {
            require(val_.isRecord) { "expected compute-pipeline-descriptor record, got ${val_.type}" }
            val fields = val_.asRecord()
            val computeVal = fields.getValue("compute")
            require(computeVal.isRecord) { "expected programmable-stage record, got ${computeVal.type}" }
            val computeFields = computeVal.asRecord()
            val entry = parseOptionalString(computeFields.getValue("entry-point"))
            return ComputePipelineDescriptor(
                compute = ProgrammableStage(
                    module = GpuHandle(computeFields.getValue("module").asU32().toInt()),
                    entryPoint = entry,
                ),
                layout = GpuHandle(fields.getValue("layout").asU32().toInt()),
                label = parseOptionalString(fields.getValue("label")),
            )
        }

        fun parseColor(val_: ComponentVal): Color {
            require(val_.isRecord) { "expected color record, got ${val_.type}" }
            val fields = val_.asRecord()
            return Color(
                r = fields.getValue("r").asF64(),
                g = fields.getValue("g").asF64(),
                b = fields.getValue("b").asF64(),
                a = fields.getValue("a").asF64(),
            )
        }

        fun parseRenderPipelineDescriptor(val_: ComponentVal): RenderPipelineDescriptor {
            require(val_.isRecord) { "expected render-pipeline-descriptor record, got ${val_.type}" }
            val fields = val_.asRecord()
            val vertexVal = fields.getValue("vertex")
            require(vertexVal.isRecord) { "expected vertex-state record, got ${vertexVal.type}" }
            val vertexFields = vertexVal.asRecord()
            val fragmentVal = fields.getValue("fragment")
            require(fragmentVal.isRecord) { "expected fragment-state record, got ${fragmentVal.type}" }
            val fragmentFields = fragmentVal.asRecord()
            val targetsVal = fragmentFields.getValue("targets")
            require(targetsVal.isList) { "expected targets list, got ${targetsVal.type}" }
            val targets = targetsVal.asList().map { el ->
                val target = el as? ComponentVal
                    ?: error("expected ComponentVal target, got ${el?.javaClass}")
                require(target.isRecord) { "expected color-target-state record" }
                ColorTargetState(format = target.asRecord().getValue("format").asU32().toInt())
            }
            val primitive = parseOptionalRecord(fields.getValue("primitive"))?.let { prim ->
                require(prim.isRecord) { "expected primitive-state record" }
                PrimitiveState(topology = prim.asRecord().getValue("topology").asU32().toInt())
            }
            return RenderPipelineDescriptor(
                vertex = VertexState(
                    module = GpuHandle(vertexFields.getValue("module").asU32().toInt()),
                    entryPoint = parseOptionalString(vertexFields.getValue("entry-point")),
                    buffers = parseVertexBufferLayouts(vertexFields.getValue("buffers")),
                ),
                fragment = FragmentState(
                    module = GpuHandle(fragmentFields.getValue("module").asU32().toInt()),
                    entryPoint = parseOptionalString(fragmentFields.getValue("entry-point")),
                    targets = targets,
                ),
                layout = GpuHandle(fields.getValue("layout").asU32().toInt()),
                primitive = primitive,
                label = parseOptionalString(fields.getValue("label")),
            )
        }

        fun parseRenderPassDescriptor(val_: ComponentVal): RenderPassDescriptor {
            require(val_.isRecord) { "expected render-pass-descriptor record, got ${val_.type}" }
            val fields = val_.asRecord()
            val attachmentsVal = fields.getValue("color-attachments")
            require(attachmentsVal.isList) { "expected color-attachments list, got ${attachmentsVal.type}" }
            val attachments = attachmentsVal.asList().map { el ->
                val att = el as? ComponentVal
                    ?: error("expected ComponentVal attachment, got ${el?.javaClass}")
                require(att.isRecord) { "expected render-pass-color-attachment record" }
                val attFields = att.asRecord()
                val clearVal = attFields.getValue("clear-value")
                val clear = if (clearVal.isOption) {
                    clearVal.asSome().map { parseColor(it) }.orElse(null)
                } else if (clearVal.isRecord) {
                    parseColor(clearVal)
                } else {
                    null
                }
                RenderPassColorAttachment(
                    view = GpuHandle(attFields.getValue("view").asU32().toInt()),
                    clearValue = clear,
                    loadOp = attFields.getValue("load-op").asU32().toInt(),
                    storeOp = attFields.getValue("store-op").asU32().toInt(),
                )
            }
            return RenderPassDescriptor(
                colorAttachments = attachments,
                label = parseOptionalString(fields.getValue("label")),
            )
        }

        fun parseCommandBufferList(val_: ComponentVal): List<Int> {
            require(val_.isList) { "expected list<command-buffer>, got ${val_.type}" }
            return val_.asList().map { el ->
                val item = el as? ComponentVal
                    ?: error("expected ComponentVal command-buffer, got ${el?.javaClass}")
                item.asU32().toInt()
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
            AbiCm.Func.DEVICE_CREATE_BIND_GROUP_LAYOUT,
            ComponentHostFunction.singleValue { params ->
                u32(
                    bindings.deviceCreateBindGroupLayout(
                        paramU32(params, 0),
                        parseBindGroupLayoutDescriptor(params[1]),
                    ),
                )
            },
        )
        define(AbiCm.Func.DEVICE_CREATE_BIND_GROUP, ComponentHostFunction.singleValue { params ->
            u32(
                bindings.deviceCreateBindGroup(
                    paramU32(params, 0),
                    parseBindGroupDescriptor(params[1]),
                ),
            )
        })
        define(AbiCm.Func.DEVICE_CREATE_TEXTURE, ComponentHostFunction.singleValue { params ->
            u32(
                bindings.deviceCreateTexture(
                    paramU32(params, 0),
                    parseTextureDescriptor(params[1]),
                ),
            )
        })
        define(AbiCm.Func.DEVICE_CREATE_SAMPLER, ComponentHostFunction.singleValue { params ->
            val descVal = params[1]
            val desc = if (descVal.isOption) {
                descVal.asSome().map { parseSamplerDescriptor(it) }.orElse(SamplerDescriptor())
            } else {
                parseSamplerDescriptor(descVal)
            }
            u32(bindings.deviceCreateSampler(paramU32(params, 0), desc))
        })
        define(
            AbiCm.Func.DEVICE_CREATE_PIPELINE_LAYOUT,
            ComponentHostFunction.singleValue { params ->
                u32(
                    bindings.deviceCreatePipelineLayout(
                        paramU32(params, 0),
                        parsePipelineLayoutDescriptor(params[1]),
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
                        parseComputePipelineDescriptor(params[1]),
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
                        parseRenderPipelineDescriptor(params[1]),
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
                        parseRenderPassDescriptor(params[1]),
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
                bindings.queueSubmit(paramU32(params, 0), parseCommandBufferList(params[1]))
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

    private data class BufferDescriptorFields(
        val size: Long,
        val usage: Int,
        val mappedAtCreation: Boolean,
        val label: String?,
    )
}
