package io.github.fenriliuguang.wasi.webgpu.experimental.abicm

import io.github.fenriliuguang.wasi.webgpu.experimental.host.CpuWasiWebGpuHost
import io.github.fenriliuguang.wasi.webgpu.experimental.host.Color
import io.github.fenriliuguang.wasi.webgpu.experimental.host.GpuBufferUsage
import io.github.fenriliuguang.wasi.webgpu.experimental.host.GpuHandle
import io.github.fenriliuguang.wasi.webgpu.experimental.host.GpuMapMode
import io.github.fenriliuguang.wasi.webgpu.experimental.host.RenderPassColorAttachment
import io.github.fenriliuguang.wasi.webgpu.experimental.host.RenderPassDescriptor
import io.github.fenriliuguang.wasi.webgpu.experimental.host.ResourceKind
import io.github.fenriliuguang.wasi.webgpu.experimental.host.SurfaceTextureStatus
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AbiCmHostBindingsTest {

    @Test
    fun adapterDeviceQueueHandlesArePositive() {
        CpuWasiWebGpuHost().use { host ->
            val bindings = AbiCmHostBindings(host)
            val adapter = bindings.requestAdapter()
            val device = bindings.adapterRequestDevice(adapter)
            val queue = bindings.deviceGetQueue(device)
            assertTrue(adapter > 0)
            assertTrue(device > 0)
            assertTrue(queue > 0)
        }
    }

    @Test
    fun createBufferDescriptorFieldsReachL2() {
        CpuWasiWebGpuHost().use { host ->
            val bindings = AbiCmHostBindings(host)
            val adapter = bindings.requestAdapter()
            val device = bindings.adapterRequestDevice(adapter)
            val usage = GpuBufferUsage.MAP_READ or GpuBufferUsage.COPY_DST
            val buffer = bindings.deviceCreateBuffer(
                device,
                size = 16,
                usage = usage,
                mappedAtCreation = false,
                label = "cm-test-buf",
            )
            assertTrue(buffer > 0)
            // Map path uses explicit mode flags (not hard-coded map-read).
            bindings.bufferMapAsync(buffer, GpuMapMode.READ, 0, 16)
            val mapped = bindings.bufferGetMappedRange(buffer, 0, 16)
            bindings.bufferUnmap(buffer)
            assertArrayEquals(ByteArray(16), mapped)
        }
    }

    @Test
    fun multiFrameAcquirePresentDoesNotAccumulateSwapchainHandles() {
        CpuWasiWebGpuHost().use { host ->
            val bindings = AbiCmHostBindings(host)
            val adapter = bindings.requestAdapter()
            val device = bindings.adapterRequestDevice(adapter)
            val surface = bindings.createSurfaceFromNativeWindow(0xDEADL)
            bindings.surfaceConfigure(surface, device, adapter, 64, 64)

            val baseline = host.handleCount()
            repeat(60) {
                val view = bindings.surfaceGetCurrentTextureView(surface)
                assertTrue(view > 0)
                assertEquals(1, bindings.trackedFramePairCount())
                bindings.surfacePresent(surface)
                assertEquals(0, bindings.trackedFramePairCount())
            }

            assertEquals(baseline, host.handleCount())
            assertEquals(0, host.handleCount(ResourceKind.Texture))
            assertEquals(0, host.handleCount(ResourceKind.TextureView))
        }
    }

    @Test
    fun dropRepAndUnconfigureClearTrackedPairs() {
        CpuWasiWebGpuHost().use { host ->
            val bindings = AbiCmHostBindings(host)
            val adapter = bindings.requestAdapter()
            val device = bindings.adapterRequestDevice(adapter)
            val surface = bindings.createSurfaceFromNativeWindow(0xBEEFL)
            bindings.surfaceConfigure(surface, device, adapter, 32, 32)

            val view = bindings.surfaceGetCurrentTextureView(surface)
            assertEquals(1, bindings.trackedFramePairCount())
            assertTrue(bindings.dropRep(view))
            assertFalse(bindings.dropRep(view))

            bindings.releaseLifetimeSafetyNets()
            assertEquals(0, bindings.trackedFramePairCount())
            assertEquals(0, host.handleCount(ResourceKind.Texture))
            assertEquals(0, host.handleCount(ResourceKind.TextureView))

            bindings.surfaceGetCurrentTextureView(surface)
            assertEquals(1, bindings.trackedFramePairCount())
            bindings.surfaceUnconfigure(surface)
            assertEquals(0, bindings.trackedFramePairCount())
        }
    }

    @Test
    fun formalBeginRenderPassAndQueueSubmitListDoesNotAccumulateHandles() {
        CpuWasiWebGpuHost().use { host ->
            val bindings = AbiCmHostBindings(host)
            val adapter = bindings.requestAdapter()
            val device = bindings.adapterRequestDevice(adapter)
            val queue = bindings.deviceGetQueue(device)
            val surface = bindings.createSurfaceFromNativeWindow(0xF00DL)
            bindings.surfaceConfigure(surface, device, adapter, 32, 32)

            val baseline = host.handleCount()
            repeat(16) {
                val view = bindings.surfaceGetCurrentTextureView(surface)
                val encoder = bindings.deviceCreateCommandEncoder(device)
                val pass = bindings.commandEncoderBeginRenderPass(
                    encoder,
                    RenderPassDescriptor(
                        colorAttachments = listOf(
                            RenderPassColorAttachment(
                                view = GpuHandle(view),
                                clearValue = Color(0.1, 0.2, 0.3, 1.0),
                            ),
                        ),
                    ),
                )
                assertTrue(pass > 0)
                bindings.renderPassEnd(pass)
                val cmd = bindings.commandEncoderFinish(encoder)
                bindings.queueSubmit(queue, listOf(cmd))
                bindings.surfacePresent(surface)
                assertEquals(0, bindings.trackedFramePairCount())
            }

            assertEquals(baseline, host.handleCount())
            assertEquals(0, host.handleCount(ResourceKind.Texture))
            assertEquals(0, host.handleCount(ResourceKind.TextureView))
            assertEquals(0, host.handleCount(ResourceKind.CommandEncoder))
            assertEquals(0, host.handleCount(ResourceKind.CommandBuffer))
            assertEquals(0, host.handleCount(ResourceKind.RenderPassEncoder))
        }
    }

    @Test
    fun twoStepGetCurrentTextureCreateViewPresentDoesNotAccumulate() {
        CpuWasiWebGpuHost().use { host ->
            val bindings = AbiCmHostBindings(host)
            val adapter = bindings.requestAdapter()
            val device = bindings.adapterRequestDevice(adapter)
            val surface = bindings.createSurfaceFromNativeWindow(0xABCDEL)
            bindings.surfaceConfigure(surface, device, adapter, 48, 48)

            val baseline = host.handleCount()
            repeat(60) {
                val result = bindings.surfaceGetCurrentTexture(surface)
                assertEquals(SurfaceTextureStatus.SuccessOptimal, result.status)
                val texture = requireNotNull(result.texture)
                val view = bindings.textureCreateView(texture.raw)
                assertTrue(view > 0)
                assertEquals(0, bindings.trackedFramePairCount())
                bindings.surfacePresent(surface)
                assertTrue(bindings.dropRep(view))
                assertTrue(bindings.dropRep(texture.raw))
            }

            assertEquals(baseline, host.handleCount())
            assertEquals(0, host.handleCount(ResourceKind.Texture))
            assertEquals(0, host.handleCount(ResourceKind.TextureView))
        }
    }
}
