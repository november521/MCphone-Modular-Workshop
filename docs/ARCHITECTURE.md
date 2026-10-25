# MCphone: Modular Workshop 架构

状态：已建立四目标空壳和统一构建入口；工作方块玩法尚未实现。
2026-10-25：四目标本地构建、12 项构建机制测试和工作流配置校验通过。

## 1. 目标与基准

同一分支维护所有平台，同一次改动、同一个附属版本号，一次构建产出与 MCphone 正式版
对应的全部 JAR。附属自身独立发版，不必与 MCphone 使用相同版本号。

本轮基准是 [MCphone v1.11.2](https://github.com/november521/mcphone/releases/tag/v1.11.2)，
提交 `67640ead07bb0bb33c3dee2a1ed7b3a02b34e97d`。对齐的是正式发布目标，
不是把主模组清单中尚未实现的五个组合也标成支持。

| 目标 | Java | Gradle | 编译加载器 |
|---|---|---|---|
| 1.20.1 Forge | 17 | 8.8 | 47.4.23 |
| 1.21.1 Fabric | 21 | 9.7.1 | 0.19.5 |
| 1.21.1 NeoForge | 21 | 9.2.1 | 21.1.248 |
| 26.1.2 NeoForge | 25 | 9.2.1 | 26.1.2.100 |

## 2. 文件结构与依赖方向

```text
gradle.properties                 唯一的附属身份、版本号和许可
versions/
  targets.json                    附属目标清单、工具链和层挂载
  layers.json                     共享层的版本／加载器约束
  upstream.lock.json              MCphone 正式版、提交、目标快照及 JAR 校验值
shared/src/main/
  java/com/november/mcphone/workshop/
    ModularWorkshop.java          两端都能加载的公共初始化
  resources/                      未来跨版本通用的语言、贴图等
layers/
  loader/neoforge/src/main/        两个 NeoForge 目标共用的入口和元数据
  version/<API边界>/src/main/      按需加入的版本兼容代码／资源
  both/<交集名>/src/main/          按需加入的加载器与版本交集
platforms/<目标>/
  build.gradle                    平台插件与依赖绑定
  settings.gradle
  gradle.properties               仅工程设置，不重复定义模组身份
  gradlew / gradlew.bat
  gradle/wrapper/                  该目标自己的 Gradle
  src/main/                       仅这个目标需要的实现／元数据
gradle/                           共用构建设置
tools/build.py                    本地与 CI 共用的构建、校验及汇总入口
tests/                            目标漂移、错误包、构建失败等关键行为测试
.github/workflows/build.yml       一次触发，矩阵编译，完整汇总
dist/                             已校验产物，不提交版本库
```

平台入口调用 `shared` 公共逻辑。公共逻辑依赖稳定接口，版本／加载器差异由层和平台实现绑定。
`shared` 不直接依赖加载器 API；附属对 MCphone 的调用只进入 `com.november.mcphone.api`，
不继承或导入主模组的 `core`、`feature`、`platform` 内部实现。

四个平台是独立 Gradle 工程，通过共同源码目录复用代码，不把互不兼容的构建插件塞进同一个
Gradle 多项目进程。所有已混淆目标使用 Mojang official mappings；26.1.2 沿用官方名称。
共享源码保持 Java 17 语法，各目标仍用自己的 Java 工具链编译。

借鉴 MCphone 的结构，不复制主模组的聊天、脚本引擎、音乐解码器及可选联动依赖。
新增兼容层必须有实际使用者；没有对应代码时不预建整套版本层。

## 3. 后续功能按模块组织

以下是后续代码的归属约定，不表示这些功能已实现：

| 包 | 职责 |
|---|---|
| `core/module` | 模块 ID、定义、目录和能力边界；不引用客户端类型 |
| `core/server` | 服务器操作入口、安装／使用资格检查、状态保存 |
| `core/network` | 共用请求与结果模型；传输注册由平台适配 |
| `core/platform` | 需要跨版本适配的最小接口 |
| `integration/mcphone/client` | MCphone SPI 的 App 注册与页面入口 |
| `feature/<工作站>/client` | 该工作站的手机页面 |
| `feature/<工作站>/server` | 加工、消耗、产物及原版机制接入 |

每个工作站作为一个可独立安装的 App 模块，ID 使用附属命名空间，例如
`mcphone_workshop:crafting`、`mcphone_workshop:furnace`。通过 MCphone 的
`IPhoneApp` / `IPhonePage` 接入；默认不预装，由应用商店安装。优先复用 MCphone
现有安装／卸载和页面生命周期，避免再造一套客户端 App 管理器。

客户端 App 和页面仅放在 `client` 包中，公共入口、网络包、模块定义不能引用它们。
SPI 服务提供者只在 MCphone 的客户端发现流程中加载，专用服务器不加载页面实现。

涉及物品、经验、燃料、加工进度的操作由服务端判定。客户端只发送意图，服务端根据当前
玩家、会话、安装资格和库存重新校验。不能把客户端“装了图标”视为能生成物品的授权。
原版容器／配方逻辑能复用时优先复用，特殊方块逐个定义适配器，不建立一个可以任意调用
所有方块的通用执行入口。

安装是否需要消耗物品、模块绑定玩家还是某部手机、熔炉离线加工与卸载时产物如何处理，
属于下一轮玩法设计。状态访问通过服务接口隔离，在确定这些规则前不写死存储结构。

## 4. 一个入口完成四目标构建

要求 Python 3.11+、Git，以及清单中需要的 Java 17、21、25。
不需要全局安装 Gradle。通过 `JAVA_HOME_17_X64`、`JAVA_HOME_21_X64`、
`JAVA_HOME_25_X64` 指定 JDK 根目录，或使用等价的 `JAVA_17_HOME` 等变量。
脚本会实际查询 Java 版本，不把随手找到的默认 Java 当成对应版本。

在仓库根运行：

```sh
python tools/build.py build
```

执行顺序：

1. 检查目标与锁定的 MCphone 发布目标完全一致，验证 Wrapper、共享层和根身份配置。
2. 检查本次所需的全部 JDK，下载并验证对应的 MCphone 官方 JAR。
3. 将依赖放进忽略提交的本地 Maven 缓存；由各加载器处理映射，MCphone 不嵌入附属包。
4. 依次调用每个平台自己的 Wrapper。某目标失败仍继续检查其余目标，最终返回失败。
5. 按确切文件名校验 JAR、元数据、MCphone 硬依赖、字节码版本与许可证。
6. 全部成功后，在 `dist/<附属版本>/<本次构建>/` 输出四个 JAR、`SHA256SUMS`
   和 `build-report.json`。报告记录源码提交、是否有未提交修改及主模组锁定信息。

完整构建使用新的临时目录收集产物，失败时不发布本轮目录，也不会拿先前构建的 JAR 补数。
每次成功构建保留独立目录，不能将旧目录误认为本次成功；以命令返回值和本次输出路径为准。

本地调试单个目标：

```sh
python tools/build.py prepare --target 1.21.1-neoforge
cd platforms/1.21.1-neoforge
./gradlew runClient
```

Windows 使用 `gradlew.bat runClient`。直接启动某个平台时也需使用与该目标匹配的 Java。

## 5. CI 与发布边界

推送、PR 或手动触发一次工作流，先跑架构检查和构建脚本测试，再从 `targets.json`
生成矩阵。每个目标单独选择 Java 并构建，`fail-fast: false` 保留所有目标的失败信息。
汇总任务仅在所有目标成功后运行，重新校验四个 JAR 后上传完整构建包。
矩阵及失败策略参考 [GitHub 官方文档](https://docs.github.com/en/actions/how-tos/write-workflows/choose-what-workflows-do/run-job-variations)。

本轮只产出构建附件，不创建正式 Release，也不自动向 Modrinth／CurseForge 发布。
后续发布工作流必须复用同一完整汇总校验，不能临时改成“哪个目标成功就发哪个”。

## 6. 主模组升级与目标变化

普通构建读取已提交的锁文件，不追踪浮动 `latest`，确保同一次构建的全部目标使用同一个
MCphone 正式版。下面的命令单独检查最新正式版是否发生变化：

```sh
python tools/build.py check-upstream
```

升级时先查看对应正式版的 `versions/targets.json` 与发布资产，更新目标快照、提交和校验值，
再补齐变化的平台或兼容层。`check` 拦住相对锁文件的遗漏；`check-upstream` 拦住相对
最新正式版的遗漏和版本漂移。新增一个目标需要确实能构建，不能仅修改锁文件冒充支持。

MCphone 新版本发布不会自动改写附属源码；需要在升级变更中完成适配、四目标构建和运行验证。

## 7. 验收层次

- 架构检查和脚本测试证明清单、锁和汇总机制按规则工作。
- 四目标实际构建证明当前源码能在对应工具链上编译、转换映射和打包。
- 客户端／专用服务器启动、安装卸载、制作和加工操作需要另行运行验证。

“编译成功”不等于工作方块功能或多人安全已验收。首个功能实现后，应在四个目标上加入
实际安装与操作测试，再决定首个可玩版本的发布。

## 参考

- [MCphone v1.11.2 目录与清单](https://github.com/november521/mcphone/tree/v1.11.2)
- [MCphone 附属接口](https://github.com/november521/mcphone/wiki/Addon-API)
- [NeoForge ModDevGradle](https://docs.neoforged.net/toolchain/docs/plugins/mdg/)
- [Fabric Loom 依赖和映射](https://docs.fabricmc.net/develop/loom/options)
- [Forge 1.20.1 工程结构](https://docs.minecraftforge.net/en/1.20.1/gettingstarted/structuring/)
