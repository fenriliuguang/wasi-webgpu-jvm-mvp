package io.github.fenriliuguang.wasi.webgpu.demo

import android.app.Activity
import android.content.Intent
import android.os.SystemClock
import android.view.Surface
import android.view.SurfaceHolder
import android.view.SurfaceView
import android.view.WindowManager
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.runner.lifecycle.ActivityLifecycleMonitorRegistry
import androidx.test.runner.lifecycle.Stage
import androidx.webgpu.helper.Util
import io.github.fenriliuguang.wasi.webgpu.experimental.dawn.DawnWasiWebGpuHost
import io.github.fenriliuguang.wasi.webgpu.experimental.runtime.cm.WasmtimeCmTriangle
import org.junit.AfterClass
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference

/**
 * Android acceptance: CM Guest `run-triangle` → Wasmtime + abi-cm → Dawn present.
 *
 * Does **not** use [androidx.test.core.app.ActivityScenario] or
 * [android.app.Instrumentation.startActivitySync]: on vivo, starting MainActivity
 * (even with an explicit Intent + extras) often resumes the launcher-task MainActivity whose
 * Intent lacks our extras. ActivityScenario then ignores lifecycle events ("intent does not
 * match"); startActivitySync waits for the Intent-matching instance to idle and can hang tens
 * of seconds → Surface never observed / instrumentation reports "Process crashed".
 *
 * Instead we [android.content.Context.startActivity] asynchronously, then pick whatever
 * [MainActivity] is RESUMED via [ActivityLifecycleMonitorRegistry] and poll the SurfaceView.
 * L2 is skipped whenever androidx.test Instrumentation is attached
 * ([MainActivity.isUnderAndroidXTestInstrumentation]).
 *
 * D2/D3: [DawnWasiWebGpuHost.releaseAllGpuObjects] between presents (WIT dtors miss).
 * D6: one Host + Session for the whole class — never [WasmtimeCmTriangle.Session.close]
 * between tests (process-global CM registry traps / SIGSEGV on linker recreate).
 *
 * Requires device/emulator with WebGPU/Vulkan and CM-patched Bionic `libwasmtime4j.so`
 * (`scripts/build-wasmtime4j-android.ps1`). Prefer `scripts/run-android-instrumented.ps1`.
 */
@RunWith(AndroidJUnit4::class)
class WasmtimeCmTriangleInstrumentedTest {

    @Test
    fun cmGuestDrawsOneShotTriangle() {
        withReadySurface { ctx ->
            runOnCmThread("cm-triangle-instrumented", timeoutSec = 60) {
                val (host, session) = ensureSharedSession(ctx.context)
                val windowHandle = Util.windowFromSurface(ctx.surface)
                session.runTriangle(windowHandle, ctx.width, ctx.height)
                releaseGpuOwnership(host)
            }
        }
    }

    /**
     * Same-process repeat: shared Dawn Host + CM Session, three [runTriangle] calls.
     * Guards process-global registry traps from back-to-back linker recreate (D6).
     */
    @Test
    fun cmGuestRepeatTriangleReusesSession() {
        withReadySurface { ctx ->
            runOnCmThread("cm-triangle-repeat-instrumented", timeoutSec = 120) {
                val (host, session) = ensureSharedSession(ctx.context)
                val windowHandle = Util.windowFromSurface(ctx.surface)
                repeat(REPEAT_COUNT) { i ->
                    session.runTriangle(windowHandle, ctx.width, ctx.height)
                    releaseGpuOwnership(host)
                    if (i < REPEAT_COUNT - 1) {
                        Thread.sleep(SURFACE_RELEASE_SETTLE_MS)
                    }
                }
            }
        }
    }

    private data class ReadySurface(
        val context: android.content.Context,
        val activity: MainActivity,
        val surface: Surface,
        val width: Int,
        val height: Int,
    )

