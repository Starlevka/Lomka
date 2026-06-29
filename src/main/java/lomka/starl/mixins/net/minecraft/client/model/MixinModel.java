package lomka.starl.mixins.net.minecraft.client.model;

import java.util.List;
import net.minecraft.client.model.Model;
import net.minecraft.client.model.geom.ModelPart;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

@Mixin(Model.class)
public class MixinModel<S> {

    /**
     * @author Starlev
     * @reason Replace Iterator with indexed loop to eliminate per-frame Iterator allocation.
     */
    @Overwrite
    public final void resetPose() {
        List<ModelPart> parts = ((Model)(Object)this).allParts();
        for (int i = 0, size = parts.size(); i < size; ++i) {
            parts.get(i).resetPose();
        }
    }
}