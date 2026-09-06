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

package lomka.starl.mixins.net.minecraft.world.phys.shapes;

import com.google.common.collect.Lists;
import java.util.Collections;
import java.util.List;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;

@Mixin(VoxelShape.class)
public abstract class MixinVoxelShape {

    @Shadow public abstract void forAllBoxes(Shapes.DoubleLineConsumer shapes_doublelineconsumer);

    @Unique private volatile List<AABB> lomka$aabbCache;

    /**
     * @author Starlev
     * @reason Caches computed AABB list on first access to eliminate ArrayList
     *         and AABB allocations on hot-path raycasting and collision queries.
     */
    @Overwrite
    public List<AABB> toAabbs() {
        List<AABB> list = this.lomka$aabbCache;
        if (list == null) {
            List<AABB> arrayList = Lists.newArrayList();
            this.forAllBoxes((d0, d1, d2, d3, d4, d5) -> {
                arrayList.add(new AABB(d0, d1, d2, d3, d4, d5));
            });
            this.lomka$aabbCache = list = Collections.unmodifiableList(arrayList);
        }
        return list;
    }
}
