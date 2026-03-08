package set.starl.injection.mixins.accessors;

import net.minecraft.client.renderer.blockentity.BlockEntityRenderDispatcher;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import set.starl.injection.access.BlockEntityRenderDispatcherAccessor;

@Mixin(BlockEntityRenderDispatcher.class)
public abstract class BlockEntityRenderDispatcherMixin implements BlockEntityRenderDispatcherAccessor {
	@Shadow
	public abstract @Nullable BlockEntityRenderState tryExtractRenderState(BlockEntity blockEntity, float partialTicks, ModelFeatureRenderer.@Nullable CrumblingOverlay breakProgress);

	@Override
	public @Nullable BlockEntityRenderState lomka$tryExtractRenderState(BlockEntity blockEntity, float partialTicks, ModelFeatureRenderer.@Nullable CrumblingOverlay breakProgress) {
		return this.tryExtractRenderState(blockEntity, partialTicks, breakProgress);
	}
}
