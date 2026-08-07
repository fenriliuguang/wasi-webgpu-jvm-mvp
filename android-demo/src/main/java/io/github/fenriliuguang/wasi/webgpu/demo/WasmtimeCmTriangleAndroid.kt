package io.github.fenriliuguang.wasi.webgpu.demo

import android.content.Context
import android.view.Surface
import androidx.webgpu.helper.Util
import io.github.fenriliuguang.wasi.webgpu.experimental.dawn.DawnWasiWebGpuHost
import io.github.fenriliuguang.wasi.webgpu.experimental.host.WasiWebGpuHost
import io.github.fenriliuguang.wasi.webgpu.experimental.runtime.cm.WasmtimeCmTriangle

/**
 * Android entry: Guest `triangle_cm.wasm` → Wasmtime CM + abi-cm → Dawn.
 *
 * Host injects native window from [Surface]; Guest only holds `surface`.
 * Call from a background / render thread (not the UI thread).
 *
 * Demo path: prefer [openSession] + repeated [WasmtimeCmTriangle.Session.runTriangle]
 * (process-global CM registry). Instrumented one-shot may still use [runOnce].
 */
object WasmtimeCmTriangleAndroid {

    fun loadGuestComponent(context: Context): ByteArray =
        context.assets.open("guest/triangle_cm.wasm").use { it.readBytes() }

    fun openSession(context: Context, host: WasiWebGpuHost): WasmtimeCmTriangle.Session {
        WasmtimeVectorAddAndroid.ensureNativeLoaded()
        return WasmtimeCmTriangle.openSession(loadGuestComponent(context), host)
    }

    fun runOnce(
        context: Context,
        surface: Surface,
        width: Int,
        height: Int,
        host: WasiWebGpuHost? = null,
    ) {
        require(surface.isValid) { "Surface is not valid" }
        require(width > 0 && height > 0) { "invalid surface size ${width}x$height" }
        WasmtimeVectorAddAndroid.ensureNativeLoaded()
        val component = loadGuestComponent(context)
        val windowHandle = Util.windowFromSurface(surface)
        val ownedHost = host == null
        val h = host ?: DawnWasiWebGpuHost.create()
        try {
            WasmtimeCmTriangle.openSession(component, h).use { session ->
                session.runTriangle(windowHandle, width, height)
            }
        } finally {
            if (ownedHost) {
                // Present/GPU settle before Host teardown (Session does not own [h] here).
                Thread.sleep(100)
                h.close()
            }
        }
    }
}
