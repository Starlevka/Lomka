package lomka.starl.mixins.com.mojang.blaze3d.platform;

import com.mojang.blaze3d.platform.InputConstants;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;

@Mixin(InputConstants.Key.class)
public abstract class MixinInputConstants {

    @Shadow @Final private InputConstants.Type type;
    @Shadow @Final private int value;

    @Unique private int lomka$hashCode;

    /**
     * @author Starlev
     * @reason Lazy-caches the Key hash code using sentinel-value pattern.
     * Completely avoids Object[] allocations in hot input-checking cycles.
     */
    @Overwrite
    public int hashCode() {
        int h = this.lomka$hashCode;
        if (h == 0) {
            int result = 31 + this.type.hashCode();
            result = 31 * result + this.value;
            h = result;
            if (h == 0) h = 1;
            this.lomka$hashCode = h;
        }
        return h;
    }
}