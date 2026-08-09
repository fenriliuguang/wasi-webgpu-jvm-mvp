package io.github.fenriliuguang.wasi.webgpu.experimental.runtime.cm

import ai.tegmentum.wasmtime4j.component.ComponentVal
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
import io.github.fenriliuguang.wasi.webgpu.experimental.host.DepthStencilState
import io.github.fenriliuguang.wasi.webgpu.experimental.host.Extent3D
import io.github.fenriliuguang.wasi.webgpu.experimental.host.FragmentState
import io.github.fenriliuguang.wasi.webgpu.experimental.host.GpuHandle
import io.github.fenriliuguang.wasi.webgpu.experimental.host.HostException
import io.github.fenriliuguang.wasi.webgpu.experimental.host.PipelineLayoutDescriptor
import io.github.fenriliuguang.wasi.webgpu.experimental.host.PrimitiveState
import io.github.fenriliuguang.wasi.webgpu.experimental.host.ProgrammableStage
import io.github.fenriliuguang.wasi.webgpu.experimental.host.RenderPassColorAttachment
import io.github.fenriliuguang.wasi.webgpu.experimental.host.RenderPassDepthStencilAttachment
import io.github.fenriliuguang.wasi.webgpu.experimental.host.RenderPassDescriptor
import io.github.fenriliuguang.wasi.webgpu.experimental.host.RenderPipelineDescriptor
import io.github.fenriliuguang.wasi.webgpu.experimental.host.SamplerBindingLayout
import io.github.fenriliuguang.wasi.webgpu.experimental.host.SamplerDescriptor
import io.github.fenriliuguang.wasi.webgpu.experimental.host.TextureBindingLayout
import io.github.fenriliuguang.wasi.webgpu.experimental.host.TextureDescriptor
import io.github.fenriliuguang.wasi.webgpu.experimental.host.VertexAttribute
import io.github.fenriliuguang.wasi.webgpu.experimental.host.VertexBufferLayout
import io.github.fenriliuguang.wasi.webgpu.experimental.host.VertexState

/**
 * Shared ComponentVal → L2 descriptor parsers for experimental and wasi CM tracks.
 */
internal object CmDescriptorParsers {

    data class BufferDescriptorFields(
        val size: Long,
        val usage: Int,
        val mappedAtCreation: Boolean,
        val label: String?,
    )

    data class WriteTextureFields(
        val texture: Int,
        val data: ByteArray,
        val width: Int,
        val height: Int,
        val bytesPerRow: Int,
    )

    fun parseOptionalString(val_: ComponentVal): String? = when {
        val_.isOption -> val_.asSome().map { it.asString() }.orElse(null)
        val_.isString -> val_.asString()
        else -> null
    }

    fun parseOptionalRecord(val_: ComponentVal): ComponentVal? =
        if (val_.isOption) {
            val_.asSome().orElse(null)
        } else if (val_.isRecord) {
            val_
        } else {
            null
        }

    fun optionalU32(val_: ComponentVal, default: Int): Int =
        if (val_.isOption) {
            val_.asSome().map { asU32Compat(it) }.orElse(default)
        } else {
            asU32Compat(val_)
        }

    fun optionalU64(val_: ComponentVal, default: Long): Long =
        if (val_.isOption) {
            val_.asSome().map { it.asU64() }.orElse(default)
        } else {
            val_.asU64()
        }

    fun optionalHandle(val_: ComponentVal): Int? =
        if (val_.isOption) {
            val_.asSome().map { asU32Compat(it) }.orElse(null)
        } else {
            runCatching { asU32Compat(val_) }.getOrNull()
        }

