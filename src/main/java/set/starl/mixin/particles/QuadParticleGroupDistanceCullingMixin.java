package set.starl.mixin.particles;

import java.util.Objects;
import net.minecraft.client.Camera;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.ParticleGroup;
import net.minecraft.client.particle.QuadParticleGroup;
import net.minecraft.client.particle.SingleQuadParticle;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.state.ParticleGroupRenderState;
import net.minecraft.client.renderer.state.QuadParticleRenderState;
import net.minecraft.CrashReport;
import net.minecraft.CrashReportCategory;
import net.minecraft.ReportedException;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.AABB;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(QuadParticleGroup.class)
public class QuadParticleGroupDistanceCullingMixin {
	@Shadow
	@Final
	QuadParticleRenderState particleTypeRenderState;

	@Shadow
	@Final
	private ParticleRenderType particleType;

	@Unique
	private static final boolean LOMKA$CULL_AABB = !"false".equalsIgnoreCase(System.getProperty("lomka.particles.cullAabb", "true"));

	@Unique
	private static final double LOMKA$MAX_DISTANCE = Double.parseDouble(System.getProperty("lomka.particles.maxDistance", "128"));

	@Unique
	private static final double LOMKA$MAX_DIST_SQ = LOMKA$MAX_DISTANCE <= 0.0 ? -1.0 : LOMKA$MAX_DISTANCE * LOMKA$MAX_DISTANCE;

	@Unique
	private static final int LOMKA$MIN_COUNT = Integer.parseInt(System.getProperty("lomka.particles.cullAabb.minCount", "128"));

	@Inject(method = "extractRenderState", at = @At("HEAD"), cancellable = true)
	private void lomka$extractRenderStateCulled(final Frustum frustum, final Camera camera, final float partialTickTime, final CallbackInfoReturnable<ParticleGroupRenderState> cir) {
		if (!LOMKA$CULL_AABB) {
			return;
		}
		if (LOMKA$MAX_DIST_SQ <= 0.0 && ((ParticleGroup)(Object)this).size() < LOMKA$MIN_COUNT) {
			return;
		}

		this.particleTypeRenderState.clear();

		double maxDistSq = LOMKA$MAX_DIST_SQ;
		Vec3 cam = camera.position();
		double camX = cam.x();
		double camY = cam.y();
		double camZ = cam.z();

		for (Object o : ((ParticleGroup)(Object)this).getAll()) {
			SingleQuadParticle particle = (SingleQuadParticle)o;
			AABB bb = particle.getBoundingBox();
			if (bb.hasNaN()) {
				continue;
			}
			AABB cullBox = bb.getSize() == 0.0 ? bb.inflate(0.25) : bb;
			if (!frustum.isVisible(cullBox)) {
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
					continue;
				}
			}

			try {
				particle.extract(this.particleTypeRenderState, camera, partialTickTime);
			} catch (Throwable t) {
				CrashReport report = CrashReport.forThrowable(t, "Rendering Particle");
				CrashReportCategory category = report.addCategory("Particle being rendered");
				Objects.requireNonNull(particle);
				category.setDetail("Particle", particle::toString);
				ParticleRenderType var10002 = this.particleType;
				Objects.requireNonNull(var10002);
				category.setDetail("Particle Type", var10002::toString);
				throw new ReportedException(report);
			}
		}

		cir.setReturnValue(this.particleTypeRenderState);
	}
}
