<img src="./common/src/main/resources/assets/krypton_fnp/icon.png" alt="Logo" align="right" width="150">

Krypton FNP
====

![all](https://img.shields.io/badge/environment-any-4caf50?style=flat-square)

[![](https://badges.moddingx.org/modrinth/downloads/krypton-fnp)](https://modrinth.com/mod/krypton-fnp)
[![](https://badges.moddingx.org/curseforge/downloads/1269169)](https://www.curseforge.com/minecraft/mc-mods/krypton-fnp)

[![modrinth](https://cdn.jsdelivr.net/npm/@intergrav/devins-badges@3/assets/cozy/available/modrinth_vector.svg)](https://modrinth.com/mod/krypton-fnp)
[![curseforge](https://cdn.jsdelivr.net/npm/@intergrav/devins-badges@3/assets/cozy/available/curseforge_vector.svg)](https://www.curseforge.com/minecraft/mc-mods/krypton-fnp)
[![github](https://cdn.jsdelivr.net/npm/@intergrav/devins-badges@3/assets/cozy/available/github_vector.svg)](https://github.com/404Setup/KryptonFNP/releases)

Krypton FNP provides powerful network optimization capabilities for all major systems.

Ported from [Krypton Fabric](https://modrinth.com/mod/krypton), with some unique optimizations added. Supports
NeoForge/Forge;

For Fabric/Paper Server, please see the [KryptonFNP Patcher](https://modrinth.com/mod/kryptonfnp-patcher)

For Velocity Server, please see the [VelocityNT Recast](https://github.com/404Setup/VelocityNT-Recast).

---

## Differentiation

> This refers to the differences between "us" and upstream "Krypton Fabric" and other Krypton Forks

- Continuously provide compatibility for stable versions (1.20.1, 1.21.1) and the latest versions
- RecastLib provides acceleration features for **Windows** (x64/arm64)
- Further scalability optimizations

## What is RecastLib

RecastLib is a native library I wrote in Rust that is compatible with Velocity Native JNI Bind.

It aims to make up for some of the shortcomings of Velocity Native compatibility,
thereby making the performance advantages of KryptonFNP more comprehensive.

In KryptonFNP for Fabric, I’m also testing the stability of the FFM APIs available in Java 22,
which, according to benchmarks, should provide more performance gains
(ultimately Fabric only. Forge/NeoForge won’t be able to run them).

Their benefits are obvious that both Server and Client can benefit, and that most popular PCs can run these native
libraries due to the replacement compatibility.

### Compatibility

| System & Arch               | RecastLib | Velocity Native |
|-----------------------------|-----------|-----------------|
| Windows x64                 | Yes       | No              |
| Windows arm64               | Yes       | No              |
| Linux x64                   | No        | Yes             |
| Linux arm64                 | No        | Yes             |
| MacOS arm64 (Apple Silicon) | No        | Yes             |

**Compatibility is "hybrid"; they provide compatibility in areas where the other does not support them,
so you don't need to worry about losing compatibility.**

Currently, there are no plans to provide compatibility for Android,
32-bit architecture operating systems, or other architectures.

Please do not open any related issues or complain to me,
even if you do, I can't help.

## Config

```yaml
mixin:
  # Replace player login validation thread with virtual thread
  loginVT: true
  # Replace text filter thread with virtual thread
  textFilterVT: true
  # Replace download thread with virtual thread
  utilVT: true
  # Optimized VarLong implementation
  bestVarLong: true
  # Enable new encryption optimizations on the client side
  clientEncrypt: true
  # Optimized RconClient implementation
  rconClient: false
fix:
  issues128:
    # Fix Traffic Statistics
    enabled: false
    # Run bandwidth statistics on sync thread, which is closer to Vanilla behavior.
    sync: true
compress:
  # The compression level for packets, between 1-9.
  compressionLevel: 4
  # Permit Oversized Packets
  permitOversizedPackets: false
  blending-mode:
    # (Experimental) Delegate data packets with poor performance in the Native implementation to the Java implementation
    enabled: false
    linux-fallback-min-size: 1024
    repetitive-threshold: 0.6
compatibility:
  allow-wide-var-int: false
netty:
  # Change Netty's default 16MiB memory allocation to 4MiB, as Minecraft has a 2MiB packet size limit.
  allocatorMaxOrder: 9
```

### Use env instead of jvm args

Some configuration items support using environment variables instead of jvm args.

| JVM ARGS                      | Environment Variable |
|-------------------------------|----------------------|
| velocity.linux-recast-enabled | ENABLE_LINUX_RECAST  |

```shell
java -Dvelocity.natives-disable=false -Dvelocity.linux-recast-enabled=true -jar neoforge_launcher.jar
```

or

```shell
ENABLE_LINUX_RECAST=true java -jar neoforge_launcher.jar
```

## Benchmark

<a href="https://github.com/404Setup/KryptonFNP/blob/1.21.11/Benchmark.md">
<img src="https://img.shields.io/badge/Github-View-4caf50?style=flat-square" alt=""/>
</a>

## Q&A

### 1

**Q:** If I install this mod on the client but not on the server, will I be unable to join the server?

**A:** I deliberately designed it to be "consistent" with Krypton Fabric, so you can connect (be connected to) even if
the
other end doesn't have this mod, unless the server administrator has installed an anti-cheat mod that detects the mod
list, in which case you should contact them to request permission.

----

### 2

**Q:** Will it help me reduce ping latency?

**A:** Krypton FNP's optimizations can save some hardware performance, which should reduce high latency caused by CPU
core
preemption. However, if your hardware resources are already very limited, or your network quality is truly poor, then
Krypton FNP can't save you much. It can't push the physical limits.

----

### 3

**Q:** Which mods is Krypton FNP compatible with?

**A:** There are a lot! You basically only need to worry about whether they will conflict with each other, without
having to
worry about Krypton FNP.

Incompatible Mods: Krypton Reforged, Ceres, Pluto, KryptonFoxified, Chionanthus, Krypton Fabric with Connector

----

### 4

**Q:** Will it cause some different behavior?

**A:** This is unavoidable, I have tried to keep the implementation as aligned as possible, and in most cases you only
need
to modify the configuration to continue using it.

----

### 5

**Q:** Can I use Krypton FNP in servers that mix Bukkit API with Forge/NeoForge?

**A:** No, absolutely not. The Bukkit API was simply not designed to support Mods, and I can't guarantee that Krypton
FNP won't break something there, or that they break Krypton FNP.

----

### 6

**Q:** I can't seem to load the native library (Velocity Native or RecastLib), what should I do?

**A:** Generally, this should be caused by missing dependent libraries on the machine.

- For Windows: You should probably
  install [MSVC 170](https://learn.microsoft.com/en-us/cpp/windows/latest-supported-vc-redist?view=msvc-170#latest-supported-redistributable-version)
  and [OpenSSL 3 Win32](https://slproweb.com/products/Win32OpenSSL.html)
- For Linux: You should have at least OpenSSL3 installed (most operating systems should come with it)
- For MacOS: You should do nothing, just make sure the system is up to date.

These libraries should be compatible with x64 and arm64 architectures, so you don’t need to worry about them.

----

## Credit

- [Krypton Fabric](https://modrinth.com/mod/krypton)
- [Velocity](https://github.com/PaperMC/Velocity)
- [VelocityNT Recast](https://github.com/404Setup/VelocityNT-Recast)
- [Paper](https://github.com/PaperMC/Paper)
- [RecastSSL](https://github.com/404Setup/RecastSSL)

## For Modpack

If you comply with the license, you can use it freely for Modpack.

Modpacks that redistribute Minecraft game body (i.e. packages that package the entire Minecraft game including Mod
files, Config, ShaderPacks, ResourcePacks, Library and launcher into a whole zip file) are not allowed to use this mod.

## License

> This work has a restrictive license in addition to the original license to prevent some unexpected behavior,
> see [404Setup Public License](https://github.com/404Setup/404Setup/blob/1.21.11/LICENSE.md)

- **Krypton FNP:** 2025-2026. Licensed "as is". Provided by 404Setup under LGPL-3.0 Only.
- **RecastLib RecastXZ:** 2025-2026 404Setup. All rights reserved. Source code is licensed under a MPL-2.0 License.
- **RecastLib RecastSSL:** 2025-2026 404Setup. All rights reserved. Source code is licensed under a BSD-3-Clause
  License.