    /** Prefer u32; fall back to enum ordinal / flags bitset when wasmtime4j presents WIT enums. */
    fun asU32Compat(val_: ComponentVal): Int {
        runCatching { return val_.asU32().toInt() }
        if (val_.isEnum) {
            // Best-effort: Dawn ordinals for common load/store ops used by L2.
            return when (val_.asEnum()) {
                "load" -> 0
                "clear" -> 1
                "store" -> 0
                "discard" -> 1
                "never" -> 0
                "less" -> 1
                "equal" -> 2
                "less-equal" -> 3
                "greater" -> 4
                "not-equal" -> 5
                "greater-equal" -> 6
                "always" -> 7
                "uniform" -> 0
                "storage" -> 1
                "read-only-storage" -> 2
                "filtering" -> 0
                "non-filtering" -> 1
                "comparison" -> 2
                "float" -> 0
                "unfilterable-float" -> 1
                "depth" -> 2
                "sint" -> 3
                "uint" -> 4
                "d1", "1d" -> 0
                "d2", "2d" -> 1
                "d3", "3d" -> 2
                "vertex" -> 0
                "instance" -> 1
                "point-list" -> 0
                "line-list" -> 1
                "line-strip" -> 2
                "triangle-list" -> 3
                "triangle-strip" -> 4
                "rgba8unorm" -> 0x16
                "depth24plus" -> 0x2e
                else -> error("unsupported enum for u32 compat: ${val_.asEnum()}")
            }
        }
        if (val_.isFlags) {
            var bits = 0
            for (name in val_.asFlags()) {
                bits = bits or when (name) {
                    "read" -> 0x0001
                    "write" -> 0x0002
                    "map-read" -> 0x0001
                    "map-write" -> 0x0002
                    "copy-src" -> 0x0004
                    "copy-dst" -> 0x0008
                    "index" -> 0x0010
                    "vertex" -> 0x0020
                    "uniform" -> 0x0040
                    "storage" -> 0x0080
                    "indirect" -> 0x0100
                    "query-resolve" -> 0x0200
                    "render-attachment" -> 0x10
                    "texture-binding" -> 0x04
                    "storage-binding" -> 0x08
                    else -> 0
                }
            }
            return bits
        }
        error("expected u32/enum/flags, got ${val_.type}")
    }

    fun parseBufferDescriptor(val_: ComponentVal): BufferDescriptorFields {
        require(val_.isRecord) { "expected buffer-descriptor record, got ${val_.type}" }
        val fields = val_.asRecord()
        val size = fields.getValue("size").asU64()
        val usage = asU32Compat(fields.getValue("usage"))
        val mappedVal = fields.getValue("mapped-at-creation")
        val mapped = when {
            mappedVal.isOption -> mappedVal.asSome().map { it.asBool() }.orElse(false)
            else -> runCatching { mappedVal.asBool() }.getOrDefault(false)
        }
        return BufferDescriptorFields(size, usage, mapped, parseOptionalString(fields.getValue("label")))
    }

    fun parseShaderModuleCode(val_: ComponentVal): String {
        if (val_.isString) return val_.asString()
        require(val_.isRecord) { "expected shader-module-descriptor record, got ${val_.type}" }
        return val_.asRecord().getValue("code").asString()
    }

    fun parseVertexAttribute(val_: ComponentVal): VertexAttribute {
        require(val_.isRecord) { "expected vertex-attribute record, got ${val_.type}" }
        val fields = val_.asRecord()
        return VertexAttribute(
            format = asU32Compat(fields.getValue("format")),
            offset = fields.getValue("offset").asU64(),
            shaderLocation = asU32Compat(fields.getValue("shader-location")),
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
            stepMode = asU32Compat(fields.getValue("step-mode")),
            attributes = attributes,
        )
    }

    fun parseVertexBufferLayouts(val_: ComponentVal): List<VertexBufferLayout> {
        val listVal = if (val_.isOption) val_.asSome().orElse(null) else val_
        if (listVal == null) return emptyList()
        require(listVal.isList) { "expected list<vertex-buffer-layout>, got ${listVal.type}" }
        return listVal.asList().mapNotNull { el ->
            val layout = el as? ComponentVal ?: return@mapNotNull null
            if (layout.isOption) {
                layout.asSome().map { parseVertexBufferLayout(it) }.orElse(null)
            } else {
                parseVertexBufferLayout(layout)
            }
        }
    }

