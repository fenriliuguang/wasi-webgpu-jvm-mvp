# Dual runtime tracks (Track A locked sync-compat / Track B custom L1)

[中文](dual-runtime-track.md) | **English**

> **In effect (2026-08-10; Track B progress synced 2026-08-12; Track A mainline closed 2026-08-15).**  
> Track B repo: [`../wasmtime-android-kt`](../../../wasmtime-android-kt) — short-term M0–M5 thin L1 **archived**; current line WASI 0.3 + `wasi:webgpu` (W1/W2 delivered; W3+ expanding).  
> Track A **mainline complete:** [`track-a-baseline-host.en.md`](track-a-baseline-host.en.md) → [`archive-track-a-baseline-host-dod.en.md`](archive-track-a-baseline-host-dod.en.md) (L2 / cube baseline care + Host follow for B).  
> Authoritative contract: Track B [`dual-track.en.md`](../../../wasmtime-android-kt/docs/scheme/dual-track.en.md).

## One-liner

This repo (**Track A**) stays the demo/CI/CM-cube baseline and is **locked to sync-compat**; true CM async and an Android-first upstream-Wasmtime L1 live in **Track B** without blocking A. Track A’s mainline (**keep L2 + cube healthy** and **follow Track B WIT with Host**) is **closed**.

## Lock (this repo)

1. Keep **sync-compat** on default and acceptance paths.  
2. **Do not** pursue true CM async via Dawn await rewrites / Linker futures / async Guest instrumentation moves.  
3. Gate archive remains valid.  
4. Non-async stability/docs/engineering work and **L2 Host follow for Track B WIT** remain allowed.  
5. Switching L1 to Track B requires RFC + dual green — never silent replacement of the primary instrumented gate.

## Care checklist (Track A gates)

Authoritative table: [`track-a-baseline-host.en.md`](track-a-baseline-host.en.md). Pinned:

- JVM: `./gradlew :host-api:test :abi-cm:test` (also `:abi-mvp:test :abi-wasi:test` recommended)
- Engineered coords: `./gradlew publishEngineeredToMavenLocal` (fail red; no remote upload)
- Device: `./scripts/run-android-instrumented.ps1` (**do not** rely on Studio UTP)
- Fake dtor: View↔Texture `tryDrop`; Session `releaseLifetimeSafetyNets`; Demo/instrumented `releaseAllGpuObjects`
- Process-global CM: `am force-stop` between waves; reuse Session in-process

## Links

- Track A mainline: [`track-a-baseline-host.en.md`](track-a-baseline-host.en.md)  
- Track A archive: [`archive-track-a-baseline-host-dod.en.md`](archive-track-a-baseline-host-dod.en.md)  
- Track B charter / dual-track; this repo [`archive-true-cm-async-dod.en.md`](archive-true-cm-async-dod.en.md); [`patches/UPSTREAM.en.md`](../../patches/UPSTREAM.en.md) §5.
