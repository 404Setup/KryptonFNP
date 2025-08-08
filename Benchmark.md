# Benchmark

The Benchmark results are not necessarily accurate,
and the final results are determined based on different JVM distributions, startup parameters, and random factors.

Those tiny performance differences of about 3% can be considered as a random result.

The data used in the benchmark tests are all randomly generated simulated data and are not the original implementation. 
They can only represent the underlying performance of the corresponding implementation at that time. 
The final results are for reference only.

Some simulation scenes have been removed because they are too time consuming.

## How to run Benchmark?

After clone KryptonFNP repo, execute `./gradlew :common:jmh` in the mod directory.
After waiting for 50–90 minutes, the result will be generated to `common/build/results/jmh/results.json`

## VarLong && VarInt Result

<a href="https://jmh.morethan.io/?source=https://raw.githubusercontent.com/404Setup/KryptonFNP/refs/heads/master/results/var.json">
<img src="https://img.shields.io/badge/VarInt & VarLong-View%20full%20report-4caf50?style=flat-square" alt=""/>
</a>

## Native Result

- ScoreUnit: ns/op
- R: RecastLib
- V: Velocity Native
- J: JNI
- F: FFM API
- W: Windows
- L: Linux

### CompressorOnly

> Raw data
>

| Test items                   | Java     | R:W:J   | R:W:F   | R:L:J   | V:L:J   |
|------------------------------|----------|---------|---------|---------|---------|
| 15:128:SMALL:REPETITIVE      | 43898    | 75078   | 75081   | 79305   | 79100   |
| 15:128:SMALL:MINECRAFT_LIKE  | 91869    | 82460   | 80095   | 85921   | 86331   |
| 15:128:MEDIUM:REPETITIVE     | 71740    | 88844   | 89597   | 98359   | 94998   |
| 15:128:MEDIUM:MINECRAFT_LIKE | 408423   | 150202  | 139369  | 148118  | 152870  |
| 15:128:LARGE:REPETITIVE      | 326230   | 213351  | 213105  | 208557  | 227857  |
| 15:128:LARGE:MINECRAFT_LIKE  | 2025450  | 769477  | 685934  | 703105  | 701689  |
| 15:512:SMALL:REPETITIVE      | 193      | 192     | 193     | 156     | 157     |
| 15:512:SMALL:MINECRAFT_LIKE  | 193      | 193     | 193     | 168     | 160     |
| 15:512:MEDIUM:REPETITIVE     | 71782    | 89535   | 89387   | 96883   | 97529   |
| 15:512:MEDIUM:MINECRAFT_LIKE | 401661   | 153261  | 140273  | 146947  | 147486  |
| 15:512:LARGE:REPETITIVE      | 325668   | 213421  | 212954  | 217036  | 215065  |
| 15:512:LARGE:MINECRAFT_LIKE  | 1995240  | 796844  | 676813  | 693356  | 692467  |
| 75:128:SMALL:REPETITIVE      | 206114   | 371652  | 363902  | 392343  | 392995  |
| 75:128:SMALL:MINECRAFT_LIKE  | 541581   | 457661  | 438110  | 476312  | 477586  |
| 75:128:MEDIUM:REPETITIVE     | 351895   | 444372  | 440285  | 467297  | 462185  |
| 75:128:MEDIUM:MINECRAFT_LIKE | 2106114  | 816382  | 774640  | 818357  | 893982  |
| 75:128:LARGE:REPETITIVE      | 1628586  | 1078597 | 1072160 | 1079700 | 1142609 |
| 75:128:LARGE:MINECRAFT_LIKE  | 10150132 | 3978401 | 3441119 | 3536119 | 3607817 |
| 75:512:SMALL:REPETITIVE      | 870      | 851     | 831     | 720     | 722     |
| 75:512:SMALL:MINECRAFT_LIKE  | 875      | 851     | 838     | 723     | 723     |
| 75:512:MEDIUM:REPETITIVE     | 359785   | 441481  | 440569  | 491272  | 471481  |
| 75:512:MEDIUM:MINECRAFT_LIKE | 2085553  | 813876  | 771821  | 834973  | 818588  |
| 75:512:LARGE:REPETITIVE      | 1637272  | 1084084 | 1069283 | 1090750 | 1086440 |
| 75:512:LARGE:MINECRAFT_LIKE  | 10190895 | 3939917 | 3421928 | 3532548 | 3562147 |

