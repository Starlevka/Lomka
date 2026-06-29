package lomka.starl.mixins.net.minecraft.client.resources.model;

import net.minecraft.client.resources.model.sprite.Material;
import net.minecraft.resources.Identifier;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;

@Mixin(Material.class)
public abstract class MixinMaterial {

    @Shadow @Final private Identifier sprite;
    @Shadow @Final private boolean forceTranslucent;

    @Unique private int lomka$hashCode;
    @Unique private boolean lomka$hashCodeCached;

    /**
     * @author Starlev
     * @reason Lazy-caches the Material hash code using an allocation-free
     * mathematical representation of Objects.hash().
     */
    @Overwrite
    public int hashCode() {
        if (!this.lomka$hashCodeCached) {
            int h = 31 + this.sprite.hashCode();
            h = 31 * h + Boolean.hashCode(this.forceTranslucent);
            this.lomka$hashCode = h;
            this.lomka$hashCodeCached = true;
        }
        return this.lomka$hashCode;
    }
}
