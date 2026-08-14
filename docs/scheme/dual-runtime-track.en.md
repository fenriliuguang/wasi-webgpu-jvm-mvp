# Dual runtime tracks (Track A locked sync-compat / Track B custom L1)

[中文](dual-runtime-track.md) | **English**

> **In effect (2026-08-10; Track B progress synced 2026-08-12; Track A mainline chartered 2026-08-14).**  
> Track B repo: [`../wasmtime-android-kt`](../../../wasmtime-android-kt) — short-term M0–M5 thin L1 **archived**; current line WASI 0.3 + `wasi:webgpu` (W1/W2 delivered; W3+ expanding).  
> Track A **current mainline plan:** [`track-a-baseline-host.en.md`](track-a-baseline-host.en.md) (L2 / cube baseline care + Host follow for B; implementation not started).  
> Authoritative contract: Track B [`dual-track.en.md`](../../../wasmtime-android-kt/docs/scheme/dual-track.en.md).

## One-liner

This repo (**Track A**) stays the demo/CI/CM-cube baseline and is **locked to sync-compat**; true CM async and an Android-first upstream-Wasmtime L1 live in **Track B** without blocking A. Track A’s current push is **keep L2 + cube healthy** and **follow Track B WIT with Host**.

## Lock (this repo)

1. Keep **sync-compat** on default and acceptance paths.  
2. **Do not** pursue true CM async via Dawn await rewrites / Linker futures / async Guest instrumentation moves.  
3. Gate archive remains valid.  
4. Non-async stability/docs/engineering work and **L2 Host follow for Track B WIT** remain allowed.  
5. Switching L1 to Track B requires RFC + dual green — never silent replacement of the primary instrumented gate.

## Links

- Track A mainline: [`track-a-baseline-host.en.md`](track-a-baseline-host.en.md)  
- Track B charter / dual-track; this repo [`archive-true-cm-async-dod.en.md`](archive-true-cm-async-dod.en.md); [`patches/UPSTREAM.en.md`](../../patches/UPSTREAM.en.md) §5.
