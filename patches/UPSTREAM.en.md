# Upstream contribution notes (wasmtime4j)

[中文](UPSTREAM.md) | **English**

> **Phase C (2026-08-08):** outward-facing contribution brief (not a submitted PR).  
> Downstream: this repo `wasi-webgpu-jvm-mvp`; upstream [tegmentum/wasmtime4j](https://github.com/tegmentum/wasmtime4j).  
> Pinned: `v47.0.2-1.5.0` (`ai.tegmentum:wasmtime4j`).

Open issues/PRs only when ready to maintain them upstream. This repo keeps its **overlay / filtered-jar / local patch** strategy until upstream lands.

---

## Status at a glance

| Item | Status | In-repo workaround |
|------|--------|--------------------|
| Native Android / CM resources patches | Diffs in-tree; build scripts `git apply` | `scripts/build-wasmtime4j-*.ps1` |
| Java `ConcurrentCallCodec` unsigned-u64 | **Primary outbound**; not upstream yet | android-demo filtered jar + local class |
| Java `Validation` TBI handles | Can ship with u64 | same overlay |
| CM resource destructor → Host `drop(rep)` | **Suggested outbound** (gap from phase B) | AbiCm View↔Texture pairing + `releaseFrame*` insurance |

---

## 1. Native patches (in-tree)

| Patch | Problem | Suggested upstream change |
|-------|---------|---------------------------|
| [`wasmtime4j-v47.0.2-1.5.0-android.patch`](wasmtime4j-v47.0.2-1.5.0-android.patch) | ART rejects `JNI_VERSION_1_8` from `JNI_OnLoad` | Return `JNI_VERSION_1_6` (or detect Android / max supported) |
| same android patch | Signed `jlong` compare treats MTE/TBI-tagged pointers as corrupt | Compare handles as `u64` (`memory_ptr as u64 < 0x1000`, same for tables) |
| [`wasmtime4j-v47.0.2-1.5.0-cm-resources.patch`](wasmtime4j-v47.0.2-1.5.0-cm-resources.patch) | CM host callbacks need `Resource` ↔ `U32(rep)`; multi-resource registration; fresh linker on instantiate | Resource marshalling helpers; `allow_shadowing` / batch re-define; process-level registry replay for `nativeInstantiateWithLinker` |

### Reproduce

```powershell
# Android .so → runtime-wasmtime/android-natives/jniLibs/
./scripts/build-wasmtime4j-android.ps1

# Desktop CM → runtime-wasmtime/desktop-natives/<platform>/
./scripts/build-wasmtime4j-desktop-cm.ps1
./gradlew :runtime-wasmtime:test
```

Re-export after editing `.deps/wasmtime4j`:

```powershell
python ./scripts/export-wasmtime4j-patches.py
```

---

## 2. Java: `ConcurrentCallCodec` unsigned-u64 (primary brief)

### Symptom

CM host callbacks marshalled via JSON:

```text
E JniComponentLinker: Host function callback failed for ID: …
  For input string: "12970367413882346528"
```

That decimal is an **unsigned u64** (often Android `ANativeWindow*` / TBI·PAC high-bit pointers) above `Long.MAX_VALUE`.

### Root cause

- Rust `serde_json` emits **unsigned** decimals for u64
- Upstream `ConcurrentCallCodec.parseNumber` uses `Long.parseLong` → `NumberFormatException`
- Same library’s `ComponentTypeCodec.parseNumber` **already** falls back to `Long.parseUnsignedLong`; `ConcurrentCallCodec` does not
- Signed U64 **serialize** also breaks high-bit handle round-trips

### Suggested upstream fix (minimal)

Align with `ComponentTypeCodec`:

1. **Parse:** on `Long.parseLong` failure, `Long.parseUnsignedLong(numStr)` (bits via `Number.longValue()`)
2. **Serialize U64:** `Long.toUnsignedString(val.asU64())`, not signed `append(long)`

### In-repo reference

- Overlay: [`android-demo/.../ConcurrentCallCodec.java`](../android-demo/src/main/java/ai/tegmentum/wasmtime4j/component/ConcurrentCallCodec.java)
- Build: [`android-demo/build.gradle.kts`](../android-demo/build.gradle.kts) filters the Maven jar (`ConcurrentCallCodec*.class`, including nested classes) and uses the local copy
- Pitfall log: [`docs/scheme/guest-onscreen-cm-blockers.md`](../docs/scheme/guest-onscreen-cm-blockers.md) **P2** (ZH)

### Suggested upstream test

Unit test: `parseNumber("18446744073709551615")` (or any decimal `> Long.MAX_VALUE`) must not throw; U64 serialize → parse bit-identical.

---

## 3. Java: `Validation` (TBI / opaque handle)

| Local class | Problem | Suggested upstream change |
|-------------|---------|---------------------------|
| `ai.tegmentum.wasmtime4j.util.Validation` | ARM64 TBI/PAC pointers look “negative” as signed `long` and are rejected | Allow non-zero opaque handles (or document bit-cast) |

Same filtered-jar + android-demo overlay. Can share a PR with §2 or be a separate issue.

---

## 4. CM resource destructor → Host `drop(rep)` (suggested outbound)

### Gap

This repo’s L1 treats WIT resource reps as **L2 `GpuHandle.raw` (u32)** and does **not** use Java `ComponentResourceDefinition.constructor` → private `resourceTable.push`.

So even with `.destructor { … }`:

1. JNI `dispatchDestructor` does `resourceTable.delete(rep)` first
2. Table miss → **Consumer never runs**
3. Guest drops leave Dawn objects pinned in the Host table (Surface/Texture, …)

In-repo mitigation (phase B): AbiCm **View↔Texture pairing** via `tryDrop` + `releaseFrameResources` / Demo `releaseAllGpuObjects` — **not** true WIT destructors.

### Suggested upstream change (pick one or both)

**A (preferred for L2-rep hosts)**  
On `resourceTable` miss, still invoke an optional `IntConsumer` / destructor with the **rep u32** (host maps into its own table).

**B**  
Document that destructors only run when `.constructor` populated `resourceTable`; provide an official “rep-only host resources” example.

Related: existing CM resources native patch (§1); destructor path is a Java/JNI incremental. Plan: [`docs/scheme/semantic-hardening.en.md`](../docs/scheme/semantic-hardening.en.md) slices B/C.

---

## 5. In-repo overlay strategy (switchover)

| Layer | Strategy | After upstream merges |
|-------|----------|------------------------|
| Native `.so` | Build-time `git apply` of in-tree patches | Bump dependency; drop matching patch hunks |
| Java Validation / ConcurrentCallCodec | Filter Maven jar + same-package overlay in android-demo | Remove `filterWasmtime4jJar` excludes + local classes |
| Frame Texture cleanup | AbiCm pairing + Host sweep | If §4A lands, narrow semantic reliance on sweep |

**Phase C does not require an upstream PR merge**; the brief being ready is enough to check C off.

---

## Suggested outbound order

1. **Issue / small PR:** `ConcurrentCallCodec` unsigned-u64 (§2) — Java-only, low risk, validated here  
2. **Issue:** `Validation` opaque handles (§§3)  
3. **Issue:** CM destructor miss → rep callback (§4) — needs design review  
4. Native patches (§1) — larger; separate milestone  
