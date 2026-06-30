package io.codex.rpmcnbridge;

import com.magmaguy.resourcepackmanager.api.ResourcePackManagerAPI;
import java.io.File;
import java.util.Locale;
import java.util.logging.Level;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.server.PluginEnableEvent;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

public final class ResourcePackManagerCustomNameplatesBridge extends JavaPlugin implements Listener {
    // 这些默认值对应 CustomNameplates 官方 Bukkit 插件的插件名和资源包输出目录。
    // ResourcePackManager 的 API 要求 pluginName 必须和 /plugins 里显示的插件名完全一致，
    // 所以这里注册 CustomNameplates，而不是本桥接插件自己的名字。
    private static final String DEFAULT_PLUGIN_NAME = "CustomNameplates";
    private static final String DEFAULT_LOCAL_PATH = "CustomNameplates/ResourcePack";
    private static final String DEFAULT_RELOAD_COMMAND = "nameplates reload";

    private String pluginName;
    private String localPath;
    private boolean encrypts;
    private boolean distributes;
    private boolean zips;
    private String reloadCommand;
    private int retryTaskId = -1;
    private int attempts;
    private int maxAttempts;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        loadBridgeConfig();
        Bukkit.getPluginManager().registerEvents(this, this);
        getLogger().info("中文名：资源包管理器-自定义名牌桥接。正在等待 CustomNameplates 资源包输出。");
        startRegistrationAttempts(20L);
    }

    @Override
    public void onDisable() {
        cancelRetryTask();
    }

    @EventHandler
    public void onPluginEnable(PluginEnableEvent event) {
        String enabledName = event.getPlugin().getName();
        if (equalsPluginName(enabledName, pluginName) || equalsPluginName(enabledName, "ResourcePackManager")) {
            // 兼容插件热加载或启动顺序变化：任一依赖稍后启用时重新尝试注册。
            startRegistrationAttempts(20L);
        }
    }

    private void loadBridgeConfig() {
        reloadConfig();
        FileConfiguration config = getConfig();
        pluginName = config.getString("custom-nameplates-plugin-name", DEFAULT_PLUGIN_NAME);
        localPath = config.getString("resource-pack-local-path", DEFAULT_LOCAL_PATH);
        encrypts = config.getBoolean("encrypts", false);
        distributes = config.getBoolean("distributes", false);
        zips = config.getBoolean("zips", false);
        reloadCommand = config.getString("custom-nameplates-reload-command", DEFAULT_RELOAD_COMMAND);
        maxAttempts = Math.max(1, config.getInt("max-registration-attempts", 30));
    }

    private void startRegistrationAttempts(long delayTicks) {
        cancelRetryTask();
        attempts = 0;
        // CustomNameplates 启动时会生成 ResourcePack 目录；这里短间隔重试，
        // 避免目录还没写完时就向 ResourcePackManager 注册失败。
        retryTaskId = Bukkit.getScheduler().scheduleSyncRepeatingTask(this, this::tryRegister, delayTicks, 40L);
    }

    private void tryRegister() {
        attempts++;

        Plugin resourcePackManager = Bukkit.getPluginManager().getPlugin("ResourcePackManager");
        Plugin customNameplates = Bukkit.getPluginManager().getPlugin(pluginName);
        if (resourcePackManager == null || !resourcePackManager.isEnabled()) {
            retryOrGiveUp("ResourcePackManager is not enabled yet.");
            return;
        }
        if (customNameplates == null || !customNameplates.isEnabled()) {
            retryOrGiveUp(pluginName + " is not enabled yet.");
            return;
        }

        File packFolder = new File(Bukkit.getPluginsFolder(), normalizePath(localPath));
        File packMeta = new File(packFolder, "pack.mcmeta");
        if (!packMeta.isFile()) {
            retryOrGiveUp("Waiting for " + packMeta.getPath() + ".");
            return;
        }

        try {
            // zips=false 表示注册的是未压缩目录，让 ResourcePackManager 在合并前自行压缩。
            ResourcePackManagerAPI.registerLocalResourcePack(
                    pluginName,
                    normalizePath(localPath),
                    encrypts,
                    distributes,
                    zips,
                    reloadCommand == null || reloadCommand.isBlank() ? null : reloadCommand);
            cancelRetryTask();
            getLogger().info("已把 " + normalizePath(localPath) + " 注册给 ResourcePackManager，注册名：" + pluginName + "。");
        } catch (Throwable throwable) {
            getLogger().log(Level.WARNING, "Failed to register CustomNameplates resource pack with ResourcePackManager.", throwable);
            cancelRetryTask();
        }
    }

    private void retryOrGiveUp(String message) {
        if (attempts == 1 || attempts % 5 == 0) {
            getLogger().info(message + " Attempt " + attempts + "/" + maxAttempts + ".");
        }
        if (attempts >= maxAttempts) {
            getLogger().warning("Gave up registering CustomNameplates resource pack after " + attempts + " attempts.");
            cancelRetryTask();
        }
    }

    private void cancelRetryTask() {
        if (retryTaskId != -1) {
            Bukkit.getScheduler().cancelTask(retryTaskId);
            retryTaskId = -1;
        }
    }

    private static boolean equalsPluginName(String left, String right) {
        if (left == null || right == null) {
            return false;
        }
        return left.toLowerCase(Locale.ROOT).equals(right.toLowerCase(Locale.ROOT));
    }

    private static String normalizePath(String path) {
        // ResourcePackManager 的 localPath 是相对 plugins 目录的路径；统一用 / 便于跨平台阅读。
        return path.replace('\\', '/');
    }
}
