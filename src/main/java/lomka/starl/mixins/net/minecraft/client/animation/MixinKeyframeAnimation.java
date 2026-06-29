package lomka.starl.mixins.net.minecraft.client.animation;

import net.minecraft.client.animation.AnimationDefinition;
import net.minecraft.client.animation.KeyframeAnimation;
import org.joml.Vector3f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;

import java.util.List;

@Mixin(KeyframeAnimation.class)
public abstract class MixinKeyframeAnimation {
    
    @Shadow private AnimationDefinition definition;
    @Shadow private List<KeyframeAnimation.Entry> entries;
    @Shadow protected abstract float getElapsedSeconds(long i);

    @Unique private final Vector3f lomka$tempVec = new Vector3f();

    /**
     * @author Starlev
     * @reason Eliminate heavy per-frame allocations during entity animations.
     * Vanilla allocates a `new Vector3f()` and an `Iterator` for every animated entity 
     * on every single frame, causing significant GC pressure. This overwrite uses 
     * a reusable vector field and an indexed for-loop to achieve zero-allocation.
     */
    @Overwrite
    public void apply(long i, float f) {
        float f1 = this.getElapsedSeconds(i);
        
        Vector3f vector3f = this.lomka$tempVec;

        List<KeyframeAnimation.Entry> localEntries = this.entries;
        for (int j = 0, len = localEntries.size(); j < len; j++) {
            localEntries.get(j).apply(f1, f, vector3f);
        }
    }
}