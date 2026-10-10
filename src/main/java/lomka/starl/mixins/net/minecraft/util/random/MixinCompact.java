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

package lomka.starl.mixins.net.minecraft.util.random;

import net.minecraft.util.random.Weighted;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

/**
 * Uses prefix sums for large Compact selectors and retains vanilla's scan for small ones.
 * WeightedList chooses Compact by total weight, so its entry count can still be very small.
 * Prefix storage is allocated only from 64 entries, and only when a weighted draw would
 * inspect at least 32 entries on average. Skew towards the front retains the cheap scan.
 * These thresholds select the algorithm; they are not an FPS claim.
 *
 * <p>WeightedList validates nonnegative weights and a total at most Integer.MAX_VALUE.
 * Valid draws find the first cumulative weight strictly above the selection. Other selections
 * use the original subtraction scan, preserving overflow and exception text.
 *
 * <p>Registered in the common >=1.21.11 mixin list. Vendored Lithium changes the outer
 * WeightedList constructor; C2ME accesses ShufflingList. Neither overwrites Compact.get.
 */
@Mixin(targets = "net.minecraft.util.random.WeightedList$Compact")
public abstract class MixinCompact {

    @Unique private static final int lomka$binaryThreshold     = 64;
    @Unique private static final int lomka$minimumExpectedScan = 32;
    @Unique private int[] lomka$cumul;

    @Shadow @Final private Weighted<?>[] entries;

    /* The target assigns its final entries before this constructor return. */
    @Inject(method = "<init>", at = @At("RETURN"))
    private void lomka$buildPrefixSums(List<Weighted<?>> list, CallbackInfo ci) {
        Weighted<?>[] entries = this.entries;
        if (entries.length < lomka$binaryThreshold) {
            this.lomka$cumul = null;
            return;
        }
        int  running  = 0;
        long scanWork = 0;
        for (int i = 0; i < entries.length; i++) {
            int weight = entries[i].weight();
            running += weight;
            // A directly constructed private selector can bypass WeightedList's total validation.
            if (running < 0) {
                this.lomka$cumul = null;
                return;
            }
            scanWork += (long) weight * (i + 1L);
        }
        if (running == 0 || scanWork < (long) running * lomka$minimumExpectedScan) {
            this.lomka$cumul = null;
            return;
        }
        int[] cumul = new int[entries.length];
        running = 0;
        for (int i = 0; i < entries.length; i++) {
            running += entries[i].weight();
            cumul[i] = running;
        }
        this.lomka$cumul = cumul;
    }

    /**
     * @author Starlev
     * @reason Searches immutable cumulative weights for large selectors while retaining vanilla's
     *         scan for small selectors and exceptional inputs, including its exception message.
     */
    @Overwrite
    public Object get(int selection) {
        int[] cumul = this.lomka$cumul;
        if (cumul == null || selection < 0 || selection >= cumul[cumul.length - 1]) {
            return this.lomka$linearScan(selection);
        }
        if (selection < cumul[0]) {
            return this.entries[0].value();
        }
        int lo = 1;
        int remaining = cumul.length - 1;
        while (remaining > 1) {
            int half = remaining >>> 1;
            lo += cumul[lo + half - 1] <= selection ? half : 0;
            remaining -= half;
        }
        return this.entries[lo].value();
    }

    @Unique
    private Object lomka$linearScan(int selection) {
        Weighted<?>[] entries = this.entries;
        for (int i = 0; i < entries.length; ++i) {
            Weighted<?> entry = entries[i];
            selection -= entry.weight();
            if (selection < 0) {
                return entry.value();
            }
        }
        throw new IllegalStateException("" + selection + " exceeded total weight");
    }
}
