# 兼容层

沿用 MCphone 的两个轴：`version/` 放跨加载器的版本差异，`loader/` 放跨版本的加载器代码，
`both/` 放两者交集。每一层在 `versions/layers.json` 声明，由 `targets.json` 显式挂载。

声明格式：`{"1.21+": {"since": "1.21"}}` 或 `{"neoforge": {"loader": "NeoForge"}}`。
对应源码和资源位于 `layers/<分类>/<层名>/src/main/java` 与 `src/main/resources`。

当前只有 `loader/neoforge` 层，两个 NeoForge 目标共用入口和模组元数据。
出现真实共用代码时再建其他层，不能为了凑齐目录预先复制 MCphone 的全部兼容实现。
