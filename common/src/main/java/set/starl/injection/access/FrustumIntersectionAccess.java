package set.starl.injection.access;

import org.joml.FrustumIntersection;

public interface FrustumIntersectionAccess {
	org.joml.FrustumIntersection lomka$getIntersection();

	boolean lomka$testAab(float minX, float minY, float minZ, float maxX, float maxY, float maxZ);
}
