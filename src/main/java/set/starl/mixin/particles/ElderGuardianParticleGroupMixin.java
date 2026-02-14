package set.starl.mixin.particles;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.Queue;
import net.minecraft.client.Camera;
import net.minecraft.client.particle.ElderGuardianParticleGroup;
import net.minecraft.client.particle.ParticleGroup;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.world.phys.AABB;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ElderGuardianParticleGroup.class)
public class ElderGuardianParticleGroupMixin {
	@Unique
	private static final ThreadLocal<ArrayList<Particle>> LOMKA$REMOVED = ThreadLocal.withInitial(ArrayList::new);

	@Unique
	private static final double LOMKA$MAX_DISTANCE = Double.parseDouble(System.getProperty("lomka.particles.maxDistance", "128"));

	@Unique
	private static final double LOMKA$MAX_DIST_SQ = LOMKA$MAX_DISTANCE <= 0.0 ? -1.0 : LOMKA$MAX_DISTANCE * LOMKA$MAX_DISTANCE;

	@Unique
	private static final int LOMKA$MIN_COUNT = Integer.parseInt(System.getProperty("lomka.particles.groupFilter.minCount", "64"));

	@Inject(method = "extractRenderState", at = @At("HEAD"))
	private void lomka$filterForRender(final Frustum frustum, final Camera camera, final float partialTickTime, final CallbackInfoReturnable<?> cir) {
		Queue particles = ((ParticleGroup)(Object)this).getAll();
		if (particles.size() < LOMKA$MIN_COUNT) {
			return;
		}
		ArrayList<Particle> removed = LOMKA$REMOVED.get();
		removed.clear();

		double maxDistSq = LOMKA$MAX_DIST_SQ;
		double camX = camera.position().x();
		double camY = camera.position().y();
		double camZ = camera.position().z();

		Iterator it = particles.iterator();
		while (it.hasNext()) {
			Particle p = (Particle)it.next();
			AABB bb = p.getBoundingBox();
			if (bb.hasNaN() || bb.getSize() == 0.0) {
				continue;
			}

			if (!frustum.isVisible(bb)) {
				it.remove();
				removed.add(p);
				continue;
			}

			if (maxDistSq > 0.0) {
				double cx = (bb.minX + bb.maxX) * 0.5;
				double cy = (bb.minY + bb.maxY) * 0.5;
				double cz = (bb.minZ + bb.maxZ) * 0.5;
				double dx = cx - camX;
				double dy = cy - camY;
				double dz = cz - camZ;
				if (dx * dx + dy * dy + dz * dz > maxDistSq) {
					it.remove();
					removed.add(p);
				}
			}
		}
	}

	@Inject(method = "extractRenderState", at = @At("RETURN"))
	private void lomka$restoreAfterRender(final Frustum frustum, final Camera camera, final float partialTickTime, final CallbackInfoReturnable<?> cir) {
		Queue particles = ((ParticleGroup)(Object)this).getAll();
		ArrayList<Particle> removed = LOMKA$REMOVED.get();
		particles.addAll(removed);
		removed.clear();
	}
}
