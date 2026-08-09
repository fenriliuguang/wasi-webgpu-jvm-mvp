//! Experimental Component Model guest: slow rotating textured cube.
//! Imports experimental:webgpu-cm/host — NOT compliant wasi:webgpu.
//! Host injects Android native window; Guest only holds `surface`.
//!
//! guest-descriptor-cube B: standard descriptors only
//! (create-render-pipeline / begin-render-pass / submit(list) /
//! create-bind-group* / create-pipeline-layout / write-texture /
//! render-pass set-bind-group) + depth-stencil + MVP uniform.

#![no_main]

use std::cell::RefCell;
use std::f32::consts::FRAC_PI_4;

wit_bindgen::generate!({
    world: "cube",
    path: "wit",
});

const SHADER: &str = concat!(
    "struct Uniforms {\n",
    "  mvp: mat4x4<f32>,\n",
    "}\n",
    "@group(0) @binding(0) var<uniform> u : Uniforms;\n",
    "@group(0) @binding(1) var samp : sampler;\n",
    "@group(0) @binding(2) var tex : texture_2d<f32>;\n",
    "\n",
    "struct VertexInput {\n",
    "  @location(0) position: vec3f,\n",
    "  @location(1) uv: vec2f,\n",
    "}\n",
    "struct VertexOutput {\n",
    "  @builtin(position) position: vec4f,\n",
    "  @location(0) uv: vec2f,\n",
    "}\n",
    "\n",
    "@vertex fn vs_main(in: VertexInput) -> VertexOutput {\n",
    "  var out: VertexOutput;\n",
    "  out.position = u.mvp * vec4f(in.position, 1.0);\n",
    "  out.uv = in.uv;\n",
    "  return out;\n",
    "}\n",
    "\n",
    "@fragment fn fs_main(in: VertexOutput) -> @location(0) vec4f {\n",
    "  return textureSample(tex, samp, in.uv);\n",
    "}\n",
);

/// VERTEX | COPY_DST
const USAGE_VERTEX: u32 = 0x28;
/// UNIFORM | COPY_DST
const USAGE_UNIFORM: u32 = 0x48;
/// COPY_DST | TEXTURE_BINDING
const USAGE_TEX: u32 = 0x06;
/// RENDER_ATTACHMENT
const USAGE_DEPTH: u32 = 0x10;

const VERTEX_FORMAT_FLOAT32X3: u32 = 0x0000_001e;
const VERTEX_FORMAT_FLOAT32X2: u32 = 0x0000_001d;
const VERTEX_STEP_VERTEX: u32 = 0x0000_0001;
const TOPOLOGY_TRIANGLE_LIST: u32 = 4;
const FORMAT_RGBA8_UNORM: u32 = 0x16;
const FORMAT_DEPTH24_PLUS: u32 = 0x2e;
const COMPARE_LESS: u32 = 2;
const LOAD_OP_CLEAR: u32 = 2;
const STORE_OP_STORE: u32 = 1;
const STAGE_VERTEX: u32 = 0x01;
const STAGE_FRAGMENT: u32 = 0x02;
const BINDING_UNIFORM: u32 = 0;
const SAMPLER_FILTERING: u32 = 0;
const SAMPLE_FLOAT: u32 = 0;
const VIEW_DIM_D2: u32 = 1;
const TEX_DIM_D2: u32 = 0x02;

const TEX_SIZE: u32 = 64;
/// ~0.02 rad/frame at ~16ms ≈ 0.4 rad/s
const ANGLE_STEP: f32 = 0.02;
const STRIDE: u64 = 20;
const VERTEX_COUNT: u32 = 36;

