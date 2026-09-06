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

package lomka.starl.mixins.net.minecraft.world.phys;

import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(AABB.class)
public class MixinAABB {

    @Shadow @Final public double minX;
    @Shadow @Final public double minY;
    @Shadow @Final public double minZ;
    @Shadow @Final public double maxX;
    @Shadow @Final public double maxY;
    @Shadow @Final public double maxZ;

    /**
     * @author Starlev
     * @reason Eliminate per-raytrace double[1] heap allocation and inline clipPoint logic with plain local t;
     *         Inlined directly (no delegation) to stay compatible with 1.20.1 where the static double-clip
     *         overload does not exist.
     */
    @Overwrite
    public Optional<Vec3> clip(Vec3 vec3, Vec3 vec31) {
        double d6 = vec31.x - vec3.x;
        double d7 = vec31.y - vec3.y;
        double d8 = vec31.z - vec3.z;
        double t  = 1.0D;
        Direction dir = null;
        double d0 = this.minX;
        double d1 = this.minY;
        double d2 = this.minZ;
        double d3 = this.maxX;
        double d4 = this.maxY;
        double d5 = this.maxZ;

        if (d6 > 1.0E-7D) {
            double t2 = (d0 - vec3.x) / d6;
            double y  = vec3.y + t2 * d7;
            double z  = vec3.z + t2 * d8;
            if (0.0D < t2 && t2 < t && y > d1 - 1.0E-7D && y < d4 + 1.0E-7D && z > d2 - 1.0E-7D && z < d5 + 1.0E-7D) {
                t   = t2;
                dir = Direction.WEST;
            }
        } else if (d6 < -1.0E-7D) {
            double t2 = (d3 - vec3.x) / d6;
            double y  = vec3.y + t2 * d7;
            double z  = vec3.z + t2 * d8;
            if (0.0D < t2 && t2 < t && y > d1 - 1.0E-7D && y < d4 + 1.0E-7D && z > d2 - 1.0E-7D && z < d5 + 1.0E-7D) {
                t   = t2;
                dir = Direction.EAST;
            }
        }

        if (d7 > 1.0E-7D) {
            double t2 = (d1 - vec3.y) / d7;
            double z  = vec3.z + t2 * d8;
            double x  = vec3.x + t2 * d6;
            if (0.0D < t2 && t2 < t && z > d2 - 1.0E-7D && z < d5 + 1.0E-7D && x > d0 - 1.0E-7D && x < d3 + 1.0E-7D) {
                t   = t2;
                dir = Direction.DOWN;
            }
        } else if (d7 < -1.0E-7D) {
            double t2 = (d4 - vec3.y) / d7;
            double z  = vec3.z + t2 * d8;
            double x  = vec3.x + t2 * d6;
            if (0.0D < t2 && t2 < t && z > d2 - 1.0E-7D && z < d5 + 1.0E-7D && x > d0 - 1.0E-7D && x < d3 + 1.0E-7D) {
                t   = t2;
                dir = Direction.UP;
            }
        }

        if (d8 > 1.0E-7D) {
            double t2 = (d2 - vec3.z) / d8;
            double x  = vec3.x + t2 * d6;
            double y  = vec3.y + t2 * d7;
            if (0.0D < t2 && t2 < t && x > d0 - 1.0E-7D && x < d3 + 1.0E-7D && y > d1 - 1.0E-7D && y < d4 + 1.0E-7D) {
                t   = t2;
                dir = Direction.NORTH;
            }
        } else if (d8 < -1.0E-7D) {
            double t2 = (d5 - vec3.z) / d8;
            double x  = vec3.x + t2 * d6;
            double y  = vec3.y + t2 * d7;
            if (0.0D < t2 && t2 < t && x > d0 - 1.0E-7D && x < d3 + 1.0E-7D && y > d1 - 1.0E-7D && y < d4 + 1.0E-7D) {
                t   = t2;
                dir = Direction.SOUTH;
            }
        }

        if (dir == null) {
            return Optional.empty();
        }
        return Optional.of(vec3.add(t * d6, t * d7, t * d8));
    }

