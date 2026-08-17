package lomka.starl.mixins.accessor;

import net.minecraft.world.phys.shapes.VoxelShape;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(targets = "net.minecraft.world.level.block.state.BlockBehaviour$BlockStateBase$Cache")
public interface AccessorCache {

    @Accessor("collisionShape")
    VoxelShape getCollisionShape();
}