    private fun runOnCmThread(name: String, timeoutSec: Long, block: () -> Unit) {
        val done = CountDownLatch(1)
        val error = AtomicReference<Throwable?>(null)
        Thread({
            try {
                block()
            } catch (t: Throwable) {
                error.set(t)
            } finally {
                done.countDown()
            }
        }, name).start()
        assertTrue("$name timed out", done.await(timeoutSec, TimeUnit.SECONDS))
        val failure = error.get()
        if (failure != null) {
            throw AssertionError("$name failed: ${failure.message}", failure)
        }
    }

    private fun withReadySurface(block: (ReadySurface) -> Unit) {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val context = instrumentation.targetContext

        val intent = Intent(context, MainActivity::class.java).apply {
            putExtra(MainActivity.EXTRA_SKIP_L2_TRIANGLE, true)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        }
        // Async launch only — never startActivitySync (vivo Intent-mismatch hang).
        context.startActivity(intent)

        val activity = waitForResumedMainActivity(timeoutMs = 20_000)
        instrumentation.runOnMainSync {
            activity.window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
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

        val deadline = SystemClock.uptimeMillis() + 20_000
        while (surfaceReady.count > 0 && SystemClock.uptimeMillis() < deadline) {
            instrumentation.runOnMainSync {
                if (surfaceReady.count == 0L) return@runOnMainSync
                if (activity.isFinishing) return@runOnMainSync
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
            "Surface not ready within timeout (activity=${activity.javaClass.simpleName} " +
                "finishing=${activity.isFinishing})",
            surfaceReady.await(1, TimeUnit.SECONDS),
        )
        Thread.sleep(SURFACE_READY_SETTLE_MS)

        try {
            block(
                ReadySurface(
                    context = context,
                    activity = activity,
                    surface = requireNotNull(surfaceRef.get()),
                    width = widthRef.get(),
                    height = heightRef.get(),
                ),
            )
        } finally {
            instrumentation.runOnMainSync { activity.finish() }
            instrumentation.waitForIdleSync()
            Thread.sleep(ACTIVITY_TEARDOWN_SETTLE_MS)
        }
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

    companion object {
        private const val REPEAT_COUNT = 3
        /** Match Demo [io.github.fenriliuguang.wasi.webgpu.demo.onscreen.TriangleCmOneShot] settle. */
        private const val SURFACE_RELEASE_SETTLE_MS = 400L
        private const val SURFACE_READY_SETTLE_MS = 300L
        private const val ACTIVITY_TEARDOWN_SETTLE_MS = 500L

        private val sessionLock = Any()
        private var sharedHost: DawnWasiWebGpuHost? = null
        private var sharedSession: WasmtimeCmTriangle.Session? = null

        private fun ensureSharedSession(
            context: android.content.Context,
        ): Pair<DawnWasiWebGpuHost, WasmtimeCmTriangle.Session> {
            synchronized(sessionLock) {
                val host = sharedHost ?: DawnWasiWebGpuHost.create().also { sharedHost = it }
                val session = sharedSession ?: run {
                    WasmtimeVectorAddAndroid.ensureNativeLoaded()
                    // Drop leftovers from a prior draw before (re)binding the window.
                    releaseGpuOwnership(host)
                    WasmtimeCmTriangleAndroid.openSession(context, host).also { sharedSession = it }
                }
                return host to session
            }
        }

        private fun releaseGpuOwnership(host: DawnWasiWebGpuHost) {
            runCatching { host.releaseAllGpuObjects() }
            host.flushEvents()
            Thread.sleep(SURFACE_RELEASE_SETTLE_MS)
        }

        /**
         * Close linker only after both triangle tests — keeps D6 reuse within the class,
         * then frees the process-global CM registry for later CM classes (vector-add).
         */
        @JvmStatic
        @AfterClass
        fun tearDownSharedSession() {
            synchronized(sessionLock) {
                val session = sharedSession
                sharedSession = null
                val host = sharedHost
                sharedHost = null
                if (host != null) {
                    releaseGpuOwnership(host)
                }
                runCatching { session?.close() }
                if (host != null) {
                    Thread.sleep(100)
                    runCatching { host.close() }
                }
            }
        }
    }
}
