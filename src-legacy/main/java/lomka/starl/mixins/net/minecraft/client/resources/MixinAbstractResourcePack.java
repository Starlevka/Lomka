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
package lomka.starl.mixins.net.minecraft.client.resources;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import org.apache.commons.io.IOUtils;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import net.minecraft.client.resources.AbstractResourcePack;
import net.minecraft.client.resources.data.IMetadataSection;
import net.minecraft.util.ResourceLocation;
//$ if >=1.12
import java.nio.charset.StandardCharsets;
import net.minecraft.client.resources.data.MetadataSerializer;
//$ else
import com.google.common.base.Charsets;
import net.minecraft.client.resources.data.IMetadataSerializer;
//$ end

/**
 * Two ports from the modern build, both on the resource-pack load path, now covering all three
 * legacy versions. The vanilla bodies are the same on 1.7.10 and 1.8.9; what differs is the
 * metadata-serializer type, the charset constant, the generic signature and the
 * {@code ResourceLocation} accessors, and each of those is branched below rather than duplicated.
 *
 * <p><b>pack.mcmeta parse cache.</b> Vanilla re-opens and re-parses {@code pack.mcmeta} on
 * <em>every</em> metadata query. There are three call sites per pack per resource reload on every
 * version - {@code LanguageManager} ("language"), {@code ResourcePackListEntryDefault} and
 * {@code ResourcePackRepository$Entry} (both "pack") - so the cache turns three parses into one.
 * This is a loading-path win, not a frame-path one, and no per-frame claim is made from it. The
 * file is immutable for the lifetime of a pack instance, so the parsed {@link JsonObject} is cached
 * and only the per-section {@code parseMetadataSection} call is repeated - that part genuinely
 * depends on the requested section name and must not be cached.
 *
 * <p><b>{@code String.format} removal.</b> {@code locationToName} builds the per-lookup resource
 * path. Vanilla routes it through {@code String.format("%s/%s/%s", ...)}, which allocates a
 * {@code Formatter}, looks the format locale up and boxes three varargs on every call. It runs from
 * both the stream lookup and the existence check, i.e. once per resource per pack across the whole
 * reload - unlike the metadata cache, this half scales with the resource count. Plain concatenation
 * produces the identical string.
 *
 * <p><b>Exception behaviour is preserved exactly.</b> Vanilla wraps any {@link RuntimeException}
 * from the parse in a {@link JsonParseException} and closes the reader. A missing
 * {@code pack.mcmeta} makes the stream lookup return null, which surfaces through the same path. The
 * cache is only populated on success, so a failing pack re-parses and re-throws on every call, just
 * as vanilla does.
 *
 * <p><b>Known gap, unchanged on all three versions:</b> {@code DefaultResourcePack} <em>overrides</em>
 * {@code getPackMetadata} with its own {@code FileInputStream} body, so this patch does not cover
 * it. That is noted rather than duplicated - two overwrites of the same virtual method on different
 * classes is a conflict waiting to happen, and the vanilla client jar's own pack is read exactly
 * once.
 *
 * @author Starlev
 */
@Mixin(AbstractResourcePack.class)
public abstract class MixinAbstractResourcePack {

//$ if >=1.8
	@Shadow protected abstract InputStream getInputStreamByName(String name) throws IOException;
//$ else
	@Shadow protected abstract InputStream getInputStreamByName(String name);
//$ end

	@Unique private JsonObject lomka$packMetadata;
	@Unique private boolean lomka$packMetadataRead;

	/**
	 * @author Starlev
	 * @reason Reads and parses {@code pack.mcmeta} once per pack instead of once per metadata
	 *         query, then parses only the requested section out of the cached JSON. Vanilla's
	 *         exception wrapping and reader cleanup are reproduced unchanged.
	 */
	@Overwrite
//$ if >=1.12
	public <T extends IMetadataSection> T getPackMetadata(MetadataSerializer metadataSerializer, String section)
			throws IOException {
		return (T) metadataSerializer.parseMetadataSection(section, this.lomka$packMetadataJson());
	}
//$ else
//$ if >=1.8
	public <T extends IMetadataSection> T getPackMetadata(IMetadataSerializer metadataSerializer, String section)
			throws IOException {
		return (T) metadataSerializer.parseMetadataSection(section, this.lomka$packMetadataJson());
	}
//$ else
	public IMetadataSection getPackMetadata(IMetadataSerializer metadataSerializer, String section) {
		return metadataSerializer.parseMetadataSection(section, this.lomka$packMetadataJson());
	}
//$ end
//$ end

	/**
	 * Reads and parses the file once per pack instance, then hands the cached JSON back. Split out of
	 * the three signature branches so the part that is actually cached exists exactly once per
	 * version, and so the {@code throws} clause stays attached only to the versions whose stream
	 * lookup declares one: 1.7.10's does not, and an override may not add a checked exception.
	 */
//$ if >=1.8
	private JsonObject lomka$packMetadataJson() throws IOException {
//$ else
	private JsonObject lomka$packMetadataJson() {
//$ end
		JsonObject metadata = this.lomka$packMetadata;

		if (metadata == null && !this.lomka$packMetadataRead) {
			BufferedReader reader = null;

			try {
//$ if >=1.12
				reader = new BufferedReader(new InputStreamReader(this.getInputStreamByName("pack.mcmeta"),
						StandardCharsets.UTF_8));
//$ else
				reader = new BufferedReader(new InputStreamReader(this.getInputStreamByName("pack.mcmeta"),
						Charsets.UTF_8));
//$ end
				metadata = new JsonParser().parse((Reader) reader).getAsJsonObject();
			} catch (RuntimeException exception) {
				try {
					throw new JsonParseException((Throwable) exception);
				} catch (Throwable throwable) {
					IOUtils.closeQuietly(reader);
					throw throwable;
				}
			}

			IOUtils.closeQuietly(reader);

			this.lomka$packMetadata = metadata;
			this.lomka$packMetadataRead = true;
		}

		return metadata;
	}

	/**
	 * @author Starlev
	 * @reason Replaces {@code String.format("%s/%s/%s", "assets", domain, path)} with direct
	 *         concatenation. Same output, without the Formatter, the locale lookup and the varargs
	 *         array, on a path taken once per resource per pack.
	 */
	@Overwrite
	private static String locationToName(ResourceLocation location) {
//$ if >=1.12
		return "assets/" + location.getNamespace() + "/" + location.getPath();
//$ else
		return "assets/" + location.getResourceDomain() + "/" + location.getResourcePath();
//$ end
	}
}
