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

[View in JMH Visualizer](https://jmh.morethan.io/?source=https://raw.githubusercontent.com/404Setup/krypton-Multi/refs/heads/master/results/results.json)

The Benchmark results are not necessarily accurate,
and the final results are determined based on different JVM distributions, startup parameters, and random factors.

Those tiny performance differences of about 1% can be considered as a random result.

Benchmark cannot simulate all scenarios, and the results are used only as reference.

### How to run Benchmark?

After clone KryptonFNP repo, execute `./gradlew :common:jmh` in the mod directory.
After waiting for 25–45 minutes, the result will be generated to `common/build/results/jmh/results.json`