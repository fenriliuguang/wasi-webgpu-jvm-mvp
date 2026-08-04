package io.github.fenriliuguang.wasi.webgpu.experimental.abi

/**
 * Guest linear memory access used by abi-mvp host bindings.
 */
interface GuestMemory {
    fun readBytes(ptr: Int, len: Int): ByteArray

    fun writeBytes(ptr: Int, data: ByteArray)

    fun readUtf8(ptr: Int, len: Int): String = readBytes(ptr, len).toString(Charsets.UTF_8)
}
