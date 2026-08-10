package io.github.fenriliuguang.wasi.webgpu.experimental.runtime.cm

import ai.tegmentum.wasmtime4j.Engine
import ai.tegmentum.wasmtime4j.RuntimeType
import ai.tegmentum.wasmtime4j.component.ComponentEngineConfig
import ai.tegmentum.wasmtime4j.component.ComponentHostFunction
import ai.tegmentum.wasmtime4j.component.ComponentLinker
import ai.tegmentum.wasmtime4j.component.ComponentResourceDefinition
import ai.tegmentum.wasmtime4j.component.ComponentVal
import ai.tegmentum.wasmtime4j.factory.WasmRuntimeFactory
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * true-cm-async slice A desktop spike (CM-patched natives).
 *
 * Proves:
 * 1. Engine can enable `concurrencySupport` + `asyncSupport` + `wasmComponentModelAsync`
 *    on patched desktop natives (`component-model` cargo feature already pulls
 *    `component-model-async`).
 * 2. Linker `setAsyncSupport` + `defineFunctionAsync` + `defineResource` register without
 *    throwing (process-global resource registry / async re-hook coexistence at **registration**).
 *
 * Does **not** prove WIT `async func` complete/reject e2e — see [CmAsyncApiSurfaceTest]
 * and docs/scheme/true-cm-async.md slice A gate.
 */
class CmAsyncHostImportSpikeTest {

    @Before
    fun requireCmNatives() {
        CmNativesGate.assumePatchedNativesPresent()
    }

    @Test
    fun asyncEngineRegistersAsyncHostImportAndResource() {
        val runtime = WasmRuntimeFactory.create(RuntimeType.JNI)
        try {
            assertTrue(
                "runtime must report Component Model support",
                runtime.supportsComponentModel(),
            )
            // Wasmtime 47: CM async needs concurrencySupport (not only legacy asyncSupport).
            val engineConfig = ComponentEngineConfig()
                .toEngineConfig()
                .concurrencySupport(true)
                .asyncSupport(true)
                .wasmComponentModelAsync(true)
            val engine: Engine = runtime.createEngine(engineConfig)
            try {
                val linker: ComponentLinker<Any> = runtime.createComponentLinker(engine)
                linker.setAsyncSupport(true)

                val ns = "experimental:webgpu-cm-async-spike"
                val iface = "spike@0.0.1"
                val resource = ComponentResourceDefinition.builder<Any>("widget").build()
                linker.defineResource(ns, iface, "widget", resource)

                val hostFn = ComponentHostFunction.singleValue {
                    ComponentVal.u32(42)
                }
                // func_new_async registration path (still sync-shaped Java callback).
                linker.defineFunctionAsync(
                    "$ns/$iface#ping",
                    hostFn,
                )
            } finally {
                runCatching { engine.close() }
            }
        } finally {
            runCatching { runtime.close() }
        }
    }
}
