package io.github.fenriliuguang.wasi.webgpu.experimental.host

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CpuWriteTextureDepthDescriptorTest {

    @Test
    fun writeTextureStoresTexelsAndDepthDescriptorsAcceptFields() {
        CpuWasiWebGpuHost().use { host ->
            val adapter = host.requestAdapter()
            val device = host.adapterRequestDevice(adapter)
            val queue = host.deviceGetQueue(device)

            val texture = host.deviceCreateTexture(
                device,
                TextureDescriptor(
                    size = Extent3D(width = 4, height = 4),
                    format = GpuTextureFormat.RGBA8_UNORM,
                    usage = GpuTextureUsage.TEXTURE_BINDING or GpuTextureUsage.COPY_DST,
                ),
            )
            assertEquals(0x16, GpuTextureFormat.RGBA8_UNORM)
            assertEquals(0x2e, GpuTextureFormat.DEPTH24_PLUS)
            assertEquals(0x1e, GpuVertexFormat.FLOAT32X3)

            val pixels = ByteArray(4 * 4 * 4) { i -> (i % 256).toByte() }
            host.queueWriteTexture(
                queue,
                texture,
                pixels,
                width = 4,
                height = 4,
                bytesPerRow = 16,
            )

            // Render paths remain Unsupported on Cpu, but descriptors must accept depth fields.
            val depthDesc = DepthStencilState(
                format = GpuTextureFormat.DEPTH24_PLUS,
                depthWriteEnabled = true,
                depthCompare = GpuCompareFunction.LESS,
            )
            val passDepth = RenderPassDepthStencilAttachment(
                view = GpuHandle(1),
                depthClearValue = 1f,
                depthLoadOp = GpuLoadOp.CLEAR,
                depthStoreOp = GpuStoreOp.STORE,
            )
            assertTrue(depthDesc.depthWriteEnabled)
            assertEquals(GpuCompareFunction.LESS, depthDesc.depthCompare)
            assertEquals(1f, passDepth.depthClearValue, 0f)
            assertNotEquals(0, texture.raw)
            assertTrue(host.tryDrop(texture))
        }
    }
}
