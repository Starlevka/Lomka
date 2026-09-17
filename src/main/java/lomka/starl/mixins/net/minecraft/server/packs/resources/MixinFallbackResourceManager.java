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

package lomka.starl.mixins.net.minecraft.server.packs.resources;

import com.google.common.collect.Maps;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
//? if <26.3 {
import java.util.function.Predicate;
//?}
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.PackResources;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.resources.FallbackResourceManager;
//? if >=26.3 {
/*import net.minecraft.server.packs.resources.ResourceManager;
*///?}
import net.minecraft.server.packs.resources.IoSupplier;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceMetadata;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;

@Mixin(FallbackResourceManager.class)
public abstract class MixinFallbackResourceManager {

    @Shadow @Final protected List<FallbackResourceManager.PackEntry> fallbacks;
    @Shadow @Final private PackType type;
    @Shadow @Final private String namespace;

    @Shadow
    private static ResourceMetadata parseMetadata(IoSupplier<InputStream> supplier) throws IOException {
        throw new AssertionError();
    }

    @Unique
    private static boolean lomka$isMetadata(Identifier id) {
        return id.getPath().endsWith(".mcmeta");
    }

    @Unique
    private static Identifier lomka$getIdentifierFromMetadata(Identifier id) {
        String path = id.getPath();
        return id.withPath(path.substring(0, path.length() - 7));
    }

    @Unique private static final Logger lomka$LOGGER = LoggerFactory.getLogger(FallbackResourceManager.class);

    @Unique
    private static IoSupplier<ResourceMetadata> lomka$convertToMetadata(IoSupplier<InputStream> supplier) {
        return () -> parseMetadata(supplier);
    }

    //? if >=26.3 {
    /*@Unique private static boolean lomka$isIncluded(ResourceManager.Selector filter, Identifier id) {
        return filter.isIncluded(id);
    }*/
    //?} else {
    @Unique private static boolean lomka$isIncluded(Predicate<Identifier> filter, Identifier id) {
        return filter.test(id);
    }
    //?}

    @Unique private record Entry(PackResources source, IoSupplier<InputStream> resource, int packIndex) {}

    @Shadow
    private static IoSupplier<InputStream> wrapForDebug(Identifier id, PackResources pack, IoSupplier<InputStream> supplier) {
        throw new AssertionError();
    }

    /**
     * @author Starlev
     * @reason Fast-path metadata check to eliminate thousands of useless .mcmeta Identifier allocations and Map lookups;
     *         a derived base->meta index (built after pack filtering, so filterAll keeps exact vanilla
     *         META-id semantics across all versions) makes the final merge a zero-alloc direct get per file;
     *         cached static logger skips a synchronized registry lookup per call. Debug-wrapped input stream
     *         is preserved when log is in debug to keep LeakedResourceWarning parity with vanilla. Composes
     *         with ModernFix's FilePackResources index and quick-pack's entries() wrap: both serve the
     *         per-pack listResources callback we consume, never the methods overwritten here.
     *         (Measured figures live in scripts/bench, not in source comments.)
     */
    @Overwrite
    //? if >=26.3 {
    /*public Map<Identifier, Resource> listResources(String directory, ResourceManager.Selector filter) {*/
    //?} else {
    public Map<Identifier, Resource> listResources(String directory, Predicate<Identifier> filter) {
    //?}
        Map<Identifier, Entry> fileEntries = new HashMap<>();
        Map<Identifier, Entry> metaEntries = new HashMap<>();
        int count = this.fallbacks.size();

        for (int i = 0; i < count; ++i) {
            FallbackResourceManager.PackEntry entry = this.fallbacks.get(i);
            entry.filterAll(fileEntries.keySet());
            entry.filterAll(metaEntries.keySet());
            PackResources pack = entry.resources();
            if (pack != null) {
                int packIndex = i;
                pack.listResources(this.type, this.namespace, directory, (id, streamSupplier) -> {
                    if (lomka$isMetadata(id)) {
                        if (lomka$isIncluded(filter, lomka$getIdentifierFromMetadata(id))) {
                            metaEntries.put(id, new Entry(pack, streamSupplier, packIndex));
                        }
                    } else if (lomka$isIncluded(filter, id)) {
                        fileEntries.put(id, new Entry(pack, streamSupplier, packIndex));
                    }
                });
            }
        }

        TreeMap<Identifier, Resource> result = Maps.newTreeMap();
        boolean debug = lomka$LOGGER.isDebugEnabled();

        if (metaEntries.isEmpty()) {
            fileEntries.forEach((id, entry) -> {
                IoSupplier<InputStream> supplier = debug
                        ? wrapForDebug(id, entry.source, entry.resource)
                        : entry.resource;
                result.put(id, new Resource(entry.source, supplier));
            });
            return result;
        }

        // Derived base->meta index: built AFTER all pack filters ran, so filterAll keeps
        // exact vanilla META-id semantics (filters are path regexes and CAN distinguish "x"
        // from "x.mcmeta"; PackEntry's filter accessor is version-specific, so re-keying
        // before filtering is NOT version-agnostic). One base-id alloc per META (rare path)
        // instead of one file+".mcmeta" concat per FILE; the hot loop below is then a
        // zero-alloc direct get. (Figures: scripts/bench.)
        Map<Identifier, Entry> metaByBase = new HashMap<>(metaEntries.size() * 2 + 1);
        metaEntries.forEach((metaId, metaEntry) ->
            metaByBase.put(lomka$getIdentifierFromMetadata(metaId), metaEntry));

        // Kept as forEach (not an entrySet loop): HashMap.forEach's internal table walk
        // measured faster than the loop. (Figures: scripts/bench.)
        fileEntries.forEach((id, entry) -> {
            Entry metaEntry = metaByBase.get(id);
            IoSupplier<InputStream> resSupplier = debug
                    ? wrapForDebug(id, entry.source, entry.resource)
                    : entry.resource;
            if (metaEntry != null && metaEntry.packIndex >= entry.packIndex) {
                result.put(id, new Resource(entry.source, resSupplier, lomka$convertToMetadata(metaEntry.resource)));
            } else {
                result.put(id, new Resource(entry.source, resSupplier));
            }
        });

        return result;
    }
}
