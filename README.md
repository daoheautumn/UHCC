# UHC Checker (UHCCMod)  
A Minecraft 1.8.9 Forge Mod for checking Hypixel UHC stats.

[中文版](https://github.com/daoheautumn/uhcc/blob/main/README_ZH.md).  

##  Features  
- **Player Stats Display**: Show UHC stats (stars, KDR, wins) in **chat, Tab list, and overlay**.  
- **Automatic Player Detection**: Parses chat messages and Tab list to track opponents.  
- **Hypixel API Support**: Fetches real-time stats using the **Hypixel API**.  
- **Customizable Overlay**: Move and resize the stats overlay.  

##  Commands  
| Command | Description |
|---------|-------------|
| `/uc` | Toggle the mod on/off. |
| `/uc stop` | Stop the mod and clear stats. |
| `/uc setapi <API_KEY>` | Set your Hypixel API key. |
| `/uc c <player>` | Manually check a player’s UHC stats. |
| `/uc toggle tab` | Toggle stats display in the Tab list. |
| `/uc toggle overlay` | Toggle stats overlay in the chat. |
| `/uc xpos <value>` | Move overlay horizontally. |
| `/uc ypos <value>` | Move overlay vertically. |
| `/uc size <value>` | Adjust overlay size (0.01 - 1.0). |
| `/uc resetpos` | Reset overlay position. |
| `/uc overlaymaxid <value>` | Set max players per column in overlay. |

##  Installation  
1. **Install Minecraft Forge 1.8.9**.  
2. Download the latest release from [GitHub Releases](https://github.com/daoheautumn/uhcc/releases).  
3. Put the `.jar` file in your **mods** folder (`.minecraft/mods`).  
4. Launch Minecraft and set your API key using `/uc setapi <your_api_key>`.  

##  Notes  
- This mod requires a **Hypixel API key** (`/api new` in Hypixel chat).  

##  License  
This project is licensed under the **MPL-2.0**.  
You are free to **modify and redistribute** as long as changes remain open-source.  
