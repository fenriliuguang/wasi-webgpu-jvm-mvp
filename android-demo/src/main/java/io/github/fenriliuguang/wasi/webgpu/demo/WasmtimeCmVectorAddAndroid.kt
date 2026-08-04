package io.github.fenriliuguang.wasi.webgpu.demo

import android.content.Context
import io.github.fenriliuguang.wasi.webgpu.experimental.dawn.DawnWasiWebGpuHost
import io.github.fenriliuguang.wasi.webgpu.experimental.host.WasiWebGpuHost
import io.github.fenriliuguang.wasi.webgpu.experimental.runtime.cm.WasmtimeCmVectorAdd

/**
 * Android entry: Guest.component → Wasmtime CM (L1) + abi-cm → [WasiWebGpuHost] (Dawn).
 *
 * Requires a Bionic `libwasmtime4j.so` built with the CM resources patch
 * (`scripts/build-wasmtime4j-android.ps1`).
 */
object WasmtimeCmVectorAddAndroid {

    fun loadGuestComponent(context: Context): ByteArray =
        context.assets.open("guest/vector_add_cm.wasm").use { it.readBytes() }

    fun run(
        context: Context,
        a: FloatArray,
        b: FloatArray,
        host: WasiWebGpuHost? = null,
    ): FloatArray {
        WasmtimeVectorAddAndroid.ensureNativeLoaded()
        val component = loadGuestComponent(context)
        val ownedHost = host == null
        val h = host ?: DawnWasiWebGpuHost.create()
        try {
            return WasmtimeCmVectorAdd.run(component, a, b, h)
        } finally {
            if (ownedHost) {
                h.close()
            }
        }
    }

    /**
     * Multiple vector-add calls on one CM linker/instance (see [WasmtimeCmVectorAdd.runAll]).
     */
    fun runAll(
        context: Context,
        cases: List<Pair<FloatArray, FloatArray>>,
        host: WasiWebGpuHost? = null,
    ): List<FloatArray> {
        WasmtimeVectorAddAndroid.ensureNativeLoaded()
        val component = loadGuestComponent(context)
        val ownedHost = host == null
        val h = host ?: DawnWasiWebGpuHost.create()
        try {
            return WasmtimeCmVectorAdd.runAll(component, cases, h)
        } finally {
            if (ownedHost) {
                h.close()
            }
        }
    }
}
