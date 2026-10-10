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

package lomka.starl.mixins.net.minecraft.client.renderer.block;

import net.minecraft.client.renderer.block.BlockAndTintGetter;
import net.minecraft.client.renderer.block.BlockModelLighter;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Memoises repeated state reads while a section's immutable render region is being meshed.
 * The window belongs to the constructing thread, like vanilla's captured ThreadLocal Cache.
 * Changing the region or window invalidates the table. Collision-shape queries remain vanilla:
 * ordinary states already cache their result.
 */
@Mixin(value = BlockModelLighter.class, priority = 500)
public abstract class MixinBlockModelLighter {

    /** Slot 0 is the generation; slot 1 is the enabled flag. Mutated only by the owning thread. */
    @Unique private static final ThreadLocal<long[]> lomka$windows = ThreadLocal.withInitial(() -> new long[2]);
    @Unique private final long[] lomka$window = lomka$windows.get();
    @Unique private final BlockState[] lomka$stateValues = new BlockState[512];
    @Unique private final long[] lomka$stateKeys         = new long[512];
    @Unique private int lomka$stateFill;
    @Unique private long lomka$seenGeneration = -1;
    @Unique private BlockAndTintGetter lomka$stateLevel;

    /* All 15 static call sites are patched; conditional branches execute only a subset per quad. */
    @Redirect(
        method = "prepareQuadAmbientOcclusion",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/renderer/block/BlockAndTintGetter;getBlockState"
                   + "(Lnet/minecraft/core/BlockPos;)Lnet/minecraft/world/level/block/state/BlockState;"
            ),
            require = 15, expect = 15, allow = 15
    )
    private BlockState lomka$memoisedState(BlockAndTintGetter level, BlockPos pos) {
        long[] window = this.lomka$window;
        if (window[1] == 0) {
            return level.getBlockState(pos);
        }
        if (this.lomka$seenGeneration != window[0] || this.lomka$stateLevel != level) {
            this.lomka$clearStates();
            this.lomka$seenGeneration = window[0];
            this.lomka$stateLevel     = level;
        }
        long key = pos.asLong();
        int mask = this.lomka$stateValues.length - 1;
        int slot = lomka$stateHash(key) & mask;
        while (this.lomka$stateValues[slot] != null) {
            if (this.lomka$stateKeys[slot] == key) {
                return this.lomka$stateValues[slot];
            }
            slot = (slot + 1) & mask;
        }
        BlockState state = level.getBlockState(pos);
        if (this.lomka$stateFill >= ((mask + 1) * 3 >> 2)) {
            this.lomka$clearStates();
            slot = lomka$stateHash(key) & mask;
        }
        this.lomka$stateKeys[slot]   = key;
        this.lomka$stateValues[slot] = state;
        this.lomka$stateFill++;
        return state;
    }

    @Unique
    private static int lomka$stateHash(long key) {
        key ^= key >>> 33;
        key *= 0xff51afd7ed558ccdL;
        key ^= key >>> 33;
        key *= 0xc4ceb9fe1a85ec53L;
        return (int) (key ^ (key >>> 33));
    }

    /* Re-opening also invalidates entries if a previous caller did not close its window. */
    @Inject(method = "enableCaching", at = @At("HEAD"))
    private static void lomka$openWindow(CallbackInfo ci) {
        long[] window = lomka$windows.get();
        window[0]++;
        window[1] = 1;
    }

    @Inject(method = "clearCache", at = @At("HEAD"))
    private static void lomka$closeWindow(CallbackInfo ci) {
        long[] window = lomka$windows.get();
        window[1] = 0;
        window[0]++;
    }

    @Unique
    private void lomka$clearStates() {
        java.util.Arrays.fill(this.lomka$stateValues, null);
        this.lomka$stateFill = 0;
    }
}
