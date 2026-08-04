package io.github.fenriliuguang.wasi.webgpu.experimental.abicm

/**
 * Experimental Component Model ABI constants for compute vector-add.
 *
 * NOT compliant wasi:webgpu — package is experimental; handles are WIT resources
 * (internally mapped to L2 [io.github.fenriliuguang.wasi.webgpu.experimental.host.GpuHandle]).
 */
object AbiCm {
    const val PACKAGE: String = "experimental:webgpu-cm"
    const val INTERFACE: String = "host"
    const val VERSION: String = "0.1.0"

    /** Full import interface id as emitted by wit-bindgen / wasm-tools. */
    const val IMPORT_INTERFACE: String = "$PACKAGE/$INTERFACE@$VERSION"

    const val EXPORT_RUN_VECTOR_ADD: String = "run-vector-add"

    object Resource {
        const val ADAPTER = "adapter"
        const val DEVICE = "device"
        const val QUEUE = "queue"
        const val BUFFER = "buffer"
        const val SHADER_MODULE = "shader-module"
        const val BIND_GROUP_LAYOUT = "bind-group-layout"
        const val BIND_GROUP = "bind-group"
        const val COMPUTE_PIPELINE = "compute-pipeline"
        const val COMMAND_ENCODER = "command-encoder"
        const val COMPUTE_PASS_ENCODER = "compute-pass-encoder"
        const val COMMAND_BUFFER = "command-buffer"

        val ALL: List<String> = listOf(
            ADAPTER,
            DEVICE,
            QUEUE,
            BUFFER,
            SHADER_MODULE,
            BIND_GROUP_LAYOUT,
            BIND_GROUP,
            COMPUTE_PIPELINE,
            COMMAND_ENCODER,
            COMPUTE_PASS_ENCODER,
            COMMAND_BUFFER,
        )
    }

    object Func {
        const val REQUEST_ADAPTER = "request-adapter"
        const val ADAPTER_REQUEST_DEVICE = "[method]adapter.request-device"
        const val DEVICE_GET_QUEUE = "[method]device.get-queue"
        const val DEVICE_CREATE_BUFFER = "[method]device.create-buffer"
        const val QUEUE_WRITE_BUFFER = "[method]queue.write-buffer"
        const val DEVICE_CREATE_SHADER_MODULE = "[method]device.create-shader-module"
        const val DEVICE_CREATE_BIND_GROUP_LAYOUT_STORAGE3 =
            "[method]device.create-bind-group-layout-storage3"
        const val DEVICE_CREATE_BIND_GROUP3 = "[method]device.create-bind-group3"
        const val DEVICE_CREATE_COMPUTE_PIPELINE = "[method]device.create-compute-pipeline"
        const val DEVICE_CREATE_COMMAND_ENCODER = "[method]device.create-command-encoder"
        const val COMMAND_ENCODER_BEGIN_COMPUTE_PASS =
            "[method]command-encoder.begin-compute-pass"
        const val COMPUTE_PASS_SET_PIPELINE = "[method]compute-pass-encoder.set-pipeline"
        const val COMPUTE_PASS_SET_BIND_GROUP = "[method]compute-pass-encoder.set-bind-group"
        const val COMPUTE_PASS_DISPATCH_WORKGROUPS =
            "[method]compute-pass-encoder.dispatch-workgroups"
        const val COMPUTE_PASS_END = "[method]compute-pass-encoder.end"
        const val COMMAND_ENCODER_COPY_BUFFER_TO_BUFFER =
            "[method]command-encoder.copy-buffer-to-buffer"
        const val COMMAND_ENCODER_FINISH = "[method]command-encoder.finish"
        const val QUEUE_SUBMIT1 = "[method]queue.submit1"
        const val BUFFER_MAP_READ = "[method]buffer.map-read"
        const val BUFFER_GET_MAPPED_RANGE = "[method]buffer.get-mapped-range"
        const val BUFFER_UNMAP = "[method]buffer.unmap"
    }
}
