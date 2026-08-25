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
import net.minecraft.world.level.Level;
*///?} else {
import net.minecraft.util.profiling.Profiler;
//?}
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientLevel.class)
public abstract class MixinClientLevel {

    @Unique private static final Map<EntityType<?>, String> lomka$TYPE_NAMES = new IdentityHashMap<>();

    @Shadow protected abstract void tickPassenger(Entity vehicle, Entity passenger);

    /**
     * Replaces the capturing profiler lambda with a cached registered-name
     * lookup; everything else mirrors vanilla byte for byte. The original
     * method is cancelled and reimplemented here. getProfiler() is reached by
     * casting to the real superclass Level (a public method there) instead of
     * an injected duck interface - that interface relied on a separate Level
     * mixin being applied, which could fail to take effect and throw
     * ClassCastException at tick time.
     */
    @Inject(method = "tickNonPassenger", at = @At("HEAD"), cancellable = true)
    private void lomka$tickNonPassenger(Entity entity, CallbackInfo ci) {
        ci.cancel();
        entity.setOldPosAndRot();
        ++entity.tickCount;

        String typeName = lomka$entityName(entity.getType());
        //? if <1.21.4 {
        /*ProfilerFiller profiler = ((Level) (Object) this).getProfiler();
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
