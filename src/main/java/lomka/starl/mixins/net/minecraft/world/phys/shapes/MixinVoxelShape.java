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

import com.google.common.collect.Lists;
import it.unimi.dsi.fastutil.doubles.DoubleList;
import java.util.Collections;
import java.util.List;
import lomka.starl.duck.IDiscreteVoxelShape;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.shapes.DiscreteVoxelShape;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;

@Mixin(VoxelShape.class)
public abstract class MixinVoxelShape {

    @Shadow @Final protected DiscreteVoxelShape shape;
    @Shadow public abstract DoubleList getCoords(Direction.Axis direction_axis);
    @Shadow protected abstract double get(Direction.Axis direction_axis, int i);
    @Shadow public abstract boolean isEmpty();

    @Unique private volatile List<AABB> lomka$aabbCache;

    /**
     * @author Starlev
     * @reason Replays the shape's cached greedy decomposition instead of re-running it. Vanilla
     *         routes every call through {@code DiscreteVoxelShape#forAllBoxes}, which clones the
     *         whole BitSet and decomposes it destructively - about 4.3k cell probes even for a full
     *         block. The boxes live in cell indices and never read the coordinate lists, so one
     *         array serves every wrapper of the same shape, and only the index -&gt; double mapping
     *         below is per-wrapper - left exactly as vanilla wrote it. Shapes whose decomposition
     *         was too fragmented to retain, and any shape that does not expose the cache, keep
     *         vanilla's path.
     */
    @Overwrite
    public void forAllBoxes(Shapes.DoubleLineConsumer consumer) {
        int[] indices = null;
        //? if >=1.17 {
        if (this.shape instanceof IDiscreteVoxelShape cache) {
            indices = cache.lomka$boxes();
        }
        //? } else {
        /*if (this.shape instanceof IDiscreteVoxelShape) {
            indices = ((IDiscreteVoxelShape) this.shape).lomka$boxes();
        }
        *///?}
        if (indices != null) {
            if (indices.length > 0) {
                for (int i = 0; i < indices.length; i += 6) {
                    consumer.consume(
                            this.getCoords(Direction.Axis.X).getDouble(indices[i]),
                            this.getCoords(Direction.Axis.Y).getDouble(indices[i + 1]),
                            this.getCoords(Direction.Axis.Z).getDouble(indices[i + 2]),
                            this.getCoords(Direction.Axis.X).getDouble(indices[i + 3]),
                            this.getCoords(Direction.Axis.Y).getDouble(indices[i + 4]),
                            this.getCoords(Direction.Axis.Z).getDouble(indices[i + 5]));
                }
                return;
            }
            if (this.isEmpty()) {
                return;
            }
        }

        this.shape.forAllBoxes((i, j, k, l, i1, j1) -> {
            consumer.consume(
                    this.get(Direction.Axis.X, i), this.get(Direction.Axis.Y, j), this.get(Direction.Axis.Z, k),
                    this.get(Direction.Axis.X, l), this.get(Direction.Axis.Y, i1), this.get(Direction.Axis.Z, j1));
        }, true);
    }

    /**
     * @author Starlev
     * @reason Caches computed AABB list on first access to eliminate ArrayList
     *         and AABB allocations on hot-path raycasting and collision queries.
     */
    @Overwrite
    public List<AABB> toAabbs() {
        List<AABB> list = this.lomka$aabbCache;
        if (list == null) {
            List<AABB> arrayList = Lists.newArrayList();
            this.forAllBoxes((d0, d1, d2, d3, d4, d5) -> {
                arrayList.add(new AABB(d0, d1, d2, d3, d4, d5));
            });
            this.lomka$aabbCache = list = Collections.unmodifiableList(arrayList);
        }
        return list;
    }
}
