package io.github.fenriliuguang.wasi.webgpu.experimental.host

import org.junit.Assert.assertEquals
import org.junit.Test

class HostErrorMappingTest {

    @Test
    fun mapsValidationToTypeAndRangeKinds() {
        val ex = HostException.Validation("bad descriptor")
        assertEquals(GpuErrorKind.ValidationError, HostErrorMapping.gpuErrorKind(ex))
        assertEquals(RequestDeviceErrorKind.TypeError, HostErrorMapping.requestDevice(ex))
        assertEquals(MapAsyncErrorKind.RangeError, HostErrorMapping.mapAsync(ex))
        assertEquals(GetMappedRangeErrorKind.TypeError, HostErrorMapping.getMappedRange(ex))
        assertEquals(GpuPipelineErrorReason.Validation, HostErrorMapping.pipelineReason(ex))
        assertEquals("type-error", HostErrorMapping.witCase(HostErrorMapping.requestDevice(ex)))
        assertEquals("validation", HostErrorMapping.witCase(HostErrorMapping.pipelineReason(ex)))
    }

    @Test
    fun mapsUnsupportedAndBackendToOperationOrInternal() {
        val unsupported = HostException.Unsupported("not wired")
        val backend = HostException.Backend("dawn failed")
        assertEquals(GpuErrorKind.InternalError, HostErrorMapping.gpuErrorKind(unsupported))
        assertEquals(RequestDeviceErrorKind.OperationError, HostErrorMapping.requestDevice(unsupported))
        assertEquals(MapAsyncErrorKind.OperationError, HostErrorMapping.mapAsync(unsupported))
        assertEquals(MapAsyncErrorKind.AbortError, HostErrorMapping.mapAsync(backend))
        assertEquals(GpuPipelineErrorReason.Internal, HostErrorMapping.pipelineReason(unsupported))
        assertEquals(UnmapErrorKind.AbortError, HostErrorMapping.unmap(unsupported))
    }

    @Test
    fun mapsInvalidHandleToValidationFamily() {
        val ex = HostException.InvalidHandle(GpuHandle(3), "expected Device")
        assertEquals(GpuErrorKind.ValidationError, HostErrorMapping.gpuErrorKind(ex))
        assertEquals(RequestDeviceErrorKind.OperationError, HostErrorMapping.requestDevice(ex))
        assertEquals(GpuPipelineErrorReason.Validation, HostErrorMapping.pipelineReason(ex))
    }
}