    fun parseBufferBindingType(val_: ComponentVal): BufferBindingType {
        val inner = if (val_.isOption) val_.asSome().orElse(null) else val_
            ?: return BufferBindingType.Uniform
        if (inner.isEnum) {
            return when (inner.asEnum()) {
                "uniform" -> BufferBindingType.Uniform
                "storage" -> BufferBindingType.Storage
                "read-only-storage" -> BufferBindingType.ReadOnlyStorage
                else -> error("unknown buffer-binding-type: ${inner.asEnum()}")
            }
        }
        val ordinal = asU32Compat(inner)
        return BufferBindingType.entries.getOrNull(ordinal)
            ?: error("unknown buffer-binding-type ordinal: $ordinal")
    }

    fun parseBufferBindingLayout(val_: ComponentVal): BufferBindingLayout {
        require(val_.isRecord) { "expected buffer-binding-layout record, got ${val_.type}" }
        val fields = val_.asRecord()
        val typeField = fields["type"] ?: fields.getValue("%type")
        val hasDyn = fields.getValue("has-dynamic-offset")
        val minSize = fields.getValue("min-binding-size")
        return BufferBindingLayout(
            type = parseBufferBindingType(typeField),
            hasDynamicOffset = if (hasDyn.isOption) {
                hasDyn.asSome().map { it.asBool() }.orElse(false)
            } else {
                runCatching { hasDyn.asBool() }.getOrDefault(false)
            },
            minBindingSize = if (minSize.isOption) {
                minSize.asSome().map { it.asU64() }.orElse(0L)
            } else {
                runCatching { minSize.asU64() }.getOrDefault(0L)
            },
        )
    }

    fun parseSamplerBindingLayout(val_: ComponentVal): SamplerBindingLayout {
        require(val_.isRecord) { "expected sampler-binding-layout record, got ${val_.type}" }
        val fields = val_.asRecord()
        val typeField = fields["type"] ?: fields["%type"]
        val type = if (typeField == null) {
            0
        } else {
            asU32Compat(if (typeField.isOption) typeField.asSome().orElse(ComponentVal.u32(0)) else typeField)
        }
        return SamplerBindingLayout(type = type)
    }

    fun parseTextureBindingLayout(val_: ComponentVal): TextureBindingLayout {
        require(val_.isRecord) { "expected texture-binding-layout record, got ${val_.type}" }
        val fields = val_.asRecord()
        return TextureBindingLayout(
            sampleType = optionalU32(fields.getValue("sample-type"), 0),
            viewDimension = optionalU32(fields.getValue("view-dimension"), 1),
            multisampled = if (fields.getValue("multisampled").isOption) {
                fields.getValue("multisampled").asSome().map { it.asBool() }.orElse(false)
            } else {
                runCatching { fields.getValue("multisampled").asBool() }.getOrDefault(false)
            },
        )
    }

