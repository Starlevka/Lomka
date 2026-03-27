package set.starl.injection.mixins.cull;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.client.Camera;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderDispatcher;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.state.LevelRenderState;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.joml.FrustumIntersection;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import set.starl.injection.access.FrustumIntersectionAccess;
import set.starl.injection.access.LevelRendererAccess;

@Mixin(LevelRenderer.class)
public abstract class BlockEntityCullingMixin {

    @Unique
    private static final boolean LOMKA$BLOCK_ENTITY_CULLING = set.starl.LomkaCore.CONFIG.culling.blockEntityCulling;

    @WrapOperation(
        method = "extractVisibleBlockEntities",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/renderer/blockentity/BlockEntityRenderDispatcher;tryExtractRenderState(Lnet/minecraft/world/level/block/entity/BlockEntity;FLnet/minecraft/client/renderer/feature/ModelFeatureRenderer$CrumblingOverlay;)Lnet/minecraft/client/renderer/blockentity/state/BlockEntityRenderState;",
            ordinal = 1
        )
    )
    private BlockEntityRenderState lomka$wrapGlobalBlockEntityExtraction(BlockEntityRenderDispatcher instance, BlockEntity blockEntity, float partialTicks, ModelFeatureRenderer.CrumblingOverlay breakProgress, Operation<BlockEntityRenderState> original, Camera camera, float deltaPartialTick, LevelRenderState levelRenderState) {
        if (!LOMKA$BLOCK_ENTITY_CULLING) {
            return original.call(instance, blockEntity, partialTicks, breakProgress);
        }

        // Frustum check via FrustumIntersection directly — zero AABB allocation
        Frustum frustum = ((LevelRendererAccess) this).lomka$getLastAppliedFrustum();
        if (frustum != null) {
            FrustumIntersection fi = ((FrustumIntersectionAccess) frustum).lomka$getIntersection();
            if (fi != null) {
                int x = blockEntity.getBlockPos().getX();
                int y = blockEntity.getBlockPos().getY();
                int z = blockEntity.getBlockPos().getZ();
                if (fi.intersectAab((float)x, (float)y, (float)z, (float)(x + 1), (float)(y + 1), (float)(z + 1)) == FrustumIntersection.OUTSIDE) {
                    return null;
                }
            }
        }

        return original.call(instance, blockEntity, partialTicks, breakProgress);
    }

}