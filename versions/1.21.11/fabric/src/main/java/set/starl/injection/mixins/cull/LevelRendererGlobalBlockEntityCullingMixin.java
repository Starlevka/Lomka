package set.starl.injection.mixins.cull;

import com.mojang.blaze3d.vertex.PoseStack;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import net.minecraft.client.Camera;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderDispatcher;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.state.LevelRenderState;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.BlockDestructionProgress;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import set.starl.injection.access.LevelRendererAccess;

import java.util.Iterator;
import java.util.SortedSet;

@Mixin(LevelRenderer.class)
public abstract class LevelRendererGlobalBlockEntityCullingMixin {
	@Shadow
	@Final
	private BlockEntityRenderDispatcher blockEntityRenderDispatcher;

	@Shadow
	@Nullable
	private ClientLevel level;

	@Unique
	private static final boolean LOMKA$CULL_GLOBAL_BLOCK_ENTITIES = set.starl.Lomka.CONFIG.culling.enable && set.starl.Lomka.CONFIG.culling.cullGlobalBlockEntities;

	@Unique
	private static final ThreadLocal<Frustum> LOMKA$CAPTURED_FRUSTUM = new ThreadLocal<>();

	@Inject(
		method = "extractVisibleEntities",
		at = @At("HEAD")
	)
	private void lomka$captureFrustum(Camera camera, Frustum frustum, DeltaTracker deltaTracker, LevelRenderState levelRenderState, CallbackInfo ci) {
		if (LOMKA$CULL_GLOBAL_BLOCK_ENTITIES) {
			LOMKA$CAPTURED_FRUSTUM.set(frustum);
		}
	}

	@Inject(
		method = "extractVisibleBlockEntities",
		at = @At("HEAD"),
		cancellable = true
	)
	private void lomka$cullGlobalBlockEntities(Camera camera, float deltaPartialTick, LevelRenderState levelRenderState, CallbackInfo ci) {
		if (!LOMKA$CULL_GLOBAL_BLOCK_ENTITIES || this.level == null) {
			return;
		}

		Frustum frustum = LOMKA$CAPTURED_FRUSTUM.get();
		if (frustum == null) {
			return;
		}

		ci.cancel();

		Vec3 cameraPos = camera.position();
		double camX = cameraPos.x();
		double camY = cameraPos.y();
		double camZ = cameraPos.z();
		PoseStack poseStack = new PoseStack();

		LevelRendererAccess access = (LevelRendererAccess) this;
		Long2ObjectMap<SortedSet<BlockDestructionProgress>> destructionProgress = access.lomka$getDestructionProgress();
		Iterator<?> iterator = access.lomka$getVisibleSections().iterator();

		while (iterator.hasNext()) {
			net.minecraft.client.renderer.chunk.SectionRenderDispatcher.RenderSection section = (net.minecraft.client.renderer.chunk.SectionRenderDispatcher.RenderSection) iterator.next();
			java.util.List<BlockEntity> renderableBlockEntities = section.getSectionMesh().getRenderableBlockEntities();
			if (!renderableBlockEntities.isEmpty() && !(section.getVisibility(net.minecraft.util.Util.getMillis()) < 0.3F)) {
				for (BlockEntity be : renderableBlockEntities) {
					net.minecraft.client.renderer.blockentity.BlockEntityRenderer<?, ?> renderer = this.blockEntityRenderDispatcher.getRenderer(be);
					if (renderer == null || renderer.shouldRenderOffScreen() || frustum.isVisible(new AABB(be.getBlockPos()))) {
						this.lomka$extractBE(be, deltaPartialTick, levelRenderState, destructionProgress, poseStack, camX, camY, camZ);
					}
				}
			}
		}

		LOMKA$CAPTURED_FRUSTUM.remove();
	}

	@Unique
	private void lomka$extractBE(BlockEntity be, float delta, LevelRenderState state, Long2ObjectMap<SortedSet<BlockDestructionProgress>> destructionProgress, PoseStack poseStack, double camX, double camY, double camZ) {
		BlockPos pos = be.getBlockPos();
		SortedSet<BlockDestructionProgress> progresses = destructionProgress.get(pos.asLong());
		ModelFeatureRenderer.CrumblingOverlay breakProgress;
		if (progresses != null && !progresses.isEmpty()) {
			poseStack.pushPose();
			poseStack.translate((double)pos.getX() - camX, (double)pos.getY() - camY, (double)pos.getZ() - camZ);
			breakProgress = new ModelFeatureRenderer.CrumblingOverlay(((BlockDestructionProgress)progresses.last()).getProgress(), poseStack.last());
			poseStack.popPose();
		} else {
			breakProgress = null;
		}

		BlockEntityRenderState beState = this.blockEntityRenderDispatcher.tryExtractRenderState(be, delta, breakProgress);
		if (beState != null) {
			state.blockEntityRenderStates.add(beState);
		}
	}
}
