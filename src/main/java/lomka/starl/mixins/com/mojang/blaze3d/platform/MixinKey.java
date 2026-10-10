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

package lomka.starl.mixins.com.mojang.blaze3d.platform;

import java.util.Objects;
import com.mojang.blaze3d.platform.InputConstants;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(InputConstants.Key.class)
public abstract class MixinKey {

    @Shadow @Final private InputConstants.Type type;
    @Shadow @Final private int value;

    /**
     * @author Starlev
     * @reason Computes the same two-element Objects.hash result directly, avoiding
     *         the temporary Object array and boxed key code during keybinding lookups.
     *         Preserves null handling and integer overflow of the original hash.
     */
    @Overwrite
    public int hashCode() {
        return 31 * (31 + Objects.hashCode(this.type)) + this.value;
    }
}
