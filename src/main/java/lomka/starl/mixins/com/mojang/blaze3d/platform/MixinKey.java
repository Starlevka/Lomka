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

    @Unique private boolean lomka$hashComputed;
    @Unique private int lomka$hashCode;

    @Overwrite
    public int hashCode() {
        if (!this.lomka$hashComputed) {
            this.lomka$hashCode = 31 * (31 + this.type.hashCode()) + this.value;
            this.lomka$hashComputed = true;
        }
        return this.lomka$hashCode;
    }
}