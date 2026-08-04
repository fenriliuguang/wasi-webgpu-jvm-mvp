package io.github.fenriliuguang.wasi.webgpu.experimental.abicm

import io.github.fenriliuguang.wasi.webgpu.experimental.host.CpuWasiWebGpuHost
import io.github.fenriliuguang.wasi.webgpu.experimental.host.GpuBufferUsage
import io.github.fenriliuguang.wasi.webgpu.experimental.host.GpuMapMode
import org.junit.Assert.assertArrayEquals
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
}
