//$ if >=1.12

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

package lomka.starl.mixins.net.minecraft.util.math;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.Vec3d;

/**
 * Port of {@code MixinAABB} to 1.12.2. The modern patch targets {@code AABB#clip}, which does
 * not exist here; the equivalent hot method is {@code calculateIntercept}, and it is the more
 * allocation-heavy of the two - vanilla allocates one {@code Vec3d} per plane it tests, so up
 * to six per ray trace, plus the {@code Vec3d} each {@code getIntermediateWithXValue} call
 * makes internally.
 *
 * The candidate points are therefore computed into locals. Everything else is a transcription:
 * the parallel-segment threshold, the inclusive {@code 0..1} parameter range, the inclusive
 * inside test on the other two axes, and the strict {@code <} of {@code isClosest} - so ties
 * keep the earlier face, which is what the WEST/EAST/DOWN/UP/NORTH/SOUTH order in vanilla
 * relies on.
 */
@Mixin(AxisAlignedBB.class)
public abstract class MixinAxisAlignedBB {

	/** Same threshold vanilla's {@code getIntermediateWithXValue} uses. */
	private static final double AXIS_EPSILON = (double) 1.0E-7F;

	@Shadow protected double minX;
	@Shadow protected double minY;
	@Shadow protected double minZ;
	@Shadow protected double maxX;
	@Shadow protected double maxY;
	@Shadow protected double maxZ;

	/**
	 * @author Starlev
	 * @reason Removes up to six {@code Vec3d} allocations per ray trace by keeping the candidate
	 * 		   point in locals; the face-selection order and the strict-closer comparison are unchanged.
	 */
	@Overwrite
	public RayTraceResult calculateIntercept(Vec3d start, Vec3d end) {
		double dx = end.x - start.x;
		double dy = end.y - start.y;
		double dz = end.z - start.z;

		boolean found = false;
		double bestX  = 0.0D;
		double bestY  = 0.0D;
		double bestZ  = 0.0D;
		double bestSq = 0.0D;
		EnumFacing bestFacing = null;

		double minX = this.minX;
		double minY = this.minY;
		double minZ = this.minZ;
		double maxX = this.maxX;
		double maxY = this.maxY;
		double maxZ = this.maxZ;

		// WEST
		if (dx * dx >= AXIS_EPSILON) {
			double t = (minX - start.x) / dx;
			if (t >= 0.0D && t <= 1.0D) {
				double px = start.x + dx * t;
				double py = start.y + dy * t;
				double pz = start.z + dz * t;
				if (py >= minY && py <= maxY && pz >= minZ && pz <= maxZ) {
					found = true;
					bestX = px;
					bestY = py;
					bestZ = pz;
					bestSq = (px - start.x) * (px - start.x) + (py - start.y) * (py - start.y)
							+ (pz - start.z) * (pz - start.z);
					bestFacing = EnumFacing.WEST;
				}
			}
		}

		// EAST
		if (dx * dx >= AXIS_EPSILON) {
			double t = (maxX - start.x) / dx;
			if (t >= 0.0D && t <= 1.0D) {
				double px = start.x + dx * t;
				double py = start.y + dy * t;
				double pz = start.z + dz * t;
				if (py >= minY && py <= maxY && pz >= minZ && pz <= maxZ) {
					double px0 = px - start.x;
					double py0 = py - start.y;
					double pz0 = pz - start.z;
					double sq = px0 * px0 + py0 * py0 + pz0 * pz0;
					if (!found || sq < bestSq) {
						found = true;
						bestX = px;
						bestY = py;
						bestZ = pz;
						bestSq = sq;
						bestFacing = EnumFacing.EAST;
					}
				}
			}
		}

		// DOWN
		if (dy * dy >= AXIS_EPSILON) {
			double t = (minY - start.y) / dy;
			if (t >= 0.0D && t <= 1.0D) {
				double px = start.x + dx * t;
				double py = start.y + dy * t;
				double pz = start.z + dz * t;
				if (px >= minX && px <= maxX && pz >= minZ && pz <= maxZ) {
					double px0 = px - start.x;
					double py0 = py - start.y;
					double pz0 = pz - start.z;
					double sq = px0 * px0 + py0 * py0 + pz0 * pz0;
					if (!found || sq < bestSq) {
						found = true;
						bestX = px;
						bestY = py;
						bestZ = pz;
						bestSq = sq;
						bestFacing = EnumFacing.DOWN;
					}
				}
			}
		}

		// UP
		if (dy * dy >= AXIS_EPSILON) {
			double t = (maxY - start.y) / dy;
			if (t >= 0.0D && t <= 1.0D) {
				double px = start.x + dx * t;
				double py = start.y + dy * t;
				double pz = start.z + dz * t;
				if (px >= minX && px <= maxX && pz >= minZ && pz <= maxZ) {
					double px0 = px - start.x;
					double py0 = py - start.y;
					double pz0 = pz - start.z;
					double sq = px0 * px0 + py0 * py0 + pz0 * pz0;
					if (!found || sq < bestSq) {
						found = true;
						bestX = px;
						bestY = py;
						bestZ = pz;
						bestSq = sq;
						bestFacing = EnumFacing.UP;
					}
				}
			}
		}

		// NORTH
		if (dz * dz >= AXIS_EPSILON) {
			double t = (minZ - start.z) / dz;
			if (t >= 0.0D && t <= 1.0D) {
				double px = start.x + dx * t;
				double py = start.y + dy * t;
				double pz = start.z + dz * t;
				if (px >= minX && px <= maxX && py >= minY && py <= maxY) {
					double px0 = px - start.x;
					double py0 = py - start.y;
					double pz0 = pz - start.z;
					double sq = px0 * px0 + py0 * py0 + pz0 * pz0;
					if (!found || sq < bestSq) {
						found = true;
						bestX = px;
						bestY = py;
						bestZ = pz;
						bestSq = sq;
						bestFacing = EnumFacing.NORTH;
					}
				}
			}
		}

		// SOUTH
		if (dz * dz >= AXIS_EPSILON) {
			double t = (maxZ - start.z) / dz;
			if (t >= 0.0D && t <= 1.0D) {
				double px = start.x + dx * t;
				double py = start.y + dy * t;
				double pz = start.z + dz * t;
				if (px >= minX && px <= maxX && py >= minY && py <= maxY) {
					double px0 = px - start.x;
					double py0 = py - start.y;
					double pz0 = pz - start.z;
					double sq = px0 * px0 + py0 * py0 + pz0 * pz0;
					if (!found || sq < bestSq) {
						found = true;
						bestX = px;
						bestY = py;
						bestZ = pz;
						bestSq = sq;
						bestFacing = EnumFacing.SOUTH;
					}
				}
			}
		}

		return found ? new RayTraceResult(new Vec3d(bestX, bestY, bestZ), bestFacing) : null;
	}
}
//$ end
