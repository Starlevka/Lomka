/*
 * This file is part of Lomka (https://github.com/Starlevka/Lomka)
 * Copyright (C) 2026 Starlev (a.k.a. Starlevka) and contributors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, version 3 of the License only.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 *
 * SPDX-License-Identifier: LGPL-3.0-only
 */

package lomka.starl.mixins.net.minecraft.world.phys.shapes;

import java.util.BitSet;
import lomka.starl.duck.IBitSetDiscreteVoxelShape;
import net.minecraft.world.phys.shapes.BitSetDiscreteVoxelShape;
import net.minecraft.world.phys.shapes.BooleanOp;
import net.minecraft.world.phys.shapes.DiscreteVoxelShape;
import net.minecraft.world.phys.shapes.IndexMerger;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(BitSetDiscreteVoxelShape.class)
public abstract class MixinBitSetDiscreteVoxelShape implements IBitSetDiscreteVoxelShape {

    @Shadow @Final private BitSet storage;
    @Shadow private int xMin;
    @Shadow private int yMin;
    @Shadow private int zMin;
    @Shadow private int xMax;
    @Shadow private int yMax;
    @Shadow private int zMax;

    @Shadow protected abstract int getIndex(int i, int j, int k);

    @Override public BitSet lomka$storage() { return this.storage; }
    @Override public int lomka$index(int x, int y, int z) { return this.getIndex(x, y, z); }

    @Override
    public void lomka$setBounds(int xMin, int yMin, int zMin, int xMax, int yMax, int zMax) {
        this.xMin = xMin; this.yMin = yMin; this.zMin = zMin;
        this.xMax = xMax; this.yMax = yMax; this.zMax = zMax;
    }

    /**
     * @author Starlev
     * @reason Hoist the boolean[] mutable-capture wrappers outside the nested
     *         forMergedIndexes loops and reset their [0] slot per iteration instead of
     *         allocating a fresh boolean[1] on every X iteration and every (X,Y) pair.
     *         Vanilla already hoists the int[] bounds accumulator (aint) the exact same
     *         way, and its own correctness already relies on forMergedIndexes running
     *         strictly sequentially (its shared-array min/max mutation would be racy
     *         otherwise) — this extends that same pre-existing assumption to the boolean
     *         flags rather than introducing a new one.
     */
    @Overwrite
    static BitSetDiscreteVoxelShape join(DiscreteVoxelShape discretevoxelshape, DiscreteVoxelShape discretevoxelshape1, IndexMerger indexmerger, IndexMerger indexmerger1, IndexMerger indexmerger2, BooleanOp booleanop) {
        BitSetDiscreteVoxelShape bitsetdiscretevoxelshape = new BitSetDiscreteVoxelShape(indexmerger.size() - 1, indexmerger1.size() - 1, indexmerger2.size() - 1);
        IBitSetDiscreteVoxelShape IBit = (IBitSetDiscreteVoxelShape) (Object) bitsetdiscretevoxelshape;
        int[] aint = new int[]{Integer.MAX_VALUE, Integer.MAX_VALUE, Integer.MAX_VALUE, Integer.MIN_VALUE, Integer.MIN_VALUE, Integer.MIN_VALUE};
        boolean[] aboolean  = new boolean[1];
        boolean[] aboolean1 = new boolean[1];

        indexmerger.forMergedIndexes((i, j, k) -> {
            aboolean[0] = false;

            indexmerger1.forMergedIndexes((l, i1, j1) -> {
                aboolean1[0] = false;

                indexmerger2.forMergedIndexes((k1, l1, i2) -> {
                    if (booleanop.apply(discretevoxelshape.isFullWide(i, l, k1), discretevoxelshape1.isFullWide(j, i1, l1))) {
                        IBit.lomka$storage().set(IBit.lomka$index(k, j1, i2));
                        aint[2] = Math.min(aint[2], i2);
                        aint[5] = Math.max(aint[5], i2);
                        aboolean1[0] = true;
                    }
                    return true;
                });
                if (aboolean1[0]) {
                    aint[1] = Math.min(aint[1], j1);
                    aint[4] = Math.max(aint[4], j1);
                    aboolean[0] = true;
                }
                return true;
            });
            if (aboolean[0]) {
                aint[0] = Math.min(aint[0], k);
                aint[3] = Math.max(aint[3], k);
            }
            return true;
        });

        IBit.lomka$setBounds(aint[0], aint[1], aint[2], aint[3] + 1, aint[4] + 1, aint[5] + 1);
        return bitsetdiscretevoxelshape;
    }
}