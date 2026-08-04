;; Experimental abi-mvp guest (NOT Component Model / NOT compliant wasi:webgpu).
;; Rebuild: wasm-tools parse guest/vector-add/vector_add.wat -o guest/vector-add/vector_add.wasm

(module
  (import "wasi-webgpu-mvp" "request_adapter" (func $request_adapter (result i32)))
  (import "wasi-webgpu-mvp" "adapter_request_device" (func $adapter_request_device (param i32) (result i32)))
  (import "wasi-webgpu-mvp" "device_get_queue" (func $device_get_queue (param i32) (result i32)))
  (import "wasi-webgpu-mvp" "device_create_buffer" (func $device_create_buffer (param i32 i32 i32) (result i32)))
  (import "wasi-webgpu-mvp" "queue_write_buffer" (func $queue_write_buffer (param i32 i32 i32 i32 i32)))
  (import "wasi-webgpu-mvp" "device_create_shader_module" (func $device_create_shader_module (param i32 i32 i32) (result i32)))
  (import "wasi-webgpu-mvp" "device_create_bind_group_layout_storage3" (func $device_create_bind_group_layout_storage3 (param i32) (result i32)))
  (import "wasi-webgpu-mvp" "device_create_bind_group3" (func $device_create_bind_group3 (param i32 i32 i32 i32 i32) (result i32)))
  (import "wasi-webgpu-mvp" "device_create_compute_pipeline" (func $device_create_compute_pipeline (param i32 i32 i32 i32 i32) (result i32)))
  (import "wasi-webgpu-mvp" "device_create_command_encoder" (func $device_create_command_encoder (param i32) (result i32)))
  (import "wasi-webgpu-mvp" "command_encoder_begin_compute_pass" (func $command_encoder_begin_compute_pass (param i32) (result i32)))
  (import "wasi-webgpu-mvp" "compute_pass_set_pipeline" (func $compute_pass_set_pipeline (param i32 i32)))
  (import "wasi-webgpu-mvp" "compute_pass_set_bind_group" (func $compute_pass_set_bind_group (param i32 i32 i32)))
  (import "wasi-webgpu-mvp" "compute_pass_dispatch" (func $compute_pass_dispatch (param i32 i32 i32 i32)))
  (import "wasi-webgpu-mvp" "compute_pass_end" (func $compute_pass_end (param i32)))
  (import "wasi-webgpu-mvp" "command_encoder_copy_buffer_to_buffer" (func $command_encoder_copy_buffer_to_buffer (param i32 i32 i32 i32 i32 i32)))
  (import "wasi-webgpu-mvp" "command_encoder_finish" (func $command_encoder_finish (param i32) (result i32)))
  (import "wasi-webgpu-mvp" "queue_submit1" (func $queue_submit1 (param i32 i32)))
  (import "wasi-webgpu-mvp" "buffer_map_read" (func $buffer_map_read (param i32 i32 i32)))
  (import "wasi-webgpu-mvp" "buffer_get_mapped_range" (func $buffer_get_mapped_range (param i32 i32 i32 i32)))
  (import "wasi-webgpu-mvp" "buffer_unmap" (func $buffer_unmap (param i32)))

  (memory (export "memory") 2)

  ;; shader @ 0 len 386 ; entry "main" @ 400 len 4
  (data (i32.const 0) "@group(0) @binding(0) var<storage, read> inputA : array<f32>;\n@group(0) @binding(1) var<storage, read> inputB : array<f32>;\n@group(0) @binding(2) var<storage, read_write> output : array<f32>;\n\n@compute @workgroup_size(64)\nfn main(@builtin(global_invocation_id) gid : vec3<u32>) {\n  let i = gid.x;\n  if (i >= arrayLength(&output)) {\n    return;\n  }\n  output[i] = inputA[i] + inputB[i];\n}")
  (data (i32.const 400) "main")

  ;; BufferUsage: STORAGE|COPY_DST|COPY_SRC = 0x80|0x08|0x04 = 0x8C
  ;; MAP_READ|COPY_DST = 0x01|0x08 = 0x09
  (func (export "run_vector_add") (param $ptr_a i32) (param $ptr_b i32) (param $ptr_out i32) (param $n i32) (result i32)
    (local $bytes i32)
    (local $adapter i32)
    (local $device i32)
    (local $queue i32)
    (local $buf_a i32)
    (local $buf_b i32)
    (local $buf_out i32)
    (local $buf_read i32)
    (local $shader i32)
    (local $layout i32)
    (local $bind_group i32)
    (local $pipeline i32)
    (local $encoder i32)
    (local $pass i32)
    (local $cmd i32)
    (local $wg i32)

    (local.set $bytes (i32.mul (local.get $n) (i32.const 4)))
    (local.set $adapter (call $request_adapter))
    (local.set $device (call $adapter_request_device (local.get $adapter)))
    (local.set $queue (call $device_get_queue (local.get $device)))

    (local.set $buf_a (call $device_create_buffer (local.get $device) (local.get $bytes) (i32.const 0x8c)))
    (local.set $buf_b (call $device_create_buffer (local.get $device) (local.get $bytes) (i32.const 0x8c)))
    (local.set $buf_out (call $device_create_buffer (local.get $device) (local.get $bytes) (i32.const 0x8c)))
    (local.set $buf_read (call $device_create_buffer (local.get $device) (local.get $bytes) (i32.const 0x09)))

    (call $queue_write_buffer (local.get $queue) (local.get $buf_a) (i32.const 0) (local.get $ptr_a) (local.get $bytes))
    (call $queue_write_buffer (local.get $queue) (local.get $buf_b) (i32.const 0) (local.get $ptr_b) (local.get $bytes))

    (local.set $shader (call $device_create_shader_module (local.get $device) (i32.const 0) (i32.const 386)))
    (local.set $layout (call $device_create_bind_group_layout_storage3 (local.get $device)))
    (local.set $bind_group (call $device_create_bind_group3
      (local.get $device) (local.get $layout) (local.get $buf_a) (local.get $buf_b) (local.get $buf_out)))
    (local.set $pipeline (call $device_create_compute_pipeline
      (local.get $device) (local.get $layout) (local.get $shader) (i32.const 400) (i32.const 4)))

    (local.set $encoder (call $device_create_command_encoder (local.get $device)))
    (local.set $pass (call $command_encoder_begin_compute_pass (local.get $encoder)))
    (call $compute_pass_set_pipeline (local.get $pass) (local.get $pipeline))
    (call $compute_pass_set_bind_group (local.get $pass) (i32.const 0) (local.get $bind_group))
    ;; workgroups = (n + 63) / 64
    (local.set $wg (i32.div_u (i32.add (local.get $n) (i32.const 63)) (i32.const 64)))
    (call $compute_pass_dispatch (local.get $pass) (local.get $wg) (i32.const 1) (i32.const 1))
    (call $compute_pass_end (local.get $pass))
    (call $command_encoder_copy_buffer_to_buffer
      (local.get $encoder) (local.get $buf_out) (i32.const 0)
      (local.get $buf_read) (i32.const 0) (local.get $bytes))
    (local.set $cmd (call $command_encoder_finish (local.get $encoder)))
    (call $queue_submit1 (local.get $queue) (local.get $cmd))
    (call $buffer_map_read (local.get $buf_read) (i32.const 0) (local.get $bytes))
    (call $buffer_get_mapped_range (local.get $buf_read) (i32.const 0) (local.get $bytes) (local.get $ptr_out))
    (call $buffer_unmap (local.get $buf_read))
    (i32.const 0)
  )
)
