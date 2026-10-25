# MCphone: Modular Workshop

**MCphone：模块工坊**

MCphone 的附属模组，将原版工作方块的功能做成可安装到 MCphone 中的模块。

## 项目目标

把工作台、熔炉、切石机等原版工作方块的功能带进手机，让玩家通过安装模块使用相应功能。

## 开发状态

已建立与 MCphone v1.11.2 对齐的四目标工程和统一构建入口，目前尚未实现工作方块玩法。
四个目标均已通过本地构建验证；客户端和专用服务器启动尚未验收。

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
