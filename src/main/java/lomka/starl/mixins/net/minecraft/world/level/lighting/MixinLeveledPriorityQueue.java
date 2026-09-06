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

package lomka.starl.mixins.net.minecraft.world.level.lighting;

import it.unimi.dsi.fastutil.longs.LongLinkedOpenHashSet;
import net.minecraft.world.level.lighting.LeveledPriorityQueue;
import net.minecraft.world.level.lighting.SpatialLongSet;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LeveledPriorityQueue.class)
public class MixinLeveledPriorityQueue {

    @Shadow @Final private LongLinkedOpenHashSet[] queues;

    /**
     * Replaces the vanilla LongLinkedOpenHashSet queues with SpatialLongSet,
     * which packs up to 64 light positions sharing a 4×4×4 key into a single
     * Long2LongLinkedOpenHashMap entry. SpatialLongSet itself extends
     * LongLinkedOpenHashSet, so super's table is wasted; passing the original
     * j (512) keeps super at ~1024 slots (~8KB) like vanilla instead of
     * j*64 (32768 -> 65536 slots ~512KB, 64x blowup, 8MB per engine).
     * The packed map inside uses j/64 outer keys (~8) and rehashes once,
     * still saving hash-table churn on the hot light queues
     * (DynamicGraphMinFixedPoint). dequeue() calls remove(long), but
     * SpatialLongSet renames removal to rem(long); the override below
     * reroutes remove -> rem so pending nodes are still deleted correctly.
     * Actually, Mojang don't have uses of SpatialLongSet at all. So this fixes that.
     */
    @Inject(method = "<init>", at = @At("RETURN"))
    private void lomka$spatialQueues(int i, int j, CallbackInfo ci) {
        for (int k = 0; k < this.queues.length; ++k) {
            this.queues[k] = new SpatialLongSet(j, 0.5F) {
                @Override
                public boolean remove(long key) {
                    return this.rem(key);
                }
            };
        }
    }
}