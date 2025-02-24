# UHC Checker (UHCC)
一个适用于 **Minecraft 1.8.9 Forge** 的用于查询 **Hypixel UHC** 统计数据的 mod。

## **功能**
- **玩家统计数据显示**：在 **Chat、Tab列表和 Nametag** 显示 **UHC 星数、KDR、胜场** 等数据。
- **自动检测玩家**：解析聊天消息和 Tab 列表，自动追踪对手。
- **支持 Hypixel API**：使用 **Hypixel API** 获取实时数据。
- **可自定义 Overlay**：可以自由移动和调整 Overlay 的位置和大小。

## **安装方法**
1. **安装 Minecraft Forge 1.8.9**。
2. 在 [**Releases**](https://github.com/daoheautumn/uhcc/releases) 下载最新版本。
3. 将下载的 `.jar` 文件放入 **mods** 文件夹（路径：`.minecraft/mods`）。
4. 获取 **Hypixel API Key**：[**点此申请**](https://developer.hypixel.net/)。
5. 启动 Minecraft，并使用 `/uc setapi <你的 API Key>` 设置 API。

## **指令列表**
| 指令 | 功能描述 |
|------|----------|
| `/uc start` | 启用 Mod，开始检测。 |
| `/uc stop` | 关闭 Mod，并清空统计数据。 |
| `/uc setapi <API_KEY>` | 设置 Hypixel API Key。 |
| `/uc c <玩家名>` | 手动查询指定玩家的 UHC 统计数据。 |
| `/uc toggle tab` | 切换 Tab 列表中的数据显示。 |
| `/uc toggle overlay` | 切换聊天中的 Overlay。 |
| `/uc toggle nametag` | 切换玩家头顶的 UHC 数据显示。 |
| `/uc xpos <数值>` | 左右移动 Overlay。 |
| `/uc ypos <数值>` | 上下移动 Overlay。 |
| `/uc size <数值>` | 调整 Overlay 大小（范围：`0.01 - 1.0`）。 |
| `/uc resetpos` | 重置 Overlay 位置。 |
| `/uc overlaymaxid <数值>` | 设置 Overlay 每列显示的最大玩家数。 |
| `/uc nametagheight <数值>` | 调整 Nametag 统计数据的高度（默认 `0.7`，可正可负）。 |

## **许可证**
本项目遵循 [MPL-2.0 开源协议](https://github.com/daoheautumn/UHCC/blob/main/LICENSE)。