struct CubeState {
    device: experimental::webgpu_cm::host::Device,
    queue: experimental::webgpu_cm::host::Queue,
    surface: experimental::webgpu_cm::host::Surface,
    pipeline: experimental::webgpu_cm::host::RenderPipeline,
    vertex_buffer: experimental::webgpu_cm::host::Buffer,
    uniform_buffer: experimental::webgpu_cm::host::Buffer,
    bind_group: experimental::webgpu_cm::host::BindGroup,
    depth_view: experimental::webgpu_cm::host::TextureView,
    /// Keep GPU resources alive while bind-group / depth view are used.
    _depth_texture: experimental::webgpu_cm::host::Texture,
    _color_view: experimental::webgpu_cm::host::TextureView,
    _color_texture: experimental::webgpu_cm::host::Texture,
    _sampler: experimental::webgpu_cm::host::Sampler,
    _bgl: experimental::webgpu_cm::host::BindGroupLayout,
    _pipeline_layout: experimental::webgpu_cm::host::PipelineLayout,
    angle: f32,
    aspect: f32,
}

thread_local! {
    static STATE: RefCell<Option<CubeState>> = RefCell::new(None);
}

struct Component;

impl Guest for Component {
    fn run_cube(window_handle: u64, width: u32, height: u32) -> Result<(), String> {
        do_init_cube(window_handle, width, height)?;
        let result = do_draw_frame();
        let _ = do_drop_cube();
        result
    }

    fn init_cube(window_handle: u64, width: u32, height: u32) -> Result<(), String> {
        do_init_cube(window_handle, width, height)
    }

    fn draw_frame() -> Result<(), String> {
        do_draw_frame()
    }

    fn drop_cube() -> Result<(), String> {
        do_drop_cube()
    }
}

fn do_init_cube(window_handle: u64, width: u32, height: u32) -> Result<(), String> {
    use experimental::webgpu_cm::host;

    validate_window(window_handle, width, height)?;

    STATE.with(|cell| {
        if cell.borrow().is_some() {
            return Err("cube already initialized; call drop-cube first".into());
        }

        let adapter = host::request_adapter();
        let device = adapter.request_device();
        let queue = device.get_queue();
        let surface = host::create_surface_from_native_window(window_handle);
        let format = surface.configure(&device, &adapter, width, height);
        let resources = create_resources(&device, &queue, format, width, height)?;

        *cell.borrow_mut() = Some(CubeState {
            device,
            queue,
            surface,
            pipeline: resources.pipeline,
            vertex_buffer: resources.vertex_buffer,
            uniform_buffer: resources.uniform_buffer,
            bind_group: resources.bind_group,
            depth_view: resources.depth_view,
            _depth_texture: resources.depth_texture,
            _color_view: resources.color_view,
            _color_texture: resources.color_texture,
            _sampler: resources.sampler,
            _bgl: resources.bgl,
            _pipeline_layout: resources.pipeline_layout,
            angle: 0.0,
            aspect: width as f32 / height as f32,
        });
        Ok(())
    })
}

fn do_draw_frame() -> Result<(), String> {
    STATE.with(|cell| {
        let mut state = cell.borrow_mut();
        let state = state
            .as_mut()
            .ok_or_else(|| "cube not initialized; call init-cube first".to_string())?;
        draw_once(state)
    })
}