    //? if >=1.21.4 {
    /**
     * @author Starlev
     * @reason Replace vanilla's double[1] box + getDirection/clipPoint indirection with a single local t and
     *         direct face tests; saves 24B alloc per raytrace and removes virtual dispatch via inline branchless plane checks.
     *         Verified against vanilla's epsilon (1e-7) and WEST/EAST/DOWN/UP/NORTH/SOUTH face ordering.
     */
    @Overwrite
    public static Optional<Vec3> clip(double d0, double d1, double d2, double d3, double d4, double d5, Vec3 vec3, Vec3 vec31) {
        double d6 = vec31.x - vec3.x;
        double d7 = vec31.y - vec3.y;
        double d8 = vec31.z - vec3.z;
        double t  = 1.0D;
        Direction dir = null;

        if (d6 > 1.0E-7D) {
            double t2 = (d0 - vec3.x) / d6;
            double y  = vec3.y + t2 * d7;
            double z  = vec3.z + t2 * d8;
            if (0.0D < t2 && t2 < t && y > d1 - 1.0E-7D && y < d4 + 1.0E-7D && z > d2 - 1.0E-7D && z < d5 + 1.0E-7D) {
                t   = t2;
                dir = Direction.WEST;
            }
        } else if (d6 < -1.0E-7D) {
            double t2 = (d3 - vec3.x) / d6;
            double y  = vec3.y + t2 * d7;
            double z  = vec3.z + t2 * d8;
            if (0.0D < t2 && t2 < t && y > d1 - 1.0E-7D && y < d4 + 1.0E-7D && z > d2 - 1.0E-7D && z < d5 + 1.0E-7D) {
                t   = t2;
                dir = Direction.EAST;
            }
        }

        if (d7 > 1.0E-7D) {
            double t2 = (d1 - vec3.y) / d7;
            double z  = vec3.z + t2 * d8;
            double x  = vec3.x + t2 * d6;
            if (0.0D < t2 && t2 < t && z > d2 - 1.0E-7D && z < d5 + 1.0E-7D && x > d0 - 1.0E-7D && x < d3 + 1.0E-7D) {
                t   = t2;
                dir = Direction.DOWN;
            }
        } else if (d7 < -1.0E-7D) {
            double t2 = (d4 - vec3.y) / d7;
            double z  = vec3.z + t2 * d8;
            double x  = vec3.x + t2 * d6;
            if (0.0D < t2 && t2 < t && z > d2 - 1.0E-7D && z < d5 + 1.0E-7D && x > d0 - 1.0E-7D && x < d3 + 1.0E-7D) {
                t   = t2;
                dir = Direction.UP;
            }
        }

        if (d8 > 1.0E-7D) {
            double t2 = (d2 - vec3.z) / d8;
            double x  = vec3.x + t2 * d6;
            double y  = vec3.y + t2 * d7;
            if (0.0D < t2 && t2 < t && x > d0 - 1.0E-7D && x < d3 + 1.0E-7D && y > d1 - 1.0E-7D && y < d4 + 1.0E-7D) {
                t   = t2;
                dir = Direction.NORTH;
            }
        } else if (d8 < -1.0E-7D) {
            double t2 = (d5 - vec3.z) / d8;
            double x  = vec3.x + t2 * d6;
            double y  = vec3.y + t2 * d7;
            if (0.0D < t2 && t2 < t && x > d0 - 1.0E-7D && x < d3 + 1.0E-7D && y > d1 - 1.0E-7D && y < d4 + 1.0E-7D) {
                t   = t2;
                dir = Direction.SOUTH;
            }
        }

        if (dir == null) {
            return Optional.empty();
        }
        return Optional.of(vec3.add(t * d6, t * d7, t * d8));
    }
    //?}