> **Simplified comparison**
>
> The larger the percentage, the faster

| Test items                   | Java    | R:W:J   | R:W:F   | R:L:J   | V:L:J   |
|------------------------------|---------|---------|---------|---------|---------|
| 15:128:SMALL:REPETITIVE      | 100.00% | 58.47%  | 58.47%  | 55.36%  | 55.49%  |
| 15:128:SMALL:MINECRAFT_LIKE  | 100.00% | 111.42% | 114.74% | 106.89% | 106.44% |
| 15:128:MEDIUM:REPETITIVE     | 100.00% | 80.72%  | 80.04%  | 72.92%  | 75.53%  |
| 15:128:MEDIUM:MINECRAFT_LIKE | 100.00% | 271.92% | 293.00% | 275.67% | 267.19% |
| 15:128:LARGE:REPETITIVE      | 100.00% | 152.93% | 153.05% | 156.37% | 143.17% |
| 15:128:LARGE:MINECRAFT_LIKE  | 100.00% | 263.25% | 295.24% | 288.07% | 288.57% |
| 15:512:SMALL:REPETITIVE      | 100.00% | 100.52% | 100.00% | 123.72% | 122.93% |
| 15:512:SMALL:MINECRAFT_LIKE  | 100.00% | 100.00% | 100.00% | 114.88% | 120.63% |
| 15:512:MEDIUM:REPETITIVE     | 100.00% | 80.20%  | 80.36%  | 74.09%  | 73.60%  |
| 15:512:MEDIUM:MINECRAFT_LIKE | 100.00% | 262.21% | 286.31% | 273.35% | 272.35% |
| 15:512:LARGE:REPETITIVE      | 100.00% | 152.56% | 152.89% | 150.06% | 151.42% |
| 15:512:LARGE:MINECRAFT_LIKE  | 100.00% | 250.39% | 294.71% | 287.79% | 288.16% |
| 75:128:SMALL:REPETITIVE      | 100.00% | 55.47%  | 56.64%  | 52.53%  | 52.44%  |
| 75:128:SMALL:MINECRAFT_LIKE  | 100.00% | 118.34% | 123.64% | 113.71% | 113.40% |
| 75:128:MEDIUM:REPETITIVE     | 100.00% | 79.19%  | 79.93%  | 75.28%  | 76.13%  |
| 75:128:MEDIUM:MINECRAFT_LIKE | 100.00% | 257.92% | 271.91% | 257.36% | 235.56% |
| 75:128:LARGE:REPETITIVE      | 100.00% | 151.00% | 151.89% | 150.84% | 142.54% |
| 75:128:LARGE:MINECRAFT_LIKE  | 100.00% | 255.15% | 294.96% | 287.08% | 281.28% |
| 75:512:SMALL:REPETITIVE      | 100.00% | 102.23% | 104.69% | 120.83% | 120.50% |
| 75:512:SMALL:MINECRAFT_LIKE  | 100.00% | 102.82% | 104.42% | 121.02% | 121.02% |
| 75:512:MEDIUM:REPETITIVE     | 100.00% | 81.50%  | 81.70%  | 73.25%  | 76.29%  |
| 75:512:MEDIUM:MINECRAFT_LIKE | 100.00% | 256.27% | 270.32% | 249.81% | 254.81% |
| 75:512:LARGE:REPETITIVE      | 100.00% | 151.00% | 153.05% | 150.08% | 150.69% |
| 75:512:LARGE:MINECRAFT_LIKE  | 100.00% | 258.67% | 297.83% | 288.45% | 286.02% |

