package dev.icey.lri.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import dev.icey.lri.LocalRecipeInjectClient;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * 配置的加载/保存。单例持有当前配置。
 */
public final class ConfigManager {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path PATH =
            FabricLoader.getInstance().getConfigDir().resolve("localrecipeinject.json");

    private static LriConfig current = new LriConfig();

    private ConfigManager() {
    }

    public static LriConfig get() {
        return current;
    }

    /** 用一份新配置替换当前配置（配置界面“保存”时调用）。 */
    public static void set(LriConfig cfg) {
        current = cfg;
        save();
    }

    public static void load() {
        try {
            if (Files.exists(PATH)) {
                String json = Files.readString(PATH);
                LriConfig parsed = GSON.fromJson(json, LriConfig.class);
                if (parsed != null) {
                    current = parsed;
                }
            } else {
                save(); // 首次运行写出默认值
            }
        } catch (Exception e) {
            LocalRecipeInjectClient.LOGGER.warn("[LRI] 读取配置失败，使用默认值", e);
            current = new LriConfig();
        }
    }

    public static void save() {
        try {
            Files.createDirectories(PATH.getParent());
            Files.writeString(PATH, GSON.toJson(current));
        } catch (IOException e) {
            LocalRecipeInjectClient.LOGGER.warn("[LRI] 保存配置失败", e);
        }
    }
}
