# 深渊侵蚀者 / Abyssal Corrupter

## 环境需求

- Minecraft: `1.21.1`
- NeoForge: `21.1.65` 或兼容版本
- Java: `21`
- GeckoLib: `4.6.6`

## 游戏内使用

创造模式物品栏中包含：

- 深渊召唤祭坛
- 深渊侵蚀者刷怪蛋

推荐正式体验使用“深渊召唤祭坛”。刷怪蛋主要用于创造模式测试。

常用测试命令需要管理员权限，并且通常用于创造模式调试：

```mcfunction
/summon abyssal_corrupter:abyssal_corrupter ~ ~ ~
/abyssal_corrupter test list
/abyssal_corrupter test state <state> <variant>
/abyssal_corrupter test cooldowns
/abyssal_corrupter test stop
```

## 配置

模组注册了 common/client 配置。可调项目包括：

- Boss 血量倍率
- Boss 伤害倍率
- 技能冷却倍率
- 二阶段冷却倍率
- 是否允许地形改变
- 粒子质量：`OFF` / `LOW` / `NORMAL` / `HIGH`
- 屏幕震动开关与强度
- 祭坛召唤时间
- 祭坛冷却
- 祭坛是否消耗材料
- 祭坛半径
- Boss 入场无敌时间

如果服务器觉得 Boss 太难、太晃或粒子太多，优先改配置，不需要重新打包。

## 开发入口

主要代码位置：

- `src/main/java/com/eddy1/tidesourcer/TideSourcerMod.java`
- `src/main/java/com/eddy1/tidesourcer/entity/custom/TideSourcerEntity.java`
- `src/main/java/com/eddy1/tidesourcer/entity/ai/SunkenTitanCombatGoal.java`
- `src/main/java/com/eddy1/tidesourcer/entity/ai/SunkenTitanCombatManager.java`
- `src/main/java/com/eddy1/tidesourcer/block/AbyssalSummoningAltarBlock.java`
- `src/main/java/com/eddy1/tidesourcer/block/entity/AbyssalSummoningAltarBlockEntity.java`
- `src/main/java/com/eddy1/tidesourcer/world/AbyssalRitualSite.java`
- `src/main/java/com/eddy1/tidesourcer/command/TideSourcerCommands.java`

主要资源位置：

- `src/main/resources/assets/abyssal_corrupter/geo`
- `src/main/resources/assets/abyssal_corrupter/animations`
- `src/main/resources/assets/abyssal_corrupter/textures`
- `src/main/resources/assets/abyssal_corrupter/lang`
- `src/main/resources/META-INF`

## 协议与第三方资源

本项目包含 EdDYON 的原创代码、玩法设计、UI、文本和部分资源，也包含第三方模型资源。两类内容的协议需要分开看：

- 根目录协议：`LICENSE`
- 打包进 jar 的代码/原创内容协议：`META-INF/LICENSE.txt`
- 打包进 jar 的第三方模型协议：`META-INF/THIRD_PARTY_ASSET_NOTICES.txt`

发布 jar 时请保留 `META-INF/LICENSE.txt` 和 `META-INF/THIRD_PARTY_ASSET_NOTICES.txt`。它们会随 `src/main/resources` 一起被 Gradle 打进最终 jar。

