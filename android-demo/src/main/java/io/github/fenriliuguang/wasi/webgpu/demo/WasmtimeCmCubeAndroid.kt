package io.github.fenriliuguang.wasi.webgpu.demo

import android.content.Context
import android.view.Surface
import androidx.webgpu.helper.Util
import io.github.fenriliuguang.wasi.webgpu.experimental.dawn.DawnWasiWebGpuHost
import io.github.fenriliuguang.wasi.webgpu.experimental.host.WasiWebGpuHost
import io.github.fenriliuguang.wasi.webgpu.experimental.runtime.cm.WasmtimeCmCube

/**
 * Android entry: Guest `cube_cm.wasm` → Wasmtime CM + abi-cm → Dawn.
 *
 * Host injects native window from [Surface]; Guest only holds `surface`.
 * Call from a background / render thread (not the UI thread).
 */
object WasmtimeCmCubeAndroid {

    fun loadGuestComponent(context: Context): ByteArray =
        context.assets.open("guest/cube_cm.wasm").use { it.readBytes() }

    fun openSession(context: Context, host: WasiWebGpuHost): WasmtimeCmCube.Session {
        WasmtimeVectorAddAndroid.ensureNativeLoaded()
        return WasmtimeCmCube.openSession(loadGuestComponent(context), host)
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
            WasmtimeCmCube.openSession(component, h).use { session ->
                session.runCube(windowHandle, width, height)
            }
        } finally {
            runCatching { h.releaseAllGpuObjects() }
            (h as? DawnWasiWebGpuHost)?.flushEvents()
            if (ownedHost) {
                Thread.sleep(400)
                h.close()
            }
        }
    }
}
