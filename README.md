# 深渊侵蚀者 / Abyssal Corrupter

一个单 Boss 模组，内容基本都围着深渊侵蚀者本体展开。

现在外部命名已经统一成：
- `modid`: `abyssal_corrupter`
- 实体 ID: `abyssal_corrupter:abyssal_corrupter`
- 测试命令: `/abyssal_corrupter`

代码里的类名还保留 `TideSourcer*` 这套旧前缀，只是历史命名，不影响运行。

## 环境

- Minecraft `1.21.1`
- NeoForge `21.1.65`
- Java `21`
- GeckoLib `4.6.6`

常用命令：

```powershell
.\gradlew.bat runClient
.\gradlew.bat build
.\gradlew.bat --no-configuration-cache compileJava
```

## 快速测试

先开创造模式，并且保证自己有命令权限。

```mcfunction
/summon abyssal_corrupter:abyssal_corrupter ~ ~ ~
/abyssal_corrupter test list
/abyssal_corrupter test ray
/abyssal_corrupter test domain
/abyssal_corrupter test state 9 1
/abyssal_corrupter test stop
```

常用子命令：
- `list` 看技能别名
- `state <state> <variant>` 精确点招
- `cooldowns` 清冷却
- `stop` 退出手动测试模式

## 主要文件

- `src/main/java/com/eddy1/TideSourcer/TideSourcerMod.java`
  模组入口。
- `src/main/java/com/eddy1/TideSourcer/entity/custom/TideSourcerEntity.java`
  Boss 本体，血条、阶段、状态、动画和死亡特效都在这里。
- `src/main/java/com/eddy1/TideSourcer/entity/ai/SunkenTitanCombatGoal.java`
  AI 选招逻辑。
- `src/main/java/com/eddy1/TideSourcer/entity/ai/SunkenTitanCombatManager.java`
  技能调度和冷却推进。
- `src/main/java/com/eddy1/TideSourcer/command/TideSourcerCommands.java`
  测试命令。
- `src/main/resources/assets/abyssal_corrupter`
  动画、模型、贴图、语言文件。

## 想改东西

想改数值：
- 看 `TideSourcerEntity.createAttributes()`
- 看各技能模块里的伤害和冷却

想改动画：
- 先看 `assets/abyssal_corrupter/animations/abyssal_corrupter.animation.json`
- 再看 `TideSourcerEntity.registerControllers(...)`
- 最后看 `TideSourcerEntity.triggerAttackAnimation(...)`

想改台词：
- `SunkenTitanSpeechManager.java`
- `assets/abyssal_corrupter/lang/en_us.json`
- `assets/abyssal_corrupter/lang/zh_cn.json`
- `assets/abyssal_corrupter/lang/zh_tw.json`

想改血条：
- 先看 `TideSourcerEntity` 里的 `bossEvent`

## 备注

可能不是最终版本

## 授权与第三方资源

- 项目原创内容的归属和适用范围见 [LICENSE](LICENSE)
- 打包进模组 jar 的授权文件位于 `META-INF/LICENSE.txt`
- 打包进模组 jar 的第三方模型协议位于 `META-INF/THIRD_PARTY_ASSET_NOTICES.txt`
