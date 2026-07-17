package lomka.starl.mixins.com.mojang.blaze3d.audio;

import com.mojang.blaze3d.audio.Listener;
import com.mojang.blaze3d.audio.ListenerTransform;
import net.minecraft.world.phys.Vec3;
import org.lwjgl.openal.AL10;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;

@Mixin(Listener.class)
public abstract class MixinListener {

    @Shadow private ListenerTransform transform;

    @Unique private final float[] lomka$orientationCache = new float[6];

    /**
     * Avoid allocating a float[6] array on every single frame.
     * Caches the orientation array to achieve zero-allocation listener updates.
     */
    @Overwrite
    public void setTransform(ListenerTransform listenertransform) {
        this.transform = listenertransform;

        Vec3 pos     = listenertransform.position();
        Vec3 forward = listenertransform.forward();
        Vec3 up      = listenertransform.up();

        AL10.alListener3f(4100, (float) pos.x, (float) pos.y, (float) pos.z);

        lomka$orientationCache[0] = (float) forward.x;
        lomka$orientationCache[1] = (float) forward.y;
        lomka$orientationCache[2] = (float) forward.z;
        lomka$orientationCache[3] = (float) up.x;
        lomka$orientationCache[4] = (float) up.y;
        lomka$orientationCache[5] = (float) up.z;

        AL10.alListenerfv(4111, lomka$orientationCache);
    }
}