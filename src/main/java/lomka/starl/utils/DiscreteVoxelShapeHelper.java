package lomka.starl.utils;

import java.util.BitSet;

public interface DiscreteVoxelShapeHelper {
    BitSet lomka$storage();
    int lomka$index(int x, int y, int z);
    void lomka$setBounds(int xMin, int yMin, int zMin, int xMax, int yMax, int zMax);
}