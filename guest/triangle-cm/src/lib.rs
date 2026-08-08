//! Experimental Component Model guest for on-screen red triangle.
//! Imports experimental:webgpu-cm/host — NOT compliant wasi:webgpu.
//! Host injects Android native window; Guest only holds `surface`.
//!
//! - `run-triangle`: one-shot configure → draw → present → unconfigure
//! - `init-triangle` / `draw-frame` / `drop-triangle`: host-driven frame loop
//!
//! Slice E: vertices live in a GPU buffer (`set-vertex-buffer` + `@location(0)`),
//! not in a WGSL `vertex_index` constant array.

#![no_main]

use std::cell::RefCell;

wit_bindgen::generate!({
    world: "triangle",
    path: "wit",
});

/// Same triangle coords / color as L2 `TriangleRenderer.SHADER`, but via vertex buffer.
const SHADER: &str = concat!(
    "struct VertexInput {\n",
    "  @location(0) position: vec2f,\n",
    "}\n",
    "@vertex fn vs_main(in: VertexInput) -> @builtin(position) vec4f {\n",
    "  return vec4f(in.position, 0.0, 1.0);\n",
    "}\n",
    "@fragment fn fs_main() -> @location(0) vec4f {\n",
    "  return vec4f(1.0, 0.15, 0.1, 1.0);\n",
    "}\n",
);

/// float32x2: (0, 0.6), (-0.6, -0.6), (0.6, -0.6) — little-endian
const VERTEX_BYTES: [u8; 24] = [
    0x00, 0x00, 0x00, 0x00, // 0.0
    0x9a, 0x99, 0x19, 0x3f, // 0.6
    0x9a, 0x99, 0x19, 0xbf, // -0.6
    0x9a, 0x99, 0x19, 0xbf, // -0.6
    0x9a, 0x99, 0x19, 0x3f, // 0.6
    0x9a, 0x99, 0x19, 0xbf, // -0.6
];

/// VERTEX | COPY_DST (`GpuBufferUsage`)
const USAGE_VERTEX: u32 = 0x28;
/// `androidx.webgpu.VertexFormat.Float32x2`
const VERTEX_FORMAT_FLOAT32X2: u32 = 0x0000_001d;
/// `androidx.webgpu.VertexStepMode.Vertex`
const VERTEX_STEP_VERTEX: u32 = 0x0000_0001;

struct TriangleState {
    device: experimental::webgpu_cm::host::Device,
    queue: experimental::webgpu_cm::host::Queue,
    surface: experimental::webgpu_cm::host::Surface,
    pipeline: experimental::webgpu_cm::host::RenderPipeline,
    vertex_buffer: experimental::webgpu_cm::host::Buffer,
}

thread_local! {
    static STATE: RefCell<Option<TriangleState>> = RefCell::new(None);
}

struct Component;

impl Guest for Component {
    fn run_triangle(window_handle: u64, width: u32, height: u32) -> Result<(), String> {
        use experimental::webgpu_cm::host;

        validate_window(window_handle, width, height)?;

        let adapter = host::request_adapter();
        let device = adapter.request_device();
        let queue = device.get_queue();

        let surface = host::create_surface_from_native_window(window_handle);
        let format = surface.configure(&device, &adapter, width, height);

        let (pipeline, vertex_buffer) = create_pipeline_and_vertices(&device, &queue, format);
        draw_once(&device, &queue, &surface, &pipeline, &vertex_buffer)?;
        surface.unconfigure();

        Ok(())
    }

    fn init_triangle(window_handle: u64, width: u32, height: u32) -> Result<(), String> {
        use experimental::webgpu_cm::host;

        validate_window(window_handle, width, height)?;

        STATE.with(|cell| {
            if cell.borrow().is_some() {
                return Err("triangle already initialized; call drop-triangle first".into());
            }

            let adapter = host::request_adapter();
            let device = adapter.request_device();
            let queue = device.get_queue();
            let surface = host::create_surface_from_native_window(window_handle);
            let format = surface.configure(&device, &adapter, width, height);
            let (pipeline, vertex_buffer) = create_pipeline_and_vertices(&device, &queue, format);

            *cell.borrow_mut() = Some(TriangleState {
                device,
                queue,
                surface,
                pipeline,
                vertex_buffer,
            });
            Ok(())
        })
    }

    fn draw_frame() -> Result<(), String> {
        STATE.with(|cell| {
            let state = cell.borrow();
            let state = state
                .as_ref()
                .ok_or_else(|| "triangle not initialized; call init-triangle first".to_string())?;
            draw_once(
                &state.device,
                &state.queue,
                &state.surface,
                &state.pipeline,
                &state.vertex_buffer,
            )
        })
    }

    fn drop_triangle() -> Result<(), String> {
        STATE.with(|cell| {
            let Some(state) = cell.borrow_mut().take() else {
                return Err("triangle not initialized".into());
            };
            state.surface.unconfigure();
            // Resources drop with `state`.
            Ok(())
        })
    }
}

fn validate_window(window_handle: u64, width: u32, height: u32) -> Result<(), String> {
    if width == 0 || height == 0 {
        return Err(format!("invalid surface size {width}x{height}"));
    }
    if window_handle == 0 {
        return Err("window-handle is null".into());
    }
    Ok(())
}

fn create_pipeline_and_vertices(
    device: &experimental::webgpu_cm::host::Device,
    queue: &experimental::webgpu_cm::host::Queue,
    format: u32,
) -> (
    experimental::webgpu_cm::host::RenderPipeline,
    experimental::webgpu_cm::host::Buffer,
) {
    use experimental::webgpu_cm::host::{
        BufferDescriptor, VertexAttribute, VertexBufferLayout,
    };

    let shader = device.create_shader_module(SHADER);
    let layouts = vec![VertexBufferLayout {
        array_stride: 8,
        step_mode: VERTEX_STEP_VERTEX,
        attributes: vec![VertexAttribute {
            format: VERTEX_FORMAT_FLOAT32X2,
            offset: 0,
            shader_location: 0,
        }],
    }];
    let pipeline = device.create_render_pipeline_triangle_buffers(&shader, format, &layouts);

    let vertex_buffer = device.create_buffer(&BufferDescriptor {
        size: VERTEX_BYTES.len() as u64,
        usage: USAGE_VERTEX,
        mapped_at_creation: false,
        label: None,
    });
    queue.write_buffer(&vertex_buffer, 0, &VERTEX_BYTES);

    (pipeline, vertex_buffer)
}

fn draw_once(
    device: &experimental::webgpu_cm::host::Device,
    queue: &experimental::webgpu_cm::host::Queue,
    surface: &experimental::webgpu_cm::host::Surface,
    pipeline: &experimental::webgpu_cm::host::RenderPipeline,
    vertex_buffer: &experimental::webgpu_cm::host::Buffer,
) -> Result<(), String> {
    let view = surface.get_current_texture_view();
    let encoder = device.create_command_encoder();
    let pass = encoder.begin_render_pass_clear(&view, 0.08, 0.09, 0.12, 1.0);
    pass.set_pipeline(pipeline);
    pass.set_vertex_buffer(0, vertex_buffer, 0, VERTEX_BYTES.len() as u64);
    pass.draw(3);
    pass.end();

    let cmd = encoder.finish();
    queue.submit1(cmd);
    surface.present();
    Ok(())
}

export!(Component with_types_in self);
