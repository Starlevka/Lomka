package set.starl.mixin.render;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.world.entity.Leashable;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.decoration.ItemFrame;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(EntityRenderer.class)
public abstract class EntityRendererCullingMixin {
	@Unique
	private static final boolean LOMKA$CULL_INVISIBLE = "true".equalsIgnoreCase(System.getProperty("lomka.entities.cullInvisible", "false"));

	@Unique
	private static final boolean LOMKA$FRUSTUM_CULL_ALL = !"false".equalsIgnoreCase(System.getProperty("lomka.entities.frustumCullAll", "true"));

	@Shadow
	protected abstract AABB getBoundingBoxForCulling(final Entity entity);

	@Inject(method = "shouldRender", at = @At("HEAD"), cancellable = true)
	private void lomka$cullInvisible(final Entity entity, final Frustum culler, final double camX, final double camY, final double camZ, final CallbackInfoReturnable<Boolean> cir) {
		if (!LOMKA$CULL_INVISIBLE) {
			return;
		}
		Player player = Minecraft.getInstance().player;
		if (player == null) {
			return;
		}
		if (entity.isInvisibleTo(player) && !entity.isCurrentlyGlowing()) {
			if (entity.hasCustomName() && entity.isCustomNameVisible()) {
				return;
			}
			if (lomka$hasVisiblePartsWhenInvisible(entity)) {
				return;
			}
			cir.setReturnValue(Boolean.FALSE);
		}
	}

	@Unique
	private static boolean lomka$hasVisiblePartsWhenInvisible(final Entity entity) {
		if (entity instanceof LivingEntity living) {
			for (EquipmentSlot slot : EquipmentSlot.values()) {
				if (!living.getItemBySlot(slot).isEmpty()) {
					return true;
				}
			}
		}
		if (entity instanceof ItemFrame frame) {
			return !frame.getItem().isEmpty();
		}
		return false;
	}

	@Inject(method = "shouldRender", at = @At("RETURN"), cancellable = true)
	private void lomka$frustumCullAll(final Entity entity, final Frustum culler, final double camX, final double camY, final double camZ, final CallbackInfoReturnable<Boolean> cir) {
		if (!LOMKA$FRUSTUM_CULL_ALL) {
			return;
		}
		if (!cir.getReturnValue().booleanValue()) {
			return;
		}

		AABB boundingBox = this.getBoundingBoxForCulling(entity).inflate((double)0.5F);
		if (boundingBox.hasNaN() || boundingBox.getSize() == (double)0.0F) {
			boundingBox = new AABB(entity.getX() - (double)2.0F, entity.getY() - (double)2.0F, entity.getZ() - (double)2.0F, entity.getX() + (double)2.0F, entity.getY() + (double)2.0F, entity.getZ() + (double)2.0F);
		}

		if (culler.isVisible(boundingBox)) {
			return;
		}

		if (entity instanceof Leashable leashable) {
			Entity leashHolder = leashable.getLeashHolder();
			if (leashHolder != null) {
				AABB leasherBox = leashHolder.getBoundingBox().inflate((double)0.5F);
				if (leasherBox.hasNaN() || leasherBox.getSize() == (double)0.0F) {
					leasherBox = new AABB(leashHolder.getX() - (double)2.0F, leashHolder.getY() - (double)2.0F, leashHolder.getZ() - (double)2.0F, leashHolder.getX() + (double)2.0F, leashHolder.getY() + (double)2.0F, leashHolder.getZ() + (double)2.0F);
				}
				if (culler.isVisible(leasherBox) || culler.isVisible(boundingBox.minmax(leasherBox))) {
					return;
				}
			}
		}

		cir.setReturnValue(Boolean.FALSE);
	}
}
