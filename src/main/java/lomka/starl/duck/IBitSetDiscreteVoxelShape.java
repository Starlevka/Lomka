package lomka.starl.duck;

import java.util.BitSet;

public interface IBitSetDiscreteVoxelShape {
    BitSet lomka$storage();
    int lomka$index(int x, int y, int z);
    void lomka$setBounds(int xMin, int yMin, int zMin, int xMax, int yMax, int zMax);
}