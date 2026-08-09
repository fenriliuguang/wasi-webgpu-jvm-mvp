package io.github.fenriliuguang.wasi.webgpu.demo

import android.os.Bundle
import android.view.Surface
import android.view.SurfaceHolder
import android.view.SurfaceView
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import io.github.fenriliuguang.wasi.webgpu.demo.onscreen.CubeCmOneShot
import io.github.fenriliuguang.wasi.webgpu.demo.onscreen.TriangleCmOneShot
import io.github.fenriliuguang.wasi.webgpu.demo.onscreen.TriangleRenderer
import kotlin.concurrent.thread

class MainActivity : AppCompatActivity() {
    private var triangleRenderer: TriangleRenderer? = null
    private var triangleCmOneShot: TriangleCmOneShot? = null
    private var cubeCmOneShot: CubeCmOneShot? = null
    private var latestSurface: Surface? = null
    private var surfaceWidth: Int = 0
    private var surfaceHeight: Int = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val status = findViewById<TextView>(R.id.status)
        val runButton = findViewById<Button>(R.id.runButton)
        val cmTriangleButton = findViewById<Button>(R.id.cmTriangleButton)
        val cmCubeButton = findViewById<Button>(R.id.cmCubeButton)
        val surfaceView = findViewById<SurfaceView>(R.id.triangleSurface)

        // Skip L2 when asked via Intent, or whenever androidx.test Instrumentation is attached
        // (vivo may ignore our launch Intent and resume launcher MainActivity during tests).
        val skipL2 = intent.getBooleanExtra(EXTRA_SKIP_L2_TRIANGLE, false) ||
            isUnderAndroidXTestInstrumentation()

        val renderer = if (skipL2) {
            null
        } else {
            TriangleRenderer { message ->
                runOnUiThread { status.text = message }
            }.also {
                triangleRenderer = it
                it.start()
            }
        }

        // Under instrumentation, skip Demo CM button wiring — instrumented tests call
        // WasmtimeCmTriangleAndroid.runOnce directly (avoids double-present / WINDOW_IN_USE).
        if (!skipL2) {
            triangleCmOneShot = TriangleCmOneShot(applicationContext) { message ->
                runOnUiThread { status.text = message }
            }
            cubeCmOneShot = CubeCmOneShot(applicationContext) { message ->
                runOnUiThread { status.text = message }
            }
        } else {
            cmTriangleButton.isEnabled = false
            cmCubeButton.isEnabled = false
            status.text = "CM-only Surface ready (L2 triangle skipped)"
        }

        surfaceView.holder.addCallback(
            object : SurfaceHolder.Callback {
                override fun surfaceCreated(holder: SurfaceHolder) {
                    val frame = holder.surfaceFrame
                    latestSurface = holder.surface
                    surfaceWidth = frame.width()
                    surfaceHeight = frame.height()
                    if (surfaceWidth > 0 && surfaceHeight > 0) {
                        renderer?.onSurfaceAvailable(holder.surface, surfaceWidth, surfaceHeight)
                    }
                }

                override fun surfaceChanged(
                    holder: SurfaceHolder,
                    format: Int,
                    width: Int,
                    height: Int,
                ) {
                    latestSurface = holder.surface
                    surfaceWidth = width
                    surfaceHeight = height
                    if (width > 0 && height > 0) {
                        renderer?.onSurfaceResized(holder.surface, width, height)
                    }
                }

                override fun surfaceDestroyed(holder: SurfaceHolder) {
                    latestSurface = null
                    surfaceWidth = 0
                    surfaceHeight = 0
                    renderer?.onSurfaceDestroyed()
                }
            },
        )

        runButton.setOnClickListener {
            runButton.isEnabled = false
            status.text = "Running vector add on Dawn host…"
            thread {
                val message = runCatching {
                    val a = floatArrayOf(1f, 2f, 3f, 4f)
                    val b = floatArrayOf(10f, 20f, 30f, 40f)
                    val out = VectorAdd.run(a, b)
                    "OK: ${out.joinToString()} (expected 11,22,33,44)"
                }.getOrElse { error ->
                    "FAILED: ${error.message ?: error::class.java.simpleName}"
                }
                runOnUiThread {
                    status.text = message
                    runButton.isEnabled = true
                }
            }
        }

