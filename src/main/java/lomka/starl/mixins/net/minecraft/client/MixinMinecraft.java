package lomka.starl.mixins.net.minecraft.client;

import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(value = Minecraft.class, priority = 500)
public class MixinMinecraft {

    /**
     * @author Starlev
     * @reason Thread.yield() at the end of runTick gives up the thread timeslice on
     *         every frame; on Windows this can add up to a scheduler quantum (probably ~15ms) of
     *         latency. Removing it costs slightly more CPU while frames are uncapped, in
     *         exchange for lower input/render latency. Valid on 1.21-1.21.11: 26.x removed
     *         the call natively (excluded from 26.x code).
     */
    @Redirect(
        method = "runTick",
        at = @At(value = "INVOKE", target = "Ljava/lang/Thread;yield()V"),
        require = 0
    )
    private void removeThreadYield() {
    // Thread.yield();
    }
}
