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

import java.util.List;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * @author Starlev
 * VoxelShape#clip falls back to AABB.clip(this.toAabbs(), ...) whenever the fast
 * single-voxel endpoint test misses — which is essentially every crosshair raycast
 * frame plus every projectile/tracing query: vanilla toAabbs() allocates a fresh
 * ArrayList and one AABB per box on each call. Shapes are effectively immutable in
 * practice (DiscreteVoxelShape#fill is public but nothing re-fills shipped shapes);
 * vanilla itself already caches VoxelShape#faces under the exact same assumption
 * and never invalidates it, so caching the box list once per shape is no stronger
 * a guarantee than vanilla already makes. The cached list is only ever read
 * (AABB.clip iterates it), never mutated by vanilla call sites.
 * Cancellable HEAD + RETURN pair: any upstream @Redirect/@Inject into toAabbs
 * keeps working — its RETURN result still gets published, and a cancelled HEAD
 * simply bypasses the cache.
 */
@Mixin(VoxelShape.class)
public abstract class MixinVoxelShape {

    @Unique private volatile List<AABB> lomka$aabbCache;

    /**
     * Serves the cached box list on repeat calls instead of reallocating ArrayList + AABBs per raycast.
     */
    @Inject(method = "toAabbs", at = @At("HEAD"), cancellable = true)
    private void lomka$serveCachedAabbs(CallbackInfoReturnable<List<AABB>> cir) {
        List<AABB> cached = this.lomka$aabbCache;

        if (cached != null) {
            cir.setReturnValue(cached);
        }
    }

    /**
     * Publishes the freshly computed list once; subsequent HEAD hits are served from the cache.
     */
    @Inject(method = "toAabbs", at = @At("RETURN"))
    private void lomka$storeAabbs(CallbackInfoReturnable<List<AABB>> cir) {
        this.lomka$aabbCache = cir.getReturnValue();
    }
}
