package lomka.starl.mixins.net.minecraft.client.sounds;

import net.minecraft.client.Camera;
import net.minecraft.client.sounds.SoundEngine;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(SoundEngine.class)
public abstract class MixinSoundEngine {

    @Unique private Vec3   lomka$lastPos  = Vec3.ZERO;
    @Unique private float  lomka$lastXRot = Float.NaN;
    @Unique private float  lomka$lastYRot = Float.NaN;

    /**
     * Skips Listener.setTransform() when the camera position/rotation has not
     * changed since the last tick, saving native OpenAL calls.
     */
    @Inject(method = "updateSource", at = @At("HEAD"), cancellable = true)
    private void lomka$skipRedundantCameraUpdates(Camera camera, CallbackInfo ci) {
        //? if >=1.21.11 {
        if (!camera.isInitialized()) return;

        Vec3  currentPos  = camera.position();
        float currentXRot = camera.xRot();
        float currentYRot = camera.yRot();
        //?} else {
        /*if (!camera.isInitialized()) return;

        Vec3  currentPos  = camera.getPosition();
        float currentXRot = camera.getXRot();
        float currentYRot = camera.getYRot();*/
        //?}

        if (currentPos.equals(this.lomka$lastPos)
                && currentXRot == this.lomka$lastXRot
                && currentYRot == this.lomka$lastYRot) {
            ci.cancel();
            return;
        }

        this.lomka$lastPos  = currentPos;
        this.lomka$lastXRot = currentXRot;
        this.lomka$lastYRot = currentYRot;
    }
}
