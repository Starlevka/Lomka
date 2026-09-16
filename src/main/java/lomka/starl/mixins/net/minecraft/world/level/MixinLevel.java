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

package lomka.starl.mixins.net.minecraft.world.level;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.function.Predicate;
import net.minecraft.world.entity.Entity;
//? if <1.21.11 {
/*import net.minecraft.world.entity.boss.EnderDragonPart;
 *///?} else {
import net.minecraft.world.entity.boss.enderdragon.EnderDragonPart;
//?}
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.entity.LevelEntityGetter;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.phys.AABB;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
//? if >=1.21.4 {
import net.minecraft.util.profiling.Profiler;
//?}

@Mixin(Level.class)
public abstract class MixinLevel {

    @Shadow protected abstract LevelEntityGetter<Entity> getEntities();

    //? if <1.21.4 {
    /*@Shadow public abstract ProfilerFiller getProfiler();
     *///?}

    //? if >=1.21.4 {
    @Shadow public abstract Collection<EnderDragonPart> dragonParts();
    //?}

    /*
     * Reentrancy guard: nested queries issued from inside a predicate (or from a
     * different thread context) must never observe their own scratch list being
     * cleared, so they take the exact vanilla allocation path.
     */
    @Unique private static final ThreadLocal<int[]> lomka$queryDepth = ThreadLocal.withInitial(() -> new int[1]);

    /*
     * Thread-confined scratch list for the entity query funnel. Every vanilla call
     * site of this overload (entity push, movement collision shapes, explosions,
     * piston/tripwire/shulker/container scans) consumes the result within the same
     * call frame, so handing out one reused list per thread is safe. The generic
     * EntityTypeTest overload is deliberately untouched: BellBlockEntity stores its
     * query result in a field across ticks and would observe a clobbered list.
     */
    @Unique private static final ThreadLocal<ArrayList<Entity>> lomka$scratch = ThreadLocal.withInitial(ArrayList::new);

    @Unique
    private void lomka$collect(Entity entity, AABB aabb, Predicate<? super Entity> predicate, List<Entity> output) {
        this.getEntities().get(aabb, (candidate) -> {
            if (candidate != entity && predicate.test(candidate)) {
                output.add(candidate);
            }

            //? if <1.21.4 {
            /*
            if (candidate instanceof EnderDragon) {
                EnderDragonPart[] parts = ((EnderDragon) candidate).getSubEntities();

                for (EnderDragonPart part : parts) {
                    if (candidate != entity && predicate.test(part)) {
                        output.add(part);
                    }
                }
            }
            *///?}
        });

        //? if >=1.21.4 {
        Collection<EnderDragonPart> parts = this.dragonParts();

        if (!parts.isEmpty()) {
            Iterator<EnderDragonPart> iterator = parts.iterator();

            while (iterator.hasNext()) {
                EnderDragonPart part = iterator.next();

                if (part != entity && part.parentMob != entity && predicate.test(part) && aabb.intersects(part.getBoundingBox())) {
                    output.add(part);
                }
            }
        }
        //?}
    }

    /**
     * @author Starlev
     * @reason Fills a thread-confined scratch list instead of allocating a fresh
     *         ArrayList per query. This overload is the hottest entity query in the game:
     *         it backs Entity.pushEntities (every entity, every tick), movement collision
     *         shape collection, explosions, and piston/tripwire/shulker/hopper scans, each
     *         of which allocates only to iterate the result immediately. A ThreadLocal
     *         depth guard routes reentrant queries through the exact vanilla allocation
     *         path, so a fill in progress can never observe its own scratch being cleared.
     *         The generic EntityTypeTest overload is intentionally left vanilla because
     *         BellBlockEntity retains its result across ticks.
     */
    @Overwrite
    public List<Entity> getEntities(Entity entity, AABB aabb, Predicate<? super Entity> predicate) {
        //? if <1.21.4 {
        /*this.getProfiler().incrementCounter("getEntities");
         *///?} else {
        Profiler.get().incrementCounter("getEntities");
        //?}

        int[] depth = lomka$queryDepth.get();

        if (depth[0] != 0) {
            ArrayList<Entity> fresh = new ArrayList<>();
            this.lomka$collect(entity, aabb, predicate, fresh);
            return fresh;
        }

        depth[0] = 1;
        try {
            ArrayList<Entity> scratch = lomka$scratch.get();
            scratch.clear();
            this.lomka$collect(entity, aabb, predicate, scratch);
            return scratch;
        } finally {
            depth[0] = 0;
        }
    }
}
