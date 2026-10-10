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

//? if >=1.21.9 {
package lomka.starl.utils.math;

import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;
import java.util.Iterator;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.level.BlockGetter.BlockStepVisitor;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public final class BlockStepSweep {

    private BlockStepSweep() {
    }

    /**
     * Head sweep over the box translated by -travel, then the DDA along the travel vector, then the
     * tail sweep over the box itself. One reused cursor and one primitive {@code LongSet} replace the
     * per-cell {@code Iterable} + {@code AbstractIterator} + {@code MutableBlockPos} trio and the
     * {@code Optional<Vec3>} box clip.
     */
    public static boolean forEachBlockIntersectedBetween(Vec3 from, Vec3 to, AABB aabb, BlockStepVisitor visitor) {
        Vec3 travel = to.subtract(from);

        if (travel.lengthSqr() < (double) Mth.square(1.0E-5F)) {
            Iterator<BlockPos> iterator = BlockPos.betweenClosed(aabb).iterator();

            while (iterator.hasNext()) {
                if (!visitor.visit(iterator.next(), 0)) {
                    return false;
                }
            }

            return true;
        }

        LongOpenHashSet visited = new LongOpenHashSet();
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();

        // head sweep: the box translated by -travel, every position is visited and recorded
        if (!lomka$walkCorners(Mth.floor(aabb.minX - travel.x), Mth.floor(aabb.minY - travel.y), Mth.floor(aabb.minZ - travel.z),
                Mth.floor(aabb.maxX - travel.x), Mth.floor(aabb.maxY - travel.y), Mth.floor(aabb.maxZ - travel.z),
                travel, cursor, visited, false, visitor, 0)) {
            return false;
        }

        int cells = lomka$addCollisionsAlongTravel(visited, travel, aabb, cursor, visitor);

        if (cells < 0) {
            return false;
        }

        // tail sweep: the box itself, positions already recorded are skipped
        return lomka$walkCorners(Mth.floor(aabb.minX), Mth.floor(aabb.minY), Mth.floor(aabb.minZ),
                Mth.floor(aabb.maxX), Mth.floor(aabb.maxY), Mth.floor(aabb.maxZ),
                travel, cursor, visited, true, visitor, cells + 1);
    }

    /**
     * Vanilla's {@code addCollisionsAlongTravel}: a DDA over the box extent, one unit-cell clip per
     * step and, on a hit, one corner sweep of the cell box walked along the travel vector.
     *
     * <p>The cell clip is written straight into the sweep loop on purpose: as a separate method it is
     * over {@code FreqInlineSize} and never inlines, which measured slower than vanilla despite
     * allocating far less.
     *
     * @return the number of hit cells, or {@code -1} when the visitor aborted
     */
    private static int lomka$addCollisionsAlongTravel(LongSet visited, Vec3 travel, AABB aabb,
            BlockPos.MutableBlockPos cursor, BlockStepVisitor visitor) {
        double boxX = aabb.getXsize();
        double boxY = aabb.getYsize();
        double boxZ = aabb.getZsize();

        // getFurthestCorner, kept as three ints with vanilla's sign permutation
        double absX = Math.abs(travel.x), absY = Math.abs(travel.y), absZ = Math.abs(travel.z);
        int signX = travel.x >= 0.0D ? 1 : -1, signY = travel.y >= 0.0D ? 1 : -1, signZ = travel.z >= 0.0D ? 1 : -1;
        int cornerX, cornerY, cornerZ;

        if (absX <= absY && absX <= absZ) {
            cornerX = -signX; cornerY = -signZ; cornerZ = signY;
        } else if (absY <= absZ) {
            cornerX = signZ; cornerY = -signY; cornerZ = -signX;
        } else {
            cornerX = -signY; cornerY = signX; cornerZ = -signZ;
        }

        double atX = Mth.lerp(0.5D, aabb.minX, aabb.maxX) + boxX * 0.5D * cornerX;
        double atY = Mth.lerp(0.5D, aabb.minY, aabb.maxY) + boxY * 0.5D * cornerY;
        double atZ = Mth.lerp(0.5D, aabb.minZ, aabb.maxZ) + boxZ * 0.5D * cornerZ;
        double fromX = atX - travel.x, fromY = atY - travel.y, fromZ = atZ - travel.z;
        int cellX = Mth.floor(fromX), cellY = Mth.floor(fromY), cellZ = Mth.floor(fromZ);
        // the sign and the divisor come from the travel vector, the fractional part from the corner
        int stepX = Mth.sign(travel.x), stepY = Mth.sign(travel.y), stepZ = Mth.sign(travel.z);
        double tDeltaX = stepX == 0 ? Double.MAX_VALUE : (double) stepX / travel.x;
        double tDeltaY = stepY == 0 ? Double.MAX_VALUE : (double) stepY / travel.y;
        double tDeltaZ = stepZ == 0 ? Double.MAX_VALUE : (double) stepZ / travel.z;
        double tX = tDeltaX * (stepX > 0 ? 1.0D - Mth.frac(fromX) : Mth.frac(fromX));
        double tY = tDeltaY * (stepY > 0 ? 1.0D - Mth.frac(fromY) : Mth.frac(fromY));
        double tZ = tDeltaZ * (stepZ > 0 ? 1.0D - Mth.frac(fromZ) : Mth.frac(fromZ));
        double dirX = atX - fromX, dirY = atY - fromY, dirZ = atZ - fromZ;
        int cells = 0;

        while (tX <= 1.0D || tY <= 1.0D || tZ <= 1.0D) {
            if (tX < tY) {
                if (tX < tZ) {
                    cellX += stepX; tX += tDeltaX;
                } else {
                    cellZ += stepZ; tZ += tDeltaZ;
                }
            } else if (tY < tZ) {
                cellY += stepY; tY += tDeltaY;
            } else {
                cellZ += stepZ; tZ += tDeltaZ;
            }

            // AABB#clip against the unit cell, inlined; a hit always yields t < 1.0, so 1.0 is the miss marker
            double t = 1.0D;

            if (dirX > FACE_EPS) {
                double c = (cellX - fromX) / dirX;
                if (lomka$hit(c, t, fromY + c * dirY, cellY, cellY + 1) && lomka$hit(c, t, fromZ + c * dirZ, cellZ, cellZ + 1)) { t = c; }
            } else if (dirX < -FACE_EPS) {
                double c = (cellX + 1 - fromX) / dirX;
                if (lomka$hit(c, t, fromY + c * dirY, cellY, cellY + 1) && lomka$hit(c, t, fromZ + c * dirZ, cellZ, cellZ + 1)) { t = c; }
            }

            if (dirY > FACE_EPS) {
                double c = (cellY - fromY) / dirY;
                if (lomka$hit(c, t, fromZ + c * dirZ, cellZ, cellZ + 1) && lomka$hit(c, t, fromX + c * dirX, cellX, cellX + 1)) { t = c; }
            } else if (dirY < -FACE_EPS) {
                double c = (cellY + 1 - fromY) / dirY;
                if (lomka$hit(c, t, fromZ + c * dirZ, cellZ, cellZ + 1) && lomka$hit(c, t, fromX + c * dirX, cellX, cellX + 1)) { t = c; }
            }

            if (dirZ > FACE_EPS) {
                double c = (cellZ - fromZ) / dirZ;
                if (lomka$hit(c, t, fromX + c * dirX, cellX, cellX + 1) && lomka$hit(c, t, fromY + c * dirY, cellY, cellY + 1)) { t = c; }
            } else if (dirZ < -FACE_EPS) {
                double c = (cellZ + 1 - fromZ) / dirZ;
                if (lomka$hit(c, t, fromX + c * dirX, cellX, cellX + 1) && lomka$hit(c, t, fromY + c * dirY, cellY, cellY + 1)) { t = c; }
            }

            if (t < 1.0D) {
                ++cells;
                double hitX = Mth.clamp(fromX + t * dirX, cellX + CELL_EPS, cellX + 1.0D - CELL_EPS);
                double hitY = Mth.clamp(fromY + t * dirY, cellY + CELL_EPS, cellY + 1.0D - CELL_EPS);
                double hitZ = Mth.clamp(fromZ + t * dirZ, cellZ + CELL_EPS, cellZ + 1.0D - CELL_EPS);
                int endX = Mth.floor(hitX - boxX * cornerX);
                int endY = Mth.floor(hitY - boxY * cornerY);
                int endZ = Mth.floor(hitZ - boxZ * cornerZ);

                if (!lomka$walkCorners(cellX, cellY, cellZ, endX, endY, endZ, travel, cursor, visited, true, visitor, cells)) {
                    return -1;
                }
            }
        }

        return cells;
    }

    /**
     * Vanilla's {@code BlockPos#betweenCornersInDirection} without the iterator: the same corner
     * normalisation, the same static axis step order, the same odometer and the same shared
     * {@code MutableBlockPos} cursor, so the visitor sees the identical position sequence.
     *
     * @param skipVisited {@code false} for the head sweep, which visits and records every position
     * @return {@code true} when the sweep finished, {@code false} when the visitor aborted
     */
    private static boolean lomka$walkCorners(int fromX, int fromY, int fromZ, int toX, int toY, int toZ, Vec3 travel,
            BlockPos.MutableBlockPos cursor, LongSet visited, boolean skipVisited, BlockStepVisitor visitor, int step) {
        int minX = Math.min(fromX, toX), minY = Math.min(fromY, toY), minZ = Math.min(fromZ, toZ);
        int maxX = Math.max(fromX, toX), maxY = Math.max(fromY, toY), maxZ = Math.max(fromZ, toZ);
        int sizeX = maxX - minX, sizeY = maxY - minY, sizeZ = maxZ - minZ;
        int startX = travel.x >= 0.0D ? minX : maxX;
        int startY = travel.y >= 0.0D ? minY : maxY;
        int startZ = travel.z >= 0.0D ? minZ : maxZ;
        List<Direction.Axis> order = Direction.axisStepOrder(travel);
        Direction.Axis first = order.get(0), second = order.get(1), third = order.get(2);
        int dirFirst = travel.get(first) >= 0.0D ? 1 : -1;
        int dirSecond = travel.get(second) >= 0.0D ? 1 : -1;
        int dirThird = travel.get(third) >= 0.0D ? 1 : -1;
        int spanFirst = lomka$span(first, sizeX, sizeY, sizeZ);
        int spanSecond = lomka$span(second, sizeX, sizeY, sizeZ);
        int spanThird = lomka$span(third, sizeX, sizeY, sizeZ);
        int firstIndex = 0, secondIndex = 0, thirdIndex = 0;

        while (true) {
            cursor.set(startX + lomka$offset(Direction.Axis.X, first, second, third, firstIndex, secondIndex, thirdIndex, dirFirst, dirSecond, dirThird),
                    startY + lomka$offset(Direction.Axis.Y, first, second, third, firstIndex, secondIndex, thirdIndex, dirFirst, dirSecond, dirThird),
                    startZ + lomka$offset(Direction.Axis.Z, first, second, third, firstIndex, secondIndex, thirdIndex, dirFirst, dirSecond, dirThird));

            boolean more;

            if (thirdIndex < spanThird) {
                ++thirdIndex;
                more = true;
            } else if (secondIndex < spanSecond) {
                ++secondIndex;
                thirdIndex = 0;
                more = true;
            } else if (firstIndex < spanFirst) {
                ++firstIndex;
                secondIndex = 0;
                thirdIndex = 0;
                more = true;
            } else {
                more = false;
            }

            if (visited.add(cursor.asLong()) || !skipVisited) {
                if (!visitor.visit(cursor, step)) {
                    return false;
                }
            }

            if (!more) {
                return true;
            }
        }
    }

    /** One face of the unit-cell clip: is the entry at {@code t} on the face and inside the other two axes. */
    private static boolean lomka$hit(double t, double best, double along, int min, int max) {
        return 0.0D < t && t < best && min - FACE_EPS < along && along < max + FACE_EPS;
    }

    private static int lomka$span(Direction.Axis axis, int sizeX, int sizeY, int sizeZ) {
        return axis == Direction.Axis.X ? sizeX : (axis == Direction.Axis.Y ? sizeY : sizeZ);
    }

    private static int lomka$offset(Direction.Axis axis, Direction.Axis first, Direction.Axis second, Direction.Axis third,
            int firstIndex, int secondIndex, int thirdIndex, int dirFirst, int dirSecond, int dirThird) {
        int offset = 0;

        if (first == axis) {
            offset += firstIndex * dirFirst;
        }

        if (second == axis) {
            offset += secondIndex * dirSecond;
        }

        if (third == axis) {
            offset += thirdIndex * dirThird;
        }

        return offset;
    }

    /** {@code AABB#clip} face epsilon. */
    private static final double FACE_EPS = 1.0E-7D;

    /**
     * The deflate epsilon vanilla clamps the hit point with; {@code (double) 1.0E-5F} rounded to a
     * double, written out so the comparison bounds stay bit-identical.
     */
    private static final double CELL_EPS = 9.999999747378752E-6D;
}
//? }
