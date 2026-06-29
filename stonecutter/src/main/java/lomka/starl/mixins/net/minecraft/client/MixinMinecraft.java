package lomka.starl.mixins.net.minecraft.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.sounds.SoundManager;
import net.minecraft.client.gui.screens.Screen;
import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.platform.Window;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = Minecraft.class, priority = 500)
public class MixinMinecraft {

    @Shadow private Window window;

    @Unique private boolean cachedShiftDown;
    @Unique private boolean cachedControlDown;
    @Unique private boolean cachedAltDown;

    /**
     * Caches modifier key states once per tick to avoid repeated JNI isKeyDown calls from hasShift/Control/AltDown.
     */
    @Inject(method = "runTick", at = @At("HEAD"), require = 1)
    private void cacheModifierKeys(boolean tick, CallbackInfo ci) {
        //? if >=1.21.9 {
        this.cachedShiftDown = InputConstants.isKeyDown(this.window, 340)
                            || InputConstants.isKeyDown(this.window, 344);
        this.cachedControlDown = InputConstants.isKeyDown(this.window, 341)
                              || InputConstants.isKeyDown(this.window, 345);
        this.cachedAltDown = InputConstants.isKeyDown(this.window, 342)
                          || InputConstants.isKeyDown(this.window, 346);
        //?} else {
        /*long handle = this.window.getWindow();
        if (handle != 0L) {
            this.cachedShiftDown = InputConstants.isKeyDown(handle, 340)
                                || InputConstants.isKeyDown(handle, 344);
            this.cachedControlDown = InputConstants.isKeyDown(handle, 341)
                                  || InputConstants.isKeyDown(handle, 345);
            this.cachedAltDown = InputConstants.isKeyDown(handle, 342)
                            || InputConstants.isKeyDown(handle, 346);
        }*/
        //?}
    }

    //? if >=1.21.9 {
    /**
     * @author Starlev
     * @reason Returns the cached modifier key state instead of calling native JNI isKeyDown every time.
     */
    @Overwrite
    public boolean hasShiftDown() {
        return this.cachedShiftDown;
    }

    /**
     * @author Starlev
     * @reason Returns the cached modifier key state instead of calling native JNI isKeyDown every time.
     */
    @Overwrite
    public boolean hasControlDown() {
        return this.cachedControlDown;
    }

    /**
     * @author Starlev
     * @reason Returns the cached modifier key state instead of calling native JNI isKeyDown every time.
     */
    @Overwrite
    public boolean hasAltDown() {
        return this.cachedAltDown;
    }
    //?}

    //? if <26.1 {
    /*@Redirect(
        method = "runTick",
        at = @At(value = "INVOKE", target = "Ljava/lang/Thread;yield()V")
    )
    private void removeThreadYield() {
    }*/
    //?}
}