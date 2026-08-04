package io.github.fenriliuguang.wasi.webgpu.experimental.host

/**
 * Host-side failures. P0 maps these to Kotlin exceptions;
 * P1+ may lift selected cases into WIT `result` payloads.
 */
sealed class HostException(message: String, cause: Throwable? = null) :
    RuntimeException(message, cause) {

    class InvalidHandle(val handle: GpuHandle, detail: String) :
        HostException("invalid handle ${handle.raw}: $detail")

    class Unsupported(detail: String) :
        HostException("unsupported in P0 compute subset: $detail")

    class Backend(detail: String, cause: Throwable? = null) :
        HostException(detail, cause)

    class Validation(detail: String) :
        HostException(detail)
}
