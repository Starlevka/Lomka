package set.starl.mixin.render;

import it.unimi.dsi.fastutil.ints.Int2LongLinkedOpenHashMap;
import it.unimi.dsi.fastutil.ints.Int2IntLinkedOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.chunk.SectionRenderDispatcher;
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
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import set.starl.bridge.FrustumIntersectionAccess;

@Mixin(EntityRenderer.class)
public abstract class EntityRendererCullingMixin {
	@Unique
	private static final boolean LOMKA$CULL_INVISIBLE = "true".equalsIgnoreCase(System.getProperty("lomka.entities.cullInvisible", "false"));

	@Unique
	private static final boolean LOMKA$FRUSTUM_CULL_ALL = !"false".equalsIgnoreCase(System.getProperty("lomka.entities.frustumCullAll", "true"));

	@Unique
	private static final EquipmentSlot[] LOMKA$EQUIPMENT_SLOTS = EquipmentSlot.values();

	@Unique
	private static final int LOMKA$FRUSTUM_HYSTERESIS_TICKS = Integer.parseInt(System.getProperty("lomka.entities.frustumHysteresisTicks", "0"));

	@Unique
	private static final int LOMKA$FRUSTUM_HYSTERESIS_LEGACY_INTERVAL = LOMKA$FRUSTUM_HYSTERESIS_TICKS > 0 ? (LOMKA$FRUSTUM_HYSTERESIS_TICKS + 1) : 0;

	@Unique
	private static final int LOMKA$FRUSTUM_VISIBLE_RECHECK = Integer.parseInt(System.getProperty(
		"lomka.entities.frustumHysteresis.visibleRecheck",
		String.valueOf(LOMKA$FRUSTUM_HYSTERESIS_LEGACY_INTERVAL)
	));

	@Unique
	private static final int LOMKA$FRUSTUM_INVISIBLE_RECHECK = Integer.parseInt(System.getProperty(
		"lomka.entities.frustumHysteresis.invisibleRecheck",
		String.valueOf(LOMKA$FRUSTUM_HYSTERESIS_LEGACY_INTERVAL)
	));

	@Unique
	private static final boolean LOMKA$FRUSTUM_HYSTERESIS_ENABLE = LOMKA$FRUSTUM_VISIBLE_RECHECK > 1 || LOMKA$FRUSTUM_INVISIBLE_RECHECK > 1;

	@Unique
	private static final int LOMKA$FRUSTUM_HYSTERESIS_MAX_ENTRIES = Integer.parseInt(System.getProperty("lomka.entities.frustumHysteresis.maxEntries", "16384"));

	@Unique
	private static final Int2LongLinkedOpenHashMap LOMKA$LAST_FRUSTUM_STATE_BY_ID = lomka$createLastFrustumStateMap();

	@Unique
	private static final Int2IntLinkedOpenHashMap LOMKA$LAST_MOTION_HASH_BY_ID = lomka$createLastMotionHashMap();

	@Unique
	private static final boolean LOMKA$FRUSTUM_SPHERE_TEST = !"false".equalsIgnoreCase(System.getProperty("lomka.entities.frustumSphereTest", "true"));

	@Unique
	private static final float LOMKA$FRUSTUM_SPHERE_MARGIN = Float.parseFloat(System.getProperty("lomka.entities.frustumSphereMargin", "0.5"));

	@Unique
	private static final boolean LOMKA$FRUSTUM_CHEAP_DISTANCE_REJECT = "true".equalsIgnoreCase(System.getProperty("lomka.entities.frustumCheapDistanceReject", "false"));

	@Unique
	private static final double LOMKA$FRUSTUM_CHEAP_DISTANCE_MARGIN = Double.parseDouble(System.getProperty("lomka.entities.frustumCheapDistanceMargin", "2.0"));

	@Unique
	private static final double LOMKA$FRUSTUM_HYSTERESIS_STATIC_MULTIPLIER = Double.parseDouble(System.getProperty("lomka.entities.frustumHysteresis.staticMultiplier", "2.0"));

