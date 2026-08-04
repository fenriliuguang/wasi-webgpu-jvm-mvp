package io.github.fenriliuguang.wasi.webgpu.demo

import android.os.Bundle
import android.view.SurfaceHolder
import android.view.SurfaceView
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import io.github.fenriliuguang.wasi.webgpu.demo.onscreen.TriangleRenderer
import kotlin.concurrent.thread

class MainActivity : AppCompatActivity() {
    private var triangleRenderer: TriangleRenderer? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val status = findViewById<TextView>(R.id.status)
        val runButton = findViewById<Button>(R.id.runButton)
        val surfaceView = findViewById<SurfaceView>(R.id.triangleSurface)

        val renderer = TriangleRenderer { message ->
            runOnUiThread { status.text = message }
        }
        triangleRenderer = renderer
        renderer.start()

        surfaceView.holder.addCallback(
            object : SurfaceHolder.Callback {
                override fun surfaceCreated(holder: SurfaceHolder) {
                    val frame = holder.surfaceFrame
                    if (frame.width() > 0 && frame.height() > 0) {
                        renderer.onSurfaceAvailable(holder.surface, frame.width(), frame.height())
                    }
                }

                override fun surfaceChanged(
                    holder: SurfaceHolder,
                    format: Int,
                    width: Int,
                    height: Int,
                ) {
                    if (width > 0 && height > 0) {
                        renderer.onSurfaceResized(holder.surface, width, height)
                    }
                }

                override fun surfaceDestroyed(holder: SurfaceHolder) {
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
    }

    override fun onDestroy() {
        triangleRenderer?.release()
        triangleRenderer = null
        super.onDestroy()
    }
}
