package set.starl.mixin.render;

import java.lang.reflect.Method;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderDispatcher;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(BlockEntityRenderDispatcher.class)
public class BlockEntityRenderDispatcherShouldRenderFastPathMixin {
	@Unique
	private static final boolean LOMKA$ENABLE = !"false".equalsIgnoreCase(System.getProperty("lomka.blockEntities.shouldRenderFastPath", "true"));

	@Unique
	private static final ConcurrentHashMap<Class<?>, Boolean> LOMKA$USES_DEFAULT_SHOULD_RENDER = new ConcurrentHashMap<>();

	@Redirect(
		method = "tryExtractRenderState",
		at = @At(
			value = "INVOKE",
			target = "Lnet/minecraft/client/renderer/blockentity/BlockEntityRenderer;shouldRender(Lnet/minecraft/world/level/block/entity/BlockEntity;Lnet/minecraft/world/phys/Vec3;)Z"
		)
	)
	private boolean lomka$shouldRenderFastPath(final BlockEntityRenderer renderer, final BlockEntity blockEntity, final Vec3 cameraPosition) {
		if (!LOMKA$ENABLE) {
			return renderer.shouldRender(blockEntity, cameraPosition);
		}

		Class<?> cls = renderer.getClass();
		boolean usesDefault = LOMKA$USES_DEFAULT_SHOULD_RENDER.computeIfAbsent(cls, (c) -> {
			try {
				Method m = c.getMethod("shouldRender", BlockEntity.class, Vec3.class);
				return Boolean.valueOf(m.isDefault());
			} catch (Exception ignored) {
				return Boolean.FALSE;
			}
		}).booleanValue();

		if (!usesDefault) {
			return renderer.shouldRender(blockEntity, cameraPosition);
		}

		int viewDistance = renderer.getViewDistance();
		if (viewDistance <= 0) {
			return false;
		}

		double cx = cameraPosition.x;
		double cy = cameraPosition.y;
		double cz = cameraPosition.z;
		double px = (double)blockEntity.getBlockPos().getX() + 0.5;
		double py = (double)blockEntity.getBlockPos().getY() + 0.5;
		double pz = (double)blockEntity.getBlockPos().getZ() + 0.5;

		double dx = px - cx;
		double dy = py - cy;
		double dz = pz - cz;
		double dist2 = dx * dx + dy * dy + dz * dz;
		double max2 = (double)viewDistance * (double)viewDistance;
		return dist2 <= max2;
	}
}