	@Unique
	private static final double LOMKA$FRUSTUM_HYSTERESIS_LIVING_MULTIPLIER = Double.parseDouble(System.getProperty("lomka.entities.frustumHysteresis.livingMultiplier", "0.5"));

	@Unique
	private static final double LOMKA$FRUSTUM_HYSTERESIS_DISPLAY_MULTIPLIER = Double.parseDouble(System.getProperty("lomka.entities.frustumHysteresis.displayMultiplier", "4.0"));

	@Unique
	private static final double LOMKA$FRUSTUM_HYSTERESIS_PROJECTILE_MULTIPLIER = Double.parseDouble(System.getProperty("lomka.entities.frustumHysteresis.projectileMultiplier", "0.25"));

	@Unique
	private static final double LOMKA$FRUSTUM_HYSTERESIS_VEHICLE_MULTIPLIER = Double.parseDouble(System.getProperty("lomka.entities.frustumHysteresis.vehicleMultiplier", "0.5"));

	@Unique
	private static final boolean LOMKA$FRUSTUM_VISIBLE_CELL_CACHE = !"false".equalsIgnoreCase(System.getProperty("lomka.entities.frustumVisibleCellCache", "true"));

	@Unique
	private static final boolean LOMKA$SECTION_OCCLUSION_CULL = "true".equalsIgnoreCase(System.getProperty("lomka.entities.sectionOcclusionCull", "false"));

	@Unique
	private static final double LOMKA$SECTION_OCCLUSION_CULL_MIN_DISTANCE = Double.parseDouble(System.getProperty("lomka.entities.sectionOcclusionCullMinDistance", "32.0"));

	@Unique
	private static final LongOpenHashSet LOMKA$VISIBLE_CELLS = new LongOpenHashSet();

	@Unique
	private static long LOMKA$VISIBLE_CELLS_EPOCH = Long.MIN_VALUE;

	@Unique
	private static final LongOpenHashSet LOMKA$OCCLUSION_VISIBLE_SECTIONS = new LongOpenHashSet();

	@Unique
	private static long LOMKA$OCCLUSION_VISIBLE_SECTIONS_EPOCH = Long.MIN_VALUE;

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
		if (!LOMKA$FRUSTUM_CULL_ALL) {
			return;
		}
		if (!cir.getReturnValue().booleanValue()) {
			return;
		}
		if (entity.hasCustomName() && entity.isCustomNameVisible()) {
			return;
		}

		Minecraft mc = Minecraft.getInstance();
		int tick = Integer.MIN_VALUE;
		long camBlockLong = 0L;
		int camHash15 = 0;
		int posHash16 = 0;
		int motionHash = 0;
		long cellKey = Long.MIN_VALUE;
		if (mc.level != null) {
			tick = (int)mc.level.getGameTime();
			int camBx = Mth.floor(camX);
			int camBy = Mth.floor(camY);
			int camBz = Mth.floor(camZ);
			camBlockLong = BlockPos.asLong(camBx, camBy, camBz);
			camHash15 = lomka$hashLongTo32(camBlockLong) & 0x7FFF;

			int ex = Mth.floor(entity.getX());
			int ey = Mth.floor(entity.getY());
			int ez = Mth.floor(entity.getZ());
			long entityBlockLong = BlockPos.asLong(ex, ey, ez);
			posHash16 = lomka$hashLongTo32(entityBlockLong) & 0xFFFF;
			cellKey = SectionPos.asLong(ex >> 4, ey >> 4, ez >> 4);
			motionHash = lomka$computeMotionHash(entity);
		}

