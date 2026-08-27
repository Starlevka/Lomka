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

package lomka.starl.mixins.com.mojang.blaze3d.buffers;

import com.mojang.blaze3d.buffers.Std140SizeCalculator;
import net.minecraft.util.Mth;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(Std140SizeCalculator.class)
public abstract class MixinStd140SizeCalculator {

    @Shadow private int size;

    /**
     * @author Starlev
     * @reason Power-of-two fast path for the size-calculating twin: every vanilla put*
     *         calls this.align(constant) first, so a single overwrite replaces every
     *         division-based rounding without touching any other method. Non-power-of-two
     *         alignments fall through to the vanilla body (Mth.roundToward), keeping the
     *         public contract byte-exact for exotic callers.
     */
    @Overwrite
    public Std140SizeCalculator align(int align) {
        if (align > 0 && (align & (align - 1)) == 0) {
            this.size = (this.size + (align - 1)) & -align;
        } else {
            this.size = Mth.roundToward(this.size, align);
        }
        return (Std140SizeCalculator) (Object) this;
    }
}
