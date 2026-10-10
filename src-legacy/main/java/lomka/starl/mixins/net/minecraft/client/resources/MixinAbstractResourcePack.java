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
import java.nio.charset.StandardCharsets;
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
import net.minecraft.client.resources.data.MetadataSerializer;
import net.minecraft.util.ResourceLocation;

/**
 * Two ports from the modern build, both on the resource-pack load path.
 *
 * <p><b>pack.mcmeta parse cache.</b> Vanilla re-opens and re-parses {@code pack.mcmeta} on
 * <em>every</em> metadata query. The call sites on this version are
 * {@code LanguageManager#func_135043_a} (once per pack, per resource reload),
 * {@code ResourcePackRepository#func_191399_b} and {@code Entry#func_110516_a}, plus
 * {@code LegacyV2Adapter}, which delegates. The file is immutable for the lifetime of a pack
 * instance, so the parsed {@link JsonObject} is cached and only the per-section
 * {@code parseMetadataSection} call is repeated - that part genuinely depends on the requested
 * section name and must not be cached.
 *
 * <p><b>{@code String.format} removal.</b> {@code locationToName} builds the per-lookup resource
 * path. Vanilla routes it through {@code String.format("%s/%s/%s", ...)}, which allocates a
 * {@code Formatter}, walks the format specifier, and boxes three varargs on every call. It runs
 * from both {@code getInputStream} and {@code resourceExists}, i.e. once per resource per pack
 * across the whole reload. Plain concatenation produces the identical string.
 *
 * <p><b>Exception behaviour is preserved exactly.</b> Vanilla wraps any {@link RuntimeException}
 * from the parse in a {@link JsonParseException} and closes the reader. A missing
 * {@code pack.mcmeta} makes {@code getInputStreamByName} return null, which surfaces through the
 * same path. The cache is only populated on success, so a failing pack re-parses and re-throws on
 * every call, just as vanilla does.
 *
 * <p><b>Known gap:</b> {@code DefaultResourcePack} <em>overrides</em>
 * {@code getPackMetadata} with its own {@code FileInputStream} body, so this patch does not cover
 * it. That is noted rather than duplicated - two overwrites of the same virtual method on
 * different classes is a conflict waiting to happen, and the vanilla client jar's own pack is
 * read exactly once.
 */
@Mixin(AbstractResourcePack.class)
public abstract class MixinAbstractResourcePack {

	@Shadow protected abstract InputStream getInputStreamByName(String name) throws IOException;

	@Unique private JsonObject lomka$packMetadata;
	@Unique private boolean lomka$packMetadataRead;

	/**
	 * @author Starlev
	 * @reason Reads and parses {@code pack.mcmeta} once per pack instead of once per metadata
	 *         query, then parses only the requested section out of the cached JSON. Vanilla's
	 *         exception wrapping and reader cleanup are reproduced unchanged.
	 */
	@Overwrite
	public <T extends IMetadataSection> T getPackMetadata(MetadataSerializer metadataSerializer, String section)
			throws IOException {
		JsonObject metadata = this.lomka$packMetadata;

		if (metadata == null && !this.lomka$packMetadataRead) {
			BufferedReader reader = null;

			try {
				reader = new BufferedReader(new InputStreamReader(this.getInputStreamByName("pack.mcmeta"),
						StandardCharsets.UTF_8));
				metadata = new JsonParser().parse((Reader) reader).getAsJsonObject();
			} catch (RuntimeException exception) {
				// Vanilla's own wrapping: rethrow as JsonParseException, closing the reader first.
				try {
					throw new JsonParseException((Throwable) exception);
				} catch (Throwable throwable) {
					IOUtils.closeQuietly(reader);
					throw throwable;
				}
			}

			IOUtils.closeQuietly((Reader) reader);

			// Only reached when the parse succeeded, so a broken pack is retried next time.
			this.lomka$packMetadata = metadata;
			this.lomka$packMetadataRead = true;
		}

		return metadataSerializer.parseMetadataSection(section, metadata);
	}

	/**
	 * @author Starlev
	 * @reason Replaces {@code String.format("%s/%s/%s", "assets", namespace, path)} with direct
	 *         concatenation. Same output, without the Formatter, the specifier walk and the
	 *         varargs array, on a path taken once per resource per pack.
	 */
	@Overwrite
	private static String locationToName(ResourceLocation location) {
		return "assets/" + location.getNamespace() + "/" + location.getPath();
	}
}