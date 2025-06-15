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

### Compression

I didn't test

### Encryption

I didn't test

### VarInt
I didn't test

### VarLong

#### getByteSize

| Implementation         | Performance (ops/s) | vs Minecraft | vs Previous Gen |
|------------------------|---------------------|--------------|-----------------|
| **Minecraft**          | 64,404              | Baseline     | -               |
| **Krypton FNP 0.2.9**  | 115,026             | **+78.6%**   | +78.6%          |
| **Krypton FNP 0.2.14** | 409,338             | **+535.7%**  | +255.8%         |

#### Write

| Implementation         | Performance (ops/s) | vs Minecraft | vs Previous Gen |
|------------------------|---------------------|--------------|-----------------|
| **Minecraft**          | 25,712              | Baseline     | -               |
| **Krypton FNP 0.2.13** | 34,237              | **+33.2%**   | +33.2%          |
| **Krypton FNP 0.2.14** | 44,431              | **+72.8%**   | +29.8%          |

#### Data Size Specialized Performance

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