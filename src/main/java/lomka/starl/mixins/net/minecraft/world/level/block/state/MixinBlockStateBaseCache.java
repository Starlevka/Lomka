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

package lomka.starl.mixins.net.minecraft.world.level.block.state;

import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.SupportType;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(targets = "net.minecraft.world.level.block.state.BlockBehaviour$BlockStateBase$Cache")
public abstract class MixinBlockStateBaseCache {

    @Shadow @Final private boolean[] faceSturdy;
    @Shadow @Final private static int SUPPORT_TYPE_COUNT;

    @Unique private long lomka$faceSturdyMask;

    /**
     * @author Starlev
     * @reason long instead of int: Java masks shift amounts to the operand's low bits
     *         (5 bits for int), so "1 << 32" silently behaves as "1 << 0". With int this only
     *         stays correct while Direction.values().length * SUPPORT_TYPE_COUNT <= 32; long
     *         raises that ceiling to 64, covering SupportType growth across the full 1.21.1-26.2
     *         range without two indices silently aliasing onto the same bit (which corrupts
     *         results via OR for whichever earlier entry shares that bit, not just the
     *         overflowing one).
     */
    @Inject(method = "<init>", at = @At("TAIL"))
    private void lomka$initSturdyMask(BlockState blockstate, CallbackInfo ci) {
        long mask = 0L;
        boolean[] array = this.faceSturdy;
        for (int i = 0, len = array.length; i < len && i < 64; i++) {
            if (array[i]) {
                mask |= (1L << i);
            }
        }
        this.lomka$faceSturdyMask = mask;
    }

    /**
     * @author Starlev
     * @reason Trades one array bounds-check + load for one shift + mask + compare on
     *         an 18-entry (currently) array that's already near-free. Value comes from call
     *         volume: isFaceSturdy() runs on redstone dust connectivity and crop/sapling
     *         growth checks, both legitimately hot server-tick paths for large builds/farms.
     */
    @Overwrite
    public boolean isFaceSturdy(Direction direction, SupportType supporttype) {
        int index = direction.ordinal() * SUPPORT_TYPE_COUNT + supporttype.ordinal();
        if (index >= 64) {
            return this.faceSturdy[index];
        }
        return (this.lomka$faceSturdyMask & (1L << index)) != 0L;
    }
}