package io.github.fenriliuguang.wasi.webgpu.experimental.runtime

import ai.tegmentum.wasmtime4j.WasmMemory
import io.github.fenriliuguang.wasi.webgpu.experimental.abi.GuestMemory

class WasmtimeGuestMemory(private val memory: WasmMemory) : GuestMemory {
    override fun readBytes(ptr: Int, len: Int): ByteArray {
        val out = ByteArray(len)
        memory.readBytes(ptr, out, 0, len)
        return out
    }

    override fun writeBytes(ptr: Int, data: ByteArray) {
        memory.writeBytes(ptr, data, 0, data.size)
    }
}