    /**
     * @author Starlev
     * @reason Avoid double[1] allocation and per-iteration AABB.move() allocation; compute moved bounds inline
     *         (a.minX + pos.getX() etc.) and keep best t/dir with plain locals. Keeps exact Lithium-compatible face tests.
     */
    @Overwrite
    public static @Nullable BlockHitResult clip(Iterable<AABB> iterable, Vec3 vec3, Vec3 vec31, BlockPos blockpos) {
        double d0    = vec31.x - vec3.x;
        double d1    = vec31.y - vec3.y;
        double d2    = vec31.z - vec3.z;
        double bestT = 1.0D;
        Direction bestDir = null;
        int bx = blockpos.getX();
        int by = blockpos.getY();
        int bz = blockpos.getZ();

        for (AABB aabb : iterable) {
            double d3 = aabb.minX + (double) bx;
            double d4 = aabb.minY + (double) by;
            double d5 = aabb.minZ + (double) bz;
            double d6 = aabb.maxX + (double) bx;
            double d7 = aabb.maxY + (double) by;
            double d8 = aabb.maxZ + (double) bz;

            if (d0 > 1.0E-7D) {
                double t2 = (d3 - vec3.x) / d0;
                double y  = vec3.y + t2 * d1;
                double z  = vec3.z + t2 * d2;
                if (0.0D < t2 && t2 < bestT && y > d4 - 1.0E-7D && y < d7 + 1.0E-7D && z > d5 - 1.0E-7D && z < d8 + 1.0E-7D) {
                    bestT   = t2;
                    bestDir = Direction.WEST;
                }
            } else if (d0 < -1.0E-7D) {
                double t2 = (d6 - vec3.x) / d0;
                double y  = vec3.y + t2 * d1;
                double z  = vec3.z + t2 * d2;
                if (0.0D < t2 && t2 < bestT && y > d4 - 1.0E-7D && y < d7 + 1.0E-7D && z > d5 - 1.0E-7D && z < d8 + 1.0E-7D) {
                    bestT   = t2;
                    bestDir = Direction.EAST;
                }
            }

            if (d1 > 1.0E-7D) {
                double t2 = (d4 - vec3.y) / d1;
                double z  = vec3.z + t2 * d2;
                double x  = vec3.x + t2 * d0;
                if (0.0D < t2 && t2 < bestT && z > d5 - 1.0E-7D && z < d8 + 1.0E-7D && x > d3 - 1.0E-7D && x < d6 + 1.0E-7D) {
                    bestT   = t2;
                    bestDir = Direction.DOWN;
                }
            } else if (d1 < -1.0E-7D) {
                double t2 = (d7 - vec3.y) / d1;
                double z  = vec3.z + t2 * d2;
                double x  = vec3.x + t2 * d0;
                if (0.0D < t2 && t2 < bestT && z > d5 - 1.0E-7D && z < d8 + 1.0E-7D && x > d3 - 1.0E-7D && x < d6 + 1.0E-7D) {
                    bestT   = t2;
                    bestDir = Direction.UP;
                }
            }

            if (d2 > 1.0E-7D) {
                double t2 = (d5 - vec3.z) / d2;
                double x  = vec3.x + t2 * d0;
                double y  = vec3.y + t2 * d1;
                if (0.0D < t2 && t2 < bestT && x > d3 - 1.0E-7D && x < d6 + 1.0E-7D && y > d4 - 1.0E-7D && y < d7 + 1.0E-7D) {
                    bestT   = t2;
                    bestDir = Direction.NORTH;
                }
            } else if (d2 < -1.0E-7D) {
                double t2 = (d8 - vec3.z) / d2;
                double x  = vec3.x + t2 * d0;
                double y  = vec3.y + t2 * d1;
                if (0.0D < t2 && t2 < bestT && x > d3 - 1.0E-7D && x < d6 + 1.0E-7D && y > d4 - 1.0E-7D && y < d7 + 1.0E-7D) {
                    bestT   = t2;
                    bestDir = Direction.SOUTH;
                }
            }
        }

        if (bestDir == null) {
            return null;
        }
        return new BlockHitResult(vec3.add(bestT * d0, bestT * d1, bestT * d2), bestDir, blockpos, false);
    }
}
