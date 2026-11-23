# 便携末影箱交接：附属候选实现

日期：2026-11-23。附属候选版本 `0.4.0-handoff.1`，仅 1.21.1 NeoForge / Fabric 接入；不是已完成联调的试玩版本。

## 已核对的主模组状态

核对主线 `f838993840ee4f1692a353eb2ff053cdfb0bd4c8`（GitHub API 源码快照）。主线清单有七个 buildable 目标，最新正式发布 v1.12.1 只有五个发布资产。不能将七目标主线支持说成七目标正式版已发布。

当前公开 API 版本为 7，没有公开的已购查询、统一设备检查或 App 所属方交接接口。现有 `AppAccess.canUse` 是内部方法，而且无报价时会免费放行，不能作为移交后的严格已购判断。现有资格来自 `PhonePlayerData.of(player).purchasedApps().has(id)`；设备逻辑在 `PhoneItem.isCarriedBy`，菜单还允许 `PhoneItem.isDevice(getCarried())`。这些内部类未在本次附属中调用或反射调用。

`PhoneScreenRegistry.loadForCurrentWorld()` 先 `ensureLoaded()` 扫描 IPhoneApp SPI，再读主屏存档。附属通过这个同一 SPI 注册原 ID；无需事后替换或重新安装已存在图标。主模组应保持这一时序及原 ID 的快捷键恢复逻辑。

## 请求 MCphone 侧提供的最小公开接口（提案，尚非现有 API）

候选包将以下三个方法集中在一个公开类中。名字可以协商，确定后同步修改附属适配层；不要据此宣称主模组已经实现。

```java
package com.november.mcphone.api.server;
public final class PortableAppAccess {
    public static boolean isProvidedBy(ResourceLocation appId, String providerModId);
    public static boolean hasPurchased(ServerPlayer player, ResourceLocation appId);
    public static boolean isDeviceCarried(Player player, ItemStack carried);
}
```

- `isProvidedBy(mcphone:ender_chest, mcphone_workshop)`：只有本次配套主模组明确移交、加载了支持接管的附属版本时才为 true。客户端和服务器都可安全查询，必须在网络注册与 SPI 扫描前确定，进存档前不可从 false 延迟变成 true。不可仅凭同名模组存在就向旧附属移交。
- `hasPurchased`：严格读取当前存档服务端玩家的现有已购记录；没有报价、创造模式、客户端已安装都不能变成已购。不得以报价缺失免费放行。异常应报告失败，而非假装未购买。
- `isDeviceCarried`：统一主模组现有手机、平板、支持的 Curios/Trinkets/Accessories 等实际已接入饰品槽逻辑；`carried` 为服务端菜单光标物品，开箱时传 EMPTY。光标设备也应被认可。附属不自己枚举饰品接口。

不需要增加库存读写 API：附属直接使用 `ServerPlayer.getEnderChestInventory()`。不需要新增购买写入 API：沿用现有 MCphone 购买事务，在附属 ICost 成功后写回原 ID 的已购资格。需要确认原商店仍允许原 ID 的已购玩家免费重装，即使之前缺少附属导致图标已清理。

## 原子交接条件：由 MCphone 侧完成

当且仅当选定本候选附属作为提供方时，MCphone 必须同时：

1. 停用内建 EnderChestApp 的 SPI 有效注册（保留无附属环境的回退与否由 MCphone 决定）。
2. 不再为 `mcphone:ender_chest` 提供内建报价，让附属提供一个原版末影箱的 ICost。
3. 旧 `mcphone:open_ender_chest` 请求不得再执行开箱；可保留解码器用于拒绝旧请求，但不得有第二条有效开箱路径。
4. 保留原已购数据，不能因目录里暂缺 App 或内建版下线而清理它。
5. 在读安装列表、主屏位置及快捷键之前完成所有 SPI 扫描；不能依赖先进入世界才注册。
6. 两端发布一致的所有权判断与设备 API，并明确实际主模组版本号／提交。

附属只在三个公开方法齐全且所有权为自身时提供 App、报价和开箱网络处理；任一条件不满足，默认停用末影箱接管，其他九类 App 仍保留。旧主模组继续拥有其内建实现。没有调用内部移除／覆盖注册表的后门，也没有客户端已购兜底。

当前通过反射发现上述**提案公开类**，目的是旧主模组缺类时安全停用；不把 API stub 打进 JAR。契约测试的假实现只存在独立测试 source set，打包验证禁止混入主模组类。正式接口确认后可改为直接编译依赖。

## 附属侧行为

- 原 ID：`mcphone:ender_chest`；沿用名称翻译键与原图标路径，并随附原版资源副本（来自 MIT 许可 MCphone 1.12.0），避免内建资源下线后出现缺图。
- 服务端核对玩家存活、非旁观、严格已购、携带设备，然后打开绑定自身原版末影箱的三行 ChestMenu。保留原版 Shift、拖拽、光标及背包同屏操作；界面采用原版箱子布局，不复用主模组内部手机外壳类。
- 菜单持续核对同一玩家、已购及设备；将真实菜单光标物品传入设备 API。关闭菜单调用原版清理流程，不清空末影箱。
- 手机、平板、方块末影箱、跨维度访问同一份原版数据。无虚拟库存、隐藏购买进度或另存资格。
- 新玩家报价一个末影箱；旧已购不再次扣费；读取资格异常拒绝购买，防止误扣。
- 本附属自己的请求 ID 为 `mcphone_workshop:open_ender_chest`，包体为空，玩家只取网络上下文。

## 客户端与服务端版本组合

| 组合 | 结果 |
|---|---|
| MCphone 1.12.0 / 1.12.1 + 工坊 0.4.0-handoff.1 | 末影箱接管停用，由主模组内建版处理；不是迁移验收组合 |
| 当前已核对主线 f838993 + 候选附属 | 同上，尚无提案公开接口 |
| MCphone 配套交接构建（版本待提供，满足以上契约） + 工坊 0.4.0-handoff.1 | 待联调；两端必须用相同交接主模组和对应加载器附属 |

两种候选均使用 Java 21。NeoForge 构建基准 21.1.248；Fabric Loader 0.19.5、Fabric API 0.116.17+1.21.1。编译仍锁定 MCphone 1.12.0，未伪造一个尚不存在的主模组交接版本。

## 自动验证与用户验收

已执行的构建／测试结果以候选包构建报告及日志为准。契约测试覆盖无接口停用、非所有者停用、所有者查询、未购拒绝和查询异常失败；假接口测试不代表 MCphone 联调完成。用户负责游戏内验收。

配套主模组提供后，先交付真正的两目标试玩组合，首要检查：

1. 备份旧存档和客户端主屏配置；同步升级两端后，原位置、原快捷键能打开同一箱物品，不重新付费。
2. 新玩家用一个末影箱购买，只扣一次；伪造开箱请求无法绕过购买和设备条件。
3. 先升级主模组进档、后补装附属：旧资格仍可免费重装，允许自行重摆图标。
4. 手机、平板、实际支持的饰品槽、光标拖动设备都能保持使用；设备真正离身后关闭。
5. 方块箱／手机／平板和跨维度物品一致，两名玩家库存隔离，Shift／拖拽／退出均无复制或丢失。
6. 日志与注册表只有一方提供 App 和报价；旧主模组请求不能再开箱。

用户确认 1.21.1 旧档验收通过后，下一阶段补齐七目标，尤其新增 1.20.1 Fabric、1.21.11 Fabric、1.21.11 NeoForge。此次未修改主模组代码，也未提前标记七目标已支持。
