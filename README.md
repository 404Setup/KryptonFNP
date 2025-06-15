![Krypton logo](https://user-images.githubusercontent.com/16436212/102424564-692de280-3fd9-11eb-98a2-ac125cb8e507.png)

# Krypton MultiLoader (Krypton FNP)

![all](https://img.shields.io/badge/environment-any-4caf50?style=flat-square)

This mod is an unofficial port of [Krypton Fabric](https://modrinth.com/mod/krypton), designed to provide Forge &
NeoForge compatibility.

It should be consistent with the upstream behavior.

Krypton is a mod that attempts to optimize the Minecraft networking stack. It derives from work
done in the [Velocity](https://velocitypowered.com/) and [Paper](https://papermc.io) projects.

Krypton derives itself from Ancient Greek _kryptos_, which means "the hidden one". This makes
it evident most of the benefit from Krypton is "hidden" but is noticeable by a server administrator.

[The wiki contains important information &ndash; read it](https://github.com/astei/krypton/wiki).
[Join my Discord](https://discord.gg/RUGArxEQ8J) to discuss the mod or get support if the wiki didn't
answer your question.

## Benchmark

The Benchmark results are not necessarily accurate,
and the final results are determined based on different JVM distributions, startup parameters, and random factors.

### VarLong

- `getByteSize`

| Implementation         | Performance (ops/s) | vs Minecraft | vs Previous Gen |
|------------------------|---------------------|--------------|-----------------|
| **Minecraft**          | 65,843              | Baseline     | -               |
| **Krypton FNP 0.2.10** | 104,184             | **+58.2%**   | +62.4%          |
| **Krypton FNP 0.2.12** | 66,312              | **+0.7%**    | -36.3%          |
| **Krypton FNP 0.2.14** | 422,719             | **+542.0%**  | +537.3%         |

- `Write`

| Implementation         | Performance (ops/s) | vs Minecraft | vs Previous Gen |
|------------------------|---------------------|--------------|-----------------|
| **Minecraft**          | 27,227              | Baseline     | -               |
| **Krypton FNP 0.2.13** | 36,395              | **+33.2%**   | +33.2%          |
| **Krypton FNP 0.2.14** | 45,683              | **+72.8%**   | +25.5%          |

- `Data Size Specialized Performance`

| Data Size Category            | Performance (ops/s) | vs Mixed Data |
|-------------------------------|---------------------|---------------|
| **Small Values (1-2 bytes)**  | 672,153             | **+1412%**    |
| **Medium Values (3-5 bytes)** | 457,188             | **+929%**     |
| **Large Values (6-10 bytes)** | 255,988             | **+476%**     |

## Compiling / Releases

**CAUTION!** I do not provide any guarantees about Krypton's stability, compatibility with other mods,
ability to be used on every server, or support for every possible setup out there. Support
for this mod is provided on a "best-effort" basis. This is not my day job, it is a hobby
growing out of related work I've done. **You have been warned.**

Releases I deem reasonably stable can be found on [GitHub](https://github.com/astei/krypton/releases),
[CurseForge](https://www.curseforge.com/minecraft/mc-mods/krypton), and on [Modrinth](https://modrinth.com/mod/krypton).
Development builds may be downloaded from my [Jenkins server](https://ci.velocitypowered.com/job/krypton/).

You can also compile the mod from source in the usual fashion.