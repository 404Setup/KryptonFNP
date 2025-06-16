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

## Benchmark

The Benchmark results are not necessarily accurate,
and the final results are determined based on different JVM distributions, startup parameters, and random factors.

Those tiny performance differences of about 1% can be considered as a random result.

Benchmark cannot simulate all scenarios, and the results are used only as reference.

### How to run Benchmark?

After clone KryptonFNP repo, execute `./gradlew :common:jmh` in the mod directory.
After waiting for 20–40 minutes, the result will be generated to `common/build/results/jmh/results.txt`

### VarInt

- `getByteSize`

| Implementation        | Performance (ops/s) | vs Minecraft | vs Previous Gen |
|-----------------------|---------------------|--------------|-----------------|
| **Minecraft**         | 99,112              | Baseline     | -               |
| **Krypton FNP 0.2.9** | 448,773             | **+352.8%**  | +352.8%         |

- `Write`

| Implementation         | Performance (ops/s) | vs Minecraft | vs Previous Gen |
|------------------------|---------------------|--------------|-----------------|
| **Minecraft**          | 32,072              | Baseline     | -               |
| **Krypton FNP 0.2.09** | 61,160              | **+90.7%**   | +90.7%          |
| **Krypton FNP 0.2.10** | 60,537              | **+88.8%**   | -1.0%           |
| **Krypton FNP 0.2.16** | 60,668              | **+89.2%**   | +0.2%           |

- `Data Size Specialized Performance | Small Values (1-2 bytes)`

| Implementation         | Performance (ops/s) | vs Minecraft | vs Previous Gen |
|------------------------|---------------------|--------------|-----------------|
| **Minecraft**          | 401,587             | Baseline     | -               |
| **Krypton FNP 0.2.09** | 735,465             | **+83.1%**   | +83.1%          |
| **Krypton FNP 0.2.10** | 734,813             | **+83.0%**   | -0.1%           |
| **Krypton FNP 0.2.16** | 740,547             | **+84.4%**   | +0.8%           |

- `Data Size Specialized Performance | Medium Values (3-4 bytes)`

| Implementation         | Performance (ops/s) | vs Minecraft | vs Previous Gen |
|------------------------|---------------------|--------------|-----------------|
| **Minecraft**          | 298,429             | Baseline     | -               |
| **Krypton FNP 0.2.09** | 538,720             | **+80.5%**   | +80.5%          |
| **Krypton FNP 0.2.10** | 575,530             | **+92.8%**   | +6.8%           |
| **Krypton FNP 0.2.16** | 586,174             | **+96.4%**   | +1.8%           |

- `Data Size Specialized Performance | Large Values (5 bytes)`

| Implementation         | Performance (ops/s) | vs Minecraft | vs Previous Gen |
|------------------------|---------------------|--------------|-----------------|
| **Minecraft**          | 236,620             | Baseline     | -               |
| **Krypton FNP 0.2.09** | 453,322             | **+91.6%**   | +91.6%          |
| **Krypton FNP 0.2.10** | 422,586             | **+78.6%**   | -6.8%           |
| **Krypton FNP 0.2.16** | 407,806             | **+72.3%**   | -3.5%           |

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