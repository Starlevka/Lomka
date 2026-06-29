package lomka.starl.mixins.com.mojang.blaze3d.audio;

import com.mojang.blaze3d.audio.Channel;
import net.minecraft.world.phys.Vec3;
import org.lwjgl.openal.AL10;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(Channel.class)
public abstract class MixinChannel {

    @Shadow @Final private int source;

    /**
     * @author Starlev
     * @reason Avoid allocating a float[3] array on every sound position update.
     *         LWJGL 3 provides an overloaded alSource3f which passes primitives directly.
     */
    @Overwrite
    public void setSelfPosition(Vec3 vec3) {
        AL10.alSource3f(this.source, 4100, (float) vec3.x, (float) vec3.y, (float) vec3.z);
    }
}