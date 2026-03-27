package set.starl.injection.mixins.accessors;

import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.chunk.SectionRenderDispatcher;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.server.level.BlockDestructionProgress;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import set.starl.injection.access.LevelRendererAccess;

import java.util.SortedSet;

@Mixin(LevelRenderer.class)
public abstract class LevelRendererAccessor implements LevelRendererAccess {
	@Shadow
	private ObjectArrayList<SectionRenderDispatcher.RenderSection> visibleSections;

	@Shadow
	private Long2ObjectMap<SortedSet<BlockDestructionProgress>> destructionProgress;

	@Shadow
	private @Nullable Frustum capturedFrustum;

	/**
	 * Stores the real frustum used for culling. Unlike {@code capturedFrustum}
	 * (which is debug-only and null in normal gameplay), this field is populated
	 * every time {@code applyFrustum} runs.
	 */
	@Unique
	private @Nullable Frustum lomka$lastAppliedFrustum;

	/**
	 * Capture the frustum that is actually used for section culling.
	 * In vanilla, {@code applyFrustum(Frustum)} is called from {@code cullTerrain}
	 * with the offset frustum. This is the real frustum used for visibility.
	 */
	@Inject(method = "applyFrustum", at = @At("HEAD"))
	private void lomka$captureAppliedFrustum(Frustum frustum, CallbackInfo ci) {
		this.lomka$lastAppliedFrustum = frustum;
	}

	@Override
	public ObjectArrayList<SectionRenderDispatcher.RenderSection> lomka$getVisibleSections() {
		return this.visibleSections;
	}

	@Override
	public Long2ObjectMap<SortedSet<BlockDestructionProgress>> lomka$getDestructionProgress() {
		return this.destructionProgress;
	}

	@Override
	public @Nullable Frustum lomka$getLastCullFrustum() {
		// Return the real frustum if available, fall back to debug frustum
		Frustum applied = this.lomka$lastAppliedFrustum;
		return applied != null ? applied : this.capturedFrustum;
	}

	@Override
	public @Nullable Frustum lomka$getLastAppliedFrustum() {
		Frustum applied = this.lomka$lastAppliedFrustum;
		return applied != null ? applied : this.capturedFrustum;
	}
}
