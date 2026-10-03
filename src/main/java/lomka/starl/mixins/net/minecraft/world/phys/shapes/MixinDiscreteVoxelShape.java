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

import it.unimi.dsi.fastutil.ints.IntArrayList;
import lomka.starl.duck.IDiscreteVoxelShape;
import net.minecraft.world.phys.shapes.DiscreteVoxelShape;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;

/**
 * Caches the greedy box decomposition of a shape in cell indices, so that every wrapper around
 * the same shape can answer {@code forAllBoxes} without re-running the decomposition.
 *
 * <p>Vanilla's {@code BitSetDiscreteVoxelShape.forAllBoxes} clones the whole BitSet and then
 * decomposes it destructively, which is why it cannot be shared. The decomposition itself,
 * however, is a pure function of the shape's bits: it never looks at coordinates, and
 * {@code VoxelShape#move} hands the <em>same</em> {@code DiscreteVoxelShape} to a fresh wrapper
 * with offset coordinate lists. The indices are therefore computed once here - by vanilla's own
 * {@code forAllBoxes}, not by a local copy of its greedy loop, so the box order cannot drift from
 * upstream - and every wrapper maps them through its own coordinate lists afterwards.
 *
 * <p>This is the path {@code Entity.move} walks per touched block:
 * {@code shape.move(new Vec3(pos)).toAabbs()} allocates a new wrapper each time, so a cache keyed
 * on the wrapper is cold on every call. (Figures: scripts/bench.)
 */
@Mixin(DiscreteVoxelShape.class)
public abstract class MixinDiscreteVoxelShape implements IDiscreteVoxelShape {

    @Shadow public abstract void forAllBoxes(DiscreteVoxelShape.IntLineConsumer intLineConsumer, boolean flag);

    /**
     * Above this many boxes the replay itself stops being cheap and the retained array starts to
     * outweigh the saving - a 520-box shape costs several microseconds to materialise either way.
     * Such shapes answer with the empty sentinel and keep vanilla's behaviour.
     */
    @Unique private static final int   LOMKA$MAX_CACHED_BOXES = 32;
    @Unique private static final int[] LOMKA$UNCACHEABLE      = new int[0];
    @Unique private volatile int[] lomka$boxes;

    /**
     * @author Starlev
     * @reason Pay the destructive greedy decomposition once per shape instead of once per wrapper.
     *         Boxes are recorded in cell indices through vanilla's own IntLineConsumer, so the
     *         sequence and its order are whatever upstream produces; the only thing added here is
     *         the memo. A benign race is possible between threads and is harmless: both racers
     *         build an equal, never-mutated array.
     */
    @Override
    public int[] lomka$boxes() {
        int[] cached = this.lomka$boxes;
        if (cached != null) {
            return cached;
        }

        IntArrayList boxes = new IntArrayList();
        this.forAllBoxes((x0, y0, z0, x1, y1, z1) -> {
            boxes.add(x0);
            boxes.add(y0);
            boxes.add(z0);
            boxes.add(x1);
            boxes.add(y1);
            boxes.add(z1);
        }, true);

        int[] built = boxes.size() / 6 > LOMKA$MAX_CACHED_BOXES ? LOMKA$UNCACHEABLE : boxes.toIntArray();
        this.lomka$boxes = built;
        return built;
    }
}
