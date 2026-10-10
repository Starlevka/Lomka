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

package lomka.starl.mixins.com.mojang.blaze3d.vertex;

import com.mojang.blaze3d.vertex.UberGpuBuffer;
import it.unimi.dsi.fastutil.objects.Object2ObjectMap;
import it.unimi.dsi.fastutil.objects.ObjectIterator;
import it.unimi.dsi.fastutil.objects.ObjectSet;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.Slice;

@Mixin(UberGpuBuffer.class)
public abstract class MixinUberGpuBuffer {

    /**
     * Entries stay local to one upload iteration, so fastutil may reuse its entry object.
     * Start after entrySet(): the preceding key iterator must still free all old allocations first.
     */
    @Redirect(
            method = "uploadStagedAllocations",
            slice = @Slice(from = @At(value = "INVOKE", target = "Lit/unimi/dsi/fastutil/objects/Object2ObjectOpenHashMap;entrySet()Lit/unimi/dsi/fastutil/objects/ObjectSet;")),
            at =@At(value = "INVOKE", target = "Lit/unimi/dsi/fastutil/objects/ObjectSet;iterator()Lit/unimi/dsi/fastutil/objects/ObjectIterator;"),
            require = 1, expect = 1, allow = 1
    )
    private ObjectIterator<?> lomka$fastStagedEntries(ObjectSet<?> entries) {
        if (entries instanceof Object2ObjectMap.FastEntrySet<?, ?> fastEntries) {
            return fastEntries.fastIterator();
        }
        return entries.iterator();
    }
}
