package set.starl.injection.mixins.accessors;

import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.chunk.SectionRenderDispatcher;
import net.minecraft.server.level.BlockDestructionProgress;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import set.starl.injection.access.LevelRendererAccess;

import java.util.SortedSet;

@Mixin(LevelRenderer.class)
public abstract class LevelRendererAccessor implements LevelRendererAccess {
	@Shadow
	private ObjectArrayList<SectionRenderDispatcher.RenderSection> visibleSections;

	@Shadow
	private Long2ObjectMap<SortedSet<BlockDestructionProgress>> destructionProgress;

	@Shadow
	private net.minecraft.client.renderer.culling.Frustum capturedFrustum;

	@Override
	public ObjectArrayList<SectionRenderDispatcher.RenderSection> lomka$getVisibleSections() {
		return this.visibleSections;
	}

	@Override
	public Long2ObjectMap<SortedSet<BlockDestructionProgress>> lomka$getDestructionProgress() {
		return this.destructionProgress;
	}

	@Override
	public net.minecraft.client.renderer.culling.Frustum lomka$getLastCullFrustum() {
		return this.capturedFrustum;
	}

	@Override
	public net.minecraft.client.renderer.culling.Frustum lomka$getLastAppliedFrustum() {
		return this.capturedFrustum;
	}
}
