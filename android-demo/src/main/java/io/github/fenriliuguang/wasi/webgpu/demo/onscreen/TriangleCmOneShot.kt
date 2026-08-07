package io.github.fenriliuguang.wasi.webgpu.demo.onscreen

import android.content.Context
import android.os.Handler
import android.os.HandlerThread
import android.util.Log
import android.view.Surface
import io.github.fenriliuguang.wasi.webgpu.demo.WasmtimeCmTriangleAndroid
import io.github.fenriliuguang.wasi.webgpu.experimental.dawn.DawnWasiWebGpuHost
import io.github.fenriliuguang.wasi.webgpu.experimental.host.WasiWebGpuHost
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

/**
 * One-shot CM Guest red triangle on an Android [Surface].
 *
 * Does not own a frame loop — call [drawOnceAndAwait] after pausing any L2 [TriangleRenderer]
 * that shares the same Surface. All Host/CM work runs on `webgpu-triangle-cm`.
 * Reuses a single [DawnWasiWebGpuHost] across draws (not shared with L2).
 */
class TriangleCmOneShot(
    private val appContext: Context,
    private val onStatus: (String) -> Unit,
) {
    private val thread = HandlerThread("webgpu-triangle-cm").also { it.start() }
    private val handler = Handler(thread.looper)
    private var host: WasiWebGpuHost? = null
    private var closed = false

    /**
     * Run CM Guest one-shot on the CM thread and wait until present/unconfigure finishes.
     * Returns false on timeout or if already released.
     */
    fun drawOnceAndAwait(
        surface: Surface,
        width: Int,
        height: Int,
        timeoutMs: Long = 60_000L,
    ): Boolean {
        val latch = CountDownLatch(1)
        var posted = false
        handler.post {
            try {
                if (closed) return@post
                posted = true
                runCatching {
                    val h = host ?: DawnWasiWebGpuHost.create().also { host = it }
                    WasmtimeCmTriangleAndroid.runOnce(
                        appContext,
                        surface,
                        width,
                        height,
                        host = h,
                    )
                }.onSuccess {
                    postStatus("CM Guest triangle OK (one-shot, host reused)")
                }.onFailure {
                    Log.e(TAG, "CM triangle failed", it)
                    postStatus("CM triangle FAILED: ${it.message}")
                }
            } finally {
                latch.countDown()
            }
        }
        val done = latch.await(timeoutMs, TimeUnit.MILLISECONDS)
        return done && posted && !closed
    }

    fun release() {
        val latch = CountDownLatch(1)
        handler.post {
            try {
                closed = true
                runCatching { host?.close() }
                host = null
            } finally {
                latch.countDown()
            }
        }
        latch.await(5_000L, TimeUnit.MILLISECONDS)
        thread.quitSafely()
    }

    private fun postStatus(message: String) {
        onStatus(message)
    }

    companion object {
        private const val TAG = "TriangleCmOneShot"
    }
}