        cmTriangleButton.setOnClickListener {
            runCmSurfaceDemo(
                status = status,
                ownButton = cmTriangleButton,
                otherButton = cmCubeButton,
                label = "triangle",
            ) { surface, w, h ->
                val cm = triangleCmOneShot
                    ?: return@runCmSurfaceDemo "CM triangle FAILED: CM path not wired"
                if (!cm.runFrameLoopAndAwait(surface, w, h)) {
                    "CM triangle FAILED: timeout or released"
                } else {
                    null
                }
            }
        }

        cmCubeButton.setOnClickListener {
            runCmSurfaceDemo(
                status = status,
                ownButton = cmCubeButton,
                otherButton = cmTriangleButton,
                label = "cube",
            ) { surface, w, h ->
                val cm = cubeCmOneShot
                    ?: return@runCmSurfaceDemo "CM cube FAILED: CM path not wired"
                if (!cm.runFrameLoopAndAwait(surface, w, h)) {
                    "CM cube FAILED: timeout or released"
                } else {
                    null
                }
            }
        }
    }

    /**
     * Pause L2 → CM frame loop → resume L2. Disables both CM buttons for the span
     * so triangle/cube sessions are not started back-to-back on shared Surface.
     */
    private fun runCmSurfaceDemo(
        status: TextView,
        ownButton: Button,
        otherButton: Button,
        label: String,
        run: (Surface, Int, Int) -> String?,
    ) {
        val surface = latestSurface
        val w = surfaceWidth
        val h = surfaceHeight
        if (surface == null || !surface.isValid || w <= 0 || h <= 0) {
            status.text = "CM $label: Surface not ready"
            return
        }
        ownButton.isEnabled = false
        otherButton.isEnabled = false
        status.text = "Running CM Guest $label (frame loop)…"
        thread {
            val l2 = triangleRenderer
            try {
                if (l2 != null && !l2.pauseSurfaceAndAwait()) {
                    runOnUiThread {
                        status.text = "CM $label FAILED: L2 pause timeout"
                    }
                    return@thread
                }
                val err = run(surface, w, h)
                if (err != null) {
                    runOnUiThread { status.text = err }
                }
            } finally {
                Thread.sleep(POST_CM_SETTLE_MS)
                val resumeSurface = latestSurface
                val rw = surfaceWidth
                val rh = surfaceHeight
                if (l2 != null &&
                    resumeSurface != null &&
                    resumeSurface.isValid &&
                    rw > 0 &&
                    rh > 0
                ) {
                    if (!l2.resumeSurfaceAndAwait(resumeSurface, rw, rh)) {
                        runOnUiThread {
                            status.text = "CM $label done but L2 resume failed/timeout"
                        }
                    }
                }
                runOnUiThread {
                    ownButton.isEnabled = true
                    otherButton.isEnabled = true
                }
            }
        }
    }

    /**
     * Pause L2 [TriangleRenderer] so CM Guest can own the Surface.
     * Safe to call from the instrumented-test thread (awaits render-thread release).
     */
    fun pauseL2TriangleForCm(): Boolean =
        triangleRenderer?.pauseSurfaceAndAwait() ?: true

    override fun onDestroy() {
        cubeCmOneShot?.release()
        cubeCmOneShot = null
        triangleCmOneShot?.release()
        triangleCmOneShot = null
        triangleRenderer?.release()
        triangleRenderer = null
        super.onDestroy()
    }

    companion object {
        /** When true, do not start L2 [TriangleRenderer] (CM instrumented tests). */
        const val EXTRA_SKIP_L2_TRIANGLE: String = "skip_l2_triangle"

        /** Extra settle after CM before L2 recreateSurface (BufferQueue disconnect latency). */
        private const val POST_CM_SETTLE_MS = 300L

        /** True when this process is running under androidx.test Instrumentation. */
        fun isUnderAndroidXTestInstrumentation(): Boolean {
            return try {
                val atClass = Class.forName("android.app.ActivityThread")
                val thread = atClass.getMethod("currentActivityThread").invoke(null) ?: return false
                val instr = atClass.getMethod("getInstrumentation").invoke(thread) ?: return false
                instr.javaClass.name.startsWith("androidx.test")
            } catch (_: Throwable) {
                false
            }
        }
    }
}
