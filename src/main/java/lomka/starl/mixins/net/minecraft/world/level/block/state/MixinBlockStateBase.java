package lomka.starl.mixins.net.minecraft.world.level.block.state;

import lomka.starl.mixins.accessor.AccessorCache;
import lomka.starl.mixins.accessor.InvokerBlockBehaviour;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(targets = "net.minecraft.world.level.block.state.BlockBehaviour$BlockStateBase")
public abstract class MixinBlockStateBase {

    @Shadow private BlockBehaviour.BlockStateBase.@Nullable Cache cache;

    @Shadow private @Nullable FluidState fluidState;

    @Shadow public abstract Block getBlock();
    @Shadow protected abstract BlockState asState();

    /**
     * @author Starlev
     * @reason Bypasses virtual Block.getCollisionShape dispatch for all non-dynamic blocks.
     *         Vanilla's 2-argument getCollisionShape(level, pos) already checks `this.cache != null ? cache.collisionShape : ...`,
     *         but the 3-argument overload used by entity collisions (BlockCollisions / EntityCollisionContext)
     *         unconditionally delegates to `this.getBlock().getCollisionShape(...)`. For non-dynamic blocks
     *         (hasDynamicShape == false) this.cache is non-null and holds the pre-computed collision shape
     *         evaluated with CollisionContext.empty(). Fluid blocks (LiquidBlock: water/lava) are not marked
     *         dynamicShape yet override getCollisionShape with context-dependent shapes, so they must keep
     *         the vanilla dispatch path.
     */
    @Overwrite
    public VoxelShape getCollisionShape(BlockGetter level, BlockPos pos, CollisionContext context) {
        return this.cache != null && this.fluidState.isEmpty()
                ? ((AccessorCache) (Object) this.cache).getCollisionShape()
                : ((InvokerBlockBehaviour) (Object) this.getBlock()).invokeGetCollisionShape(this.asState(), level, pos, context);
    }
}
