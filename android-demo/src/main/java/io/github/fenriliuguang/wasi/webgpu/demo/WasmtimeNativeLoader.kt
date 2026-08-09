package io.github.fenriliuguang.wasi.webgpu.demo

/**
 * Loads `libwasmtime4j.so` from the APK jniLibs before wasmtime4j's JAR extractor runs.
 */
object WasmtimeNativeLoader {

    @Volatile
    private var nativeReady = false

    fun ensureLoaded() {
        if (nativeReady) return
        synchronized(this) {
            if (nativeReady) return
            System.loadLibrary("wasmtime4j")
            nativeReady = true
        }
    }
}
