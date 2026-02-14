package set.starl;

import net.minecraft.client.renderer.culling.Frustum;
import org.jspecify.annotations.Nullable;

public interface ILevelRenderer {
	@Nullable
	Frustum lomka$getLastCullFrustum();

	@Nullable
	Frustum lomka$getLastAppliedFrustum();
}

