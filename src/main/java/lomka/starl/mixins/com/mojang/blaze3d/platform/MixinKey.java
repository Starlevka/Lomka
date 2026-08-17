package lomka.starl.mixins.com.mojang.blaze3d.platform;

import com.mojang.blaze3d.platform.InputConstants;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;

@Mixin(InputConstants.Key.class)
public abstract class MixinKey {

    @Shadow @Final private InputConstants.Type type;
    @Shadow @Final private int value;

    @Unique private int lomka$hashCode;

    /**
     * @author Starlev
     * @reason Caches the computed key hash so repeated lookups avoid reconstructing the
     *         same value on every call while keeping the original identity semantics intact.
     */
    @Overwrite
    public int hashCode() {
        int h = this.lomka$hashCode;
        if (h == 0) {
            h = 31 * (31 + this.type.hashCode()) + this.value;
            if (h == 0) h = 1;
            this.lomka$hashCode = h;
        }
        return h;
    }
}