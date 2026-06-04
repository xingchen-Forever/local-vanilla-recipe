package dev.icey.lri.core;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import dev.icey.lri.config.LriConfig;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.recipe.NetworkRecipeId;
import net.minecraft.recipe.Recipe;
import net.minecraft.recipe.RecipeDisplayEntry;
import net.minecraft.recipe.book.RecipeBookCategory;
import net.minecraft.recipe.display.RecipeDisplay;
import net.minecraft.registry.RegistryOps;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.OptionalInt;

/**
 * 把本地配方 JSON 解析成客户端配方书条目（对着 1.21.11 yarn 反编译确认过签名）。
 *
 * <p>1.21.2 重写后客户端不再持有完整 {@code Recipe} 集；REI/JEI 与配方书读取的是
 * {@link RecipeDisplayEntry}（展示导向：{@link RecipeDisplay} 描述输入/输出 + 分类）。
 * {@code RecipeManager} 接口在客户端只剩 propertySet/切石机，没有通用配方——
 * 所以注入目标是配方书条目。
 *
 * <p>本类只负责“解析 → 生成 {@link RecipeDisplayEntry} 列表”；真正下发由
 * {@link RecipeInjector} 合成 {@code RecipeBookAddS2CPacket} 走 vanilla 路径完成。
 */
public final class ClientRecipeBridge {

    /** 构建结果：可下发的条目 + 统计。 */
    public record BuildResult(List<RecipeDisplayEntry> entries, int skippedSpecial, int failed) {
    }

    private ClientRecipeBridge() {
    }

    public static BuildResult buildEntries(ClientPlayNetworkHandler handler,
                                           List<LocalRecipeLoader.RawRecipe> raws,
                                           LriConfig cfg) {
        // 客户端注册表已随连接同步；用它构造能解析物品 id 的 RegistryOps
        RegistryOps<JsonElement> ops = RegistryOps.of(JsonOps.INSTANCE, handler.getRegistryManager());

        List<RecipeDisplayEntry> entries = new ArrayList<>();
        int skippedSpecial = 0;
        int failed = 0;

        // 负数自减：与服务器下发的非负 NetworkRecipeId 不冲突；确定性 → 重注入覆盖而非累积
        int nextId = -1;

        for (LocalRecipeLoader.RawRecipe raw : raws) {
            final JsonElement el;
            final String type;
            try {
                el = JsonParser.parseString(raw.json());
                JsonObject obj = el.getAsJsonObject();
                type = obj.has("type") ? obj.get("type").getAsString() : "";
            } catch (Exception e) {
                failed++;
                continue;
            }

            if (cfg.skipSpecialRecipes && isSpecial(type)) {
                skippedSpecial++;
                continue;
            }

            Recipe<?> recipe = Recipe.CODEC.parse(ops, el).result().orElse(null);
            if (recipe == null) {
                failed++;
                continue;
            }

            try {
                RecipeBookCategory category = recipe.getRecipeBookCategory();
                List<RecipeDisplay> displays = recipe.getDisplays();
                for (RecipeDisplay display : displays) {
                    entries.add(new RecipeDisplayEntry(
                            new NetworkRecipeId(nextId--),
                            display,
                            OptionalInt.empty(),
                            category,
                            Optional.empty()));
                }
            } catch (Exception e) {
                failed++;
            }
        }

        return new BuildResult(entries, skippedSpecial, failed);
    }

    private static boolean isSpecial(String type) {
        return type.startsWith("minecraft:crafting_special_")
                || type.endsWith("_special")
                || type.contains(":special");
    }
}
