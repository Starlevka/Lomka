package set.starl.injection.access;

import net.minecraft.client.renderer.blockentity.BlockEntityRenderDispatcher;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.jspecify.annotations.Nullable;

public interface BlockEntityRenderDispatcherAccessor {
	@Nullable BlockEntityRenderState lomka$tryExtractRenderState(BlockEntity blockEntity, float partialTicks, ModelFeatureRenderer.@Nullable CrumblingOverlay breakProgress);
}
