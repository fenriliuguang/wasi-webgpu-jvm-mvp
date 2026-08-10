package io.github.fenriliuguang.wasi.webgpu.experimental.abi

import io.github.fenriliuguang.wasi.webgpu.experimental.host.CpuWasiWebGpuHost
import io.github.fenriliuguang.wasi.webgpu.experimental.host.GpuBufferUsage
import io.github.fenriliuguang.wasi.webgpu.experimental.host.GpuTextureFormat
import io.github.fenriliuguang.wasi.webgpu.experimental.host.GpuTextureUsage
import io.github.fenriliuguang.wasi.webgpu.experimental.host.ResourceKind
import io.github.fenriliuguang.wasi.webgpu.experimental.host.VectorAddScenario
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AbiMvpHostBindingsTest {

    @Test
    fun flattenedImportsRunVectorAdd() {
        val heap = ByteArray(64 * 1024)
        val memory = object : GuestMemory {
            override fun readBytes(ptr: Int, len: Int): ByteArray = heap.copyOfRange(ptr, ptr + len)
            override fun writeBytes(ptr: Int, data: ByteArray) {
                System.arraycopy(data, 0, heap, ptr, data.size)
            }
        }

        CpuWasiWebGpuHost().use { host ->
            val abi = AbiMvpHostBindings(host) { memory }
            val shaderBytes = VectorAddScenario.SHADER.toByteArray(Charsets.UTF_8)
            val entryBytes = "main".toByteArray(Charsets.UTF_8)
            val a = floatArrayOf(1f, 2f, 3f, 4f)
            val b = floatArrayOf(10f, 20f, 30f, 40f)
            val aBytes = VectorAddScenario.floatsToBytes(a)
            val bBytes = VectorAddScenario.floatsToBytes(b)

            var cursor = 0
            fun place(data: ByteArray): Int {
                val ptr = cursor
                System.arraycopy(data, 0, heap, ptr, data.size)
                cursor += data.size
                return ptr
            }

            val shaderPtr = place(shaderBytes)
            val entryPtr = place(entryBytes)
            val aPtr = place(aBytes)
            val bPtr = place(bBytes)
            val outPtr = cursor
            cursor += aBytes.size

            val adapter = abi.requestAdapter()
            val device = abi.adapterRequestDevice(adapter)
            val queue = abi.deviceGetQueue(device)
            val usage = GpuBufferUsage.STORAGE or GpuBufferUsage.COPY_DST or GpuBufferUsage.COPY_SRC
            val bufA = abi.deviceCreateBuffer(device, aBytes.size, usage)
            val bufB = abi.deviceCreateBuffer(device, bBytes.size, usage)
            val bufOut = abi.deviceCreateBuffer(device, aBytes.size, usage)
            val bufRead = abi.deviceCreateBuffer(
                device,
                aBytes.size,
                GpuBufferUsage.MAP_READ or GpuBufferUsage.COPY_DST,
            )
            abi.queueWriteBuffer(queue, bufA, 0, aPtr, aBytes.size)
            abi.queueWriteBuffer(queue, bufB, 0, bPtr, bBytes.size)
            val shader = abi.deviceCreateShaderModule(device, shaderPtr, shaderBytes.size)
            val layout = abi.deviceCreateBindGroupLayoutStorage3(device)
            val bindGroup = abi.deviceCreateBindGroup3(device, layout, bufA, bufB, bufOut)
            val pipeline = abi.deviceCreateComputePipeline(
                device,
                layout,
                shader,
                entryPtr,
                entryBytes.size,
            )
            val encoder = abi.deviceCreateCommandEncoder(device)
            val pass = abi.commandEncoderBeginComputePass(encoder)
            abi.computePassSetPipeline(pass, pipeline)
            abi.computePassSetBindGroup(pass, 0, bindGroup)
            abi.computePassDispatch(pass, 1, 1, 1)
            abi.computePassEnd(pass)
            abi.commandEncoderCopyBufferToBuffer(encoder, bufOut, 0, bufRead, 0, aBytes.size)
            val cmd = abi.commandEncoderFinish(encoder)
            abi.queueSubmit1(queue, cmd)
            abi.bufferMapRead(bufRead, 0, aBytes.size)
            abi.bufferGetMappedRange(bufRead, 0, aBytes.size, outPtr)
            abi.bufferUnmap(bufRead)

            val actual = VectorAddScenario.bytesToFloats(heap.copyOfRange(outPtr, outPtr + aBytes.size))
            assertArrayEquals(floatArrayOf(11f, 22f, 33f, 44f), actual, 1e-5f)
            assertEquals(AbiMvp.MODULE, "wasi-webgpu-mvp")
        }
    }

    @Test
    fun flattenedSurfaceMultiFrameDoesNotAccumulateSwapchainHandles() {
        CpuWasiWebGpuHost().use { host ->
            val abi = AbiMvpHostBindings(host) { error("no guest memory") }
            val adapter = abi.requestAdapter()
            val device = abi.adapterRequestDevice(adapter)
            val surface = abi.createSurfaceFromNativeWindow(0xDEADL)
            val format = abi.surfaceConfigure(surface, device, adapter, 64, 64)
            assertEquals(GpuTextureFormat.RGBA8_UNORM, format)

            val baseline = host.handleCount()
            repeat(60) {
                val view = abi.surfaceGetCurrentTextureView(surface)
                assertTrue(view > 0)
                assertEquals(1, abi.trackedFramePairCount())
                abi.surfacePresent(surface)
                assertEquals(0, abi.trackedFramePairCount())
            }

            assertEquals(baseline, host.handleCount())
            assertEquals(0, host.handleCount(ResourceKind.Texture))
            assertEquals(0, host.handleCount(ResourceKind.TextureView))
        }
    }

    @Test
    fun flattenedRenderMainChainOnCpuHost() {
        val heap = ByteArray(4 * 1024)
        val memory = object : GuestMemory {
            override fun readBytes(ptr: Int, len: Int): ByteArray = heap.copyOfRange(ptr, ptr + len)
            override fun writeBytes(ptr: Int, data: ByteArray) {
                System.arraycopy(data, 0, heap, ptr, data.size)
            }
        }

        CpuWasiWebGpuHost().use { host ->
            val abi = AbiMvpHostBindings(host) { memory }
            val shaderBytes = "@vertex fn vs_main() -> @builtin(position) vec4f { return vec4f(0.0); }".toByteArray()
            System.arraycopy(shaderBytes, 0, heap, 0, shaderBytes.size)
            val texels = ByteArray(4) { 0xFF.toByte() }
            System.arraycopy(texels, 0, heap, 256, texels.size)

            val adapter = abi.requestAdapter()
            val device = abi.adapterRequestDevice(adapter)
            val queue = abi.deviceGetQueue(device)
            val surface = abi.createSurfaceFromNativeWindow(0xBEEFL)
            val format = abi.surfaceConfigure(surface, device, adapter, 32, 32)
            val shader = abi.deviceCreateShaderModule(device, 0, shaderBytes.size)
            val pipeline = abi.deviceCreateRenderPipelineTriangle(device, shader, format)
            assertTrue(pipeline > 0)

            val albedo = abi.deviceCreateTexture2d(
                device,
                1,
                1,
                GpuTextureFormat.RGBA8_UNORM,
                GpuTextureUsage.COPY_DST or GpuTextureUsage.TEXTURE_BINDING,
            )
            abi.queueWriteTexture(queue, albedo, 256, texels.size, 1, 1, 4)
            val depth = abi.deviceCreateTexture2d(
                device,
                32,
                32,
                GpuTextureFormat.DEPTH24_PLUS,
                GpuTextureUsage.RENDER_ATTACHMENT,
            )
            val depthView = abi.textureCreateView(depth)
            assertTrue(depthView > 0)

            val colorView = abi.surfaceGetCurrentTextureView(surface)
            val encoder = abi.deviceCreateCommandEncoder(device)
            val pass = abi.commandEncoderBeginRenderPassColorDepth(
                encoder,
                colorView,
                depthView,
                0.1f,
                0.2f,
                0.3f,
                1f,
            )
            abi.renderPassSetPipeline(pass, pipeline)
            abi.renderPassDraw(pass, 3)
            abi.renderPassEnd(pass)
            val cmd = abi.commandEncoderFinish(encoder)
            abi.queueSubmit1(queue, cmd)
            abi.surfacePresent(surface)
            assertEquals(0, abi.trackedFramePairCount())

            // Color-only clear path still wires.
            val view2 = abi.surfaceGetCurrentTextureView(surface)
            val encoder2 = abi.deviceCreateCommandEncoder(device)
            val pass2 = abi.commandEncoderBeginRenderPassClear(encoder2, view2, 0f, 0f, 0f, 1f)
            abi.renderPassSetPipeline(pass2, pipeline)
            abi.renderPassEnd(pass2)
            abi.queueSubmit1(queue, abi.commandEncoderFinish(encoder2))
            abi.surfaceUnconfigure(surface)
            assertEquals(0, abi.trackedFramePairCount())
        }
    }
}
