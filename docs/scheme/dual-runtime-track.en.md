# Dual runtime tracks (Track A locked sync-compat / Track B custom L1)

[中文](dual-runtime-track.md) | **English**

> **In effect (2026-08-10).**  
> Track B repo: [`wasmtime-android-kt`](../../../wasmtime-android-kt) (docs chartered; no code yet).  
> Authoritative contract: Track B [`dual-track.en.md`](../../../wasmtime-android-kt/docs/scheme/dual-track.en.md).

## One-liner

This repo (**Track A**) stays the demo/CI/CM-cube baseline and is **locked to sync-compat**; true CM async and an Android-first upstream-Wasmtime L1 live in **Track B** without blocking A.

## Lock (this repo)

1. Keep **sync-compat** on default and acceptance paths.  
2. **Do not** pursue true CM async via Dawn await rewrites / Linker futures / async Guest instrumentation moves.  
3. Gate archive remains valid.  
4. Non-async stability/docs/engineering work remains allowed.  
5. Switching L1 to Track B requires RFC + dual green — never silent replacement of the primary instrumented gate.

## Links

Track B charter / dual-track; this repo [`archive-true-cm-async-dod.en.md`](archive-true-cm-async-dod.en.md); [`patches/UPSTREAM.en.md`](../../patches/UPSTREAM.en.md) §5.
