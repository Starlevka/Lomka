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

package lomka.starl.mixins.net.minecraft.world.level.chunk;

import lomka.starl.duck.IPalettedContainer;
import lomka.starl.utils.cache.UniformPaletteCache;
import net.minecraft.world.level.chunk.PalettedContainer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(PalettedContainer.class)
public abstract class MixinPalettedContainer<T> implements IPalettedContainer {

    @Shadow private volatile PalettedContainer.Data<T> data;

    /*
     * Uniform-section cache: one immutable (Data, value) holder behind a single volatile
     * reference, so the pair is published atomically. A plain value field next to a volatile Data
     * field is NOT equivalent - two concurrent fillers can interleave and leave the Data
     * reference paired with another section's value, which would hand back a wrong block. The
     * holder is allocated once per uniform Data generation, not per read, so the hot path stays
     * one volatile load, one identity check and one plain load.
     *
     * Only uniform sections (zero-bit storage + single-entry palette) are cached; heterogeneous
     * ones never allocate and fall through to the vanilla lookup.
     *
     * Identity is a valid key because a uniform section cannot change value under a stable Data:
     * ZeroBitStorage#set/getAndSet are validated no-ops that require value 0, so any real change
     * overflows the palette and routes through PaletteResize#onResize, which installs a fresh
     * Data. The one exception is read(), whose createOrReuseData returns the *same* Data when
     * the incoming bit count maps to the current Configuration and then overwrites palette and
     * raw storage in place - hence the explicit hook below. onResize stays as a second guard.
     * The set/getAndSet/getAndSetUnchecked hooks this mixin used to carry were provably
     * redundant (zero-bit storage cannot be written) and cost an injected call per block write.
     */
    @Unique private volatile UniformPaletteCache<T> lomka$uniform;

    @Unique
    private void lomka$invalidateUniform() {
        this.lomka$uniform = null;
    }

    @Inject(method = "read", at = @At("HEAD"))
    private void lomka$invalidateBeforeRead(CallbackInfo ci) {
        this.lomka$invalidateUniform();
    }

    @Inject(method = "onResize", at = @At("HEAD"))
    private void lomka$invalidateBeforeResize(CallbackInfoReturnable<Integer> cir) {
        this.lomka$invalidateUniform();
    }

    @Override
    @Unique
    public Object lomka$uniformValue() {
        PalettedContainer.Data<T> d = this.data;
        UniformPaletteCache<T> cached = this.lomka$uniform;

        if (cached != null && cached.data() == d) {
            return cached.value();
        }

        if (d.storage().getBits() != 0 || d.palette().getSize() != 1) {
            return null;
        }

        T value = d.palette().valueFor(0);
        this.lomka$uniform = new UniformPaletteCache<>(d, value);

        return value;
    }

    /**
     * @author Starlev
     * @reason Caches the value of zero-bit uniform palettes (air, deep stone, ocean water) so a
     *         read costs one volatile load, an identity check and a plain load, skipping the
     *         quadrimorphic Palette#valueFor dispatch and BitStorage#get bit arithmetic. The pair
     *         lives in one immutable holder so concurrent fillers cannot publish a mismatched
     *         Data/value combination. Non-uniform sections take the exact vanilla path.
     *         Declared public even though vanilla's is protected: Mixin resolves the target
     *         signature as PUBLIC and rejects an @Overwrite that would reduce it, which aborts
     *         the launch with "cannot reduce visibility of PUBLIC target method".
     *         (Measured figures live in scripts/bench, not in source comments.)
     */
    @Overwrite
    public T get(int index) {
        PalettedContainer.Data<T> d = this.data;
        UniformPaletteCache<T> cached = this.lomka$uniform;

        if (cached != null && cached.data() == d) {
            return cached.value();
        }

        T value = d.palette().valueFor(d.storage().get(index));

        if (d.storage().getBits() == 0 && d.palette().getSize() == 1) {
            this.lomka$uniform = new UniformPaletteCache<>(d, value);
        }

        return value;
    }
}
