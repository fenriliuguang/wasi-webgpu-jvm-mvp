//! Experimental Component Model guest for vector-add.
//! Imports experimental:webgpu-cm/host — NOT compliant wasi:webgpu.
//! Handles are WIT resources (not flat u32).

#![no_main]

wit_bindgen::generate!({
    world: "vector-add",
    path: "wit",
});

/// Must match `VectorAddScenario.SHADER` / abi-mvp guest data section byte-for-byte.
/// Use concat! (not `\` line continuation) — Rust strips whitespace after `\`.
const SHADER: &str = concat!(
    "@group(0) @binding(0) var<storage, read> inputA : array<f32>;\n",
    "@group(0) @binding(1) var<storage, read> inputB : array<f32>;\n",
    "@group(0) @binding(2) var<storage, read_write> output : array<f32>;\n",
    "\n",
    "@compute @workgroup_size(64)\n",
    "fn main(@builtin(global_invocation_id) gid : vec3<u32>) {\n",
    "  let i = gid.x;\n",
    "  if (i >= arrayLength(&output)) {\n",
    "    return;\n",
    "  }\n",
    "  output[i] = inputA[i] + inputB[i];\n",
    "}",
);

/// STORAGE | COPY_DST | COPY_SRC
const USAGE_STORAGE: u32 = 0x8c;
/// MAP_READ | COPY_DST
const USAGE_MAP_READ: u32 = 0x09;

struct Component;

impl Guest for Component {
    fn run_vector_add(a: Vec<f32>, b: Vec<f32>) -> Result<Vec<f32>, String> {
        use experimental::webgpu_cm::host;

        if a.is_empty() || a.len() != b.len() {
            return Err("a/b length mismatch or empty".into());
        }
        let n = a.len() as u32;
        let bytes = (a.len() * 4) as u64;

        let adapter = host::request_adapter();
        let device = adapter.request_device();
        let queue = device.get_queue();

        let buf_a = device.create_buffer(bytes, USAGE_STORAGE);
        let buf_b = device.create_buffer(bytes, USAGE_STORAGE);
        let buf_out = device.create_buffer(bytes, USAGE_STORAGE);
        let buf_read = device.create_buffer(bytes, USAGE_MAP_READ);

        queue.write_buffer(&buf_a, 0, &floats_to_bytes(&a));
        queue.write_buffer(&buf_b, 0, &floats_to_bytes(&b));

        let shader = device.create_shader_module(SHADER);
        let layout = device.create_bind_group_layout_storage3();
        let bind_group = device.create_bind_group3(&layout, &buf_a, &buf_b, &buf_out);
        let pipeline = device.create_compute_pipeline(&layout, &shader, "main");

        let encoder = device.create_command_encoder();
        let pass = encoder.begin_compute_pass();
        pass.set_pipeline(&pipeline);
        pass.set_bind_group(0, &bind_group);
        let wg = (n + 63) / 64;
        pass.dispatch_workgroups(wg, 1, 1);
        pass.end();

        encoder.copy_buffer_to_buffer(&buf_out, 0, &buf_read, 0, bytes);
        let cmd = encoder.finish();
        queue.submit1(cmd);

        buf_read.map_read(0, bytes);
        let mapped = buf_read.get_mapped_range(0, bytes);
        buf_read.unmap();

        if mapped.len() != a.len() * 4 {
            return Err(format!(
                "mapped range size mismatch: got {} want {}",
                mapped.len(),
                a.len() * 4
            ));
        }
        Ok(bytes_to_floats(&mapped))
    }
}

export!(Component with_types_in self);

fn floats_to_bytes(values: &[f32]) -> Vec<u8> {
    let mut out = Vec::with_capacity(values.len() * 4);
    for v in values {
        out.extend_from_slice(&v.to_le_bytes());
    }
    out
}

fn bytes_to_floats(bytes: &[u8]) -> Vec<f32> {
    bytes
        .chunks_exact(4)
        .map(|c| f32::from_le_bytes([c[0], c[1], c[2], c[3]]))
        .collect()
}
