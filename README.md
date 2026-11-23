# TACZ Turrets

NeoForge 1.21.1 模组：三个使用 [TACZ](https://modrinth.com/mod/tacz)（永恒枪械工坊：零）枪械开火的炮塔底座。

A NeoForge 1.21.1 mod adding three turret bases that fire [TACZ](https://modrinth.com/mod/tacz) guns.

---

## 三个炮塔 / The three turrets

| 炮塔 | 控制方式 | 弹药 |
|---|---|---|
| **固定式炮塔** `fixed_turret` | 固定在 [Sable](https://modrinth.com/mod/sable) 物理方块上，只随所在物理实体转向；由链接的 AeroIE 控制椅开火键触发 | TACZ 弹药 / 弹药箱 / 无限弹药箱 |
| **座椅联动炮塔** `seat_turret` | 用 AeroIE 链接器连到控制椅，跟随玩家鼠标瞄准，按开火键射击 | 同上 |
| **Create 炮塔** `cbc_turret` | 由 [Create: Big Cannons](https://modrinth.com/mod/create-big-cannons) 火炮底座驱动，读取底座的偏航/俯仰轴转速；底座红石信号触发开火；机械臂可装填 | 同上 |

三者都保留 TACZ 枪械的全部效果（弹道、爆炸、音效、曳光等），枪械模型可在炮塔上放大/缩小。

All three keep full TACZ gun behavior (ballistics, explosions, sounds, tracers). The gun model on the turret can be scaled up/down.

---

## 依赖 / Dependencies

| 模组 | 版本 |
|---|---|
| NeoForge | 21.1.x |
| Minecraft | 1.21.1 |
| [TACZ](https://modrinth.com/mod/tacz) | 1.2.0+ |
| [AeroIE](https://modrinth.com/mod/aeroie) (mod id `vsie`) | 1.1.10+ |
| [Sable](https://modrinth.com/mod/sable) | 2.0.0+ |
| [Create](https://modrinth.com/mod/create) | 6.0.7–6.1.0 |
| [Create: Big Cannons](https://modrinth.com/mod/create-big-cannons) | 5.11.7+ |
| [GeckoLib](https://modrinth.com/mod/geckolib) | 4.9.0+ |

---

## 构建 / Building

需要 JDK 21。依赖 jar 不在本仓库内（第三方二进制，不提交），需先下载到 `libs/` 目录 —— 清单见 [`libs/README.md`](libs/README.md)。

```bash
# 下载 libs 后：
./gradlew build          # Linux/macOS
gradlew.bat build        # Windows
```

产物在 `build/libs/taczturrets-1.0.0.jar`。

Requires JDK 21. Put the dependency jars in `libs/` (see [`libs/README.md`](libs/README.md)), then run `gradlew build`. Output is `build/libs/taczturrets-1.0.0.jar`.

---

## 使用 / Usage

- **装枪**：手持 TACZ 枪械右键炮塔（或绑定的"装枪"快捷键）。装枪会清空枪内自带弹药，改为从炮塔的弹药槽取弹。
- **装弹**：手持 TACZ 弹药 / 弹药箱右键炮塔（弹药需与枪械类型匹配）。机械臂可向 Create 炮塔装填。
- **打开界面**：空手右键（或"打开物品界面"快捷键），界面里有枪械槽 + 9 弹药槽 + 缩放/偏移/初始角度/武器频道的数值框。
- **缩放/偏移**：界面里直接填数值，实时生效；频道 1–4 切换后炮塔回到可设置的初始角度。
- **改朝向**：手持 Create 扳手右键切换方块朝向。
- **模型微调（无需重启）**：游戏内聊天栏 `/tczmodel`（`list` / `set <fixed|seat|mount> <yaw|pitch|roll> <角度>` / `add` / `save`）。
- **武器频道**：界面 1–4 按钮，与 AeroIE 控制椅频道对应。

---

## 许可 / License

[MIT](LICENSE) — 你可自由使用、修改、分发；但本模组依赖的 TACZ / AeroIE / Create 等第三方模组各自保留其许可。
