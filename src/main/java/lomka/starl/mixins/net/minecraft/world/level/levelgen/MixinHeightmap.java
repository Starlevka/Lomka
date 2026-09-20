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

package lomka.starl.mixins.net.minecraft.world.level.levelgen;

import java.util.Set;
import java.util.function.Predicate;
import lomka.starl.duck.IHeightmap;
import lomka.starl.duck.IPalettedContainer;
import net.minecraft.util.BitStorage;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.level.chunk.PalettedContainer;
import net.minecraft.world.level.levelgen.Heightmap;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;

@Mixin(value = Heightmap.class, priority = 999) // Compatibility with others Heightmaps
public abstract class MixinHeightmap implements IHeightmap {

    @Shadow @Final private BitStorage data;

    /*
     * Priming writes into every requested map, not just the one this shadow belongs to,
     * and a @Shadow only resolves against the receiving instance - so the write goes
     * through IHeightmap, implemented here on behalf of every map.
     */
    @Override
    @Unique
    public void lomka$setHeightAt(int index, int height) {
        this.data.set(index, height);
    }

    /**
     * @author Starlev
     * @reason Vanilla reaches every block through a MutableBlockPos lookup on the chunk, walking
     *         Y by Y from the highest filled section down until the requested types are all
     *         satisfied, and rebuilds part of its scratch state per column: up to 26.2 that is a
     *         fresh Set iterator in each of the 256 columns (on top of one ObjectArrayList, one
     *         ObjectListIterator and one MutableBlockPos per call), while 26.3 switched to a
     *         cleared two-list pair and allocates nothing per column anymore. This replaces the
     *         whole thing with state owned by the call - a flat Heightmap[]/Predicate[] pair
     *         resolved once, an int bitmask for "still looking" - and scans columns section by
     *         section in local coordinates, skipping null/hasOnlyAir sections in O(1) and resolving
     *         uniform sections with one predicate evaluation per type instead of a full walk. The
     *         walk order, the write index and the air filter stay vanilla's (see the notes below),
     *         so every primed heightmap is bit-identical and nothing is allocated inside the
     *         column loops; the per-Y lookup walk this removes is present in every version, so the
     *         win does not depend on the 26.3 scratch-state change.
     */
    @Overwrite
    public static void primeHeightmaps(ChunkAccess chunk, Set<Heightmap.Types> types) {
        if (types.isEmpty()) {
            return;
        }

        int requested = types.size();
        IHeightmap[] maps = new IHeightmap[requested];
        @SuppressWarnings("unchecked")
        Predicate<BlockState>[] predicates = new Predicate[requested];
        int count = 0;
        for (Heightmap.Types type : types) {
            maps[count] = (IHeightmap) chunk.getOrCreateHeightmapUnprimed(type);
            predicates[count] = type.isOpaque();
            count++;
        }

        // Heightmap.Types has six constants, so the pending set always fits in an int
        int initialPending = (1 << count) - 1;

        //? if >=1.21.4 {
        int minY = chunk.getMinY();
        //?} else {
        /*int minY = chunk.getMinBuildHeight();
        *///?}

        LevelChunkSection[] sections = chunk.getSections();

        /*
         * One uniform state per section, null meaning "walk the blocks". A null/non-air
         * section is also what bounds the walk: vanilla starts at the highest filled
         * section (same hasOnlyAir criterion) and above it no block can match.
         */
        BlockState[] uniforms = new BlockState[sections.length];
        int topSection = -1;
        for (int section = 0; section < sections.length; section++) {
            LevelChunkSection levelChunkSection = sections[section];
            if (levelChunkSection == null || levelChunkSection.hasOnlyAir()) {
                continue;
            }

            topSection = section;
            PalettedContainer<BlockState> states = levelChunkSection.getStates();
            if (states instanceof IPalettedContainer) {
                BlockState uniform = (BlockState) ((IPalettedContainer) states).lomka$uniformValue();
                if (uniform != null && !uniform.isAir()) {
                    uniforms[section] = uniform;
                }
            }
        }

        for (int x = 0; x < 16; x++) {
            column:
            for (int z = 0; z < 16; z++) {
                int index   = x + z * 16;
                int pending = initialPending;

                for (int section = topSection; section >= 0; section--) {
                    LevelChunkSection levelChunkSection = sections[section];
                    if (levelChunkSection == null || levelChunkSection.hasOnlyAir()) {
                        continue;
                    }

                    BlockState uniform = uniforms[section];
                    if (uniform != null) {
                        /*
                         * The whole section is one state, so it either matches a predicate
                         * everywhere or nowhere. Vanilla's setHeight(x, z, y + 1) stores
                         * y + 1 - minY for the top block of the section, i.e. (section << 4) + 16.
                         */
                        int uniformHeight = (section << 4) + 16;
                        int remaining = pending;

                        do {
                            int type = Integer.numberOfTrailingZeros(remaining);
                            remaining &= remaining - 1;
                            if (predicates[type].test(uniform)) {
                                maps[type].lomka$setHeightAt(index, uniformHeight);
                                pending &= ~(1 << type);
                            }
                        } while (remaining != 0);

                        if (pending == 0) {
                            continue column;
                        }

                        continue;
                    }

                    int baseY = minY + (section << 4);
                    for (int y = 15; y >= 0; y--) {
                        BlockState state = levelChunkSection.getBlockState(x, y, z);
                        // Vanilla tests is(Blocks.AIR) here; no Types predicate matches any
                        // air variant, so skipping all of them visits the same blocks.
                        if (state.isAir()) {
                            continue;
                        }

                        int remaining = pending;
                        do {
                            int type = Integer.numberOfTrailingZeros(remaining);
                            remaining &= remaining - 1;
                            if (predicates[type].test(state)) {
                                maps[type].lomka$setHeightAt(index, baseY + y + 1 - minY);
                                pending &= ~(1 << type);
                            }
                        } while (remaining != 0);

                        if (pending == 0) {
                            continue column;
                        }
                    }
                }
            }
        }
    }
}
