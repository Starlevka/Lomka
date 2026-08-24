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

package lomka.starl.mixins.net.minecraft.client.multiplayer;

import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.Map;

//? if <1.21.4 {
/*import net.minecraft.util.profiling.ProfilerFiller;
*///?} else {
import net.minecraft.util.profiling.Profiler;
//?}
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import lomka.starl.duck.ILevelProfiler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;

@Mixin(ClientLevel.class)
public abstract class MixinClientLevel {

    @Unique private static final Map<EntityType<?>, String> lomka$TYPE_NAMES = new IdentityHashMap<>();

    @Shadow protected abstract void tickPassenger(Entity vehicle, Entity passenger);

    /**
     * @author Starlev
     * @reason Replaces the capturing profiler lambda with a cached registered-name 
     *         lookup; everything else mirrors vanilla byte for byte.
     */
    @Overwrite
    public void tickNonPassenger(Entity entity) {
        entity.setOldPosAndRot();
        ++entity.tickCount;

        String typeName = lomka$entityName(entity.getType());
        //? if <1.21.4 {
        /*ProfilerFiller profiler = ((ILevelProfiler) (Object) this).getProfiler();
        profiler.push(typeName);
        entity.tick();
        profiler.pop();
        *///?} else {
        Profiler.get().push(typeName);
        entity.tick();
        Profiler.get().pop();
        //?}

        Iterator<Entity> iterator = entity.getPassengers().iterator();
        while (iterator.hasNext()) {
            this.tickPassenger(entity, iterator.next());
        }
    }

    @Unique
    private static String lomka$entityName(EntityType<?> type) {
        String name = lomka$TYPE_NAMES.get(type);
        if (name == null) {
            name = BuiltInRegistries.ENTITY_TYPE.getKey(type).toString();
            lomka$TYPE_NAMES.put(type, name);
        }
        return name;
    }
}