		if (LOMKA$SECTION_OCCLUSION_CULL && tick != Integer.MIN_VALUE && cellKey != Long.MIN_VALUE && !(entity instanceof Player)) {
			if (!(entity instanceof Leashable leashable) || leashable.getLeashHolder() == null) {
				double minDist = LOMKA$SECTION_OCCLUSION_CULL_MIN_DISTANCE;
				if (minDist <= 0.0) {
					minDist = 0.0;
				}
				double dx = entity.getX() - camX;
				double dy = entity.getY() - camY;
				double dz = entity.getZ() - camZ;
				if (minDist == 0.0 || dx * dx + dy * dy + dz * dz >= minDist * minDist) {
					long epoch = lomka$computeOcclusionEpoch(mc, camBlockLong);
					if (epoch != LOMKA$OCCLUSION_VISIBLE_SECTIONS_EPOCH) {
						LOMKA$OCCLUSION_VISIBLE_SECTIONS_EPOCH = epoch;
						LOMKA$OCCLUSION_VISIBLE_SECTIONS.clear();
						ObjectArrayList sections = mc.levelRenderer.getVisibleSections();
						for (int i = 0, s = sections.size(); i < s; i++) {
							SectionRenderDispatcher.RenderSection rs = (SectionRenderDispatcher.RenderSection)sections.get(i);
							LOMKA$OCCLUSION_VISIBLE_SECTIONS.add(rs.getSectionNode());
						}
					}
					if (!LOMKA$OCCLUSION_VISIBLE_SECTIONS.isEmpty() && !LOMKA$OCCLUSION_VISIBLE_SECTIONS.contains(cellKey)) {
						lomka$recordFrustumDecision(entity, tick, camHash15, posHash16, motionHash, false);
						cir.setReturnValue(Boolean.FALSE);
						return;
					}
				}
			}
		}

		if (LOMKA$FRUSTUM_VISIBLE_CELL_CACHE && tick != Integer.MIN_VALUE) {
			long epoch = (((long)tick) << 32) ^ camBlockLong;
			if (epoch != LOMKA$VISIBLE_CELLS_EPOCH) {
				LOMKA$VISIBLE_CELLS_EPOCH = epoch;
				LOMKA$VISIBLE_CELLS.clear();
			}
			if (cellKey != Long.MIN_VALUE && LOMKA$VISIBLE_CELLS.contains(cellKey)) {
				lomka$recordFrustumDecision(entity, tick, camHash15, posHash16, motionHash, true);
				return;
			}
		}

		if (LOMKA$FRUSTUM_HYSTERESIS_ENABLE && tick != Integer.MIN_VALUE) {
			long state = LOMKA$LAST_FRUSTUM_STATE_BY_ID.get(entity.getId());
			if (state != Long.MIN_VALUE) {
				int lastTick = (int)(state >>> 1);
				boolean lastVisible = (state & 1L) != 0L;
				int interval = lastVisible ? LOMKA$FRUSTUM_VISIBLE_RECHECK : LOMKA$FRUSTUM_INVISIBLE_RECHECK;
				interval = lomka$adjustIntervalByType(entity, interval);
				if (interval > 1 && tick - lastTick < interval) {
					int lastCamHash15 = (int)((state >>> 33) & 0x7FFFL);
					int lastPosHash16 = (int)((state >>> 48) & 0xFFFFL);
					if (lastCamHash15 == camHash15 && lastPosHash16 == posHash16) {
						int lastMotionHash = LOMKA$LAST_MOTION_HASH_BY_ID.get(entity.getId());
						if (lastMotionHash == Integer.MIN_VALUE || lastMotionHash == motionHash) {
							if (!lastVisible) {
								cir.setReturnValue(Boolean.FALSE);
							}
							return;
						}
					}
				}
			}
		}

		AABB baseBox = this.getBoundingBoxForCulling(entity);
		if (baseBox.hasNaN() || baseBox.getSize() == (double)0.0F) {
			baseBox = new AABB(entity.getX() - (double)2.0F, entity.getY() - (double)2.0F, entity.getZ() - (double)2.0F, entity.getX() + (double)2.0F, entity.getY() + (double)2.0F, entity.getZ() + (double)2.0F);
		}

		double cx = (baseBox.minX + baseBox.maxX) * 0.5;
		double cy = (baseBox.minY + baseBox.maxY) * 0.5;
		double cz = (baseBox.minZ + baseBox.maxZ) * 0.5;

