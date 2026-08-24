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

import it.unimi.dsi.fastutil.longs.LongConsumer;
import java.util.Spliterators.AbstractSpliterator;
import java.util.function.Consumer;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;
import net.minecraft.core.Cursor3D;
import net.minecraft.core.Direction;
import net.minecraft.core.SectionPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(SectionPos.class)
public abstract class MixinSectionPos {

    @Shadow
    public static int y(long packed) {
        throw new AssertionError();
    }

    @Shadow
    public static int z(long packed) {
        throw new AssertionError();
    }

    @Shadow
    public static int blockToSectionCoord(int coord) {
        throw new AssertionError();
    }

    /**
     * @author Starlev
     * @reason Direct 64-bit packing in a single expression without stack variables.
     */
    @Overwrite
    public static long asLong(int x, int y, int z) {
        return ((long) x & 4194303L) << 42
             | ((long) z & 4194303L) << 20
             | (long) (y & 1048575);
    }

    /**
     * @author Starlev
     * @reason Removes redundant no-op shift left by zero.
     */
    @Overwrite
    public static int x(long packed) {
        return (int) (packed >> 42);
    }

    /**
     * @author Starlev
     * @reason Direct bitwise translation from a packed BlockPos to a packed SectionPos without
     *         unpacking coordinates or method calls: section coords are the block coords >> 4,
     *         which for the packed layout reduces to field-aligned masks and shifts.
     */
    @Overwrite
    public static long blockToSection(long blockPosLong) {
        return (blockPosLong & -4398046511104L)
                | ((blockPosLong << 4)          & 4398045462528L)
                | (((blockPosLong << 52) >> 56) & 1048575L);
    }

    /**
     * @author Starlev
     * @reason Fast single-axis offsets; EAST/WEST become direct 64-bit adds because the X field
     *         occupies the top bits of the packed value.
     */
    @Overwrite
    public static long offset(long packed, Direction direction) {
        switch (direction) {
            case EAST:
                return packed + (1L << 42);
            case WEST:
                return packed - (1L << 42);
            case UP:
                return (packed & ~1048575L)
                     | (long) (y(packed) + 1 & 1048575);
            case DOWN:
                return (packed & ~1048575L)
                     | (long) (y(packed) - 1 & 1048575);
            case SOUTH:
                return (packed & ~4398045462528L)
                     | (((long) (z(packed) + 1) & 4194303L) << 20);
            case NORTH:
                return (packed & ~4398045462528L)
                     | (((long) (z(packed) - 1) & 4194303L) << 20);
            default:
                return packed;
        }
    }

    /**
     * @author Starlev
     * @reason Hoists invariant packing bit operations out of the nested iteration loops and adds
     *         a single-section fast path covering the overwhelmingly common case where a block
     *         change touches exactly one section.
     */
    @Overwrite
    public static void aroundAndAtBlockPos(int x, int y, int z, LongConsumer consumer) {
        int minX = blockToSectionCoord(x - 1);
        int maxX = blockToSectionCoord(x + 1);
        int minY = blockToSectionCoord(y - 1);
        int maxY = blockToSectionCoord(y + 1);
        int minZ = blockToSectionCoord(z - 1);
        int maxZ = blockToSectionCoord(z + 1);

        if (minX == maxX && minY == maxY && minZ == maxZ) {
            consumer.accept(asLong(minX, minY, minZ));
        } else {
            for (int curX = minX; curX <= maxX; ++curX) {
                long xPart = ((long) curX & 4194303L) << 42;
                for (int curY = minY; curY <= maxY; ++curY) {
                    long xyPart = xPart | ((long) curY & 1048575L);
                    for (int curZ = minZ; curZ <= maxZ; ++curZ) {
                        consumer.accept(xyPart | (((long) curZ & 4194303L) << 20));
                    }
                }
            }
        }
    }

    /**
     * @author Starlev
     * @reason Spliterator with an allocation-free forEachRemaining loop instead of per-element
     *         Cursor3D stepping. The loop nesting mirrors Cursor3D's traversal order (X fastest,
     *         Z slowest) so sequential and split consumption observe identical sequences.
     */
    @Overwrite
    public static Stream<SectionPos> betweenClosedStream(final int minX, final int minY, final int minZ, final int maxX, final int maxY, final int maxZ) {
        return StreamSupport.stream(new AbstractSpliterator<SectionPos>((long) (maxX - minX + 1) * (maxY - minY + 1) * (maxZ - minZ + 1), 64) {
            final Cursor3D cursor = new Cursor3D(minX, minY, minZ, maxX, maxY, maxZ);

            @Override
            public boolean tryAdvance(Consumer<? super SectionPos> consumer) {
                if (this.cursor.advance()) {
                    consumer.accept(new SectionPos(this.cursor.nextX(), this.cursor.nextY(), this.cursor.nextZ()));
                    return true;
                }
                return false;
            }

            @Override
            public void forEachRemaining(Consumer<? super SectionPos> consumer) {
                for (int z = minZ; z <= maxZ; ++z) {
                    for (int y = minY; y <= maxY; ++y) {
                        for (int x = minX; x <= maxX; ++x) {
                            consumer.accept(new SectionPos(x, y, z));
                        }
                    }
                }
            }
        }, false);
    }
}
