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

package lomka.starl.mixins.net.minecraft.stats;

import java.util.Map;
import net.minecraft.stats.Stat;
import net.minecraft.stats.StatFormatter;
import net.minecraft.stats.StatType;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(StatType.class)
public abstract class MixinStatType<T> {

    @Shadow @Final private Map<T, Stat<T>> map;

    /**
     * @author Starlev
     * @reason Replaces computeIfAbsent's capturing lambda (one allocation per
     *         call) and its redundant get/putIfAbsent/re-get map probes with a
     *         direct get-then-put. This method is the funnel for every stat
     *         registration and value lookup (Stats.* fields, streamCodec), so
     *         the swap removes an allocation and two map lookups from the
     *         per-action stat increment path.
     */
    @Overwrite
    public Stat<T> get(T value, StatFormatter formatter) {
        Stat<T> stat = this.map.get(value);
        if (stat == null) {
            stat = new Stat<>((StatType<T>) (Object) this, value, formatter);
            this.map.put(value, stat);
        }
        return stat;
    }
}