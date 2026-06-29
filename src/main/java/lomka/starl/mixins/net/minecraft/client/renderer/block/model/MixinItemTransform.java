package lomka.starl.mixins.net.minecraft.client.renderer.block.model;

//? if >=26.1 {
/*import net.minecraft.client.resources.model.cuboid.ItemTransform;*/
//?} else {
import net.minecraft.client.renderer.block.model.ItemTransform;
//?}
import org.joml.Quaternionf;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(ItemTransform.class)
public class MixinItemTransform {

    @Unique private final Quaternionf lomka$qua = new Quaternionf();

    /**
     * @author Starlev
     * @reason Eliminates per-call Quaternionf allocation in apply(), which is
     * invoked every frame for every rendered item (hand, dropped, frames).
     */
    @Redirect(
        method = "apply",
        remap = false,
        at = @At(value = "NEW", target = "org/joml/Quaternionf")
    )
    private Quaternionf lomka$cachedQua() {
        return this.lomka$qua;
    }
}
