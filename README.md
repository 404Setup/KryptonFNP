<img src="./common/src/main/resources/assets/krypton_fnp/icon.png" alt="Logo" align="right" width="150">

Krypton FNP
====

![all](https://img.shields.io/badge/environment-any-4caf50?style=flat-square)

<a href="https://modrinth.com/mod/krypton-fnp"><img src="https://badges.moddingx.org/modrinth/downloads/krypton-fnp" alt=""></a>
<a href="https://www.curseforge.com/minecraft/mc-mods/krypton-fnp"><img src="https://badges.moddingx.org/curseforge/downloads/1269169" alt=""></a>

<a href="https://modrinth.com/mod/krypton-fnp"><img alt="modrinth" height="56" src="https://cdn.jsdelivr.net/npm/@intergrav/devins-badges@3/assets/cozy/available/modrinth_vector.svg"/></a>
<a href="https://www.curseforge.com/minecraft/mc-mods/krypton-fnp"><img alt="curseforge" height="56" src="https://cdn.jsdelivr.net/npm/@intergrav/devins-badges@3/assets/cozy/available/curseforge_vector.svg" /></a>
<a href="https://github.com/404Setup/KryptonFNP/releases"><img alt="github" height="56" src="https://cdn.jsdelivr.net/npm/@intergrav/devins-badges@3/assets/cozy/available/github_vector.svg"></a>


This mod is an unofficial port of [Krypton Fabric](https://modrinth.com/mod/krypton), designed to provide Forge &
NeoForge compatibility.

If you are looking for KryptonFNP Fabric, visit from the following link:

- Github: https://github.com/404Setup/FNP-Patcher
- Modrinth: https://modrinth.com/mod/kryptonfnp-patcher
- CurseForge: https://www.curseforge.com/minecraft/mc-mods/kryptonfnp-patcher

---

Krypton is a mod that attempts to optimize the Minecraft networking stack. It derives from work
done in the [Velocity](https://velocitypowered.com/), [VelocityNT Recast](https://github.com/404Setup/VelocityNT-Recast)
and [Paper](https://papermc.io) projects.

Krypton derives itself from Ancient Greek _kryptos_, which means "the hidden one". This makes
it evident most of the benefit from Krypton is "hidden" but is noticeable by a server administrator.

[The wiki contains important information &ndash; read it](https://github.com/astei/krypton/wiki).

## Feature

- More related minor optimizations
- Implemented RecastLib
- Support NeoForge/Forge

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
| Linux x64                   | Yes       | Yes             |
| Linux arm64                 | No        | Yes             |
| MacOS arm64 (Apple Silicon) | No        | Yes             |

Currently, there are no plans to provide compatibility for Android,
32-bit architecture operating systems, or other architectures.

Please do not open any related issues or complain to me,
even if you do, I can't help.

## Config

Add the following parameters to the Java startup parameters to control the feature enablement:

| Parameter                     | Description                | Default value |
|-------------------------------|----------------------------|---------------|
| velocity.natives-disable      | Disable Native             | false         |
| velocity.linux-recast-enabled | Enable RecastLib for Linux | false         |
| krypton.loginVT               | -                          | true          |
| krypton.textFilterVT          | -                          | true          |
| krypton.utilVT                | -                          | true          |
| krypton.bestVarLong           | -                          | true          |

For more configuration, see the configuration file

example:

```shell
java -Dvelocity.natives-disable=true -jar neoforge_launcher.jar
```

### Use env instead of jvm args

Some configuration items support using environment variables instead of jvm args.

| JVM ARGS                      | Environment Variable |
|-------------------------------|----------------------|
| velocity.linux-recast-enabled | ENABLE_LINUX_RECAST  |

## Benchmark
<a href="https://github.com/404Setup/KryptonFNP/blob/master/Benchmark.md">
<img src="https://img.shields.io/badge/Github-View-4caf50?style=flat-square" alt=""/>
</a>

## License

This work has a restrictive license in addition to the original license to prevent some unexpected behavior,
see [404Setup Public License](https://github.com/404Setup/404Setup/blob/main/LICENSE.md)