fn do_drop_cube() -> Result<(), String> {
    STATE.with(|cell| {
        let Some(state) = cell.borrow_mut().take() else {
            return Err("cube not initialized".into());
        };
        state.surface.unconfigure();
        Ok(())
    })
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

struct CubeResources {
    pipeline: experimental::webgpu_cm::host::RenderPipeline,
    vertex_buffer: experimental::webgpu_cm::host::Buffer,
    uniform_buffer: experimental::webgpu_cm::host::Buffer,
    bind_group: experimental::webgpu_cm::host::BindGroup,
    depth_view: experimental::webgpu_cm::host::TextureView,
    depth_texture: experimental::webgpu_cm::host::Texture,
    color_view: experimental::webgpu_cm::host::TextureView,
    color_texture: experimental::webgpu_cm::host::Texture,
    sampler: experimental::webgpu_cm::host::Sampler,
    bgl: experimental::webgpu_cm::host::BindGroupLayout,
    pipeline_layout: experimental::webgpu_cm::host::PipelineLayout,
}

fn create_resources(
    device: &experimental::webgpu_cm::host::Device,
    queue: &experimental::webgpu_cm::host::Queue,
    surface_format: u32,
    width: u32,
    height: u32,
) -> Result<CubeResources, String> {
    use experimental::webgpu_cm::host::{
        BindGroupDescriptor, BindGroupEntry, BindGroupLayoutDescriptor, BindGroupLayoutEntry,
        BufferBinding, BufferBindingLayout, BufferDescriptor, ColorTargetState,
        DepthStencilState, Extent3D, FragmentState, PipelineLayoutDescriptor, PrimitiveState,
        RenderPipelineDescriptor, SamplerBindingLayout, SamplerDescriptor, TextureBindingLayout,
        TextureDescriptor, VertexAttribute, VertexBufferLayout, VertexState,
    };

    let shader = device.create_shader_module(SHADER);

    let bgl = device.create_bind_group_layout(&BindGroupLayoutDescriptor {
        entries: vec![
            BindGroupLayoutEntry {
                binding: 0,
                visibility: STAGE_VERTEX,
                buffer: Some(BufferBindingLayout {
                    type_: BINDING_UNIFORM,
                    has_dynamic_offset: false,
                    min_binding_size: 64,
                }),
                sampler: None,
                texture: None,
            },
            BindGroupLayoutEntry {
                binding: 1,
                visibility: STAGE_FRAGMENT,
                buffer: None,
                sampler: Some(SamplerBindingLayout {
                    type_: SAMPLER_FILTERING,
                }),
                texture: None,
            },
            BindGroupLayoutEntry {
                binding: 2,
                visibility: STAGE_FRAGMENT,
                buffer: None,
                sampler: None,
                texture: Some(TextureBindingLayout {
                    sample_type: SAMPLE_FLOAT,
                    view_dimension: VIEW_DIM_D2,
                    multisampled: false,
                }),
            },
        ],
        label: Some("cube-bgl".into()),
    });

    let pipeline_layout = device.create_pipeline_layout(&PipelineLayoutDescriptor {
        bind_group_layouts: vec![&bgl],
        label: Some("cube-pl".into()),
    });

    let pipeline = device.create_render_pipeline(&RenderPipelineDescriptor {
        vertex: VertexState {
            module: &shader,
            entry_point: Some("vs_main".into()),
            buffers: vec![VertexBufferLayout {
                array_stride: STRIDE,
                step_mode: VERTEX_STEP_VERTEX,
                attributes: vec![
                    VertexAttribute {
                        format: VERTEX_FORMAT_FLOAT32X3,
                        offset: 0,
                        shader_location: 0,
                    },
                    VertexAttribute {
                        format: VERTEX_FORMAT_FLOAT32X2,
                        offset: 12,
                        shader_location: 1,
                    },
                ],
            }],
        },
        fragment: FragmentState {
            module: &shader,
            entry_point: Some("fs_main".into()),
            targets: vec![ColorTargetState {
                format: surface_format,
            }],
        },
        layout: &pipeline_layout,
        primitive: Some(PrimitiveState {
            topology: TOPOLOGY_TRIANGLE_LIST,
        }),
        depth_stencil: Some(DepthStencilState {
            format: FORMAT_DEPTH24_PLUS,
            depth_write_enabled: true,
            depth_compare: COMPARE_LESS,
        }),
        label: Some("cube-pipeline".into()),
    });

    let verts = cube_vertices();
    let vertex_buffer = device.create_buffer(&BufferDescriptor {
        size: verts.len() as u64,
        usage: USAGE_VERTEX,
        mapped_at_creation: false,
        label: Some("cube-vb".into()),
    });
    queue.write_buffer(&vertex_buffer, 0, &verts);

    let uniform_buffer = device.create_buffer(&BufferDescriptor {
        size: 64,
        usage: USAGE_UNIFORM,
        mapped_at_creation: false,
        label: Some("cube-ub".into()),
    });

    let color_texture = device.create_texture(&TextureDescriptor {
        size: Extent3D {
            width: TEX_SIZE,
            height: TEX_SIZE,
            depth_or_array_layers: 1,
        },
        format: FORMAT_RGBA8_UNORM,
        usage: USAGE_TEX,
        mip_level_count: 1,
        sample_count: 1,
        dimension: TEX_DIM_D2,
        label: Some("cube-tex".into()),
    });
    let texels = checkerboard_rgba(TEX_SIZE as usize);
    queue.write_texture(
        &color_texture,
        &texels,
        TEX_SIZE,
        TEX_SIZE,
        TEX_SIZE * 4,
    );
    let color_view = color_texture.create_view();
    let sampler = device.create_sampler(Some(&SamplerDescriptor {
        label: Some("cube-sampler".into()),
    }));

    let bind_group = device.create_bind_group(&BindGroupDescriptor {
        layout: &bgl,
        entries: vec![
            BindGroupEntry {
                binding: 0,
                buffer: Some(BufferBinding {
                    buffer: &uniform_buffer,
                    offset: 0,
                    size: Some(64),
                }),
                sampler: None,
                texture_view: None,
            },
            BindGroupEntry {
                binding: 1,
                buffer: None,
                sampler: Some(&sampler),
                texture_view: None,
            },
            BindGroupEntry {
                binding: 2,
                buffer: None,
                sampler: None,
                texture_view: Some(&color_view),
            },
        ],
        label: Some("cube-bg".into()),
    });

    let depth_texture = device.create_texture(&TextureDescriptor {
        size: Extent3D {
            width,
            height,
            depth_or_array_layers: 1,
        },
        format: FORMAT_DEPTH24_PLUS,
        usage: USAGE_DEPTH,
        mip_level_count: 1,
        sample_count: 1,
        dimension: TEX_DIM_D2,
        label: Some("cube-depth".into()),
    });
    let depth_view = depth_texture.create_view();

    Ok(CubeResources {
        pipeline,
        vertex_buffer,
        uniform_buffer,
        bind_group,
        depth_view,
        depth_texture,
        color_view,
        color_texture,
        sampler,
        bgl,
        pipeline_layout,
    })
}

fn draw_once(state: &mut CubeState) -> Result<(), String> {
    use experimental::webgpu_cm::host::{
        Color, RenderPassColorAttachment, RenderPassDepthStencilAttachment, RenderPassDescriptor,
    };

    state.angle += ANGLE_STEP;
    let mvp = mvp_matrix(state.angle, state.aspect);
    let mut bytes = [0u8; 64];
    for (i, f) in mvp.iter().enumerate() {
        bytes[i * 4..(i + 1) * 4].copy_from_slice(&f.to_le_bytes());
    }
    state
        .queue
        .write_buffer(&state.uniform_buffer, 0, &bytes);

    let view = state.surface.get_current_texture_view();
    let encoder = state.device.create_command_encoder();
    let pass = encoder.begin_render_pass(&RenderPassDescriptor {
        color_attachments: vec![RenderPassColorAttachment {
            view: &view,
            clear_value: Some(Color {
                r: 0.08,
                g: 0.09,
                b: 0.12,
                a: 1.0,
            }),
            load_op: LOAD_OP_CLEAR,
            store_op: STORE_OP_STORE,
        }],
        depth_stencil_attachment: Some(RenderPassDepthStencilAttachment {
            view: &state.depth_view,
            depth_clear_value: 1.0,
            depth_load_op: LOAD_OP_CLEAR,
            depth_store_op: STORE_OP_STORE,
        }),
        label: Some("cube-pass".into()),
    });
    pass.set_pipeline(&state.pipeline);
    pass.set_bind_group(0, &state.bind_group);
    pass.set_vertex_buffer(0, &state.vertex_buffer, 0, (VERTEX_COUNT as u64) * STRIDE);
    pass.draw(VERTEX_COUNT);
    pass.end();

    let cmd = encoder.finish();
    state.queue.submit(&[&cmd]);
    state.surface.present();
    Ok(())
}

/// Original CC0 64×64 RGBA8 checkerboard for this project (see ATTRIBUTION.md).
fn checkerboard_rgba(size: usize) -> Vec<u8> {
    let cell = 8usize;
    let mut data = vec![0u8; size * size * 4];
    for y in 0..size {
        for x in 0..size {
            let on = ((x / cell) + (y / cell)) % 2 == 0;
            let i = (y * size + x) * 4;
            if on {
                data[i] = 220;
                data[i + 1] = 90;
                data[i + 2] = 40;
                data[i + 3] = 255;
            } else {
                data[i] = 40;
                data[i + 1] = 100;
                data[i + 2] = 190;
                data[i + 3] = 255;
            }
        }
    }
    data
}

/// Interleaved float32x3 position + float32x2 uv; 36 non-indexed verts.
fn cube_vertices() -> Vec<u8> {
    // 6 faces × 2 tris × 3 verts; each face uses full UV [0,1]².
    let faces: [[([f32; 3], [f32; 2]); 6]; 6] = [
        // +Z
        [
            ([-0.5, -0.5, 0.5], [0.0, 1.0]),
            ([0.5, -0.5, 0.5], [1.0, 1.0]),
            ([0.5, 0.5, 0.5], [1.0, 0.0]),
            ([-0.5, -0.5, 0.5], [0.0, 1.0]),
            ([0.5, 0.5, 0.5], [1.0, 0.0]),
            ([-0.5, 0.5, 0.5], [0.0, 0.0]),
        ],
        // -Z
        [
            ([0.5, -0.5, -0.5], [0.0, 1.0]),
            ([-0.5, -0.5, -0.5], [1.0, 1.0]),
            ([-0.5, 0.5, -0.5], [1.0, 0.0]),
            ([0.5, -0.5, -0.5], [0.0, 1.0]),
            ([-0.5, 0.5, -0.5], [1.0, 0.0]),
            ([0.5, 0.5, -0.5], [0.0, 0.0]),
        ],
        // +X
        [
            ([0.5, -0.5, 0.5], [0.0, 1.0]),
            ([0.5, -0.5, -0.5], [1.0, 1.0]),
            ([0.5, 0.5, -0.5], [1.0, 0.0]),
            ([0.5, -0.5, 0.5], [0.0, 1.0]),
            ([0.5, 0.5, -0.5], [1.0, 0.0]),
            ([0.5, 0.5, 0.5], [0.0, 0.0]),
        ],
        // -X
        [
            ([-0.5, -0.5, -0.5], [0.0, 1.0]),
            ([-0.5, -0.5, 0.5], [1.0, 1.0]),
            ([-0.5, 0.5, 0.5], [1.0, 0.0]),
            ([-0.5, -0.5, -0.5], [0.0, 1.0]),
            ([-0.5, 0.5, 0.5], [1.0, 0.0]),
            ([-0.5, 0.5, -0.5], [0.0, 0.0]),
        ],
        // +Y
        [
            ([-0.5, 0.5, 0.5], [0.0, 1.0]),
            ([0.5, 0.5, 0.5], [1.0, 1.0]),
            ([0.5, 0.5, -0.5], [1.0, 0.0]),
            ([-0.5, 0.5, 0.5], [0.0, 1.0]),
            ([0.5, 0.5, -0.5], [1.0, 0.0]),
            ([-0.5, 0.5, -0.5], [0.0, 0.0]),
        ],
        // -Y
        [
            ([-0.5, -0.5, -0.5], [0.0, 1.0]),
            ([0.5, -0.5, -0.5], [1.0, 1.0]),
            ([0.5, -0.5, 0.5], [1.0, 0.0]),
            ([-0.5, -0.5, -0.5], [0.0, 1.0]),
            ([0.5, -0.5, 0.5], [1.0, 0.0]),
            ([-0.5, -0.5, 0.5], [0.0, 0.0]),
        ],
    ];

    let mut out = Vec::with_capacity(36 * 20);
    for face in &faces {
        for (pos, uv) in face {
            for f in pos {
                out.extend_from_slice(&f.to_le_bytes());
            }
            for f in uv {
                out.extend_from_slice(&f.to_le_bytes());
            }
        }
    }
    out
}

/// Column-major MVP: perspective * lookAt * rotateY(angle).
fn mvp_matrix(angle: f32, aspect: f32) -> [f32; 16] {
    let proj = perspective(FRAC_PI_4, aspect.max(0.01), 0.1, 100.0);
    let view = look_at(
        [1.6, 1.2, 2.2],
        [0.0, 0.0, 0.0],
        [0.0, 1.0, 0.0],
    );
    let model = rotate_y(angle);
    mat4_mul(proj, mat4_mul(view, model))
}

fn perspective(fovy: f32, aspect: f32, near: f32, far: f32) -> [f32; 16] {
    let f = 1.0 / (fovy * 0.5).tan();
    let nf = 1.0 / (near - far);
    [
        f / aspect,
        0.0,
        0.0,
        0.0,
        0.0,
        f,
        0.0,
        0.0,
        0.0,
        0.0,
        (far + near) * nf,
        -1.0,
        0.0,
        0.0,
        2.0 * far * near * nf,
        0.0,
    ]
}

fn look_at(eye: [f32; 3], center: [f32; 3], up: [f32; 3]) -> [f32; 16] {
    let f = normalize([
        center[0] - eye[0],
        center[1] - eye[1],
        center[2] - eye[2],
    ]);
    let s = normalize(cross(f, up));
    let u = cross(s, f);
    [
        s[0],
        u[0],
        -f[0],
        0.0,
        s[1],
        u[1],
        -f[1],
        0.0,
        s[2],
        u[2],
        -f[2],
        0.0,
        -dot(s, eye),
        -dot(u, eye),
        dot(f, eye),
        1.0,
    ]
}

fn rotate_y(angle: f32) -> [f32; 16] {
    let (s, c) = angle.sin_cos();
    [
        c, 0.0, -s, 0.0, 0.0, 1.0, 0.0, 0.0, s, 0.0, c, 0.0, 0.0, 0.0, 0.0, 1.0,
    ]
}

fn mat4_mul(a: [f32; 16], b: [f32; 16]) -> [f32; 16] {
    let mut out = [0f32; 16];
    for col in 0..4 {
        for row in 0..4 {
            out[col * 4 + row] = a[row] * b[col * 4]
                + a[4 + row] * b[col * 4 + 1]
                + a[8 + row] * b[col * 4 + 2]
                + a[12 + row] * b[col * 4 + 3];
        }
    }
    out
}

fn cross(a: [f32; 3], b: [f32; 3]) -> [f32; 3] {
    [
        a[1] * b[2] - a[2] * b[1],
        a[2] * b[0] - a[0] * b[2],
        a[0] * b[1] - a[1] * b[0],
    ]
}

fn dot(a: [f32; 3], b: [f32; 3]) -> f32 {
    a[0] * b[0] + a[1] * b[1] + a[2] * b[2]
}

fn normalize(v: [f32; 3]) -> [f32; 3] {
    let len = (v[0] * v[0] + v[1] * v[1] + v[2] * v[2]).sqrt().max(1e-8);
    [v[0] / len, v[1] / len, v[2] / len]
}

export!(Component with_types_in self);
