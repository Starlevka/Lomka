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

package lomka.starl.mixins.net.minecraft.client.gui.font;

import it.unimi.dsi.fastutil.ints.Int2FloatOpenHashMap;
import lomka.starl.duck.IGlyphSource;
import net.minecraft.client.gui.font.FontSet;
import net.minecraft.client.gui.font.glyphs.BakedGlyph;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;

@Mixin(FontSet.Source.class)
public abstract class MixinSource implements IGlyphSource {

    @Unique private static final int LOMKA$MAX_CACHE_ENTRIES = 16384;

    @Unique private final Int2FloatOpenHashMap lomka$advanceCache = new Int2FloatOpenHashMap(16, 0.75F);

    @Shadow protected abstract BakedGlyph getGlyph(int codepoint);

    @Override
    public void lomka$clear() {
        synchronized (this.lomka$advanceCache) {
            this.lomka$advanceCache.clear();
        }
    }

    @Override
    public float getAdvance(int codepoint, boolean bold) {
        int key = codepoint << 1 | (bold ? 1 : 0);
        synchronized (this.lomka$advanceCache) {
            float cached = this.lomka$advanceCache.getOrDefault(key, Float.NaN);
            if (!Float.isNaN(cached)) {
                return cached;
            }
            float advance = this.getGlyph(codepoint).info().getAdvance(bold);
            if (this.lomka$advanceCache.size() >= LOMKA$MAX_CACHE_ENTRIES) {
                this.lomka$advanceCache.clear();
            }
            this.lomka$advanceCache.put(key, advance);
            return advance;
        }
    }
}