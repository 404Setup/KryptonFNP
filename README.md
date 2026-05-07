<img src="./common/src/main/resources/assets/kreno/icon.png" alt="Logo" align="right" width="150">

Krypton Reno
====

![all](https://img.shields.io/badge/environment-any-4caf50?style=flat-square)

[![](https://badges.moddingx.org/modrinth/downloads/krypton-fnp)](https://modrinth.com/mod/krypton-fnp)
[![](https://badges.moddingx.org/curseforge/downloads/1269169)](https://www.curseforge.com/minecraft/mc-mods/krypton-fnp)
[![](https://img.shields.io/github/downloads/404Setup/KryptonReno/total?style=flat&logo=github&label=Github%20Downloads&args=14)](https://github.com/404Setup/KryptonReno/releases)

[![modrinth](https://cdn.jsdelivr.net/npm/@intergrav/devins-badges@3/assets/cozy/available/modrinth_vector.svg)](https://modrinth.com/mod/krypton-fnp)
[![curseforge](https://cdn.jsdelivr.net/npm/@intergrav/devins-badges@3/assets/cozy/available/curseforge_vector.svg)](https://www.curseforge.com/minecraft/mc-mods/krypton-fnp)
[![github](https://cdn.jsdelivr.net/npm/@intergrav/devins-badges@3/assets/cozy/available/github_vector.svg)](https://github.com/404Setup/KryptonReno/releases)

**As of 26.1, all my mods no longer offer Forge compatibility.**

Krypton Reno provides powerful network optimization capabilities for all major systems.

Ported from [Krypton Fabric](https://modrinth.com/mod/krypton), with some unique optimizations added. Supports
NeoForge/Forge;

For Fabric/Paper Server, please see the [Krypton Reno Fabric](https://modrinth.com/mod/kryptonfnp-patcher)

For Velocity Server, please see the [VelocityNT Recast](https://github.com/404Setup/VelocityNT-Recast).

---

## Differentiation

> This refers to the differences between "us" and upstream "Krypton Fabric" and other Krypton Forks

- Continuously provide compatibility for stable versions (1.20.1, 1.21.1) and the latest versions
- RecastLib provides acceleration features for **Windows** (x64/arm64)
- Support RFC 8305 Happy Eyeballs for client connections
- Server-side asynchronous (possibly) entity hiding – Hides entities that are not visible to the player
- Further scalability optimizations

## Mandatory dependencies

Starting with KReno 26.3.0, a mandatory dependency will be added **Spring Lotus** (LibSL).

This is not to deliberately increase downloads, but to reduce template code. I can update LibSL only once to quickly fix
some bugs or optimize some dependent libraries without having to write the same code for each of my mods, or push all
updates at once.

LibSL will be released under the less restrictive Apache-2.0 license and FPL-1.2 license, which you can also use for
your own mods.

## What is RecastLib

RecastLib is a native library I wrote in Rust that is compatible with Velocity Native JNI Bind.

It aims to make up for some of the shortcomings of Velocity Native compatibility,
thereby making the performance advantages of Krypton Reno more comprehensive.

In Krypton Reno for Fabric, I’m also testing the stability of the FFM APIs available in Java 22,
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
  # Skips sending movement packets if the entity hasn't moved, and downgrades position+rotation packets to just rotation if the entity only turned
  serverEntityMoveOpt: false
  # Reduces object allocation and lock contention in the Connection class
  connectionMicroOpt: true
  # Reduces some potentially useless particle packets. This configuration only takes effect on the server side.
  particlePacketOpt: true
  # Optimizes entity packet broadcasting and integrates with server-side entity culling
  trackedEntityOpt: true
fix:
  issues128:
    # Fix Traffic Statistics
    enabled: false
    # Run bandwidth statistics on sync thread, which is closer to Vanilla behavior.
    sync: true
culling:
  # Smart entity culling on server side
  entity: true
  # Smart block/block entity culling on server side
  block: true
  # Replaces completely hidden blocks in chunk packets with air to save bandwidth
  chunk_block: true
  # Treat light sections whose data array is fully zero as empty to skip 2KiB payload per section in ClientboundLevelChunkWithLightPacket / ClientboundLightUpdatePacket
  chunk_light: true
compress:
  # The compression level for packets, between 1-9.
  compressionLevel: 4
  # Permit Oversized Packets
  permitOversizedPackets: false
gui:
  # Replace Minecraft style KReno UI with a newly designed OreUI
  oreui: false
compatibility:
  allow-wide-var-int: false
netty:
  # Change Netty's default 16MiB memory allocation to 4MiB, as Minecraft has a 2MiB packet size limit.
  allocatorMaxOrder: 9
  # Enable Happy Eyeballs (RFC 8305) for client connections to race IPv6 and IPv4. May cause some servers (like Velocity) to temporarily refuse connections.
  happyEyeballs: false
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

<a href="https://github.com/404Setup/KryptonReno/blob/26.1-new/Benchmark.md">
<img src="https://img.shields.io/badge/Github-View-4caf50?style=flat-square" alt=""/>
</a>

## Q&A

### 1

**Q:** If I install this mod on the client but not on the server, will I be unable to join the server?

**A:** I deliberately designed it to be "consistent" with Krypton Fabric, so you can connect (be connected to) even if
the
other end doesn't have this mod, unless the server administrator has installed an anti-cheat mod that detects the mod
list, in which case you should contact them to request permission.

Unless otherwise specified, most of the functions can be run on the other end without this mod being installed.

----

### 2

**Q:** Will it help me reduce ping latency?

**A:** Krypton Reno's optimizations can save some hardware performance, which should reduce high latency caused by CPU
core
preemption. However, if your hardware resources are already very limited, or your network quality is truly poor, then
Krypton Reno can't save you much. It can't push the physical limits.

----

### 3

**Q:** Which mods is Krypton Reno compatible with?

**A:** There are a lot! You basically only need to worry about whether they will conflict with each other, without
having to
worry about Krypton Reno.

Incompatible Mods: Krypton Reforged, Ceres, Pluto, KryptonFoxified, Krypton Hybrid, Chionanthus, Krypton Fabric with
Connector

----

### 4

**Q:** Will it cause some different behavior?

**A:** This is unavoidable, I have tried to keep the implementation as aligned as possible, and in most cases you only
need
to modify the configuration to continue using it.

----

### 5

**Q:** Can I use Krypton Reno in servers that mix Bukkit API with Forge/NeoForge?

**A:** No, absolutely not. The Bukkit API was simply not designed to support Mods, and I can't guarantee that Krypton
Reno won't break something there, or that they break Krypton Reno.

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

### 7

**Q:** Is the fork of KryptonFNP/Krypton Reno better than the original?

**A:** Don't ask me this question. There are already so-called "fork users" causing trouble in my Issues and insulting
my mod as a "garbage mod".

----

### 8

**Q:** I want to create a mod that competes with KryptonReno/Krypton Fabric. Is that possible?

**A:** I won't stop you from doing this, but some authors might not like it. We haven't stopped updating, yet you're
competing with us using the same loader, the same codebase, and the same version, and you've removed us from the author
list—that's not good.

----

### 9

**Q:** What is entity culling and block culling?

**A:** To reduce packet sending or improve compression rates, I've added a culling system. This culling system only
works on dedicated servers.

If an entity is not visible in a player's line of sight, KReno will not send that entity's data packets to the player,
but the player can still hear sounds emitted by the entity.

The block culling system is closer to Paper's AntiXRay. It quickly scans the palette before sending chunk data to the
player, identifies blocks that are surrounded by opaque blocks, and batch replaces them with stone.
I'm not sure if this will improve compression rates, but if it conflicts with your AntiXRay, you can also disable it.

When a block is exposed to air, KReno will send the correct block to the player, ensuring visual accuracy.
Currently, block culling may generate a large number of fake ores, but I don't want to fix this issue anymore, as it
only affects cheaters anyway.

----

## Credit

- [Krypton Fabric](https://modrinth.com/mod/krypton)
- [Velocity](https://github.com/PaperMC/Velocity)
- [VelocityNT Recast](https://github.com/404Setup/VelocityNT-Recast)
- [Paper](https://github.com/PaperMC/Paper)
- [RecastSSL](https://github.com/404Setup/RecastSSL)
- [RecastXZ](https://github.com/404Setup/RecastXZ)
- [SpringLotus](https://github.com/404Setup/SpringLotus)

## For Modpack

If you comply with the license, you can use it freely for Modpack.

Modpacks that redistribute Minecraft game body (i.e. packages that package the entire Minecraft game including Mod
files, Config, ShaderPacks, ResourcePacks, Library and launcher into a whole zip file) are not allowed to use this mod.

## License

> This work has a restrictive license in addition to the original license to prevent some unexpected behavior,
> see [404Setup Public License](https://github.com/404Setup/404Setup/blob/main/LICENSE.md)

- **Krypton Reno:** 2025-2026. Licensed "as is". Provided by 404Setup under LGPL-3.0 Only.
- **RecastLib RecastXZ:** 2025-2026 404Setup. All rights reserved. Source code is licensed under a MPL-2.0 License.
- **RecastLib RecastSSL:** 2025-2026 404Setup. All rights reserved. Source code is licensed under a BSD-3-Clause
  License.
- **SpringLotus:** 2026 404Setup. All rights reserved. Source code is licensed under a LGPL-3.0 Only License.
  Redistribution is strictly prohibited.