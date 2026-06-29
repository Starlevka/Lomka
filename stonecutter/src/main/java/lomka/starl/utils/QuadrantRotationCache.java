package lomka.starl.utils;

import com.mojang.math.OctahedralGroup;
import com.mojang.math.Quadrant;

/**
 * Pre-computes all possible OctahedralGroup compositions for Quadrant
 * rotation combinations into flat arrays, avoiding repeated matrix
 * composition during model rendering.
 */
public final class QuadrantRotationCache {
    private static final OctahedralGroup[] XY_CACHE = new OctahedralGroup[16];
    private static final OctahedralGroup[] XYZ_CACHE = new OctahedralGroup[64];

    static {
        Quadrant[] quadrants = Quadrant.values();
        for (int x = 0; x < 4; x++) {
            for (int y = 0; y < 4; y++) {
                OctahedralGroup xyGroup = quadrants[y].rotationY.compose(quadrants[x].rotationX);
                XY_CACHE[(x << 2) | y] = xyGroup;
                for (int z = 0; z < 4; z++) {
                    XYZ_CACHE[(x << 4) | (y << 2) | z] = quadrants[z].rotationZ.compose(xyGroup);
                }
            }
        }
    }

    public static OctahedralGroup getXY(int index) {
        return XY_CACHE[index];
    }

    public static OctahedralGroup getXYZ(int index) {
        return XYZ_CACHE[index];
    }

    private QuadrantRotationCache() {}
}
