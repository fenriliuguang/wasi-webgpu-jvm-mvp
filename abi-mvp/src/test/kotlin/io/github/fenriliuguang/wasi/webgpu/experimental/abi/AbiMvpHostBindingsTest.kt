package io.github.fenriliuguang.wasi.webgpu.experimental.abi

import io.github.fenriliuguang.wasi.webgpu.experimental.host.CpuWasiWebGpuHost
import io.github.fenriliuguang.wasi.webgpu.experimental.host.GpuBufferUsage
import io.github.fenriliuguang.wasi.webgpu.experimental.host.VectorAddScenario
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
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
}
