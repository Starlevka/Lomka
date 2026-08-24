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

package lomka.starl.mixins.net.minecraft.client.resources.model;

import net.minecraft.client.resources.model.Material;
import net.minecraft.resources.Identifier;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;

@Mixin(Material.class)
public abstract class MixinMaterial {

    @Shadow @Final private Identifier atlasLocation;
    @Shadow @Final private Identifier texture;

    @Unique private int lomka$hashCode;

    /**
     * @author Starlev
     * @reason Caches the material hash so sprite lookups stop allocating - vanilla's
     *         Objects.hash wraps both identifiers in a fresh Object[] on every call, and
     *         AtlasManager.get feeds materialLookup.get on the per-frame sign, banner,
     *         water-overlay and GUI-item draw paths.
     */
    @Overwrite
    public int hashCode() {
        int h = this.lomka$hashCode;
        if (h == 0) {
            h = this.atlasLocation.hashCode() * 31 + this.texture.hashCode();
            if (h == 0) h = 1;
            this.lomka$hashCode = h;
        }
        return h;
    }
}
