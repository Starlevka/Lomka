package set.starl.injection.mixins.cull;

import net.minecraft.core.Direction;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import set.starl.Lomka;

@Mixin(LeavesBlock.class)
public class SmartLeavesCullingMixin {
	@Inject(method = "skipRendering", at = @At("HEAD"), cancellable = true)
	private void lomka$smartCull(BlockState state, BlockState neighbor, Direction direction, CallbackInfoReturnable<Boolean> cir) {
		if (Lomka.CONFIG.culling.smartLeavesCulling) {
			if (neighbor.getBlock() instanceof LeavesBlock) {
				if (state.hasProperty(BlockStateProperties.WATERLOGGED)
					&& neighbor.hasProperty(BlockStateProperties.WATERLOGGED)
					&& state.getValue(BlockStateProperties.WATERLOGGED) != neighbor.getValue(BlockStateProperties.WATERLOGGED)) {
					return;
				}
				cir.setReturnValue(true);
			}
		}
	}
}
