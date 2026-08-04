package io.github.fenriliuguang.wasi.webgpu.demo

import android.content.Context
import io.github.fenriliuguang.wasi.webgpu.experimental.dawn.DawnWasiWebGpuHost
import io.github.fenriliuguang.wasi.webgpu.experimental.host.WasiWebGpuHost
import io.github.fenriliuguang.wasi.webgpu.experimental.runtime.WasmtimeVectorAdd

/**
 * Android entry: Guest.wasm → Wasmtime (L1) + abi-mvp → [WasiWebGpuHost] (Dawn).
 *
 * Loads `libwasmtime4j.so` from the APK jniLibs before wasmtime4j's JAR extractor runs.
 */
object WasmtimeVectorAddAndroid {

    @Volatile
    private var nativeReady = false

    fun ensureNativeLoaded() {
        if (nativeReady) return
        synchronized(this) {
            if (nativeReady) return
            System.loadLibrary("wasmtime4j")
            nativeReady = true
        }
    }

    fun loadGuestWasm(context: Context): ByteArray =
        context.assets.open("guest/vector_add.wasm").use { it.readBytes() }

    fun run(
        context: Context,
        a: FloatArray,
        b: FloatArray,
        host: WasiWebGpuHost? = null,
    ): FloatArray {
        ensureNativeLoaded()
        val wasm = loadGuestWasm(context)
        val ownedHost = host == null
        val h = host ?: DawnWasiWebGpuHost.create()
        try {
            return WasmtimeVectorAdd.run(wasm, a, b, h)
        } finally {
            if (ownedHost) {
                h.close()
            }
        }
    }
}
