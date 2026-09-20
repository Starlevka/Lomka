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
import net.minecraft.world.level.chunk.PalettedContainer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;

@Mixin(PalettedContainer.class)
public abstract class MixinPalettedContainer<T> implements IPalettedContainer {

    @Shadow private volatile PalettedContainer.Data<T> data;

    /*
     * Uniform-section cache as a (value, data) pair. The value is written with a
     * plain store and published by the volatile store of the matching Data
     * reference; readers acquire on the volatile Data load, so the pair is always
     * observed consistently. Only uniform sections (zero-bit storage + single-entry
     * palette) are cached; heterogeneous sections never allocate.
     */
    @Unique private volatile PalettedContainer.Data<T> lomka$uniformData;
    @Unique private T lomka$uniformValue;

    /**
     * @author Starlev
     * @reason Uniform-section fast path. A palette holding exactly one entry with
     *         zero-bit storage (air, deep stone, ocean water) is provably immutable for the
     *         lifetime of its Data record: any new distinct value overflows SingleValuePalette
     *         and routes through PaletteResize#onResize, which swaps this.data for a fresh
     *         identity. Such sections resolve with one volatile load plus a reference identity
     *         check and a plain value load, skipping the quadrimorphic Palette#valueFor
     *         dispatch (Single/Linear/Hash/Global palettes defeat JIT inlining) and
     *         BitStorage#get bit arithmetic on the hottest read path in the game (collision,
     *         lighting, chunk meshing, ticking). Non-uniform sections fall through to the
     *         exact vanilla path.
     */
    @Overwrite
    protected T get(int index) {
        PalettedContainer.Data<T> d = this.data;

        if (this.lomka$uniformData == d) {
            return this.lomka$uniformValue;
        }

        T value = d.palette().valueFor(d.storage().get(index));

        if (d.storage().getBits() == 0 && d.palette().getSize() == 1) {
            this.lomka$uniformValue = value;
            this.lomka$uniformData = d;
        }

        return value;
    }

    /*
     * Uniform-value probe for consumers that would otherwise sample a whole section
     * (MixinHeightmap, see IPalettedContainer). The answer is parked in the same
     * (value, volatile Data) pair the hot path uses, so it warms the cache instead of
     * duplicating it: the volatile Data store publishes the plain value store, and a
     * later get() on the same container is served from it. Heterogeneous containers
     * return null and are never cached - the same discipline as get().
     */
    @Override
    @Unique
    public Object lomka$uniformValue() {
        PalettedContainer.Data<T> d = this.data;

        if (this.lomka$uniformData == d) {
            return this.lomka$uniformValue;
        }

        if (d.storage().getBits() != 0 || d.palette().getSize() != 1) {
            return null;
        }

        T value = d.palette().valueFor(0);
        this.lomka$uniformValue = value;
        this.lomka$uniformData = d;

        return value;
    }
}
