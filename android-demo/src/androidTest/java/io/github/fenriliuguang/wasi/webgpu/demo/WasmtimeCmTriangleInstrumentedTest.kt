package io.github.fenriliuguang.wasi.webgpu.demo

import android.app.Activity
import android.content.Intent
import android.os.SystemClock
import android.view.Surface
import android.view.SurfaceHolder
import android.view.SurfaceView
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.runner.lifecycle.ActivityLifecycleMonitorRegistry
import androidx.test.runner.lifecycle.Stage
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference

/**
 * Android acceptance: CM Guest `run-triangle` → Wasmtime + abi-cm → Dawn one-shot present.
 *
 * Does **not** use [androidx.test.core.app.ActivityScenario]: on vivo, starting MainActivity
 * (even with an explicit Intent + extras) often resumes the launcher-task MainActivity whose
 * Intent lacks our extras. ActivityScenario then ignores all lifecycle events ("intent does not
 * match") and [scenario.onActivity] never runs — Surface callbacks are never attached.
 *
 * Instead we start the activity best-effort, then pick whatever [MainActivity] is RESUMED via
 * [ActivityLifecycleMonitorRegistry]. L2 is skipped whenever androidx.test Instrumentation is
 * attached ([MainActivity.isUnderAndroidXTestInstrumentation]).
 *
 * Requires device/emulator with WebGPU/Vulkan and CM-patched Bionic `libwasmtime4j.so`
 * (`scripts/build-wasmtime4j-android.ps1`).
 */
@RunWith(AndroidJUnit4::class)
class WasmtimeCmTriangleInstrumentedTest {

    @Test
    fun cmGuestDrawsOneShotTriangle() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val context = instrumentation.targetContext

        val intent = Intent(context, MainActivity::class.java).apply {
            putExtra(MainActivity.EXTRA_SKIP_L2_TRIANGLE, true)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        }
        // Best-effort start; vivo may still show launcher-intent MainActivity — that is OK.
        runCatching { instrumentation.startActivitySync(intent) }

        val activity = waitForResumedMainActivity(timeoutMs = 20_000)
        val surfaceReady = CountDownLatch(1)
        val surfaceRef = AtomicReference<Surface?>(null)
        val widthRef = AtomicReference(0)
        val heightRef = AtomicReference(0)

        instrumentation.runOnMainSync {
            val surfaceView = activity.findViewById<SurfaceView>(R.id.triangleSurface)
            fun capture(surface: Surface, width: Int, height: Int) {
                if (!surface.isValid || width <= 0 || height <= 0) return
                surfaceRef.set(surface)
                widthRef.set(width)
                heightRef.set(height)
                surfaceReady.countDown()
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

        // Poll as well: callback may have fired before we registered.
        val deadline = SystemClock.uptimeMillis() + 20_000
        while (surfaceReady.count > 0 && SystemClock.uptimeMillis() < deadline) {
            instrumentation.runOnMainSync {
                if (surfaceReady.count == 0L) return@runOnMainSync
                val surfaceView = activity.findViewById<SurfaceView>(R.id.triangleSurface)
                val holder = surfaceView.holder
                val frame = holder.surfaceFrame
                val surface = holder.surface
                if (surface != null && surface.isValid && frame.width() > 0 && frame.height() > 0) {
                    surfaceRef.set(surface)
                    widthRef.set(frame.width())
                    heightRef.set(frame.height())
                    surfaceReady.countDown()
                }
            }
            if (surfaceReady.count > 0) {
                Thread.sleep(50)
            }
        }
        assertTrue(
            "Surface not ready within timeout (activity=${activity.javaClass.simpleName})",
            surfaceReady.await(1, TimeUnit.SECONDS),
        )
        Thread.sleep(300)

        val surface = requireNotNull(surfaceRef.get())
        val width = widthRef.get()
        val height = heightRef.get()
        val done = CountDownLatch(1)
        val error = AtomicReference<Throwable?>(null)
        Thread({
            try {
                WasmtimeCmTriangleAndroid.runOnce(context, surface, width, height)
            } catch (t: Throwable) {
                error.set(t)
            } finally {
                done.countDown()
            }
        }, "cm-triangle-instrumented").start()
        assertTrue("CM runOnce timed out", done.await(60, TimeUnit.SECONDS))
        val failure = error.get()
        if (failure != null) {
            throw AssertionError("CM Guest triangle failed: ${failure.message}", failure)
        }

        instrumentation.runOnMainSync { activity.finish() }
        instrumentation.waitForIdleSync()
    }

    private fun waitForResumedMainActivity(timeoutMs: Long): MainActivity {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val monitor = ActivityLifecycleMonitorRegistry.getInstance()
        val deadline = SystemClock.uptimeMillis() + timeoutMs
        while (SystemClock.uptimeMillis() < deadline) {
            val found = AtomicReference<MainActivity?>(null)
            instrumentation.runOnMainSync {
                @Suppress("UNCHECKED_CAST")
                val resumed = monitor.getActivitiesInStage(Stage.RESUMED) as Collection<Activity>
                found.set(resumed.filterIsInstance<MainActivity>().firstOrNull())
            }
            found.get()?.let { return it }
            Thread.sleep(50)
        }
        error("MainActivity not RESUMED within ${timeoutMs}ms")
    }
}
