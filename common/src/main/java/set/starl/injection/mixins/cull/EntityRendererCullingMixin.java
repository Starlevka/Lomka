package set.starl.injection.mixins.cull;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.core.SectionPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Leashable;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.decoration.ItemFrame;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.AbstractHurtingProjectile;
import net.minecraft.world.entity.vehicle.VehicleEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.FrustumIntersection;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import set.starl.LomkaCore;
import set.starl.injection.access.EntityCullingAccessor;
import set.starl.injection.access.FrustumIntersectionAccess;
import set.starl.util.SectionVisibilityCache;

@Mixin(EntityRenderer.class)
public abstract class EntityRendererCullingMixin {
    @Unique
    private static final boolean LOMKA$FRUSTUM_CULL_ALL = LomkaCore.CONFIG.culling.enable && LomkaCore.CONFIG.culling.frustumCullAll;
	@Unique
	private static final int LOMKA$FRUSTUM_HYSTERESIS_TICKS = LomkaCore.CONFIG.culling.frustumHysteresisTicks;
	@Unique
	private static final boolean LOMKA$FRUSTUM_HYSTERESIS_ENABLE = LOMKA$FRUSTUM_HYSTERESIS_TICKS > 0;
	@Unique
	private static final boolean LOMKA$SECTION_OCCLUSION_CULL = LomkaCore.CONFIG.culling.enable && LomkaCore.CONFIG.culling.sectionOcclusionCull;
    @Unique
    private static final double LOMKA$SECTION_OCCLUSION_MIN_DIST_SQ = 32.0 * 32.0;

	@Shadow
	protected abstract AABB getBoundingBoxForCulling(Entity entity);

    // === Frustum cull all with hysteresis, motion hash, camera hash, section occlusion ===
    @Inject(method = "shouldRender", at = @At("RETURN"), cancellable = true)
	private void lomka$frustumCullAll(Entity entity, Frustum culler, double camX, double camY, double camZ, CallbackInfoReturnable<Boolean> cir) {
		if (!LOMKA$FRUSTUM_CULL_ALL) return;
		if (!cir.getReturnValue()) return;
		if (entity.isCustomNameVisible()) return;

		if (!(entity instanceof EntityCullingAccessor accessor)) return;

		Minecraft mc = Minecraft.getInstance();
		int tick = mc.level != null ? (int) mc.level.getGameTime() : Integer.MIN_VALUE;

        // Compute camera hash (15-bit) for hysteresis — detect camera movement
        int camHash15 = LomkaCore.MathHelper.hash3(
            SectionPos.blockToSectionCoord(camX * 2.0),
            SectionPos.blockToSectionCoord(camY * 2.0),
            SectionPos.blockToSectionCoord(camZ * 2.0)
        ) & 32767;

		// Compute motion hash — detect entity movement/rotation
		int motionHash = lomka$computeMotionHash(entity);

		// Hysteresis: skip recheck if within interval and camera/motion unchanged
		if (LOMKA$FRUSTUM_HYSTERESIS_ENABLE && tick != Integer.MIN_VALUE) {
			int lastTick = accessor.lomka$getLastRenderTick();
			int interval = lomka$adjustIntervalByType(entity, LOMKA$FRUSTUM_HYSTERESIS_TICKS + 1);
			if (tick - lastTick < interval
				&& camHash15 == accessor.lomka$getLastRenderCamHash()
				&& motionHash == accessor.lomka$getLastRenderMotionHash()) {
				if (!accessor.lomka$isRenderVisible()) {
					cir.setReturnValue(Boolean.FALSE);
				}
				return;
			}
		}

        // Distance-based fast rejects for non-player entities
        if (mc.level != null && !(entity instanceof Player)) {
            double distSq = LomkaCore.MathHelper.distSq(entity.getX(), entity.getY(), entity.getZ(), camX, camY, camZ);

            // Section occlusion cull — skip entities in invisible sections
            if (LOMKA$SECTION_OCCLUSION_CULL && distSq >= LOMKA$SECTION_OCCLUSION_MIN_DIST_SQ) {
                if (!(entity instanceof Leashable leashable) || leashable.getLeashHolder() == null) {
                    SectionVisibilityCache.ensureCameraCache((long) tick);
                    int esx = SectionPos.blockToSectionCoord(entity.getX());
                    int esz = SectionPos.blockToSectionCoord(entity.getZ());
                    int edx = esx - SectionVisibilityCache.getCamChunkX();
                    int edz = esz - SectionVisibilityCache.getCamChunkZ();
                    if (edx * edx + edz * edz > 16) {
                        long cellKey = SectionPos.asLong(
                            esx,
                            SectionPos.blockToSectionCoord(entity.getY()),
                            esz
                        );
                        if (!SectionVisibilityCache.isSectionVisible(cellKey)) {
                            lomka$recordFrustumDecision(accessor, tick, camHash15, motionHash, false);
                            cir.setReturnValue(Boolean.FALSE);
                            return;
                        }
                    }
                }
            }
        }
		// Frustum test — reuse AABB, avoid allocation
        AABB baseBox = this.getBoundingBoxForCulling(entity);
        if (baseBox.hasNaN() || baseBox.getSize() == 0.0) {
            double ex = entity.getX();
            double ey = entity.getY();
            double ez = entity.getZ();
            baseBox = new AABB(ex - 2.0, ey - 2.0, ez - 2.0, ex + 2.0, ey + 2.0, ez + 2.0);
        }
        // Primary: FrustumIntersection directly — zero allocation, faster than vanilla AABB test
        boolean visible;
        if (culler instanceof FrustumIntersectionAccess fia) {
            FrustumIntersection fi = fia.lomka$getIntersection();
            if (fi != null) {
                float fMinX = (float) (baseBox.minX - 0.5 - camX);
                float fMinY = (float) (baseBox.minY - 0.5 - camY);
                float fMinZ = (float) (baseBox.minZ - 0.5 - camZ);
                float fMaxX = (float) (baseBox.maxX + 0.5 - camX);
                float fMaxY = (float) (baseBox.maxY + 0.5 - camY);
                float fMaxZ = (float) (baseBox.maxZ + 0.5 - camZ);
                visible = fi.intersectAab(fMinX, fMinY, fMinZ, fMaxX, fMaxY, fMaxZ) != FrustumIntersection.OUTSIDE;
            } else {
                visible = culler.isVisible(baseBox);
            }
        } else {
            visible = culler.isVisible(baseBox);
        }

		if (visible) {
			lomka$recordFrustumDecision(accessor, tick, camHash15, motionHash, true);
		} else {
			// Don't cull leashed entities
			if (entity instanceof Leashable leashable && leashable.getLeashHolder() != null) {
				return;
			}
			lomka$recordFrustumDecision(accessor, tick, camHash15, motionHash, false);
			cir.setReturnValue(Boolean.FALSE);
		}
	}

