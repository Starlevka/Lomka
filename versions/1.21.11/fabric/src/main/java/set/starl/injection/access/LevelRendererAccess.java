package set.starl.injection.access;

import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.client.renderer.chunk.SectionRenderDispatcher;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.server.level.BlockDestructionProgress;
import org.jspecify.annotations.Nullable;

import java.util.SortedSet;

public interface LevelRendererAccess {
	@Nullable
	Frustum lomka$getLastCullFrustum();

	@Nullable
	Frustum lomka$getLastAppliedFrustum();

	ObjectArrayList<SectionRenderDispatcher.RenderSection> lomka$getVisibleSections();

	Long2ObjectMap<SortedSet<BlockDestructionProgress>> lomka$getDestructionProgress();
}
