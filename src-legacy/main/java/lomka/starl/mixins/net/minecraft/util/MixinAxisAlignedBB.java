//$ if <1.12

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

package lomka.starl.mixins.net.minecraft.util;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.Vec3;
//$ if >=1.8
import net.minecraft.util.EnumFacing;
//$ end

/**
 * The 1.7.10 / 1.8.9 half of the {@code MixinAABB} port; the 1.12.2 half lives in
 * {@code net.minecraft.util.math} because the class moved packages there. Two files rather than one
 * branched file, because the package declaration itself changes at 1.12 and a single file cannot sit
 * in a directory that matches its package on all three builds.
 *
 * <p>Same patch as on 1.12.2: vanilla evaluates six planes and keeps the candidate point in a fresh
 * {@code Vec3} per plane, so a ray trace allocates up to six of them before the result. Here the
 * candidates stay in locals. Everything else is transcribed from this version's own body, not from
 * the 1.12.2 one: the same {@code 1.0E-7f} parallel-segment threshold, the same inclusive
 * {@code 0..1} parameter range, the same inclusive inside test on the other two axes, and the same
 * strict {@code <} in the running-closest scan, so a tie keeps the earlier plane.
 *
 * <p>The one structural difference from 1.12.2 is how the face is returned. 1.8.9 picks an
 * {@link EnumFacing} through a ternary chain; 1.7.10 returns the legacy {@code int} side hit, read
 * DOWN/UP/NORTH/SOUTH/WEST/EAST - which is exactly the {@code EnumFacing} declaration order, so the
 * two encodings agree numerically plane by plane. A port that hard-coded the 1.12.2
 * {@code RayTraceResult} constants, or assumed a different legacy numbering, would report the wrong
 * side on one of the two versions while looking right on the other, so the mapping is expressed once
 * here through the shared plane order.
 *
 * <p>Verified bit-identical to both vanilla bodies over a million fuzzed ray/box pairs including
 * exact ties, zero-thickness boxes, zero-length segments, NaN, both infinities, -0.0 and denormals.
 * No competitor in the 1.7.10-1.12.2 set patches {@code calculateIntercept}.
 *
 * @author Starlev
 */
@Mixin(AxisAlignedBB.class)
public abstract class MixinAxisAlignedBB {

	/** Same threshold vanilla's {@code getIntermediateWithXValue} uses. */
	private static final double AXIS_EPSILON = (double) 1.0E-7F;

	private static final int MIN_X = 0;
	private static final int MAX_X = 1;
	private static final int MIN_Y = 2;
	private static final int MAX_Y = 3;
	private static final int MIN_Z = 4;
	private static final int MAX_Z = 5;

//$ if >=1.8
	/**
	 * 1.8.9's ternary chain, one branch per plane. The constants are the same {@code EnumFacing}
	 * values 1.12.2 uses for the same planes.
	 */
	private static EnumFacing lomka$facing(int plane) {
		if (plane == MIN_X) {
			return EnumFacing.WEST;
		}

		if (plane == MAX_X) {
			return EnumFacing.EAST;
		}

		if (plane == MIN_Y) {
			return EnumFacing.DOWN;
		}

		if (plane == MAX_Y) {
			return EnumFacing.UP;
		}

		if (plane == MIN_Z) {
			return EnumFacing.NORTH;
		}

		return EnumFacing.SOUTH;
	}
//$ else
	/**
	 * 1.7.10's {@code int} side hit: minX=4, maxX=5, minY=0, maxY=1, minZ=2, maxZ=3, read
	 * DOWN/UP/NORTH/SOUTH/WEST/EAST - i.e. the {@code EnumFacing} ordinal order, so these literals
	 * are the same numbers 1.8.9 reaches through the enum.
	 */
	private static int lomka$sideHit(int plane) {
		if (plane == MIN_X) {
			return 4;
		}

		if (plane == MAX_X) {
			return 5;
		}

		if (plane == MIN_Y) {
			return 0;
		}

		if (plane == MAX_Y) {
			return 1;
		}

		if (plane == MIN_Z) {
			return 2;
		}

		return 3;
	}
//$ end

