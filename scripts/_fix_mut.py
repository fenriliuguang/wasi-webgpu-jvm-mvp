from pathlib import Path

p = Path(
    r"C:\Users\clt\Desktop\project\playground\android\wasi-webgpu-jvm-mvp"
    r"\.deps\wasmtime4j\wasmtime4j-native\src\component\linker.rs"
)
t = p.read_text(encoding="utf-8")
old = "move |store_ctx: StoreContextMut<'_, ComponentStoreData>,"
new = "move |mut store_ctx: StoreContextMut<'_, ComponentStoreData>,"
n = t.count(old)
print("occurrences", n)
p.write_text(t.replace(old, new), encoding="utf-8", newline="\n")
