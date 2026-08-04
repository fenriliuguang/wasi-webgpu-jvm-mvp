from pathlib import Path

p = Path(
    r"C:\Users\clt\Desktop\project\playground\android\wasi-webgpu-jvm-mvp"
    r"\.deps\wasmtime4j\wasmtime4j-native\src\jni\component_linker.rs"
)
t = p.read_text(encoding="utf-8")
old = 'let interface_path = format!("{}:{}", namespace_str, interface_str);'
new = 'let interface_path = format!("{}/{}", namespace_str, interface_str);'
if old not in t:
    raise SystemExit(f"pattern missing; sample={t.find('interface_path')}")
p.write_text(t.replace(old, new, 1), encoding="utf-8", newline="\n")
print("re-patched interface_path OK")
