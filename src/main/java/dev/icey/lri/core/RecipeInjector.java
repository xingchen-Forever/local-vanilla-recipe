package dev.icey.lri.core;

import dev.icey.lri.LocalRecipeInjectClient;
import dev.icey.lri.config.ConfigManager;
import dev.icey.lri.config.LriConfig;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.network.packet.s2c.play.RecipeBookAddS2CPacket;
import net.minecraft.recipe.RecipeDisplayEntry;

import java.util.ArrayList;
import java.util.List;

/**
 * 注入流程编排。由 {@code ClientPlayNetworkHandlerMixin} 在配方书整表同步后调用。
 *
 * <p>把本地配方合成一个 {@code replace=false} 的 {@link RecipeBookAddS2CPacket} 丢回
 * {@code handler.onRecipeBookAdd(...)}，复用 vanilla 完整下发路径——既填充配方书对象，
 * 又会触发任何挂在该处的 viewer（REI/JEI）逻辑。{@code replace=false} 不会触发本模组
 * mixin 的重入（它只在 {@code replace=true} 时动），也不会清空服务器已下发的条目。
 */
public final class RecipeInjector {

    private RecipeInjector() {
    }

    public static void injectIntoBook(ClientPlayNetworkHandler handler) {
        LriConfig cfg = ConfigManager.get();
        if (!cfg.forceLocalRecipes) {
            return;
        }
        // 此时玩家应已就绪（初始 replace 包已成功访问过配方书）；保险起见判空
        if (MinecraftClient.getInstance().player == null) {
            return;
        }

        long t0 = System.nanoTime();
        List<LocalRecipeLoader.RawRecipe> raws = LocalRecipeLoader.scanAll(cfg.includeModdedRecipes);
        ClientRecipeBridge.BuildResult built = ClientRecipeBridge.buildEntries(handler, raws, cfg);

        if (!built.entries().isEmpty()) {
            List<RecipeBookAddS2CPacket.Entry> packetEntries = new ArrayList<>(built.entries().size());
            for (RecipeDisplayEntry e : built.entries()) {
                // showNotification=false（不弹解锁提示）、highlighted=false
                packetEntries.add(new RecipeBookAddS2CPacket.Entry(e, false, false));
            }
            // replace=false：追加而非清空，且不会触发本模组 mixin 重入
            handler.onRecipeBookAdd(new RecipeBookAddS2CPacket(packetEntries, false));
        }

        if (cfg.logInjection) {
            double ms = (System.nanoTime() - t0) / 1_000_000.0;
            LocalRecipeInjectClient.LOGGER.info(
                    "[LRI] 注入配方书：扫描 {} 条，注入 {}，跳过特殊 {}，失败 {}（{} ms）",
                    raws.size(), built.entries().size(), built.skippedSpecial(), built.failed(),
                    String.format("%.1f", ms));
        }
    }
}