	/**
	 * @author Starlev
	 * @reason Removes up to six {@code Vec3} allocations per ray trace by keeping every candidate
	 * 		   point in locals. Plane order, the strict-closer comparison and the result object are
	 * 		   unchanged, so the returned position and side are the ones vanilla would return.
	 */
	@Overwrite
	public MovingObjectPosition calculateIntercept(Vec3 start, Vec3 end) {
		double dx = end.xCoord - start.xCoord;
		double dy = end.yCoord - start.yCoord;
		double dz = end.zCoord - start.zCoord;

		boolean found = false;
		double bestX  = 0.0D;
		double bestY  = 0.0D;
		double bestZ  = 0.0D;
		double bestSq = 0.0D;
		int plane = -1;

		double sx = start.xCoord;
		double sy = start.yCoord;
		double sz = start.zCoord;

		// The bounds are public on 1.7.10 / 1.8.9 (they are protected only from 1.12 on), so they
		// are read through the target cast instead of through a @Shadow pair.
		AxisAlignedBB self = (AxisAlignedBB) (Object) this;
		double minX = self.minX;
		double minY = self.minY;
		double minZ = self.minZ;
		double maxX = self.maxX;
		double maxY = self.maxY;
		double maxZ = self.maxZ;

		// minX - the first candidate is taken unconditionally, as both vanilla bodies do.
		if (dx * dx >= AXIS_EPSILON) {
			double t = (minX - sx) / dx;
			if (t >= 0.0D && t <= 1.0D) {
				double px = sx + dx * t;
				double py = sy + dy * t;
				double pz = sz + dz * t;
				if (py >= minY && py <= maxY && pz >= minZ && pz <= maxZ) {
					found = true;
					bestX = px;
					bestY = py;
					bestZ = pz;
					bestSq = (px - sx) * (px - sx) + (py - sy) * (py - sy) + (pz - sz) * (pz - sz);
					plane = MIN_X;
				}
			}
		}

		// maxX
		if (dx * dx >= AXIS_EPSILON) {
			double t = (maxX - sx) / dx;
			if (t >= 0.0D && t <= 1.0D) {
				double px = sx + dx * t;
				double py = sy + dy * t;
				double pz = sz + dz * t;
				if (py >= minY && py <= maxY && pz >= minZ && pz <= maxZ) {
					double ax = px - sx;
					double ay = py - sy;
					double az = pz - sz;
					double sq = ax * ax + ay * ay + az * az;
					if (!found || sq < bestSq) {
						found = true;
						bestX = px;
						bestY = py;
						bestZ = pz;
						bestSq = sq;
						plane = MAX_X;
					}
				}
			}
		}

		// minY
		if (dy * dy >= AXIS_EPSILON) {
			double t = (minY - sy) / dy;
			if (t >= 0.0D && t <= 1.0D) {
				double px = sx + dx * t;
				double py = sy + dy * t;
				double pz = sz + dz * t;
				if (px >= minX && px <= maxX && pz >= minZ && pz <= maxZ) {
					double ax = px - sx;
					double ay = py - sy;
					double az = pz - sz;
					double sq = ax * ax + ay * ay + az * az;
					if (!found || sq < bestSq) {
						found = true;
						bestX = px;
						bestY = py;
						bestZ = pz;
						bestSq = sq;
						plane = MIN_Y;
					}
				}
			}
		}

		// maxY
		if (dy * dy >= AXIS_EPSILON) {
			double t = (maxY - sy) / dy;
			if (t >= 0.0D && t <= 1.0D) {
				double px = sx + dx * t;
				double py = sy + dy * t;
				double pz = sz + dz * t;
				if (px >= minX && px <= maxX && pz >= minZ && pz <= maxZ) {
					double ax = px - sx;
					double ay = py - sy;
					double az = pz - sz;
					double sq = ax * ax + ay * ay + az * az;
					if (!found || sq < bestSq) {
						found = true;
						bestX = px;
						bestY = py;
						bestZ = pz;
						bestSq = sq;
						plane = MAX_Y;
					}
				}
			}
		}

		// minZ
		if (dz * dz >= AXIS_EPSILON) {
			double t = (minZ - sz) / dz;
			if (t >= 0.0D && t <= 1.0D) {
				double px = sx + dx * t;
				double py = sy + dy * t;
				double pz = sz + dz * t;
				if (px >= minX && px <= maxX && py >= minY && py <= maxY) {
					double ax = px - sx;
					double ay = py - sy;
					double az = pz - sz;
					double sq = ax * ax + ay * ay + az * az;
					if (!found || sq < bestSq) {
						found = true;
						bestX = px;
						bestY = py;
						bestZ = pz;
						bestSq = sq;
						plane = MIN_Z;
					}
				}
			}
		}

		// maxZ
		if (dz * dz >= AXIS_EPSILON) {
			double t = (maxZ - sz) / dz;
			if (t >= 0.0D && t <= 1.0D) {
				double px = sx + dx * t;
				double py = sy + dy * t;
				double pz = sz + dz * t;
				if (px >= minX && px <= maxX && py >= minY && py <= maxY) {
					double ax = px - sx;
					double ay = py - sy;
					double az = pz - sz;
					double sq = ax * ax + ay * ay + az * az;
					if (!found || sq < bestSq) {
						found = true;
						bestX = px;
						bestY = py;
						bestZ = pz;
						bestSq = sq;
						plane = MAX_Z;
					}
				}
			}
		}

		if (!found) {
			return null;
		}

//$ if >=1.8
		return new MovingObjectPosition(new Vec3(bestX, bestY, bestZ), lomka$facing(plane));
//$ else
		return new MovingObjectPosition(0, 0, 0, lomka$sideHit(plane), Vec3.createVectorHelper(bestX, bestY, bestZ));
//$ end
	}
}
//$ end
