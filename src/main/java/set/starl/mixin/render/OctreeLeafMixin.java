package set.starl.mixin.render;

import net.minecraft.client.renderer.Octree;
import net.minecraft.client.renderer.chunk.SectionRenderDispatcher;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.world.phys.AABB;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(targets = "net.minecraft.client.renderer.Octree$Leaf")
public class OctreeLeafMixin {
	@Shadow
	@Final
	private SectionRenderDispatcher.RenderSection section;

	@Inject(method = "visitNodes", at = @At("HEAD"), cancellable = true)
	private void lomka$visitNodesFast(
		final Octree.OctreeVisitor visitor,
		final boolean skipFrustumCheck,
		final Frustum frustum,
		final int depth,
		final int closeDistance,
		final boolean isClose,
		final CallbackInfo ci
	) {
		AABB boundingBox = this.section.getBoundingBox();
		if (skipFrustumCheck || frustum.isVisible(boundingBox)) {
			boolean close = isClose;
			if (close) {
				double cameraX = frustum.getCamX();
				double cameraY = frustum.getCamY();
				double cameraZ = frustum.getCamZ();
				close = cameraX > boundingBox.minX - (double)closeDistance
					&& cameraX < boundingBox.maxX + (double)closeDistance
					&& cameraY > boundingBox.minY - (double)closeDistance
					&& cameraY < boundingBox.maxY + (double)closeDistance
					&& cameraZ > boundingBox.minZ - (double)closeDistance
					&& cameraZ < boundingBox.maxZ + (double)closeDistance;
			}

			visitor.visit((Octree.Node)(Object)this, skipFrustumCheck, depth, close);
		}

		ci.cancel();
	}
}
