package lomka.starl.mixins.net.minecraft.client.renderer.block.model;

import net.minecraft.client.resources.model.cuboid.ItemTransform;
import org.joml.Quaternionf;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(ItemTransform.class)
public class MixinItemTransform {

    @Unique private static final Quaternionf lomka$QUAT = new Quaternionf();

    @Redirect(
        method = "apply",
        at = @At(value = "NEW", target = "org/joml/Quaternionf")
    )
    private Quaternionf lomka$cachedQuat() {
        return lomka$QUAT;
    }
}
