# UHCCMod

适用于 Minecraft 1.8.9 的 Forge Mod，用于查询 Hypixel UHC 统计数据。

[English version](https://github.com/daoheautumn/uhcc/blob/main/README_EN.md)

## 功能

- **玩家统计显示**：在 **聊天、Tab 列表、nametag 和 HUD Overlay** 中显示 UHC 统计数据（星级、KDR、胜场）。
- **自动玩家检测**：解析聊天消息和 Tab 列表，自动追踪对手。
- **Hypixel API 支持**：实时获取玩家数据。
- **可定制覆盖层**：自由调整 **大小、位置、显示内容**。

## 指令

| 指令 | 描述 |
|------|------|
| `/uc start` | 启用 Mod。 |
| `/uc stop` | 停止 Mod 并清除统计数据。 |
| `/uc setapi <API_KEY>` | 设置 Hypixel API Key。 |
| `/uc c <player>` | 查询指定玩家的 UHC 统计数据。 |
| `/uc toggle tab` | 切换 **Tab 列表** 统计显示。 |
| `/uc toggle overlay` | 切换 **HUD 覆盖层** 统计显示。 |
| `/uc toggle nametag` | 切换 **玩家 Nametag 统计数据**（在头顶显示 UHC 数据）。 |
| `/uc xpos <value>` | **调整覆盖层 X 轴位置**。 |
| `/uc ypos <value>` | **调整覆盖层 Y 轴位置**。 |
| `/uc size <value>` | **调整覆盖层大小**（0.01 - 1.0）。 |
| `/uc resetpos` | **重置覆盖层位置**。 |
| `/uc overlaymaxid <value>` | **设置覆盖层每列最大玩家数**。 |
| `/uc nametagheight <value>` | **调整 Nametag 统计数据的高度**（默认 `0.7`，可以正负调整）。 |

## 安装

1. 安装 **Minecraft Forge 1.8.9**。  
2. 从 [GitHub Releases](https://github.com/daoheautumn/uhcc/releases) 下载 `.jar` 文件。  
3. 将 `.jar` 放入 `.minecraft/mods/` 目录。  
4. 启动游戏，输入 `/uc setapi <你的API Key>` 进行配置。  

## 注意事项

- **需要 Hypixel API Key**，[Hypixel developer Dashboard](https://developer.hypixel.net/)。  
