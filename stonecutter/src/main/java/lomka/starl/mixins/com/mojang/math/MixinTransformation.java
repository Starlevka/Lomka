package lomka.starl.mixins.com.mojang.math;

import com.mojang.math.Transformation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Unique;

@Mixin(Transformation.class)
public abstract class MixinTransformation {

    @Unique private int lomka$hashCode;

    /**
     * @author Starlev
     * @reason Lazily caches the hash code to eliminate repeated hashCode()
     * computation and Objects.hash() allocation in hot model and animation
     * rendering cycles. Transformation is immutable, so the hash never changes.
     * Uses the sentinel-value pattern (0 = uncached) to avoid volatile overhead
     * while remaining safely race-tolerant.
     */
    @Overwrite(remap = false)
    public int hashCode() {
        int h = this.lomka$hashCode;
        if (h == 0) {
            h = 31 + ((Transformation)(Object)this).getMatrix().hashCode();
            if (h == 0) h = 1;
            this.lomka$hashCode = h;
        }
        return h;
    }
}
