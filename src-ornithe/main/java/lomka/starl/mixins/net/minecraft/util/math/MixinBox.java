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
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.HitResult;

/**
 * Port of the {@code MixinAABB} ray-trace patch. Feather names the class {@code Box} and gives it
 * the modern {@code clip(Vec3d, Vec3d)} entry point returning {@code HitResult}.
 *
 * The candidate points are computed into locals. Everything else is a transcription: the
 * parallel-segment threshold, the inclusive {@code 0..1} parameter range, the inclusive
 * inside test on the other two axes, and the strict {@code <} of the running-closest scan - so ties
 * keep the earlier face, which is what the WEST/EAST/DOWN/UP/NORTH/SOUTH order in vanilla
 * relies on.
 *
 * The bounds are public on the target, so they are read through the target cast rather than
 * shadowed: a shadow of a final field would need a {@code @Final} declaration and buys nothing
 * that a cast does not already give.
 */
@Mixin(Box.class)
public abstract class MixinBox {

	/** Same threshold vanilla's per-axis helper uses. */
	private static final double AXIS_EPSILON = (double) 1.0E-7F;

	/**
	 * @author Starlev
	 * @reason Removes up to six {@code Vec3d} allocations per ray trace by keeping the candidate
	 * 		   point in locals; the face-selection order and the strict-closer comparison are unchanged.
	 */
	@Overwrite
	public HitResult clip(Vec3d start, Vec3d end) {
		Box self = (Box) (Object) this;

		double minX = self.minX;
		double minY = self.minY;
		double minZ = self.minZ;
		double maxX = self.maxX;
		double maxY = self.maxY;
		double maxZ = self.maxZ;

		double dx = end.x - start.x;
		double dy = end.y - start.y;
		double dz = end.z - start.z;

		boolean found = false;
		double bestX  = 0.0D;
		double bestY  = 0.0D;
		double bestZ  = 0.0D;
		double bestSq = 0.0D;
		Direction bestFacing = null;

		// WEST
		if (dx * dx >= AXIS_EPSILON) {
			double t = (minX - start.x) / dx;
			if (t >= 0.0D && t <= 1.0D) {
				double px = start.x + dx * t;
				double py = start.y + dy * t;
				double pz = start.z + dz * t;
				if (py >= minY && py <= maxY && pz >= minZ && pz <= maxZ) {
					found = true;
					bestX  = px;
					bestY  = py;
					bestZ  = pz;
					bestSq = (px - start.x) * (px - start.x) + (py - start.y) * (py - start.y)
					       + (pz - start.z) * (pz - start.z);
					bestFacing = Direction.WEST;
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
					double sq  = px0 * px0 + py0 * py0 + pz0 * pz0;
					if (!found || sq < bestSq) {
						found = true;
						bestX  = px;
						bestY  = py;
						bestZ  = pz;
						bestSq = sq;
						bestFacing = Direction.EAST;
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
						bestFacing = Direction.DOWN;
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
					double sq  = px0 * px0 + py0 * py0 + pz0 * pz0;
					if (!found || sq < bestSq) {
						found = true;
						bestX  = px;
						bestY  = py;
						bestZ  = pz;
						bestSq = sq;
						bestFacing = Direction.UP;
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
					double sq  = px0 * px0 + py0 * py0 + pz0 * pz0;
					if (!found || sq < bestSq) {
						found = true;
						bestX  = px;
						bestY  = py;
						bestZ  = pz;
						bestSq = sq;
						bestFacing = Direction.NORTH;
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
						bestFacing = Direction.SOUTH;
					}
				}
			}
		}

		//$ if >=1.8
		return found ? new HitResult(new Vec3d(bestX, bestY, bestZ), bestFacing) : null;
//$ else
		// 1.7.10 stores the block hit as four ints plus the position, and keeps the face as the
		// enum's id rather than the constant. Its Direction declares EAST before WEST, so
		// ordinal() is not the same number as on 1.8+ - verified against the private id field
		// rather than assumed. The branches above pick their face by named constant, so the
		// geometry above this point is identical across all three versions.
		return found ? new HitResult(0, 0, 0, bestFacing.ordinal(), lomka$vec(bestX, bestY, bestZ)) : null;
//$ end
	}

//$ if >=1.8
//$ else
	// 1.7.10 keeps the three-argument constructor protected and exposes a factory instead; the
	// public constructor only exists from 1.8 on.
	private static Vec3d lomka$vec(double x, double y, double z) {
		return Vec3d.of(x, y, z);
	}
//$ end
}