### DeflateBatch

> Raw data
>

| Test items                   | Java     | R:W:J   | R:W:F   | R:L:J   | V:L:J   |
|------------------------------|----------|---------|---------|---------|---------|
| 15:128:SMALL:REPETITIVE      | 41241    | 76302   | 74640   | 78299   | 79486   |
| 15:128:SMALL:MINECRAFT_LIKE  | 89536    | 83247   | 81242   | 86682   | 86592   |
| 15:128:MEDIUM:REPETITIVE     | 70480    | 90628   | 90384   | 93568   | 95000   |
| 15:128:MEDIUM:MINECRAFT_LIKE | 404538   | 151873  | 140620  | 149284  | 149419  |
| 15:128:LARGE:REPETITIVE      | 326112   | 215790  | 219075  | 216971  | 214854  |
| 15:128:LARGE:MINECRAFT_LIKE  | 2027554  | 778229  | 683401  | 739439  | 722911  |
| 15:512:SMALL:REPETITIVE      | 612      | 613     | 613     | 477     | 477     |
| 15:512:SMALL:MINECRAFT_LIKE  | 615      | 614     | 614     | 480     | 476     |
| 15:512:MEDIUM:REPETITIVE     | 70492    | 90145   | 89107   | 93337   | 99534   |
| 15:512:MEDIUM:MINECRAFT_LIKE | 410454   | 152518  | 141519  | 148855  | 148953  |
| 15:512:LARGE:REPETITIVE      | 322899   | 233437  | 219570  | 215222  | 211406  |
| 15:512:LARGE:MINECRAFT_LIKE  | 2014107  | 836731  | 687124  | 712303  | 709857  |
| 75:128:SMALL:REPETITIVE      | 206769   | 385512  | 371412  | 392076  | 399915  |
| 75:128:SMALL:MINECRAFT_LIKE  | 540993   | 464038  | 493259  | 478117  | 483776  |
| 75:128:MEDIUM:REPETITIVE     | 358706   | 448247  | 450595  | 482262  | 471773  |
| 75:128:MEDIUM:MINECRAFT_LIKE | 2096730  | 817261  | 779468  | 817516  | 835129  |
| 75:128:LARGE:REPETITIVE      | 1624101  | 1089687 | 1079245 | 1076438 | 1120596 |
| 75:128:LARGE:MINECRAFT_LIKE  | 10055430 | 3939499 | 3502948 | 3656222 | 3670874 |
| 75:512:SMALL:REPETITIVE      | 2953     | 2963    | 2976    | 2284    | 2271    |
| 75:512:SMALL:MINECRAFT_LIKE  | 2928     | 2951    | 2982    | 2318    | 2439    |
| 75:512:MEDIUM:REPETITIVE     | 352555   | 452718  | 448684  | 485677  | 476755  |
| 75:512:MEDIUM:MINECRAFT_LIKE | 2105619  | 824410  | 779336  | 842231  | 824191  |
| 75:512:LARGE:REPETITIVE      | 1627274  | 1085445 | 1087664 | 1080360 | 1083126 |
| 75:512:LARGE:MINECRAFT_LIKE  | 10034662 | 3934812 | 3502194 | 3623281 | 3661596 |

> **Simplified comparison**
>
> The larger the percentage, the faster

