package io.github.fenriliuguang.wasi.webgpu.demo

import android.os.Bundle
import android.view.Surface
import android.view.SurfaceHolder
import android.view.SurfaceView
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import io.github.fenriliuguang.wasi.webgpu.demo.onscreen.TriangleCmOneShot
import io.github.fenriliuguang.wasi.webgpu.demo.onscreen.TriangleRenderer
import kotlin.concurrent.thread

class MainActivity : AppCompatActivity() {
    private var triangleRenderer: TriangleRenderer? = null
    private var triangleCmOneShot: TriangleCmOneShot? = null
    private var latestSurface: Surface? = null
    private var surfaceWidth: Int = 0
    private var surfaceHeight: Int = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val status = findViewById<TextView>(R.id.status)
        val runButton = findViewById<Button>(R.id.runButton)
        val cmTriangleButton = findViewById<Button>(R.id.cmTriangleButton)
        val surfaceView = findViewById<SurfaceView>(R.id.triangleSurface)

        val renderer = TriangleRenderer { message ->
            runOnUiThread { status.text = message }
        }
        triangleRenderer = renderer
        renderer.start()

        triangleCmOneShot = TriangleCmOneShot(applicationContext) { message ->
            runOnUiThread {
                status.text = message
                cmTriangleButton.isEnabled = true
            }
        }

        surfaceView.holder.addCallback(
            object : SurfaceHolder.Callback {
                override fun surfaceCreated(holder: SurfaceHolder) {
                    val frame = holder.surfaceFrame
                    latestSurface = holder.surface
                    surfaceWidth = frame.width()
                    surfaceHeight = frame.height()
                    if (surfaceWidth > 0 && surfaceHeight > 0) {
                        renderer.onSurfaceAvailable(holder.surface, surfaceWidth, surfaceHeight)
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
                        renderer.onSurfaceResized(holder.surface, width, height)
                    }
                }

                override fun surfaceDestroyed(holder: SurfaceHolder) {
                    latestSurface = null
                    surfaceWidth = 0
                    surfaceHeight = 0
                    renderer.onSurfaceDestroyed()
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
            val surface = latestSurface
            val w = surfaceWidth
            val h = surfaceHeight
            if (surface == null || !surface.isValid || w <= 0 || h <= 0) {
                status.text = "CM triangle: Surface not ready"
                return@setOnClickListener
            }
            cmTriangleButton.isEnabled = false
            // Pause L2 loop so CM Guest can own the Surface for one shot.
            renderer.onSurfaceDestroyed()
            status.text = "Running CM Guest triangle (one-shot)…"
            triangleCmOneShot?.drawOnce(surface, w, h)
        }
    }

    /** Pause L2 [TriangleRenderer] so CM Guest can own the Surface (demo / instrumented). */
    fun pauseL2TriangleForCm() {
        triangleRenderer?.onSurfaceDestroyed()
    }

    override fun onDestroy() {
        triangleCmOneShot?.release()
        triangleCmOneShot = null
        triangleRenderer?.release()
        triangleRenderer = null
        super.onDestroy()
    }
}
