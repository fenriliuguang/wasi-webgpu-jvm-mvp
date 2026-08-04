package io.github.fenriliuguang.wasi.webgpu.demo

import io.github.fenriliuguang.wasi.webgpu.experimental.dawn.DawnWasiWebGpuHost
import io.github.fenriliuguang.wasi.webgpu.experimental.host.VectorAddScenario
import io.github.fenriliuguang.wasi.webgpu.experimental.host.WasiWebGpuHost

/**
 * P0/P1 acceptance path: Kotlin → [WasiWebGpuHost] → Dawn vector add + readback.
 */
object VectorAdd {

    fun run(a: FloatArray, b: FloatArray): FloatArray {
        require(a.size == b.size && a.isNotEmpty())
        DawnWasiWebGpuHost.create().use { host ->
            return runOn(host, a, b)
        }
    }

    fun runOn(host: WasiWebGpuHost, a: FloatArray, b: FloatArray): FloatArray =
        VectorAddScenario.runOn(host, a, b)
}
