//? if <26.1 {
package lomka.starl.mixins.net.minecraft.client;

import net.minecraft.client.Minecraft;
import com.mojang.blaze3d.platform.Window;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(value = Minecraft.class, priority = 500)
public class MixinMinecraft {

    @Shadow private Window window;

    /*@Redirect(
        method = "runTick",
        at = @At(value = "INVOKE", target = "Ljava/lang/Thread;yield()V")
    )
    private void removeThreadYield() {
    // Thread.yield();
    }*/
}
//?}