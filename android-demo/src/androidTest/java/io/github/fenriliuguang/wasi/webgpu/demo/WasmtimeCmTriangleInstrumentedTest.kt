package io.github.fenriliuguang.wasi.webgpu.demo

import android.view.Surface
import android.view.SurfaceHolder
import android.view.SurfaceView
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference

/**
 * Android acceptance: CM Guest `run-triangle` → Wasmtime + abi-cm → Dawn one-shot present.
 *
 * Requires device/emulator with WebGPU/Vulkan and CM-patched Bionic `libwasmtime4j.so`
 * (`scripts/build-wasmtime4j-android.ps1`).
 */
@RunWith(AndroidJUnit4::class)
class WasmtimeCmTriangleInstrumentedTest {

    @Test
    fun cmGuestDrawsOneShotTriangle() {
        val ready = CountDownLatch(1)
        val surfaceRef = AtomicReference<Surface?>(null)
        val widthRef = AtomicReference(0)
        val heightRef = AtomicReference(0)

        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                val surfaceView = activity.findViewById<SurfaceView>(R.id.triangleSurface)
                fun capture(surface: Surface, width: Int, height: Int) {
                    if (!surface.isValid || width <= 0 || height <= 0) return
                    surfaceRef.set(surface)
                    widthRef.set(width)
                    heightRef.set(height)
                    ready.countDown()
                }
                val holder = surfaceView.holder
                val frame = holder.surfaceFrame
                val existing = holder.surface
                if (existing != null && existing.isValid && frame.width() > 0 && frame.height() > 0) {
                    capture(existing, frame.width(), frame.height())
                }
                holder.addCallback(
                    object : SurfaceHolder.Callback {
                        override fun surfaceCreated(holder: SurfaceHolder) {
                            val f = holder.surfaceFrame
                            capture(holder.surface, f.width(), f.height())
                        }

                        override fun surfaceChanged(
                            holder: SurfaceHolder,
                            format: Int,
                            width: Int,
                            height: Int,
                        ) {
                            capture(holder.surface, width, height)
                        }

                        override fun surfaceDestroyed(holder: SurfaceHolder) = Unit
                    },
                )
            }

            assertTrue("Surface not ready within timeout", ready.await(20, TimeUnit.SECONDS))
            val surface = requireNotNull(surfaceRef.get())
            val width = widthRef.get()
            val height = heightRef.get()

            scenario.onActivity { activity ->
                activity.pauseL2TriangleForCm()
            }

            val context = InstrumentationRegistry.getInstrumentation().targetContext
            WasmtimeCmTriangleAndroid.runOnce(context, surface, width, height)
        }
    }
}
