# Local Recipe Inject · 本地配方注入

一个 **Fabric 客户端 Mod**：把客户端本地（自带 mod / 原版 jar 里的配方）注入客户端配方书，
让它们在 **JEI / REI / EMI** 等查询类 mod 与配方书中视为“已解锁”，
绕过 **Minecraft 1.21.2+ 服务器不再下发未解锁配方** 导致这些 mod 失效的问题。
可在 **模组菜单（Mod Menu）** 里可视化开关。

> 适用于 **Minecraft 1.21.11 · Fabric**。仅客户端，无需服务端安装。

---

## 背景：1.21.2 改了什么

自 Minecraft 1.21.2 起，配方数据由服务器持有，**默认不再下发完整配方表**给客户端，
只发送已解锁配方的展示数据（`RecipeDisplayEntry`）。后果是 JEI/REI 这类纯客户端
查询 mod 只能看到“已解锁”的配方，查询体验大幅退化。

### 这个 Mod 的取舍

- **对未改配方的原版服务器**：配方在服务端本就存在，只是没进你的配方书；
  注入本地配方后，查询恢复、手动合成照样成功。
- **预期内的“出错”**：服务器**改过**原版配方、或有**纯服务端**新增配方时，
  本地副本会与实际不符或缺失。因此主开关**默认关闭**，需显式开启。
- **viewer 无关**：注入的是客户端配方书条目本身，而非某个 XEI 的插件，
  故 JEI / REI / EMI 都受益。

---

## 配置项（模组菜单可视化）

| 开关 | 默认 | 说明 |
|---|---|---|
| 强制启用本地配方 | 关 | 主开关。关闭 = 原版行为，不注入。 |
| 包含模组配方 | 开 | 同时注入已安装 mod 的配方，否则只补原版。 |
| 标记冲突配方 | 开 | 与服务器同 id 但内容不同时跳过/标记，避免误导。 |
| 跳过特殊配方 | 开 | 跳过无固定展示的动态/特殊配方（如地图复制）。 |
| 打印注入日志 | 开 | 每次进入世界打印注入统计。 |

配置文件：`config/localrecipeinject.json`。

---

## 工作原理

1.21.2 重写后，客户端不再持有完整 `Recipe` 集（`RecipeManager` 接口在客户端只剩
propertySet/切石机）。REI/JEI 与配方书读取的是 `ClientRecipeBook` 里的 `RecipeDisplayEntry`。
因此注入目标是**配方书条目**：

```
本地 data/<ns>/recipe/*.json
  → Recipe.CODEC.parse(RegistryOps, json)    解析为 Recipe<?>
  → recipe.getDisplays()                      取展示
  → 包成 RecipeDisplayEntry(负数 NetworkRecipeId, display, …)
  → 合成 RecipeBookAddS2CPacket(entries, replace=false)
  → handler.onRecipeBookAdd(packet)           走 vanilla 完整下发路径
```

触发点：`ClientPlayNetworkHandlerMixin` 挂在 `onRecipeBookAdd` 的 TAIL，
仅当服务器发来 `replace=true`（进世界初始同步）时补注入，用 `replace=false` 重发本地条目，
既不清空服务器条目、也不触发自身重入。重放整个 add 包（而非直接 `book.add`），
是为了同时覆盖“viewer 读配方书对象”与“viewer 自己挂在 onRecipeBookAdd 上”两种实现。

---

## 构建

```bash
./gradlew build
# 产物：build/libs/local-recipe-inject-1.0.0.jar
```

### 工具链版本（已核实）

| 组件 | 版本 |
|---|---|
| Minecraft | 1.21.11 |
| Yarn | 1.21.11+build.6 |
| Fabric Loader | 0.19.2 |
| Fabric API | 0.141.4+1.21.11 |
| Mod Menu | 17.0.0 |
| Loom | 1.13.6 |
| Java | 21 |

---

## 安装与使用

1. 把 `local-recipe-inject-*.jar` + **Fabric API** + **Mod Menu** +（想看配方的那些 mod）
   一起放进 `mods/`。可选装 **REI / JEI / EMI**。
2. 进服务器后，在**模组菜单 → 本地配方注入**里打开 **强制启用本地配方**。
3. **重连一次世界/服务器**触发注入（开关在“进世界初始同步”时生效，
   世界内中途打开需重连）。
4. 日志出现 `[LRI] 注入配方书：扫描 N，注入 M …` 即成功。

> 若 REI 仍不显示：它可能在注入前就缓存了索引。先试资源重载（F3+T）或重连。

---

## 代码结构

```
dev.icey.lri
├─ LocalRecipeInjectClient        客户端入口：加载配置
├─ config/                        LriConfig（POJO）+ ConfigManager（GSON 读写）
├─ ui/LriConfigScreen             手写配置界面（零额外依赖）
├─ compat/ModMenuIntegration      Mod Menu 配置入口
├─ mixin/ClientPlayNetworkHandlerMixin  在配方书整表同步后触发注入
└─ core/
   ├─ LocalRecipeLoader           扫描各 mod jar 的 data/<ns>/recipe/*.json
   ├─ ClientRecipeBridge          解析 JSON → RecipeDisplayEntry
   └─ RecipeInjector              合成 add 包并走 vanilla 路径下发
```

---

## 许可

MIT