| Test items                   | Java    | R:W:J   | R:W:F   | R:L:J   | V:L:J   |
|------------------------------|---------|---------|---------|---------|---------|
| 15:128:SMALL:REPETITIVE      | 100.00% | 54.04%  | 55.24%  | 52.66%  | 51.88%  |
| 15:128:SMALL:MINECRAFT_LIKE  | 100.00% | 107.56% | 110.21% | 103.29% | 103.41% |
| 15:128:MEDIUM:REPETITIVE     | 100.00% | 77.77%  | 77.93%  | 75.31%  | 74.19%  |
| 15:128:MEDIUM:MINECRAFT_LIKE | 100.00% | 266.43% | 287.70% | 270.97% | 270.68% |
| 15:128:LARGE:REPETITIVE      | 100.00% | 151.17% | 148.92% | 150.27% | 151.81% |
| 15:128:LARGE:MINECRAFT_LIKE  | 100.00% | 260.55% | 296.66% | 274.28% | 280.55% |
| 15:512:SMALL:REPETITIVE      | 100.00% | 99.84%  | 99.84%  | 128.30% | 128.30% |
| 15:512:SMALL:MINECRAFT_LIKE  | 100.00% | 100.16% | 100.16% | 128.13% | 129.20% |
| 15:512:MEDIUM:REPETITIVE     | 100.00% | 78.21%  | 79.11%  | 75.52%  | 70.83%  |
| 15:512:MEDIUM:MINECRAFT_LIKE | 100.00% | 269.09% | 290.09% | 275.73% | 275.51% |
| 15:512:LARGE:REPETITIVE      | 100.00% | 138.32% | 147.06% | 150.04% | 152.78% |
| 15:512:LARGE:MINECRAFT_LIKE  | 100.00% | 240.68% | 293.09% | 282.76% | 283.79% |
| 75:128:SMALL:REPETITIVE      | 100.00% | 53.64%  | 55.69%  | 52.75%  | 51.72%  |
| 75:128:SMALL:MINECRAFT_LIKE  | 100.00% | 116.57% | 109.68% | 113.14% | 111.83% |
| 75:128:MEDIUM:REPETITIVE     | 100.00% | 80.03%  | 79.61%  | 74.38%  | 76.05%  |
| 75:128:MEDIUM:MINECRAFT_LIKE | 100.00% | 256.52% | 268.99% | 256.40% | 251.02% |
| 75:128:LARGE:REPETITIVE      | 100.00% | 149.06% | 150.50% | 150.88% | 144.94% |
| 75:128:LARGE:MINECRAFT_LIKE  | 100.00% | 255.24% | 287.00% | 275.04% | 273.88% |
| 75:512:SMALL:REPETITIVE      | 100.00% | 99.66%  | 99.16%  | 129.28% | 130.01% |
| 75:512:SMALL:MINECRAFT_LIKE  | 100.00% | 99.24%  | 98.19%  | 126.35% | 120.08% |
| 75:512:MEDIUM:REPETITIVE     | 100.00% | 77.87%  | 78.56%  | 72.59%  | 73.93%  |
| 75:512:MEDIUM:MINECRAFT_LIKE | 100.00% | 255.37% | 270.23% | 250.05% | 255.46% |
| 75:512:LARGE:REPETITIVE      | 100.00% | 149.90% | 149.60% | 150.64% | 150.24% |
| 75:512:LARGE:MINECRAFT_LIKE  | 100.00% | 255.07% | 286.49% | 276.96% | 274.13% |

### InflateBatch

> Raw data
>

| Test items               | Java    | R:W:J   | R:W:F   | R:L:J   | V:L:J   |
|--------------------------|---------|---------|---------|---------|---------|
| 15:SMALL:REPETITIVE      | 4699    | 4810    | 4937    | 6035    | 6991    |
| 15:SMALL:MINECRAFT_LIKE  | 17544   | 18714   | 18454   | 19470   | 21214   |
| 15:MEDIUM:REPETITIVE     | 11080   | 8870    | 9039    | 23621   | 23575   |
| 15:MEDIUM:MINECRAFT_LIKE | 91644   | 48567   | 47862   | 51346   | 51080   |
| 15:LARGE:REPETITIVE      | 89015   | 65192   | 61973   | 185807  | 185325  |
| 15:LARGE:MINECRAFT_LIKE  | 502070  | 234425  | 211077  | 226622  | 224817  |
| 75:SMALL:REPETITIVE      | 25172   | 27894   | 24235   | 29223   | 29778   |
| 75:SMALL:MINECRAFT_LIKE  | 91705   | 99387   | 92230   | 98419   | 98457   |
| 75:MEDIUM:REPETITIVE     | 55176   | 46544   | 45263   | 117275  | 131154  |
| 75:MEDIUM:MINECRAFT_LIKE | 538383  | 262620  | 250409  | 262980  | 290594  |
| 75:LARGE:REPETITIVE      | 439838  | 317350  | 312560  | 929131  | 954126  |
| 75:LARGE:MINECRAFT_LIKE  | 2567475 | 1197005 | 1129717 | 1232148 | 1322266 |

