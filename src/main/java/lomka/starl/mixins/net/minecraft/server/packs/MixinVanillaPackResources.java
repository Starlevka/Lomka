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

package lomka.starl.mixins.net.minecraft.server.packs;

import com.google.gson.JsonObject;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.JsonOps;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.PackResources;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.PathPackResources;
import net.minecraft.server.packs.VanillaPackResources;
import net.minecraft.server.packs.resources.IoSupplier;
import net.minecraft.util.GsonHelper;
//? if >=1.21.11 {
import net.minecraft.util.FileUtil;
//?} else {
/*import net.minecraft.FileUtil;
*///?}
//? if >=1.21.4 {
import net.minecraft.server.packs.metadata.MetadataSectionType;
//?} else {
/*import net.minecraft.server.packs.metadata.MetadataSectionSerializer;
*///?}
//? if <26.1 {
import net.minecraft.server.packs.BuiltInMetadata;
//?}
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;

@Mixin(VanillaPackResources.class)
public abstract class MixinVanillaPackResources implements PackResources {

    @Shadow @Final private static Logger LOGGER;
    @Shadow @Final private Set<String> namespaces;
    @Shadow @Final private Map<PackType, List<Path>> pathsForType;

    @Shadow
    private static void getResources(PackResources.ResourceOutput output, String namespace, Path root, List<String> decomposed) {
        throw new AssertionError();
    }

    @Unique private static final LinkOption[] NO_OPTIONS = new LinkOption[0];

    //? if <26.1 {
    @Shadow @Final private BuiltInMetadata metadata;

    @Unique private JsonObject lomka$cachedMetadata;
    @Unique private boolean    lomka$parsed;
    //?}

    /**
     * @author Starlev
     * @reason Fast-path namespace check; validated path segments are re-joined into a single
     *         relative string so each root costs one Path resolution instead of one per segment.
     */
    @Overwrite
    public @Nullable IoSupplier<InputStream> getResource(PackType packType, Identifier identifier) {
        if (!this.namespaces.contains(identifier.getNamespace())) {
            return null;
        }

        List<Path> paths = this.pathsForType.get(packType);
        if (paths == null || paths.isEmpty()) {
            return null;
        }

        DataResult<List<String>> result = FileUtil.decomposePath(identifier.getPath());
        List<String> segments = result.result().orElse(null);
        if (segments == null) {
            LOGGER.error("Invalid path {}: {}", identifier, result.error().map(error -> error.message()).orElse(""));
            return null;
        }

        String relative = identifier.getNamespace() + "/" + String.join("/", segments);
        for (int i = 0; i < paths.size(); i++) {
            Path path = paths.get(i).resolve(relative);
            if (Files.exists(path, NO_OPTIONS) && PathPackResources.validatePath(path)) {
                return IoSupplier.create(path);
            }
        }
        return null;
    }

    /**
     * @author Starlev
     * @reason Fast-path namespace check and eliminated lambda closure overhead
     */
    @Overwrite
    public void listResources(PackType packType, String namespace, String pathPrefix, PackResources.ResourceOutput output) {
        if (!this.namespaces.contains(namespace)) {
            return;
        }

        List<Path> paths = this.pathsForType.get(packType);
        if (paths == null || paths.isEmpty()) {
            return;
        }

        DataResult<List<String>> result = FileUtil.decomposePath(pathPrefix);
        List<String> segments = result.result().orElse(null);
        if (segments == null) {
            LOGGER.error("Invalid path {}: {}", pathPrefix, result.error().map(error -> error.message()).orElse(""));
            return;
        }

        int size = paths.size();
        if (size == 1) {
            getResources(output, namespace, paths.get(0), segments);
        } else {
            Map<Identifier, IoSupplier<InputStream>> map = new HashMap<>();
            for (int j = 0; j < size - 1; ++j) {
                getResources(map::putIfAbsent, namespace, paths.get(j), segments);
            }

            Path lastPath = paths.get(size - 1);
            if (map.isEmpty()) {
                getResources(output, namespace, lastPath, segments);
            } else {
                getResources(map::putIfAbsent, namespace, lastPath, segments);
                map.forEach(output);
            }
        }
    }

    /**
     * @author Starlev
     * @reason Fast-path namespace check and single-resolve per root (see getResource).
     */
    @Overwrite
    public void listRawPaths(PackType packType, Identifier identifier, Consumer<Path> consumer) {
        if (!this.namespaces.contains(identifier.getNamespace())) {
            return;
        }

        List<Path> paths = this.pathsForType.get(packType);
        if (paths == null || paths.isEmpty()) {
            return;
        }

        DataResult<List<String>> result = FileUtil.decomposePath(identifier.getPath());
        List<String> segments = result.result().orElse(null);
        if (segments == null) {
            LOGGER.error("Invalid path {}: {}", identifier, result.error().map(error -> error.message()).orElse(""));
            return;
        }

        String relative = identifier.getNamespace() + "/" + String.join("/", segments);
        for (int i = 0; i < paths.size(); i++) {
            consumer.accept(paths.get(i).resolve(relative));
        }
    }
    /**
     * @author Starlev
     * @reason Cache parsed pack.mcmeta to avoid re-reading and re-parsing JSON for every metadata query.
     */
    //? if <26.1 {
    //? if >=1.21.4 {
    @Overwrite
    public <T> @Nullable T getMetadataSection(MetadataSectionType<T> type) {
        if (!this.lomka$parsed) {
            this.lomka$parsed = true;
            IoSupplier<InputStream> supplier = this.getRootResource("pack.mcmeta");
            if (supplier != null) {
                try (InputStream stream = supplier.get();
                     BufferedReader reader = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8))) {
                    this.lomka$cachedMetadata = GsonHelper.parse((Reader) reader);
                } catch (Exception ignored) {
                }
            }
        }

        T section = this.lomka$cachedMetadata != null && this.lomka$cachedMetadata.has(type.name())
                ? type.codec().parse(JsonOps.INSTANCE, this.lomka$cachedMetadata.get(type.name())).result().orElse(null)
                : null;
        return section != null ? section : this.metadata.get(type);
    }
    //?} else {
    /*@Overwrite
    public <T> @Nullable T getMetadataSection(MetadataSectionSerializer<T> serializer) {
        if (!this.lomka$parsed) {
            this.lomka$parsed = true;
            IoSupplier<InputStream> supplier = this.getRootResource("pack.mcmeta");
            if (supplier != null) {
                try (InputStream stream = supplier.get();
                     BufferedReader reader = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8))) {
                    this.lomka$cachedMetadata = GsonHelper.parse((Reader) reader);
                } catch (Exception ignored) {
                }
            }
        }

        T section = this.lomka$cachedMetadata != null && this.lomka$cachedMetadata.has(serializer.getMetadataSectionName())
                ? serializer.fromJson(GsonHelper.getAsJsonObject(this.lomka$cachedMetadata, serializer.getMetadataSectionName()))
                : null;
        return section != null ? section : this.metadata.get(serializer);
    }
    *///?}
    //?}
}