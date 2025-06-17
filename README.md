![Krypton logo](https://user-images.githubusercontent.com/16436212/102424564-692de280-3fd9-11eb-98a2-ac125cb8e507.png)

# Krypton FNP

![all](https://img.shields.io/badge/environment-any-4caf50?style=flat-square)

This mod is an unofficial port of [Krypton Fabric](https://modrinth.com/mod/krypton), designed to provide Forge &
NeoForge compatibility.

It should be consistent with the upstream behavior.

Krypton is a mod that attempts to optimize the Minecraft networking stack. It derives from work
done in the [Velocity](https://velocitypowered.com/) and [Paper](https://papermc.io) projects.

Krypton derives itself from Ancient Greek _kryptos_, which means "the hidden one". This makes
it evident most of the benefit from Krypton is "hidden" but is noticeable by a server administrator.

[The wiki contains important information &ndash; read it](https://github.com/astei/krypton/wiki).

## Feature
- More basic optimizations
- The Velocity Native library based on Rust brings exponential decompression performance improvements in simulated performance testing (provided by [VelocityNT Recast](https://github.com/404Setup/VelocityNT-Recast))
- Support NeoForge/Forge

## Benchmark

The Benchmark results are not necessarily accurate,
and the final results are determined based on different JVM distributions, startup parameters, and random factors.

I don't have a native Linux development environment; 
all my Linux tests are done in WSL2 (Windows Subsystem for Linux 2), which can bring about ~15%-25% additional loss.

Those tiny performance differences of about 1% can be considered as a random result.

Benchmark cannot simulate all scenarios, and the results are used only as reference.

### View

[VarInt & VarLong | View in JMH Visualizer](https://jmh.morethan.io/?source=https://raw.githubusercontent.com/404Setup/KryptonFNP/refs/heads/master/results/results.json)

[Native Compress for Windows | View in JMH Visualizer](https://jmh.morethan.io/?source=https://raw.githubusercontent.com/404Setup/KryptonFNP/refs/heads/master/results/native_compress_windows.json)

[Native Compress for Linux {WSL2} | View in JMH Visualizer](https://jmh.morethan.io/?source=https://raw.githubusercontent.com/404Setup/KryptonFNP/refs/heads/master/results/native_compress_linux.json)

### How to run Benchmark?

After clone KryptonFNP repo, execute `./gradlew :common:jmh` in the mod directory.
After waiting for 50–90 minutes, the result will be generated to `common/build/results/jmh/results.json`
