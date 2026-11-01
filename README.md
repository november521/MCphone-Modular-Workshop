# MCphone: Modular Workshop

**MCphone：模块工坊**

MCphone 的附属模组，将原版工作方块的功能做成可安装到 MCphone 中的模块。

## 项目目标

把工作台、熔炉、切石机等原版工作方块的功能带进手机，让玩家通过安装模块使用相应功能。

## 开发状态

已实现工作台 App：在 MCphone 应用商店安装“工作台”，点击后打开独立的原版工作台界面。
使用原版九宫格、配方书、背包、Shift 操作、合成与关闭返还逻辑。

安装需消耗 **1 个原版工作台**，购买资格按玩家保存；卸载后重新安装不重复收费。使用时需在背包或副手携带 MCphone 手机或平板；取走设备后工作台会关闭。
当前未接入 Curios 等外部装备栏的设备检测，也未增加永久储存格。
客户端和服务端均须安装本附属及 **MCphone ≥ 1.10.2**，并选择对应的 Minecraft 和加载器版本。

`0.1.0-alpha.3` 起不再限定单个 MCphone 版本。编译依赖仍锁定 1.12.0，以便复现构建；
运行时允许 1.10.2 及以后版本。已核对 1.10.2 和 1.12.0 的所用公开接口，游戏内验收仍需进行。
MCphone 1.10.2 尚无 26.1.2 发布包，该目标需使用后来提供的对应平台包。
升级时删除旧附属 JAR，换入对应平台的新 JAR；联机两端同步替换。购买记录沿用原有标识。

客户端与专用服务器的完整游戏内验收仍需进行，详见 [工作台验收清单](docs/CRAFTING.md)。

| Minecraft | 加载器 | Java |
|---|---|---|
| 1.20.1 | Forge | 17 |
| 1.21.1 | Fabric / NeoForge | 21 |
| 26.1.2 | NeoForge | 25 |

## 一次构建所有版本

准备 Python 3.11+、Git 和 Java 17 / 21 / 25，通过 `JAVA_HOME_17_X64`、
`JAVA_HOME_21_X64`、`JAVA_HOME_25_X64` 分别指定 JDK 根目录，在仓库根运行：

```sh
python tools/build.py build
```

四个目标全部成功后，产物、SHA-256 校验值和构建报告统一输出到 `dist/` 下的本次构建目录。
GitHub Actions 同样一次触发构建全部目标，上传完整构建包。

详见 [项目架构](docs/ARCHITECTURE.md)，其中包含目录职责、模块划分、主模组版本对齐和升级规则。

```sh
python tools/build.py check             # 离线校验架构与版本清单
python tools/build.py check-upstream    # 联网核对最新 MCphone 正式版
python -m unittest discover -s tests -v # 构建机制测试
```

## 主模组

[MCphone](https://github.com/november521/mcphone)

## 许可证

本项目采用 [MIT 许可证](LICENSE)。
