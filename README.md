# 资源包管理器-自定义名牌桥接

中文名：资源包管理器-自定义名牌桥接  
英文插件名：`ResourcePackManagerCustomNameplatesBridge`

这是一个很小的 Bukkit/Paper/Youer 桥接插件，用来让 [ResourcePackManager](https://wiki.nightbreak.io/zh-Hans/ResourcePackManager/api) 识别 [CustomNameplates](https://github.com/Xiao-MoMi/CustomNameplates) 生成的资源包目录，并把自定义名牌的数据一起合并进最终发给客户端的服务器资源包。

## 功能

- 开服后自动等待 `CustomNameplates` 和 `ResourcePackManager` 启用。
- 检查 `plugins/CustomNameplates/ResourcePack/pack.mcmeta` 是否已经生成。
- 使用 ResourcePackManager 官方 API 注册本地资源包：
  - `pluginName = CustomNameplates`
  - `localPath = CustomNameplates/ResourcePack`
  - `zips = false`
- 让 ResourcePackManager 后续自动压缩、合并和发送资源包。
- 通过 `loadbefore: ResourcePackManager` 抢在 ResourcePackManager 首次合并前完成注册，避免启动时出现两轮资源包合并互相抢 `output` 目录。
- 不修改 `ResourcePackManager` 或 `CustomNameplates` 原 jar，删除本桥接插件即可撤销联动。

## 为什么需要它

`ResourcePackManager/config.yml` 里的 `priorityOrder` 只决定合并优先级，不会自动发现 CustomNameplates 的资源包目录。  
ResourcePackManager 官方 API 要求其他插件在运行时调用 `registerLocalResourcePack(...)` 注册资源包。本插件就是专门补上这一步，并且尽量在 ResourcePackManager 首次打包前完成注册。

## 安装

1. 安装并启用 `ResourcePackManager`。
2. 安装并启用 `CustomNameplates`。
3. 把本插件 jar 放入服务器 `plugins` 目录。
4. 确认 `plugins/ResourcePackManager/config.yml` 的 `priorityOrder` 中包含：

```yaml
priorityOrder:
- ResourcePackManager
- EliteMobs
- FreeMinecraftModels
- ModelEngine
- CustomNameplates
```

5. 重启服务器。

正常日志会出现类似内容：

```text
[资源包管理器-自定义名牌桥接]: 已把 CustomNameplates/ResourcePack 注册给 ResourcePackManager，注册名：CustomNameplates。
```

ResourcePackManager 的 `mixer` 目录中会生成：

```text
plugins/ResourcePackManager/mixer/CustomNameplates_resource_pack.zip
```

最终整合包中应包含：

```text
assets/nameplates/...
```

## 配置

默认配置位于：

```text
plugins/ResourcePackManagerCustomNameplatesBridge/config.yml
```

默认内容：

```yaml
custom-nameplates-plugin-name: CustomNameplates
resource-pack-local-path: CustomNameplates/ResourcePack
encrypts: false
distributes: false
zips: false
custom-nameplates-reload-command: nameplates reload
max-registration-attempts: 30
```

参数说明：

- `custom-nameplates-plugin-name`：注册给 ResourcePackManager 的插件名，必须与服务器插件列表中的 `CustomNameplates` 完全一致。
- `resource-pack-local-path`：资源包路径，相对 `plugins` 目录填写。
- `zips`：`false` 表示路径是未压缩资源包文件夹，由 ResourcePackManager 自行压缩。
- `max-registration-attempts`：启动后最多尝试注册次数，每 2 秒尝试一次。

## 构建

需要 Java 21，以及以下编译依赖：

- Bukkit/Paper API jar，例如 Youer 服务器里的 `libraries/com/mohistmc/installation/data/paper-remap.jar`
- `ResourcePackManager` 插件 jar

本仓库提供 PowerShell 构建脚本：

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\build.ps1 `
  -ServerDir 'H:\mcziyou-1.21.1-Server'
```

构建产物默认输出到：

```text
build/jar/ResourcePackManagerCustomNameplatesBridge-1.0.2[资源包管理器-自定义名牌桥接].jar
```

## 实现说明

核心调用如下：

```java
ResourcePackManagerAPI.registerLocalResourcePack(
    "CustomNameplates",
    "CustomNameplates/ResourcePack",
    false,
    false,
    false,
    "nameplates reload"
);
```

插件只负责注册，不主动并发触发 ResourcePackManager 的合并任务。

1.0.2 起，本插件会在 Bukkit 加载顺序上排到 ResourcePackManager 前面，并通过 `PluginEnableEvent` 在 ResourcePackManager 刚启用时立即注册 CustomNameplates，避免注册太晚造成 ResourcePackManager 启动期两次混合任务重叠。

## 适用环境

- Minecraft 1.21.x Bukkit/Paper/Youer 系服务端
- Java 21
- ResourcePackManager 1.7.3 测试通过
- CustomNameplates Bukkit 3.0.40 测试通过

## 许可证

MIT
