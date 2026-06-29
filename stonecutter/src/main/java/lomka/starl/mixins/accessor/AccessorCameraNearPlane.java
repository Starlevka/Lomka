package lomka.starl.mixins.accessor;

import net.minecraft.client.Camera;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(Camera.NearPlane.class)
public interface AccessorCameraNearPlane {
    
    @Invoker("<init>")
    static Camera.NearPlane create(Vec3 forward, Vec3 left, Vec3 up) {
        throw new AssertionError("Untransformed Mixin!");
    }

    @Accessor("forward")
    Vec3 getForward();
}