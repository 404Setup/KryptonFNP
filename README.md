<img src="./common/src/main/resources/assets/krypton/icon.png" alt="Logo" align="right" width="150">

Krypton FNP
====

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
- Support RecastLib (Velocity Native rewritten in Rust, compatible with Windows)
- Support NeoForge/Forge

## Config

Add the following parameters to the Java startup parameters to control the mixin enablement:

| Parameter                     | Description                                                                                 |
|-------------------------------|---------------------------------------------------------------------------------------------|
| krypton.loginVT               | Enable Login VirtualThread optimization                                                     |
| krypton.textFilterVT          | Enable TextFilter VirtualThread optimization                                                |
| krypton.utilVT                | Enable Util VirtualThread optimization                                                      |
| krypton.bestVarLong           | Enable VarLong optimization                                                                 |
| velocity.linux-recast-enabled | Enable RecastLib (default: false; Only works on Linux, RecastLib for Windows is mandatory.) |

example:

```shell
java -Dkrypton.loginVT=false -jar neoforge_launcher.jar
```

### Use env instead of jvm args

Some configuration items support using environment variables instead of jvm args.

| JVM ARGS                      | Environment Variable |
|-------------------------------|----------------------|
| velocity.linux-recast-enabled | ENABLE_LINUX_RECAST  |

## Benchmark

The Benchmark results are not necessarily accurate,
and the final results are determined based on different JVM distributions, startup parameters, and random factors.

I don't have a native Linux development environment;
all my Linux tests are done in WSL2 (Windows Subsystem for Linux), which can bring about ~15%-25% additional loss.

Those tiny performance differences of about 1% can be considered as a random result.

Benchmark cannot simulate all scenarios, and the results are used only as reference.

### View

[VarInt & VarLong | View in JMH Visualizer](https://jmh.morethan.io/?source=https://raw.githubusercontent.com/404Setup/KryptonFNP/refs/heads/master/results/var.json)

[Native Compress for Windows {RecastLib2} | View in JMH Visualizer](https://jmh.morethan.io/?source=https://raw.githubusercontent.com/404Setup/KryptonFNP/refs/heads/master/results/native_compress_windows_recastlib.json)

[Native Compress for WSL2 {RecastLib2} | View in JMH Visualizer](https://jmh.morethan.io/?source=https://raw.githubusercontent.com/404Setup/KryptonFNP/refs/heads/master/results/native_compress_linux_recastlib.json)

[Native Compress for WSL2 {VelocityNative} | View in JMH Visualizer](https://jmh.morethan.io/?source=https://raw.githubusercontent.com/404Setup/KryptonFNP/refs/heads/master/results/native_compress_linux_vc.json)

### How to run Benchmark?

After clone KryptonFNP repo, execute `./gradlew :common:jmh` in the mod directory.
After waiting for 50–90 minutes, the result will be generated to `common/build/results/jmh/results.json`
