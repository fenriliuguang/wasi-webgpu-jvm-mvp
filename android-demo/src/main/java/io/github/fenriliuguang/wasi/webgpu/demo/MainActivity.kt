package io.github.fenriliuguang.wasi.webgpu.demo

import android.os.Bundle
import android.view.Surface
import android.view.SurfaceHolder
import android.view.SurfaceView
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import io.github.fenriliuguang.wasi.webgpu.demo.onscreen.CubeCmOneShot
import kotlin.concurrent.thread

class MainActivity : AppCompatActivity() {
    private var cubeCmOneShot: CubeCmOneShot? = null
    private var latestSurface: Surface? = null
    private var surfaceWidth: Int = 0
    private var surfaceHeight: Int = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val status = findViewById<TextView>(R.id.status)
        val cmCubeButton = findViewById<Button>(R.id.cmCubeButton)
        val surfaceView = findViewById<SurfaceView>(R.id.demoSurface)

        // Skip Demo auto-wiring when asked via Intent, or whenever androidx.test
        // Instrumentation is attached (vivo may ignore launch Intent extras).
        val skipDemo = intent.getBooleanExtra(EXTRA_SKIP_DEMO_AUTORUN, false) ||
            isUnderAndroidXTestInstrumentation()

        if (!skipDemo) {
            cubeCmOneShot = CubeCmOneShot(applicationContext) { message ->
                runOnUiThread { status.text = message }
            }
            status.text = getString(R.string.status_idle)
        } else {
            cmCubeButton.isEnabled = false
            status.text = "CM Surface ready (Demo autorun skipped)"
        }

        surfaceView.holder.addCallback(
            object : SurfaceHolder.Callback {
                override fun surfaceCreated(holder: SurfaceHolder) {
                    val frame = holder.surfaceFrame
                    latestSurface = holder.surface
                    surfaceWidth = frame.width()
                    surfaceHeight = frame.height()
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
                }

                override fun surfaceDestroyed(holder: SurfaceHolder) {
                    latestSurface = null
                    surfaceWidth = 0
                    surfaceHeight = 0
                }
            },
        )

        cmCubeButton.setOnClickListener {
            val surface = latestSurface
            val w = surfaceWidth
            val h = surfaceHeight
            if (surface == null || !surface.isValid || w <= 0 || h <= 0) {
                status.text = "CM cube: Surface not ready"
                return@setOnClickListener
            }
            val cm = cubeCmOneShot
            if (cm == null) {
                status.text = "CM cube FAILED: CM path not wired"
                return@setOnClickListener
            }
            cmCubeButton.isEnabled = false
            status.text = "Running CM Guest cube (frame loop)…"
            thread {
                try {
                    val ok = cm.runFrameLoopAndAwait(surface, w, h)
                    runOnUiThread {
                        if (!ok) {
                            status.text = "CM cube FAILED: timeout or released"
                        }
                    }
                } finally {
                    runOnUiThread { cmCubeButton.isEnabled = true }
                }
            }
        }
    }

    override fun onDestroy() {
        cubeCmOneShot?.release()
        cubeCmOneShot = null
        super.onDestroy()
    }

    companion object {
        /** When true, do not wire Demo CM button (instrumented tests drive cube directly). */
        const val EXTRA_SKIP_DEMO_AUTORUN: String = "skip_demo_autorun"

        /** @deprecated Use [EXTRA_SKIP_DEMO_AUTORUN]. */
        const val EXTRA_SKIP_L2_TRIANGLE: String = EXTRA_SKIP_DEMO_AUTORUN

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