> **Simplified comparison**
>
> The larger the percentage, the faster

| Test items               | Java (100%) | R:W:J   | R:W:F   | R:L:J   | V:L:J   |
|--------------------------|-------------|---------|---------|---------|---------|
| 15:SMALL:REPETITIVE      | 100.00%     | 97.70%  | 95.17%  | 77.84%  | 67.22%  |
| 15:SMALL:MINECRAFT_LIKE  | 100.00%     | 93.73%  | 95.09%  | 90.10%  | 82.70%  |
| 15:MEDIUM:REPETITIVE     | 100.00%     | 124.91% | 122.58% | 46.90%  | 47.01%  |
| 15:MEDIUM:MINECRAFT_LIKE | 100.00%     | 188.66% | 191.46% | 178.52% | 179.46% |
| 15:LARGE:REPETITIVE      | 100.00%     | 136.56% | 143.62% | 47.91%  | 48.04%  |
| 15:LARGE:MINECRAFT_LIKE  | 100.00%     | 214.15% | 237.81% | 221.47% | 223.32% |
| 75:SMALL:REPETITIVE      | 100.00%     | 90.24%  | 103.86% | 86.13%  | 84.55%  |
| 75:SMALL:MINECRAFT_LIKE  | 100.00%     | 92.26%  | 99.46%  | 93.17%  | 93.13%  |
| 75:MEDIUM:REPETITIVE     | 100.00%     | 118.52% | 121.88% | 47.04%  | 42.06%  |
| 75:MEDIUM:MINECRAFT_LIKE | 100.00%     | 204.95% | 214.99% | 204.71% | 185.23% |
| 75:LARGE:REPETITIVE      | 100.00%     | 138.62% | 140.71% | 47.33%  | 46.10%  |
| 75:LARGE:MINECRAFT_LIKE  | 100.00%     | 214.49% | 227.23% | 208.34% | 194.20% |

### Full Result

<a href="https://jmh.morethan.io/?source=https://raw.githubusercontent.com/404Setup/KryptonFNP/refs/heads/master/results/compress/windows.recastlib.jni.json">
<img src="https://img.shields.io/badge/R:W:J-View full report-4caf50?style=flat-square" alt=""/>
</a>

<a href="https://jmh.morethan.io/?source=https://raw.githubusercontent.com/404Setup/KryptonFNP/refs/heads/master/results/compress/windows.recastlib.ffm.json">
<img src="https://img.shields.io/badge/R:W:F-View full report-4caf50?style=flat-square" alt=""/>
</a>

<a href="https://jmh.morethan.io/?source=https://raw.githubusercontent.com/404Setup/KryptonFNP/refs/heads/master/results/compress/linux.recastlib.jni.json">
<img src="https://img.shields.io/badge/R:L:J-View full report-4caf50?style=flat-square" alt=""/>
</a>

<a href="https://jmh.morethan.io/?source=https://raw.githubusercontent.com/404Setup/KryptonFNP/refs/heads/master/results/compress/linux.velocity.jni.json">
<img src="https://img.shields.io/badge/V:L:J-View full report-4caf50?style=flat-square" alt=""/>
</a>