package io.github.fenriliuguang.wasi.webgpu.experimental.abiwasi

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AbiWasiTest {
    @Test
    fun importInterfaceMatchesPin() {
        assertEquals("wasi:webgpu", AbiWasi.PACKAGE)
        assertEquals("webgpu", AbiWasi.INTERFACE)
        assertEquals("0.3.0-rc.2", AbiWasi.VERSION)
        assertEquals("wasi:webgpu/webgpu@0.3.0-rc.2", AbiWasi.IMPORT_INTERFACE)
    }

    @Test
    fun resourceAndFuncInventoryNonEmpty() {
        assertTrue(AbiWasi.Resource.ALL.contains("gpu"))
        assertTrue(AbiWasi.Resource.ALL.contains("gpu-device"))
        assertTrue(AbiWasi.Func.ALL.contains("[method]gpu.request-adapter"))
        assertTrue(AbiWasi.Func.ALL.contains("[method]gpu-device.create-buffer"))
        assertEquals(33, AbiWasi.Resource.ALL.size)
        assertEquals(224, AbiWasi.Func.ALL.size)
    }
}