		double bdx = (baseBox.maxX - baseBox.minX) + 1.0;
		double bdy = (baseBox.maxY - baseBox.minY) + 1.0;
		double bdz = (baseBox.maxZ - baseBox.minZ) + 1.0;
		double baseRadius = 0.5 * Math.sqrt(bdx * bdx + bdy * bdy + bdz * bdz);

		AABB boundingBox = null;
		if (LOMKA$FRUSTUM_CHEAP_DISTANCE_REJECT) {
			double entityRenderDistance = (double)Minecraft.getInstance().options.getEffectiveRenderDistance() * 16.0 * Entity.getViewScale();
			double max = entityRenderDistance + baseRadius + LOMKA$FRUSTUM_CHEAP_DISTANCE_MARGIN;
			double ddx = cx - camX;
			double ddy = cy - camY;
			double ddz = cz - camZ;
			if (ddx * ddx + ddy * ddy + ddz * ddz > max * max) {
				if (!(entity instanceof Leashable leashable) || leashable.getLeashHolder() == null) {
					lomka$recordFrustumDecision(entity, tick, camHash15, posHash16, motionHash, false);
					cir.setReturnValue(Boolean.FALSE);
					return;
				}
				boundingBox = baseBox.inflate((double)0.5F);
			}
		}

		if (LOMKA$FRUSTUM_SPHERE_TEST && culler instanceof FrustumIntersectionAccess access) {
			float radius = (float)baseRadius + LOMKA$FRUSTUM_SPHERE_MARGIN;
			float rx = (float)(cx - culler.getCamX());
			float ry = (float)(cy - culler.getCamY());
			float rz = (float)(cz - culler.getCamZ());
			if (!access.lomka$getIntersection().testSphere(rx, ry, rz, radius)) {
				if (!(entity instanceof Leashable leashable) || leashable.getLeashHolder() == null) {
					lomka$recordFrustumDecision(entity, tick, camHash15, posHash16, motionHash, false);
					cir.setReturnValue(Boolean.FALSE);
					return;
				}
				if (boundingBox == null) {
					boundingBox = baseBox.inflate((double)0.5F);
				}
			}
		}

		if (boundingBox == null) {
			if (culler.isVisible(baseBox)) {
				if (LOMKA$FRUSTUM_VISIBLE_CELL_CACHE && cellKey != Long.MIN_VALUE) {
					LOMKA$VISIBLE_CELLS.add(cellKey);
				}
				lomka$recordFrustumDecision(entity, tick, camHash15, posHash16, motionHash, true);
				return;
			}

			boundingBox = baseBox.inflate((double)0.5F);
			if (culler.isVisible(boundingBox)) {
				if (LOMKA$FRUSTUM_VISIBLE_CELL_CACHE && cellKey != Long.MIN_VALUE) {
					LOMKA$VISIBLE_CELLS.add(cellKey);
				}
				lomka$recordFrustumDecision(entity, tick, camHash15, posHash16, motionHash, true);
				return;
			}
		}

		if (entity instanceof Leashable leashable) {
			Entity leashHolder = leashable.getLeashHolder();
			if (leashHolder != null) {
				AABB leasherBase = leashHolder.getBoundingBox();
				if (leasherBase.hasNaN() || leasherBase.getSize() == (double)0.0F) {
					leasherBase = new AABB(leashHolder.getX() - (double)2.0F, leashHolder.getY() - (double)2.0F, leashHolder.getZ() - (double)2.0F, leashHolder.getX() + (double)2.0F, leashHolder.getY() + (double)2.0F, leashHolder.getZ() + (double)2.0F);
				}
				if (culler.isVisible(leasherBase)) {
					if (LOMKA$FRUSTUM_VISIBLE_CELL_CACHE && cellKey != Long.MIN_VALUE) {
						LOMKA$VISIBLE_CELLS.add(cellKey);
					}
					lomka$recordFrustumDecision(entity, tick, camHash15, posHash16, motionHash, true);
					return;
				}
				AABB leasherBox = new AABB(
					leasherBase.minX - (double)0.5F,
					leasherBase.minY - (double)0.5F,
					leasherBase.minZ - (double)0.5F,
					leasherBase.maxX + (double)0.5F,
					leasherBase.maxY + (double)0.5F,
					leasherBase.maxZ + (double)0.5F
				);
				if (culler.isVisible(leasherBox) || culler.isVisible(boundingBox.minmax(leasherBox))) {
					if (LOMKA$FRUSTUM_VISIBLE_CELL_CACHE && cellKey != Long.MIN_VALUE) {
						LOMKA$VISIBLE_CELLS.add(cellKey);
					}
					lomka$recordFrustumDecision(entity, tick, camHash15, posHash16, motionHash, true);
					return;
				}
			}
		}

