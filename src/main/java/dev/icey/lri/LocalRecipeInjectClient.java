package dev.icey.lri;

import dev.icey.lri.config.ConfigManager;
import net.fabricmc.api.ClientModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 客户端入口：加载配置。
 * 实际注入由 {@code ClientPlayNetworkHandlerMixin} 在配方书整表同步后触发，
 * 这样能精确接在服务器初始下发之后，不会被清空。
 */
public class LocalRecipeInjectClient implements ClientModInitializer {

    public static final String MOD_ID = "localrecipeinject";
    public static final Logger LOGGER = LoggerFactory.getLogger("LocalRecipeInject");

    @Override
    public void onInitializeClient() {
        ConfigManager.load();
        LOGGER.info("[LRI] 已加载（强制本地配方={}）", ConfigManager.get().forceLocalRecipes);
    }
}
