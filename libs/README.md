# libs/ 目录 —— 依赖 jar 清单

这些是第三方模组 jar（版权归各自作者），**不提交到 git**。构建前请把下列 jar 下载到这个 `libs/` 目录：

| 文件 | 来源 |
|---|---|
| `AeroIE-1.1.10.jar` | AeroIE（mod id `vsie`）1.1.10 |
| `sable-neoforge-1.21.1-2.0.6.jar` | Sable 2.0.6 |
| `sable-companion-common-1.21.1-1.6.0.jar` | Sable 内嵌的 companion 库（在 sable jar 的 `META-INF/jarjar/` 里） |
| `tacz-1.2.0.jar` | TACZ（永恒枪械工坊：零）1.2.0 |
| `create-1.21.1-6.0.10.jar` | Create 6.0.10 |
| `createbigcannons-5.11.7+mc.1.21.1.jar` | Create: Big Cannons 5.11.7 |
| `geckolib-neoforge-1.21.1-4.9.3.jar` | GeckoLib 4.9.3 |
| `ritchiesprojectilelib-2.1.2+mc.1.21.1-neoforge.jar` | RitchiesProjectileLib 2.1.2 |
| `photon-neoforge-1.21.1-2.1.5.jar` | Photon 2.1.5 |
| `patchouli-1.21.1-93-neoforge.jar` | Patchouli 93 |
| `ponder-neoforge-1.0.82+mc1.21.1.jar` | Ponder 1.0.82（Create 内嵌，在 create jar 的 `META-INF/jarjar/`） |
| `Registrate-MC1.21-1.3.0+67.jar` | Registrate 1.3.0（Create 内嵌） |

下载渠道：CurseForge / Modrinth 对应项目页。sable-companion 和 Ponder / Registrate 可直接从 sable / create 的 jar 里 `META-INF/jarjar/` 解出来。

> 说明：`build.gradle` 用 `implementation fileTree(dir: 'libs', include: ['*.jar'])` 引用这些 jar，所以只需要把文件放进来即可编译。
