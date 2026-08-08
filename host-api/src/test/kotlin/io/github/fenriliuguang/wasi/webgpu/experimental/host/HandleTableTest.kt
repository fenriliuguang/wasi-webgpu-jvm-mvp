package io.github.fenriliuguang.wasi.webgpu.experimental.host

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class HandleTableTest {

    @Test
    fun insertGetAndDrop() {
        val table = HandleTable()
        val handle = table.insert(ResourceKind.Buffer, "buf-a")

        assertTrue(table.contains(handle))
        assertEquals("buf-a", table.get<String>(handle, ResourceKind.Buffer))
        assertEquals(1, table.size())

        val dropped = table.drop(handle)
        assertEquals(ResourceKind.Buffer, dropped.kind)
        assertEquals("buf-a", dropped.resource)
        assertFalse(table.contains(handle))
        assertEquals(0, table.size())
    }

    @Test(expected = HostException.InvalidHandle::class)
    fun dropUnknownThrows() {
        HandleTable().drop(GpuHandle(42))
    }

    @Test(expected = HostException.InvalidHandle::class)
    fun wrongKindThrows() {
        val table = HandleTable()
        val handle = table.insert(ResourceKind.Queue, "q")
        table.get<String>(handle, ResourceKind.Buffer)
    }

    @Test(expected = HostException.InvalidHandle::class)
    fun doubleDropThrows() {
        val table = HandleTable()
        val handle = table.insert(ResourceKind.Device, "dev")
        table.drop(handle)
        table.drop(handle)
    }

    @Test
    fun tryDropIsIdempotent() {
        val table = HandleTable()
        val handle = table.insert(ResourceKind.TextureView, "view")
        assertTrue(table.tryDrop(handle) != null)
        assertTrue(table.tryDrop(handle) == null)
        assertFalse(table.contains(handle))
    }

    @Test
    fun handlesOfKindSnapshot() {
        val table = HandleTable()
        val s1 = table.insert(ResourceKind.Surface, "surf-1")
        table.insert(ResourceKind.Device, "dev")
        val s2 = table.insert(ResourceKind.Surface, "surf-2")
        assertEquals(listOf(s1, s2), table.handlesOfKind(ResourceKind.Surface))
    }
}
