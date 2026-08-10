package io.github.fenriliuguang.wasi.webgpu.experimental.abi

/**
 * Experimental core-wasm ABI for P1 (not Component Model / not compliant wasi:webgpu).
 */
object AbiMvp {
    const val MODULE: String = "wasi-webgpu-mvp"

    object Func {
        const val REQUEST_ADAPTER = "request_adapter"
        const val ADAPTER_REQUEST_DEVICE = "adapter_request_device"
        const val DEVICE_GET_QUEUE = "device_get_queue"
        const val DEVICE_CREATE_BUFFER = "device_create_buffer"
        const val QUEUE_WRITE_BUFFER = "queue_write_buffer"
        const val DEVICE_CREATE_SHADER_MODULE = "device_create_shader_module"
        const val DEVICE_CREATE_BIND_GROUP_LAYOUT_STORAGE3 = "device_create_bind_group_layout_storage3"
        const val DEVICE_CREATE_BIND_GROUP3 = "device_create_bind_group3"
        const val DEVICE_CREATE_COMPUTE_PIPELINE = "device_create_compute_pipeline"
        const val DEVICE_CREATE_COMMAND_ENCODER = "device_create_command_encoder"
        const val COMMAND_ENCODER_BEGIN_COMPUTE_PASS = "command_encoder_begin_compute_pass"
        const val COMPUTE_PASS_SET_PIPELINE = "compute_pass_set_pipeline"
        const val COMPUTE_PASS_SET_BIND_GROUP = "compute_pass_set_bind_group"
        const val COMPUTE_PASS_DISPATCH = "compute_pass_dispatch"
        const val COMPUTE_PASS_END = "compute_pass_end"
        const val COMMAND_ENCODER_COPY_BUFFER_TO_BUFFER = "command_encoder_copy_buffer_to_buffer"
        const val COMMAND_ENCODER_FINISH = "command_encoder_finish"
        const val QUEUE_SUBMIT1 = "queue_submit1"
        const val BUFFER_MAP_READ = "buffer_map_read"
        const val BUFFER_GET_MAPPED_RANGE = "buffer_get_mapped_range"
        const val BUFFER_UNMAP = "buffer_unmap"

        // --- Surface / render (flat helpers; engineering-handoff B) ---
        const val CREATE_SURFACE_FROM_NATIVE_WINDOW = "create_surface_from_native_window"
        const val SURFACE_CONFIGURE = "surface_configure"
        const val SURFACE_GET_CURRENT_TEXTURE_VIEW = "surface_get_current_texture_view"
        const val SURFACE_PRESENT = "surface_present"
        const val SURFACE_UNCONFIGURE = "surface_unconfigure"
        const val DEVICE_CREATE_TEXTURE_2D = "device_create_texture_2d"
        const val TEXTURE_CREATE_VIEW = "texture_create_view"
        const val QUEUE_WRITE_TEXTURE = "queue_write_texture"
        const val DEVICE_CREATE_RENDER_PIPELINE_TRIANGLE = "device_create_render_pipeline_triangle"
        const val COMMAND_ENCODER_BEGIN_RENDER_PASS_CLEAR = "command_encoder_begin_render_pass_clear"
        const val COMMAND_ENCODER_BEGIN_RENDER_PASS_COLOR_DEPTH =
            "command_encoder_begin_render_pass_color_depth"
        const val RENDER_PASS_SET_PIPELINE = "render_pass_set_pipeline"
        const val RENDER_PASS_SET_BIND_GROUP = "render_pass_set_bind_group"
        const val RENDER_PASS_SET_VERTEX_BUFFER = "render_pass_set_vertex_buffer"
        const val RENDER_PASS_DRAW = "render_pass_draw"
        const val RENDER_PASS_END = "render_pass_end"
    }

    /** Guest export that runs the vector-add scenario. */
    const val EXPORT_RUN_VECTOR_ADD: String = "run_vector_add"
}
