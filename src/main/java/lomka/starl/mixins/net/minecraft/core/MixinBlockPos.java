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

package lomka.starl.mixins.net.minecraft.core;

import com.google.common.collect.AbstractIterator;
import java.util.Collections;
import net.minecraft.core.BlockPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

@Mixin(BlockPos.class)
public class MixinBlockPos {

    /**
     * @author Starlev
     * @reason Replaces two divisions and two remainders per block with incremental stepped counters;
     *         avoids integer division bottleneck (20-90 cycles) on block-fill/lighting/structure iterations
     *         while preserving byte-identical X->Y->Z iteration order.
     */
    @Overwrite
    public static Iterable<BlockPos> betweenClosed(int minX, int minY, int minZ, int maxX, int maxY, int maxZ) {
        if (minX > maxX
         || minY > maxY
         || minZ > maxZ) {
            return Collections.emptyList();
        }
        return () -> new AbstractIterator<BlockPos>() {
            private final BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
            private int x = minX;
            private int y = minY;
            private int z = minZ;
            private boolean done = false;

            @Override
            protected BlockPos computeNext() {
                if (this.done) {
                    return this.endOfData();
                }
                BlockPos ret = this.cursor.set(this.x, this.y, this.z);
                if (this.x < maxX) {
                    this.x++;
                } else {
                    this.x = minX;
                    if (this.y < maxY) {
                        this.y++;
                    } else {
                        this.y = minY;
                        if (this.z < maxZ) {
                            this.z++;
                        } else {
                            this.done = true;
                        }
                    }
                }
                return ret;
            }
        };
    }
}