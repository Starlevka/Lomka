package lomka.starl.mixins.net.minecraft.client.particle;

import net.minecraft.client.particle.SingleQuadParticle;
import org.joml.Quaternionf;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(SingleQuadParticle.class)
public class MixinSingleQuadParticle {

    @Unique private final Quaternionf lomka$qua = new Quaternionf();

    /**
     * @author Starlev
     * @reason Eliminates per-particle-per-frame Quaternionf allocation in extract().
     * The quaternion's primitive fields are consumed before the next particle's
     * extract() call, making per-instance reuse safe.
     */
    @Redirect(
        method = "extract",
        remap = false,
        at = @At(value = "NEW", target = "org/joml/Quaternionf")
    )
    private Quaternionf lomka$cachedQua() {
        return this.lomka$qua;
    }
}
