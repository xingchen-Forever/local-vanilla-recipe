package dev.icey.lri.core;

import dev.icey.lri.LocalRecipeInjectClient;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.ModContainer;
import net.minecraft.util.Identifier;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

/**
 * 从客户端本地（每个已加载 mod / 原版 jar 的 data/&lt;ns&gt;/recipe/*.json）扫描配方文件。
 *
 * <p>关键点：mod 既然装在客户端，它的配方 JSON 就在它自己的 jar 里，
 * 通过 {@link ModContainer#getRootPaths()} 直接读，<b>不依赖服务器下发</b>。
 * 这一步只用稳定的 Fabric / NIO API + {@link Identifier}，与具体 MC 内部结构无关。
 *
 * <p>注意：1.21 起配方目录是单数 {@code recipe}（旧版是 {@code recipes}）。
 */
public final class LocalRecipeLoader {

    /** 一条未解析的原始配方：id + 原始 JSON 文本 + 来源 mod。 */
    public record RawRecipe(Identifier id, String json, String sourceMod) {
    }

    private LocalRecipeLoader() {
    }

    public static List<RawRecipe> scanAll(boolean includeModded) {
        List<RawRecipe> out = new ArrayList<>();
        for (ModContainer mod : FabricLoader.getInstance().getAllMods()) {
            String modId = mod.getMetadata().getId();
            boolean isVanilla = modId.equals("minecraft");
            // 跳过本模组自身及非内容型加载器 mod 无所谓——它们没有 data/recipe 目录
            if (!includeModded && !isVanilla) {
                continue;
            }
            scanMod(mod, modId, out);
        }
        if (LocalRecipeInjectClient.LOGGER.isDebugEnabled()) {
            LocalRecipeInjectClient.LOGGER.debug("[LRI] 共扫描到 {} 条本地配方文件", out.size());
        }
        return out;
    }

    private static void scanMod(ModContainer mod, String modId, List<RawRecipe> out) {
        for (Path root : mod.getRootPaths()) {
            Path dataDir = root.resolve("data");
            if (!Files.isDirectory(dataDir)) {
                continue;
            }
            try (Stream<Path> namespaces = Files.list(dataDir)) {
                namespaces.filter(Files::isDirectory).forEach(nsDir -> scanNamespace(nsDir, modId, out));
            } catch (Exception e) {
                LocalRecipeInjectClient.LOGGER.warn("[LRI] 扫描 {} 的 data 目录失败", modId, e);
            }
        }
    }

    private static void scanNamespace(Path nsDir, String modId, List<RawRecipe> out) {
        String namespace = stripSlashes(nsDir.getFileName().toString());
        Path recipeDir = nsDir.resolve("recipe");
        if (!Files.isDirectory(recipeDir)) {
            return;
        }
        try (Stream<Path> files = Files.walk(recipeDir)) {
            files.filter(Files::isRegularFile)
                    .filter(p -> p.getFileName().toString().endsWith(".json"))
                    .forEach(p -> {
                        String rel = recipeDir.relativize(p).toString().replace('\\', '/');
                        String path = rel.substring(0, rel.length() - ".json".length());
                        try {
                            String json = Files.readString(p);
                            out.add(new RawRecipe(Identifier.of(namespace, path), json, modId));
                        } catch (Exception e) {
                            LocalRecipeInjectClient.LOGGER.warn("[LRI] 读取配方文件失败: {}", p, e);
                        }
                    });
        } catch (Exception e) {
            LocalRecipeInjectClient.LOGGER.warn("[LRI] 遍历 {} 的 recipe 目录失败", namespace, e);
        }
    }

    private static String stripSlashes(String s) {
        // jar 内 NIO 路径的文件名可能带尾部分隔符
        return s.replace("/", "").replace("\\", "");
    }
}
