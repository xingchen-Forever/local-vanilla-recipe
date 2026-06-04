package dev.icey.lri.compat;

import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;
import dev.icey.lri.ui.LriConfigScreen;

/**
 * Mod Menu 入口：在模组菜单里给本模组挂一个“配置”按钮，点开 -> LriConfigScreen。
 * 由 fabric.mod.json 的 "modmenu" entrypoint 注册。
 */
public class ModMenuIntegration implements ModMenuApi {

    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory() {
        return LriConfigScreen::new;
    }
}
