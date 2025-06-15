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

Benchmark cannot simulate all scenarios, and the results are used only as reference.

### How to run Benchmark?

After clone KryptonFNP repo, execute `./gradlew :common:jmh` in the mod directory.
After waiting for 20–40 minutes, the result will be generated to `common/build/results/jmh/results.txt`

### VarInt

- `getByteSize`

| Implementation        | Performance (ops/s) | vs Minecraft | vs Previous Gen |
|-----------------------|---------------------|--------------|-----------------|
| **Minecraft**         | 87,630              | Baseline     | -               |
| **Krypton FNP 0.2.9** | 443,558             | **+406.1%**  | +406.1%         |

- `Write`

| Implementation         | Performance (ops/s) | vs Minecraft | vs Previous Gen |
|------------------------|---------------------|--------------|-----------------|
| **Minecraft**          | 32,152              | Baseline     | -               |
| **Krypton FNP 0.2.09** | 53,787              | **+67.2%**   | +67.2%          |
| **Krypton FNP 0.2.10** | 63,200              | **+96.5%**   | +17.5%          |

- `Data Size Specialized Performance | Small Values (1-2 bytes)`

| Data Size Category     | Performance (ops/s) | vs Minecraft | vs Previous Gen |
|------------------------|---------------------|--------------|-----------------|
| **Minecraft**          | 445,110             | Baseline     | -               |
| **Krypton FNP 0.2.09** | 882,266             | **+98.2%**   | +98.2%          |
| **Krypton FNP 0.2.10** | 754,263             | **+69.4%**   | -16.9%          |

- `Data Size Specialized Performance | Medium Values (3-4 bytes)`

| Data Size Category     | Performance (ops/s) | vs Minecraft | vs Previous Gen |
|------------------------|---------------------|--------------|-----------------|
| **Minecraft**          | 294,285             | Baseline     | -               |
| **Krypton FNP 0.2.09** | 543,529             | **+84.6%**   | +84.6%          |
| **Krypton FNP 0.2.10** | 536,198             | **+82.2%**   | -1.3%           |

- `Data Size Specialized Performance | Large Values (5 bytes)`

| Data Size Category     | Performance (ops/s) | vs Minecraft | vs Previous Gen |
|------------------------|---------------------|--------------|-----------------|
| **Minecraft**          | 233,121             | Baseline     | -               |
| **Krypton FNP 0.2.09** | 415,558             | **+78.2%**   | +78.2%          |
| **Krypton FNP 0.2.10** | 417,760             | **+79.2%**   | +0.5%           |

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

- `Data Size Specialized Performance | Small Values (1-2 bytes)`

| Data Size Category     | Performance (ops/s) | vs Minecraft | vs Previous Gen |
|------------------------|---------------------|--------------|-----------------|
| **Minecraft**          | 430,115             | Baseline     | -               |
| **Krypton FNP 0.2.13** | 613,444             | **+42.6%**   | +42.6%          |
| **Krypton FNP 0.2.14** | 730,696             | **+69.8%**   | +19.1%          |

- `Data Size Specialized Performance | Medium Values (3-5 bytes)`

| Data Size Category     | Performance (ops/s) | vs Minecraft | vs Previous Gen |
|------------------------|---------------------|--------------|-----------------|
| **Minecraft**          | 278,078             | Baseline     | -               |
| **Krypton FNP 0.2.13** | 490,109             | **+76.2%**   | +76.2%          |
| **Krypton FNP 0.2.14** | 484,071             | **+74.1%**   | -1.2%           |

- `Data Size Specialized Performance | Large Values (6-10 bytes)`

| Data Size Category     | Performance (ops/s) | vs Minecraft | vs Previous Gen |
|------------------------|---------------------|--------------|-----------------|
| **Minecraft**          | 150,128             | Baseline     | -               |
| **Krypton FNP 0.2.13** | 160,425             | **+6.8%**    | +6.8%           |
| **Krypton FNP 0.2.14** | 262,583             | **+74.9%**   | +63.6%          |

## Compiling / Releases

**CAUTION!** I do not provide any guarantees about Krypton's stability, compatibility with other mods,
ability to be used on every server, or support for every possible setup out there. Support
for this mod is provided on a "best-effort" basis. This is not my day job, it is a hobby
growing out of related work I've done. **You have been warned.**

Releases I deem reasonably stable can be found on [GitHub](https://github.com/astei/krypton/releases),
[CurseForge](https://www.curseforge.com/minecraft/mc-mods/krypton), and on [Modrinth](https://modrinth.com/mod/krypton).
Development builds may be downloaded from my [Jenkins server](https://ci.velocitypowered.com/job/krypton/).

You can also compile the mod from source in the usual fashion.