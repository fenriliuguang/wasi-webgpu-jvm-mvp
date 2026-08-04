package io.github.fenriliuguang.wasi.webgpu.experimental.abicm

import io.github.fenriliuguang.wasi.webgpu.experimental.host.CpuWasiWebGpuHost
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
}
