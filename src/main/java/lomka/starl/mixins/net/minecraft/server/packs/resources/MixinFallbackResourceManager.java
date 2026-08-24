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
import java.util.function.Predicate;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.PackResources;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.resources.FallbackResourceManager;
import net.minecraft.server.packs.resources.IoSupplier;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceMetadata;
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

    @Unique
    private static Identifier lomka$getMetadataLocation(Identifier id) {
        return id.withPath(id.getPath() + ".mcmeta");
    }

    @Unique
    private static IoSupplier<ResourceMetadata> lomka$convertToMetadata(IoSupplier<InputStream> supplier) {
        return () -> parseMetadata(supplier);
    }

    @Unique private record Entry(PackResources source, IoSupplier<InputStream> resource, int packIndex) {}

    /**
     * @author Starlev
     * @reason Fast-path metadata check to eliminate thousands of useless .mcmeta Identifier allocations and Map lookups;
     *         use direct Resource instantiation with EMPTY_SUPPLIER when metadata is absent. Probably a bugfix.
     */
    @Overwrite
    public Map<Identifier, Resource> listResources(String directory, Predicate<Identifier> filter) {
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
                        if (filter.test(lomka$getIdentifierFromMetadata(id))) {
                            metaEntries.put(id, new Entry(pack, streamSupplier, packIndex));
                        }
                    } else if (filter.test(id)) {
                        fileEntries.put(id, new Entry(pack, streamSupplier, packIndex));
                    }
                });
            }
        }

        TreeMap<Identifier, Resource> result = Maps.newTreeMap();

        if (metaEntries.isEmpty()) {
            fileEntries.forEach((id, entry) -> {
                result.put(id, new Resource(entry.source, entry.resource));
            });
            return result;
        }

        fileEntries.forEach((id, entry) -> {
            Identifier metaId = lomka$getMetadataLocation(id);
            Entry metaEntry = metaEntries.get(metaId);
            if (metaEntry != null && metaEntry.packIndex >= entry.packIndex) {
                result.put(id, new Resource(entry.source, entry.resource, lomka$convertToMetadata(metaEntry.resource)));
            } else {
                result.put(id, new Resource(entry.source, entry.resource));
            }
        });

        return result;
    }
}
