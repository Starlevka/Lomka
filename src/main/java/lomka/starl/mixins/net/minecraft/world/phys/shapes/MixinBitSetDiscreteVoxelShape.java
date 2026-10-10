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
import org.spongepowered.asm.mixin.Unique;

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

    /*
     * Writes the accumulated contiguous bit run out. BitSet.set(from, to) edits the backing
     * long words directly and never routes through the single-bit set(int), so a BitSet subclass
     * that keeps derived state in an overridden set(int) desynchronises the moment the range
     * overload is used: Palladium's BitSetDiscreteVoxelShape mixin swaps the storage for a
     * FastBitSet whose get(int) is the sole reader of a LongOpenHashSet mirror that only
     * set(int)/clear(int) maintain, so every cell of a shape built here reports empty while its
     * bounds stay correct - which silently empties collision shapes, ray hits and entity reach
     * queries for anything built through Shapes.join (bed shapes, stairs, slabs, ...). The run
     * detector is therefore only enabled for a stock java.util.BitSet storage; any other
     * implementation falls back to vanilla's per-bit set(int).
     */
    @Unique
    private static void lomka$flushRun(BitSet storage, int[] run, boolean bulkRuns) {
        if (bulkRuns) {
            storage.set(run[0], run[1] + 1);
        } else {
            for (int bit = run[0]; bit <= run[1]; bit++) {
                storage.set(bit);
            }
        }
    }

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
     *         Additionally, the innermost loop writes bit indices one BitSet.set() call at
     *         a time even though the merged index (third consumer arg) advances
     *         sequentially over 0..size-1 for every IndexMerger implementation, and the bit
     *         layout ((x*ySize + y)*zSize + z) makes consecutive z indices map to
     *         consecutive bit indices — so the writes form contiguous runs. Runs are
     *         accumulated in a hoisted int[2] scratch and flushed with a single
     *         BitSet.set(from, to) bulk word write instead of N per-bit calls; the run
     *         detector falls back to smaller runs transparently for any merger that ever
     *         yields non-consecutive result indices. Vanilla already hoists the int[]
     *         bounds accumulator (aint) the exact same way, and its own correctness
     *         already relies on forMergedIndexes running strictly sequentially (its
     *         shared-array min/max mutation would be racy otherwise) — this extends that
     *         same pre-existing assumption to the boolean flags and the run scratch rather
     *         than introducing a new one. The bulk write is restricted to a stock
     *         java.util.BitSet storage - see lomka$flushRun for why that guard is required.
     */
    @Overwrite
    public static BitSetDiscreteVoxelShape join(DiscreteVoxelShape discretevoxelshape, DiscreteVoxelShape discretevoxelshape1, IndexMerger indexmerger, IndexMerger indexmerger1, IndexMerger indexmerger2, BooleanOp booleanop) {
        //? if >=1.18 {
        int xSize = indexmerger.size() - 1;
        int ySize = indexmerger1.size() - 1;
        int zSize = indexmerger2.size() - 1;
        //? } else {
        /*int xSize = indexmerger.getList().size() - 1;
        int ySize = indexmerger1.getList().size() - 1;
        int zSize = indexmerger2.getList().size() - 1;
        *///?}
        BitSetDiscreteVoxelShape bitsetdiscretevoxelshape = new BitSetDiscreteVoxelShape(xSize, ySize, zSize);
        IBitSetDiscreteVoxelShape IBit = (IBitSetDiscreteVoxelShape) (Object) bitsetdiscretevoxelshape;
        BitSet storage = IBit.lomka$storage();
        boolean bulkRuns = storage.getClass() == BitSet.class;
        int[] aint = new int[]{Integer.MAX_VALUE, Integer.MAX_VALUE, Integer.MAX_VALUE, Integer.MIN_VALUE, Integer.MIN_VALUE, Integer.MIN_VALUE};
        int[] run  = new int[2];
        boolean[] aboolean  = new boolean[1];
        boolean[] aboolean1 = new boolean[1];

        indexmerger.forMergedIndexes((i, j, k) -> {
            aboolean[0] = false;

            indexmerger1.forMergedIndexes((l, i1, j1) -> {
                aboolean1[0] = false;
                run[0] = -1;

                indexmerger2.forMergedIndexes((k1, l1, i2) -> {
                    if (booleanop.apply(discretevoxelshape.isFullWide(i, l, k1), discretevoxelshape1.isFullWide(j, i1, l1))) {
                        int bitIndex = IBit.lomka$index(k, j1, i2);

                        if (run[0] == -1) {
                            run[0] = bitIndex;
                        } else if (bitIndex != run[1] + 1) {
                            lomka$flushRun(storage, run, bulkRuns);
                            run[0] = bitIndex;
                        }

                        run[1] = bitIndex;
                        aint[2] = Math.min(aint[2], i2);
                        aint[5] = Math.max(aint[5], i2);
                        aboolean1[0] = true;
                    }
                    return true;
                });
                if (run[0] != -1) {
                    lomka$flushRun(storage, run, bulkRuns);
                    run[0] = -1;
                }
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