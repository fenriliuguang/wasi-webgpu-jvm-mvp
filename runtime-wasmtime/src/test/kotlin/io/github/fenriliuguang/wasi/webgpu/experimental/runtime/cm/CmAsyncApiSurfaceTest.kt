package io.github.fenriliuguang.wasi.webgpu.experimental.runtime.cm

import ai.tegmentum.wasmtime4j.component.ComponentHostFunction
import ai.tegmentum.wasmtime4j.component.FutureAny
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * true-cm-async slice A (gate evidence, no natives required).
 *
 * wasmtime4j 47.0.2-1.5.0 exposes opaque [FutureAny] handles and sync-shaped
 * [ComponentHostFunction] factories, but **no** host API to create / write /
 * complete / reject a Component Model future. That blocks WIT `async func`
 * e2e without a native overlay (out of scope; no upstream PR).
 */
class CmAsyncApiSurfaceTest {

    @Test
    fun futureAnyHasNoWriteOrCompleteApi() {
        val methodNames = FutureAny::class.java.methods.map { it.name }.toSet()
        val forbidden = listOf(
            "write", "complete", "reject", "resolve", "fail",
            "writeValue", "completeValue", "rejectValue",
            "futureWrite", "completeFuture", "rejectFuture",
        )
        for (name in forbidden) {
            assertFalse(
                "FutureAny must not expose host future writer `$name` " +
                    "(slice A gate: no CM future complete/reject API)",
                methodNames.any { it.equals(name, ignoreCase = true) },
            )
        }
        // Documented surface: handle + close (+ typed payload metadata).
        assertTrue(methodNames.contains("getHandle"))
        assertTrue(methodNames.contains("close"))
        assertTrue(methodNames.contains("isValid"))
    }

    @Test
    fun componentHostFunctionFactoriesAreSyncOnly() {
        val methodNames = ComponentHostFunction::class.java.methods.map { it.name }.toSet()
        val asyncFactories = listOf(
            "createAsync",
            "singleValueAsync",
            "voidFunctionAsync",
            "fromCompletableFuture",
            "async",
        )
        for (name in asyncFactories) {
            assertFalse(
                "ComponentHostFunction must not expose async factory `$name`",
                methodNames.any { it.equals(name, ignoreCase = true) },
            )
        }
        assertTrue(methodNames.contains("create") || methodNames.contains("singleValue"))
    }
}
