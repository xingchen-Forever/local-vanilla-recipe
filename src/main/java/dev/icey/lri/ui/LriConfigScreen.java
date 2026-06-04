package dev.icey.lri.ui;

import dev.icey.lri.config.ConfigManager;
import dev.icey.lri.config.LriConfig;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.tooltip.Tooltip;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.CyclingButtonWidget;
import net.minecraft.text.Text;

import java.util.function.Consumer;

/**
 * 手写配置界面（零额外依赖，只用原版 GUI widget）。
 * 由 Mod Menu 的“配置”按钮打开；保存写回 ConfigManager，取消回滚到 parent。
 */
public class LriConfigScreen extends Screen {

    private final Screen parent;
    private final LriConfig draft;

    public LriConfigScreen(Screen parent) {
        super(Text.translatable("lri.config.title"));
        this.parent = parent;
        this.draft = ConfigManager.get().copy(); // 编辑副本，取消即丢弃
    }

    @Override
    protected void init() {
        int w = 240;
        int x = this.width / 2 - w / 2;
        int y = 40;
        final int gap = 24;

        addToggle(x, y, w, "lri.config.forceLocalRecipes",
                draft.forceLocalRecipes, v -> draft.forceLocalRecipes = v);
        y += gap;
        addToggle(x, y, w, "lri.config.includeModded",
                draft.includeModdedRecipes, v -> draft.includeModdedRecipes = v);
        y += gap;
        addToggle(x, y, w, "lri.config.markConflicts",
                draft.markConflicts, v -> draft.markConflicts = v);
        y += gap;
        addToggle(x, y, w, "lri.config.skipSpecial",
                draft.skipSpecialRecipes, v -> draft.skipSpecialRecipes = v);
        y += gap;
        addToggle(x, y, w, "lri.config.logInjection",
                draft.logInjection, v -> draft.logInjection = v);

        int by = this.height - 32;
        int half = w / 2 - 4;
        addDrawableChild(ButtonWidget.builder(Text.translatable("lri.config.save"), b -> {
            ConfigManager.set(draft);
            close();
        }).dimensions(x, by, half, 20).build());

        addDrawableChild(ButtonWidget.builder(Text.translatable("lri.config.cancel"), b -> close())
                .dimensions(x + half + 8, by, half, 20).build());
    }

    private void addToggle(int x, int y, int w, String key, boolean initial, Consumer<Boolean> setter) {
        CyclingButtonWidget<Boolean> btn = CyclingButtonWidget.onOffBuilder(initial)
                .tooltip(v -> Tooltip.of(Text.translatable(key + ".tip")))
                .build(x, y, w, 20, Text.translatable(key), (button, value) -> setter.accept(value));
        addDrawableChild(btn);
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        super.render(context, mouseX, mouseY, delta);
        context.drawCenteredTextWithShadow(this.textRenderer, this.title, this.width / 2, 18, 0xFFFFFF);
    }

    @Override
    public void close() {
        this.client.setScreen(parent);
    }
}
