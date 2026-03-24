package set.starl.util;

import net.minecraft.client.renderer.culling.Frustum;

public record CullingContext(
	long gameTime,
	double camX,
	double camY,
	double camZ,
	int camChunkX,
	int camChunkZ,
	Frustum frustum
) {
}