	@Unique
	private static void lomka$recordFrustumDecision(EntityCullingAccessor accessor, int tick, int camHash15, int motionHash, boolean visible) {
		accessor.lomka$setLastRenderTick(tick);
		accessor.lomka$setLastRenderCamHash(camHash15);
		accessor.lomka$setLastRenderMotionHash(motionHash);
		accessor.lomka$setRenderVisible(visible);
	}

    @Unique
    private static int lomka$adjustIntervalByType(Entity entity, int interval) {
        if (interval <= 1) return interval;
        // Fast path: most entities use default interval
        if (entity instanceof ItemFrame) return interval * 2;
        if (entity instanceof AbstractHurtingProjectile) return Math.max(1, interval / 4);
        if (entity instanceof VehicleEntity || entity instanceof LivingEntity) return Math.max(1, interval / 2);
        return interval;
    }

	@Unique
	private static int lomka$computeMotionHash(Entity entity) {
		int qx = SectionPos.blockToSectionCoord(entity.getX() * 2.0);
		int qy = SectionPos.blockToSectionCoord(entity.getY() * 2.0);
		int qz = SectionPos.blockToSectionCoord(entity.getZ() * 2.0);
		int h = 1;
		h = 31 * h + qx;
		h = 31 * h + qy;
		h = 31 * h + qz;

		boolean isVehicle = entity instanceof VehicleEntity;
		boolean includeRot = entity instanceof LivingEntity || isVehicle;
		boolean includeVel = entity instanceof AbstractHurtingProjectile || isVehicle;

		if (includeRot) {
			int yawQ = Math.round((entity.getYRot() + 180.0f) * 2.0f);
			int pitchQ = Math.round((Math.max(-90.0f, Math.min(90.0f, entity.getXRot())) + 90.0f) * 2.0f);
			h = 31 * h + yawQ;
			h = 31 * h + pitchQ;
		}

		if (includeVel) {
			Vec3 v = entity.getDeltaMovement();
			int vx = SectionPos.blockToSectionCoord(v.x * 20.0);
			int vy = SectionPos.blockToSectionCoord(v.y * 20.0);
			int vz = SectionPos.blockToSectionCoord(v.z * 20.0);
			h = 31 * h + vx;
			h = 31 * h + vy;
			h = 31 * h + vz;
		}

		return h;
	}
}