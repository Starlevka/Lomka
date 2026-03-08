package set.starl.injection.mixins.cull;

import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Leashable;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.decoration.ItemFrame;
import net.minecraft.world.entity.decoration.painting.Painting;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.Display;
import net.minecraft.world.entity.vehicle.boat.AbstractBoat;
import net.minecraft.world.entity.vehicle.minecart.AbstractMinecart;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.client.gui.components.debug.DebugScreenEntries;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import set.starl.injection.access.FrustumIntersectionAccess;
import set.starl.injection.access.EntityCullingAccessor;
import set.starl.util.SectionVisibilityCache;

@Mixin(EntityRenderer.class)
public abstract class EntityRendererCullingMixin {
	@Unique
	private static final boolean LOMKA$CULL_INVISIBLE = set.starl.Lomka.CONFIG.entities.enable && set.starl.Lomka.CONFIG.entities.cullInvisible;

	@Unique
	private static final boolean LOMKA$FRUSTUM_CULL_ALL = set.starl.Lomka.CONFIG.entities.enable && set.starl.Lomka.CONFIG.entities.frustumCullAll;

	@Unique
	private static final EquipmentSlot[] LOMKA$EQUIPMENT_SLOTS = EquipmentSlot.values();

	@Unique
	private static final int LOMKA$FRUSTUM_HYSTERESIS_TICKS = set.starl.Lomka.CONFIG.entities.frustumHysteresisTicks;

	@Unique
	private static final int LOMKA$FRUSTUM_HYSTERESIS_LEGACY_INTERVAL = LOMKA$FRUSTUM_HYSTERESIS_TICKS > 0 ? (LOMKA$FRUSTUM_HYSTERESIS_TICKS + 1) : 0;

	@Unique
	private static final int LOMKA$FRUSTUM_VISIBLE_RECHECK = LOMKA$FRUSTUM_HYSTERESIS_LEGACY_INTERVAL;

	@Unique
	private static final int LOMKA$FRUSTUM_INVISIBLE_RECHECK = LOMKA$FRUSTUM_HYSTERESIS_LEGACY_INTERVAL;

	@Unique
	private static final boolean LOMKA$FRUSTUM_HYSTERESIS_ENABLE = LOMKA$FRUSTUM_VISIBLE_RECHECK > 1 || LOMKA$FRUSTUM_INVISIBLE_RECHECK > 1;

	@Unique
	private static final int LOMKA$FRUSTUM_HYSTERESIS_MAX_ENTRIES = 16384;

	@Unique
	private static final boolean LOMKA$FRUSTUM_SPHERE_TEST = LOMKA$FRUSTUM_HYSTERESIS_ENABLE;

	@Unique
	private static final float LOMKA$FRUSTUM_SPHERE_MARGIN = 0.5f;

	@Unique
	private static final boolean LOMKA$FRUSTUM_CHEAP_DISTANCE_REJECT = false;

	@Unique
	private static final double LOMKA$FRUSTUM_CHEAP_DISTANCE_MARGIN = 2.0;

	@Unique
	private static final double LOMKA$FRUSTUM_HYSTERESIS_STATIC_MULTIPLIER = 2.0;

	@Unique
	private static final double LOMKA$FRUSTUM_HYSTERESIS_LIVING_MULTIPLIER = 0.5;

	@Unique
	private static final double LOMKA$FRUSTUM_HYSTERESIS_DISPLAY_MULTIPLIER = 4.0;

	@Unique
	private static final double LOMKA$FRUSTUM_HYSTERESIS_PROJECTILE_MULTIPLIER = 0.25;

	@Unique
	private static final double LOMKA$FRUSTUM_HYSTERESIS_VEHICLE_MULTIPLIER = 0.5;

	@Unique
	private static final double LOMKA$SECTION_OCCLUSION_CULL_MIN_DISTANCE = 32.0;

	@Unique
	private static final boolean LOMKA$SECTION_OCCLUSION_CULL = set.starl.Lomka.CONFIG.entities.sectionOcclusionCull;

	@Unique
	private static final double LOMKA$DISPLAY_CULL_DISTANCE = set.starl.Lomka.CONFIG.entities.displayCullDistance;


	@Shadow
	protected abstract AABB getBoundingBoxForCulling(final Entity entity);

