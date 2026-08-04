package io.github.fenriliuguang.wasi.webgpu.demo

import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import kotlin.concurrent.thread

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val status = findViewById<TextView>(R.id.status)
        val runButton = findViewById<Button>(R.id.runButton)

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
}
