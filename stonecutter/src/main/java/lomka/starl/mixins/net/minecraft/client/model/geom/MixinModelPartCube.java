package lomka.starl.mixins.net.minecraft.client.model.geom;

import net.minecraft.client.model.geom.ModelPart;
import org.joml.Vector3f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(ModelPart.Cube.class)
public class MixinModelPartCube {

    @Unique private final Vector3f lomka$vec = new Vector3f();

    /**
     * Redirects the single new Vector3f() in compile() to a per-instance
     * cached buffer. compile() is called O(entities × parts × cubes) times
     * per frame, making this one of the highest-frequency allocations in
     * entity rendering.
     */
    @Redirect(
        method = "compile",
        remap = false,
        at = @At(value = "NEW", target = "org/joml/Vector3f")
    )
    private Vector3f lomka$cachedVec() {
        return this.lomka$vec;
    }
}