    fun parseBindGroupLayoutEntry(val_: ComponentVal): BindGroupLayoutEntry {
        require(val_.isRecord) { "expected bind-group-layout-entry record, got ${val_.type}" }
        val fields = val_.asRecord()
        val buffer = parseOptionalRecord(fields.getValue("buffer"))?.let { parseBufferBindingLayout(it) }
        val sampler = parseOptionalRecord(fields.getValue("sampler"))?.let { parseSamplerBindingLayout(it) }
        val texture = parseOptionalRecord(fields.getValue("texture"))?.let { parseTextureBindingLayout(it) }
        return BindGroupLayoutEntry(
            binding = asU32Compat(fields.getValue("binding")),
            visibility = asU32Compat(fields.getValue("visibility")),
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
        val offsetVal = fields.getValue("offset")
        val offset = if (offsetVal.isOption) {
            offsetVal.asSome().map { it.asU64() }.orElse(0L)
        } else {
            runCatching { offsetVal.asU64() }.getOrDefault(0L)
        }
        val sizeVal = fields.getValue("size")
        val size = if (sizeVal.isOption) {
            sizeVal.asSome().map { it.asU64() }.orElse(null)
        } else {
            runCatching { sizeVal.asU64() }.getOrNull()
        }
        return BufferBinding(
            buffer = GpuHandle(asU32Compat(fields.getValue("buffer"))),
            offset = offset,
            size = size,
        )
    }

    fun parseBindGroupEntry(val_: ComponentVal): BindGroupEntry {
        require(val_.isRecord) { "expected bind-group-entry record, got ${val_.type}" }
        val fields = val_.asRecord()
        val binding = asU32Compat(fields.getValue("binding"))
        val resourceField = fields["resource"] ?: fields["%resource"]
        if (resourceField != null && resourceField.isVariant) {
            val variant = resourceField.asVariant()
            val payload = variant.payload.orElse(null)
            val resource = when (variant.caseName) {
                "gpu-buffer-binding" -> {
                    require(payload != null) { "gpu-buffer-binding missing payload" }
                    BindingResource.Buffer(parseBufferBinding(payload))
                }
                "gpu-buffer" -> {
                    require(payload != null) { "gpu-buffer missing payload" }
                    BindingResource.Buffer(BufferBinding(GpuHandle(asU32Compat(payload))))
                }
                "gpu-sampler" -> {
                    require(payload != null) { "gpu-sampler missing payload" }
                    BindingResource.Sampler(GpuHandle(asU32Compat(payload)))
                }
                "gpu-texture-view" -> {
                    require(payload != null) { "gpu-texture-view missing payload" }
                    BindingResource.TextureView(GpuHandle(asU32Compat(payload)))
                }
                else -> error("unsupported gpu-binding-resource case: ${variant.caseName}")
            }
            return BindGroupEntry(binding = binding, resource = resource)
        }
        val bufferOpt = parseOptionalRecord(fields.getValue("buffer"))
        val samplerVal = fields.getValue("sampler")
        val viewVal = fields.getValue("texture-view")
        val sampler = if (samplerVal.isOption) {
            samplerVal.asSome().map { GpuHandle(asU32Compat(it)) }.orElse(null)
        } else {
            null
        }
        val textureView = if (viewVal.isOption) {
            viewVal.asSome().map { GpuHandle(asU32Compat(it)) }.orElse(null)
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
            layout = GpuHandle(asU32Compat(fields.getValue("layout"))),
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
        val depthField = sizeFields["depth-or-array-layers"]
        return TextureDescriptor(
            size = Extent3D(
                width = asU32Compat(sizeFields.getValue("width")),
                height = asU32Compat(sizeFields.getValue("height")),
                depthOrArrayLayers = if (depthField == null) {
                    1
                } else {
                    optionalU32(depthField, 1)
                },
            ),
            format = asU32Compat(fields.getValue("format")),
            usage = asU32Compat(fields.getValue("usage")),
            mipLevelCount = optionalU32(fields.getValue("mip-level-count"), 1),
            sampleCount = optionalU32(fields.getValue("sample-count"), 1),
            dimension = optionalU32(fields.getValue("dimension"), 1),
            label = parseOptionalString(fields.getValue("label")),
        )
    }

    fun parseSamplerDescriptor(val_: ComponentVal): SamplerDescriptor {
        require(val_.isRecord) { "expected sampler-descriptor record, got ${val_.type}" }
        val fields = val_.asRecord()
        return SamplerDescriptor(label = parseOptionalString(fields.getValue("label")))
    }

    fun parsePipelineLayoutHandle(val_: ComponentVal): GpuHandle {
        if (val_.isVariant) {
            val variant = val_.asVariant()
            return when (variant.caseName) {
                "specific" -> {
                    val payload = variant.payload.orElseThrow {
                        IllegalArgumentException("gpu-layout-mode.specific missing payload")
                    }
                    GpuHandle(asU32Compat(payload))
                }
                "auto" -> throw HostException.Unsupported("gpu-layout-mode.auto not wired")
                else -> error("unknown gpu-layout-mode: ${variant.caseName}")
            }
        }
        return GpuHandle(asU32Compat(val_))
    }

    fun parsePipelineLayoutDescriptor(val_: ComponentVal): PipelineLayoutDescriptor {
        require(val_.isRecord) { "expected pipeline-layout-descriptor record, got ${val_.type}" }
        val fields = val_.asRecord()
        val layoutsVal = fields.getValue("bind-group-layouts")
        require(layoutsVal.isList) { "expected bind-group-layouts list, got ${layoutsVal.type}" }
        val layouts = layoutsVal.asList().mapNotNull { el ->
            val item = el as? ComponentVal ?: return@mapNotNull null
            val handle = optionalHandle(item) ?: return@mapNotNull null
            GpuHandle(handle)
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
                module = GpuHandle(asU32Compat(computeFields.getValue("module"))),
                entryPoint = entry,
            ),
            layout = parsePipelineLayoutHandle(fields.getValue("layout")),
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
        val fragmentFields = when {
            fragmentVal.isOption -> {
                val some = fragmentVal.asSome().orElseThrow {
                    IllegalArgumentException("render-pipeline fragment required in this subset")
                }
                require(some.isRecord) { "expected fragment-state record" }
                some.asRecord()
            }
            fragmentVal.isRecord -> fragmentVal.asRecord()
            else -> error("expected fragment-state, got ${fragmentVal.type}")
        }
        val targetsVal = fragmentFields.getValue("targets")
        require(targetsVal.isList) { "expected targets list, got ${targetsVal.type}" }
        val targets = targetsVal.asList().mapNotNull { el ->
            val target = el as? ComponentVal ?: return@mapNotNull null
            val record = if (target.isOption) target.asSome().orElse(null) else target
            if (record == null) return@mapNotNull null
            require(record.isRecord) { "expected color-target-state record" }
            ColorTargetState(format = asU32Compat(record.asRecord().getValue("format")))
        }
        val primitive = parseOptionalRecord(fields.getValue("primitive"))?.let { prim ->
            require(prim.isRecord) { "expected primitive-state record" }
            val topologyVal = prim.asRecord()["topology"]
            PrimitiveState(
                topology = if (topologyVal == null) 3 else optionalU32(topologyVal, 3),
            )
        }
        val depthStencil = parseOptionalRecord(fields.getValue("depth-stencil"))?.let { ds ->
            require(ds.isRecord) { "expected depth-stencil-state record" }
            val dsFields = ds.asRecord()
            val writeEnabled = dsFields["depth-write-enabled"]
            val compare = dsFields["depth-compare"]
            DepthStencilState(
                format = asU32Compat(dsFields.getValue("format")),
                depthWriteEnabled = if (writeEnabled == null) {
                    true
                } else if (writeEnabled.isOption) {
                    writeEnabled.asSome().map { it.asBool() }.orElse(true)
                } else {
                    writeEnabled.asBool()
                },
                depthCompare = if (compare == null) 1 else optionalU32(compare, 1),
            )
        }
        return RenderPipelineDescriptor(
            vertex = VertexState(
                module = GpuHandle(asU32Compat(vertexFields.getValue("module"))),
                entryPoint = parseOptionalString(vertexFields.getValue("entry-point")),
                buffers = parseVertexBufferLayouts(
                    vertexFields["buffers"] ?: ComponentVal.list(emptyList()),
                ),
            ),
            fragment = FragmentState(
                module = GpuHandle(asU32Compat(fragmentFields.getValue("module"))),
                entryPoint = parseOptionalString(fragmentFields.getValue("entry-point")),
                targets = targets,
            ),
            layout = parsePipelineLayoutHandle(fields.getValue("layout")),
            primitive = primitive,
            depthStencil = depthStencil,
            label = parseOptionalString(fields.getValue("label")),
        )
    }

    fun parseRenderPassDescriptor(val_: ComponentVal): RenderPassDescriptor {
        require(val_.isRecord) { "expected render-pass-descriptor record, got ${val_.type}" }
        val fields = val_.asRecord()
        val attachmentsVal = fields.getValue("color-attachments")
        require(attachmentsVal.isList) { "expected color-attachments list, got ${attachmentsVal.type}" }
        val attachments = attachmentsVal.asList().mapNotNull { el ->
            val att = el as? ComponentVal ?: return@mapNotNull null
            val record = if (att.isOption) att.asSome().orElse(null) else att
            if (record == null) return@mapNotNull null
            require(record.isRecord) { "expected render-pass-color-attachment record" }
            val attFields = record.asRecord()
            val clearVal = attFields.getValue("clear-value")
            val clear = if (clearVal.isOption) {
                clearVal.asSome().map { parseColor(it) }.orElse(null)
            } else if (clearVal.isRecord) {
                parseColor(clearVal)
            } else {
                null
            }
            RenderPassColorAttachment(
                view = GpuHandle(asU32Compat(attFields.getValue("view"))),
                clearValue = clear,
                loadOp = asU32Compat(attFields.getValue("load-op")),
                storeOp = asU32Compat(attFields.getValue("store-op")),
            )
        }
        val depthAttachment = parseOptionalRecord(fields.getValue("depth-stencil-attachment"))?.let { depth ->
            require(depth.isRecord) { "expected render-pass-depth-stencil-attachment record" }
            val depthFields = depth.asRecord()
            val clear = depthFields["depth-clear-value"]
            val load = depthFields["depth-load-op"]
            val store = depthFields["depth-store-op"]
            RenderPassDepthStencilAttachment(
                view = GpuHandle(asU32Compat(depthFields.getValue("view"))),
                depthClearValue = if (clear == null) {
                    1f
                } else if (clear.isOption) {
                    clear.asSome().map { it.asF32() }.orElse(1f)
                } else {
                    clear.asF32()
                },
                depthLoadOp = if (load == null) 1 else optionalU32(load, 1),
                depthStoreOp = if (store == null) 0 else optionalU32(store, 0),
            )
        }
        return RenderPassDescriptor(
            colorAttachments = attachments,
            depthStencilAttachment = depthAttachment,
            label = parseOptionalString(fields.getValue("label")),
        )
    }

    fun parseCommandBufferList(val_: ComponentVal): List<Int> {
        require(val_.isList) { "expected list<command-buffer>, got ${val_.type}" }
        return val_.asList().map { el ->
            val item = el as? ComponentVal
                ?: error("expected ComponentVal command-buffer, got ${el?.javaClass}")
            asU32Compat(item)
        }
    }

    fun parseWriteTexture(
        destination: ComponentVal,
        data: ComponentVal,
        layout: ComponentVal,
        size: ComponentVal,
    ): WriteTextureFields {
        require(destination.isRecord) { "expected gpu-texel-copy-texture-info, got ${destination.type}" }
        require(layout.isRecord) { "expected gpu-texel-copy-buffer-layout, got ${layout.type}" }
        require(size.isRecord) { "expected gpu-extent3-d, got ${size.type}" }
        val destFields = destination.asRecord()
        val layoutFields = layout.asRecord()
        val sizeFields = size.asRecord()
        val width = asU32Compat(sizeFields.getValue("width"))
        val height = asU32Compat(sizeFields.getValue("height"))
        val bytes = data.asByteArray()
        val bytesPerRow = optionalU32(layoutFields.getValue("bytes-per-row"), width * 4)
        return WriteTextureFields(
            texture = asU32Compat(destFields.getValue("texture")),
            data = bytes,
            width = width,
            height = height,
            bytesPerRow = bytesPerRow,
        )
    }

    fun sliceBytes(data: ByteArray, dataOffset: Long, size: Long): ByteArray {
        val start = dataOffset.toInt().coerceIn(0, data.size)
        val end = if (size < 0) data.size else (start + size.toInt()).coerceIn(start, data.size)
        return if (start == 0 && end == data.size) data else data.copyOfRange(start, end)
    }
}