		lomka$recordFrustumDecision(entity, tick, camHash15, posHash16, motionHash, false);
		cir.setReturnValue(Boolean.FALSE);
	}

	@Unique
	private static Int2LongLinkedOpenHashMap lomka$createLastFrustumStateMap() {
		Int2LongLinkedOpenHashMap m = new Int2LongLinkedOpenHashMap();
		m.defaultReturnValue(Long.MIN_VALUE);
		return m;
	}

	@Unique
	private static Int2IntLinkedOpenHashMap lomka$createLastMotionHashMap() {
		Int2IntLinkedOpenHashMap m = new Int2IntLinkedOpenHashMap();
		m.defaultReturnValue(Integer.MIN_VALUE);
		return m;
	}

	@Unique
	private static void lomka$recordFrustumDecision(final Entity entity, final int tick, final int camHash15, final int posHash16, final int motionHash, final boolean visible) {
		if (!LOMKA$FRUSTUM_HYSTERESIS_ENABLE) {
			return;
		}
		if (tick == Integer.MIN_VALUE) {
			return;
		}
		int max = LOMKA$FRUSTUM_HYSTERESIS_MAX_ENTRIES;
		if (max <= 0) {
			return;
		}

		Int2LongLinkedOpenHashMap m = LOMKA$LAST_FRUSTUM_STATE_BY_ID;
		Int2IntLinkedOpenHashMap mh = LOMKA$LAST_MOTION_HASH_BY_ID;
		int id = entity.getId();
		m.remove(id);
		mh.remove(id);
		long state = (((long)tick) << 1) | (visible ? 1L : 0L);
		state |= ((long)camHash15 & 0x7FFFL) << 33;
		state |= ((long)posHash16 & 0xFFFFL) << 48;
		m.put(id, state);
		mh.put(id, motionHash);
		while (m.size() > max) {
			var it = m.keySet().iterator();
			if (!it.hasNext()) {
				break;
			}
			int evictedId = it.nextInt();
			it.remove();
			mh.remove(evictedId);
		}
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
		int qx = Mth.floor(entity.getX() * 4.0);
		int qy = Mth.floor(entity.getY() * 4.0);
		int qz = Mth.floor(entity.getZ() * 4.0);
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

	@Unique
	private static long lomka$computeOcclusionEpoch(final Minecraft mc, final long camBlockLong) {
		int yawQ = 0;
		int pitchQ = 0;
		if (mc.gameRenderer != null) {
			var cam = mc.gameRenderer.getMainCamera();
			yawQ = Mth.floor((Mth.wrapDegrees(cam.yRot()) + 180.0F) * 2.0F);
			pitchQ = Mth.floor((Mth.wrapDegrees(cam.xRot()) + 180.0F) * 2.0F);
		}
		return ((long)yawQ & 0xFFFFFFFFL) ^ (((long)pitchQ & 0xFFFFFFFFL) << 32) ^ (camBlockLong * 0x9E3779B97F4A7C15L);
	}

	@Unique
	private static int lomka$hashLongTo32(final long v) {
		int h = (int)v ^ (int)(v >>> 32);
		h ^= h >>> 16;
		h *= 0x7feb352d;
		h ^= h >>> 15;
		h *= 0x846ca68b;
		h ^= h >>> 16;
		return h;
	}
}
