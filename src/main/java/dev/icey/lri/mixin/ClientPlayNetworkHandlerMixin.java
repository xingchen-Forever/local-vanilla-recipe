package dev.icey.lri.mixin;

import dev.icey.lri.config.ConfigManager;
import dev.icey.lri.core.RecipeInjector;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.network.packet.s2c.play.RecipeBookAddS2CPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 在客户端处理“配方书整表替换”（进世界初始同步）之后，补注入本地配方。
 *
 * <p>为什么挂在这里：服务器进世界时会发一个 {@code replace=true} 的 {@link RecipeBookAddS2CPacket}
 * 把配方书清空并填入“已解锁”集合。我们在它 TAIL 之后追加本地配方，既不会被随后清空，
 * 又能在服务器每次整表替换时自动重补（用确定性负数 id，重复注入会覆盖而非累积）。
 *
 * <p>此回调运行在客户端主线程（vanilla 包处理已 forceMainThread），可安全访问 player / 配方书。
 */
@Mixin(ClientPlayNetworkHandler.class)
public class ClientPlayNetworkHandlerMixin {

    @Inject(method = "onRecipeBookAdd", at = @At("TAIL"))
    private void lri$afterRecipeBookAdd(RecipeBookAddS2CPacket packet, CallbackInfo ci) {
        if (!ConfigManager.get().forceLocalRecipes) {
            return;
        }
        // 只在整表替换（初始同步 / 服务器主动重置）后补，单条解锁通知不重复触发
        if (!packet.replace()) {
            return;
        }
        RecipeInjector.injectIntoBook((ClientPlayNetworkHandler) (Object) this);
    }
}
