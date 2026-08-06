//! Experimental Component Model guest for a single-shot on-screen red triangle.
//! Imports experimental:webgpu-cm/host — NOT compliant wasi:webgpu.
//! Host injects Android native window; Guest only holds `surface`.

#![no_main]

wit_bindgen::generate!({
    world: "triangle",
    path: "wit",
});

/// Must match `TriangleRenderer.SHADER` (L2 Kotlin demo) for visual parity.
const SHADER: &str = concat!(
    "@vertex fn vs_main(@builtin(vertex_index) vertexIndex : u32) -> @builtin(position) vec4f {\n",
    "  let pos = array(\n",
    "    vec2f( 0.0,  0.6),\n",
    "    vec2f(-0.6, -0.6),\n",
    "    vec2f( 0.6, -0.6)\n",
    "  );\n",
    "  return vec4f(pos[vertexIndex], 0.0, 1.0);\n",
    "}\n",
    "@fragment fn fs_main() -> @location(0) vec4f {\n",
    "  return vec4f(1.0, 0.15, 0.1, 1.0);\n",
    "}\n",
);

struct Component;

impl Guest for Component {
    fn run_triangle(window_handle: u64, width: u32, height: u32) -> Result<(), String> {
        use experimental::webgpu_cm::host;

        if width == 0 || height == 0 {
            return Err(format!("invalid surface size {width}x{height}"));
        }
        if window_handle == 0 {
            return Err("window-handle is null".into());
        }

        let adapter = host::request_adapter();
        let device = adapter.request_device();
        let queue = device.get_queue();

        let surface = host::create_surface_from_native_window(window_handle);
        let format = surface.configure(&device, &adapter, width, height);

        let shader = device.create_shader_module(SHADER);
        let pipeline = device.create_render_pipeline_triangle(&shader, format);

        let view = surface.get_current_texture_view();
        let encoder = device.create_command_encoder();
        let pass = encoder.begin_render_pass_clear(&view, 0.08, 0.09, 0.12, 1.0);
        pass.set_pipeline(&pipeline);
        pass.draw(3);
        pass.end();

        let cmd = encoder.finish();
        queue.submit1(cmd);
        surface.present();
        surface.unconfigure();

        Ok(())
    }
}

export!(Component with_types_in self);