	@Inject(method = "shouldRender", at = @At("HEAD"), cancellable = true)
	private void lomka$cullInvisible(final Entity entity, final Frustum culler, final double camX, final double camY, final double camZ, final CallbackInfoReturnable<Boolean> cir) {
		if (!LOMKA$CULL_INVISIBLE) {
			return;
		}
		// Short circuit: only check players if invisible
		if (!(entity instanceof Player)) {
			if (entity.isInvisible() && !entity.isCurrentlyGlowing()) {
				Player player = Minecraft.getInstance().player;
				if (player != null && entity.isInvisibleTo(player)) {
					if (entity.hasCustomName() && entity.isCustomNameVisible()) {
						return;
					}
					if (lomka$hasVisiblePartsWhenInvisible(entity)) {
						return;
					}
					cir.setReturnValue(Boolean.FALSE);
				}
			}
		}
	}

	@Unique
	private static boolean lomka$hasVisiblePartsWhenInvisible(final Entity entity) {
		if (entity instanceof LivingEntity living) {
			for (EquipmentSlot slot : LOMKA$EQUIPMENT_SLOTS) {
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
		if (!LOMKA$FRUSTUM_CULL_ALL || !cir.getReturnValue().booleanValue() || entity.isCustomNameVisible()) {
			return;
		}

		Minecraft mc = Minecraft.getInstance();
		if (!(entity instanceof EntityCullingAccessor accessor)) {
			return;
		}
		final int tick = mc.level != null ? (int)mc.level.getGameTime() : Integer.MIN_VALUE;
		
		final int camHash15 = (int)((((long)Mth.floor(camX * 2.0)) ^ ((long)Mth.floor(camY * 2.0)) ^ ((long)Mth.floor(camZ * 2.0))) >>> 15) & 0x7FFF;
		final int motionHash = lomka$computeMotionHash(entity);

		if (LOMKA$FRUSTUM_HYSTERESIS_ENABLE && tick != Integer.MIN_VALUE) {
			final int lastTick = accessor.lomka$getLastRenderTick();
			// Multi-tick hysteresis skip: reuse visibility if both camera and entity hashes are stable.
			// camHash15 now uses 0.5 block quantization, and motionHash also uses 0.5 block quantization.
			if (tick - lastTick < lomka$adjustIntervalByType(entity, LOMKA$FRUSTUM_HYSTERESIS_TICKS + 1) &&
				camHash15 == accessor.lomka$getLastRenderCamHash() &&
				motionHash == accessor.lomka$getLastRenderMotionHash()) {
				
				if (!accessor.lomka$isRenderVisible()) {
					cir.setReturnValue(Boolean.FALSE);
				}
				return;
			}
		}

		// Occlusion and Distance
		if (mc.level != null && !(entity instanceof Player)) {
			double ddx = entity.getX() - camX;
			double ddy = entity.getY() - camY;
			double ddz = entity.getZ() - camZ;
			double distSq = ddx * ddx + ddy * ddy + ddz * ddz;

			if (entity instanceof net.minecraft.world.entity.Marker) {
				lomka$recordFrustumDecision(accessor, tick, camHash15, motionHash, false);
				cir.setReturnValue(Boolean.FALSE);
				return;
			}

			if (entity instanceof Display || entity instanceof net.minecraft.world.entity.Interaction) {
				if (distSq > LOMKA$DISPLAY_CULL_DISTANCE * LOMKA$DISPLAY_CULL_DISTANCE) {
					lomka$recordFrustumDecision(accessor, tick, camHash15, motionHash, false);
					cir.setReturnValue(Boolean.FALSE);
					return;
				}
			}

			if (LOMKA$SECTION_OCCLUSION_CULL && (!(entity instanceof Leashable leashable) || leashable.getLeashHolder() == null)) {
				if (distSq >= LOMKA$SECTION_OCCLUSION_CULL_MIN_DISTANCE * LOMKA$SECTION_OCCLUSION_CULL_MIN_DISTANCE) {
					SectionVisibilityCache.ensureCameraCache(tick);
					// Use blockToSectionCoord for slightly cleaner logic
					final int sx = SectionPos.blockToSectionCoord(entity.getX());
					final int sy = SectionPos.blockToSectionCoord(entity.getY());
					final int sz = SectionPos.blockToSectionCoord(entity.getZ());
					if (!SectionVisibilityCache.isSectionVisible(SectionPos.asLong(sx, sy, sz))) {
						lomka$recordFrustumDecision(accessor, tick, camHash15, motionHash, false);
						cir.setReturnValue(Boolean.FALSE);
						return;
					}
				}
			}
		}

		AABB baseBox = this.getBoundingBoxForCulling(entity);
		if (baseBox.hasNaN() || baseBox.getSize() == 0.0) {
			baseBox = new AABB(entity.getX() - 2.0, entity.getY() - 2.0, entity.getZ() - 2.0, entity.getX() + 2.0, entity.getY() + 2.0, entity.getZ() + 2.0);
		}

		boolean visible = culler.isVisible(baseBox);
		if (!visible && culler instanceof FrustumIntersectionAccess fia) {
			// Zero-allocation inflated check: avoid AABB.inflate() by passing coordinates directly to JOML
			final org.joml.FrustumIntersection intersection = fia.lomka$getIntersection();
			if (intersection != null) {
				visible = intersection.intersectAab(
					(float)(baseBox.minX - 0.5 - camX),
					(float)(baseBox.minY - 0.5 - camY),
					(float)(baseBox.minZ - 0.5 - camZ),
					(float)(baseBox.maxX + 0.5 - camX),
					(float)(baseBox.maxY + 0.5 - camY),
					(float)(baseBox.maxZ + 0.5 - camZ)
				) != 0;
			}
		}

		if (visible) {
			lomka$recordFrustumDecision(accessor, tick, camHash15, motionHash, true);
		} else {
			if (!(entity instanceof Leashable l) || l.getLeashHolder() == null) {
				lomka$recordFrustumDecision(accessor, tick, camHash15, motionHash, false);
				cir.setReturnValue(Boolean.FALSE);
			}
		}
	}

	@Unique
	private static void lomka$recordFrustumDecision(final EntityCullingAccessor accessor, final int tick, final int camHash15, final int motionHash, final boolean visible) {
		accessor.lomka$setLastRenderTick(tick);
		accessor.lomka$setLastRenderCamHash(camHash15);
		accessor.lomka$setLastRenderMotionHash(motionHash);
		accessor.lomka$setRenderVisible(visible);
	}

	@Unique
	private static int lomka$adjustIntervalByType(final Entity entity, final int interval) {
		if (interval <= 1) {
			return interval;
		}

		double mult = 1.0;
		if (entity instanceof Display) {
			mult = LOMKA$FRUSTUM_HYSTERESIS_DISPLAY_MULTIPLIER;
		} else if (entity instanceof ArmorStand || entity instanceof ItemFrame || entity instanceof Painting) {
			mult = LOMKA$FRUSTUM_HYSTERESIS_STATIC_MULTIPLIER;
		} else if (entity instanceof Projectile) {
			mult = LOMKA$FRUSTUM_HYSTERESIS_PROJECTILE_MULTIPLIER;
		} else if (entity instanceof AbstractBoat || entity instanceof AbstractMinecart) {
			mult = LOMKA$FRUSTUM_HYSTERESIS_VEHICLE_MULTIPLIER;
		} else if (entity instanceof LivingEntity) {
			mult = LOMKA$FRUSTUM_HYSTERESIS_LIVING_MULTIPLIER;
		}

		if (mult == 1.0) {
			return interval;
		}

		int scaled = (int)Math.round((double)interval * mult);
		if (scaled < 1) {
			return 1;
		}
		return scaled;
	}

	@Unique
	private static int lomka$computeMotionHash(final Entity entity) {
		// Quantize to 0.5 blocks to increase hysteresis hit rate for slow moving entities
		int qx = Mth.floor(entity.getX() * 2.0);
		int qy = Mth.floor(entity.getY() * 2.0);
		int qz = Mth.floor(entity.getZ() * 2.0);
		int h = 1;
		h = 31 * h + qx;
		h = 31 * h + qy;
		h = 31 * h + qz;

		boolean isVehicle = entity instanceof AbstractBoat || entity instanceof AbstractMinecart;
		boolean includeRot = entity instanceof LivingEntity || isVehicle;
		boolean includeVel = entity instanceof Projectile || isVehicle;
		if (includeRot) {
			int yawQ = Mth.floor((Mth.wrapDegrees(entity.getYRot()) + 180.0F) * 2.0F);
			int pitchQ = Mth.floor((Mth.clamp(entity.getXRot(), -90.0F, 90.0F) + 90.0F) * 2.0F);
			h = 31 * h + yawQ;
			h = 31 * h + pitchQ;
		}
		if (includeVel) {
			Vec3 v = entity.getDeltaMovement();
			int vx = Mth.floor(v.x * 20.0);
			int vy = Mth.floor(v.y * 20.0);
			int vz = Mth.floor(v.z * 20.0);
			h = 31 * h + vx;
			h = 31 * h + vy;
			h = 31 * h + vz;
		}

		return h;
	}
}
