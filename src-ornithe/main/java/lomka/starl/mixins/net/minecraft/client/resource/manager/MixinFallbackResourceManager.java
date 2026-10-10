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

package lomka.starl.mixins.net.minecraft.client.resource.manager;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import com.google.common.collect.Lists;
import net.minecraft.client.resource.Resource;
import net.minecraft.client.resource.SimpleResource;
import net.minecraft.client.resource.manager.FallbackResourceManager;
import net.minecraft.client.resource.metadata.ResourceMetadataSerializers;
import net.minecraft.client.resource.pack.ResourcePack;
//$ if >=1.8
import net.minecraft.resource.Identifier;
//$ else
import net.minecraft.client.resource.Identifier;
//$ end

/**
 * Defers the {@code .mcmeta} lookup in the legacy resource manager.
 *
 * Vanilla builds the metadata location eagerly, at the top of both lookups: the metadata helper
 * allocates a new {@code Identifier} whose path is {@code location.getPath() + ".mcmeta"} - one
 * string concat plus one object per call, before a single pack has been consulted. Most resources
 * have no metadata sidecar at all, so on a resource-heavy load this is an allocation and a map
 * probe spent on nothing.
 *
 * Here the sidecar is only materialised once a pack has actually answered {@code hasResource}
 * true, and the resulting location is reused for the stream open. That is the same shape as the
 * modern build's fast-path metadata presence check, inverted to match this version's per-pack
 * fallback walk.
 *
 * Iteration order, the highest-priority-pack-wins selection and the {@link FileNotFoundException}
 * on a total miss are all unchanged.
 *
 * Three shapes are gated, all established from the mapped jars rather than from memory:
 * <ul>
 *   <li><b>1.7.10</b> has no {@code wrapResource} helper - it opens the stream straight off the
 *       pack - and its {@code SimpleResource} constructor takes no source name. Both of those
 *       arrive in 1.8; the boundary was read off the mapped jars, not off MCP lore.</li>
 *   <li><b>1.7.10</b> keeps {@code Identifier} in {@code net.minecraft.client.resource}.</li>
 *   <li><b>1.7.10 / 1.8.9</b> have no path validation; the {@code ".."} rejection is vanilla's
 *       from 1.12 on, so below that there is nothing to preserve.</li>
 * </ul>
 */
@Mixin(FallbackResourceManager.class)
public abstract class MixinFallbackResourceManager {

	@Shadow @Final protected List<ResourcePack> fallbacks;

	@Shadow @Final private ResourceMetadataSerializers metadataSerializers;

	@Shadow
	private static Identifier getMetadataLocation(Identifier location) {
		throw new AssertionError();
	}

//$ if >=1.8
	@Shadow
	protected abstract InputStream wrapResource(Identifier location, ResourcePack pack)
			throws IOException;
//$ end

//$ if >=1.12
	@Shadow
	private void validateLocation(Identifier location) throws IOException {
		throw new AssertionError();
	}
//$ end

	/**
	 * @author Starlev
	 * @reason Builds the {@code .mcmeta} sidecar location lazily instead of on every call, so a
	 *         resource without metadata no longer pays a string concat and an Identifier
	 *         allocation before the pack walk even starts.
	 */
	@Overwrite
	public Resource getResource(Identifier location) throws IOException {
//$ if >=1.12
		this.validateLocation(location);
//$ end

		ResourcePack metadataPack = null;
		Identifier metadataLocation = null;

		for (int i = this.fallbacks.size() - 1; i >= 0; --i) {
			ResourcePack pack = this.fallbacks.get(i);

			// Resolved at most once, and only for a pack that reports having the sidecar.
			if (metadataPack == null) {
				Identifier candidate = getMetadataLocation(location);
				if (pack.hasResource(candidate)) {
					metadataPack = pack;
					metadataLocation = candidate;
				}
			}

			if (!pack.hasResource(location)) {
				continue;
			}

			InputStream metadata = null;
			if (metadataPack != null) {
				metadata = lomka$open(metadataPack, metadataLocation);
			}

			InputStream stream = lomka$open(pack, location);

//$ if >=1.8
			return new SimpleResource(pack.getName(), location, stream, metadata,
					this.metadataSerializers);
//$ else
			return new SimpleResource(location, stream, metadata, this.metadataSerializers);
//$ end
		}

		throw new FileNotFoundException(location.toString());
	}

	/**
	 * @author Starlev
	 * @reason Same lazy sidecar resolution as {@code getResource}, applied to the multi-pack walk.
	 *         The sidecar is built once and reused across every pack in the list.
	 */
	@Overwrite
	public List<Resource> getResources(Identifier location) throws IOException {
//$ if >=1.12
		this.validateLocation(location);
//$ end

		List<Resource> resources = Lists.newArrayList();
		Identifier metadataLocation = null;
		boolean metadataResolved = false;

		for (ResourcePack pack : this.fallbacks) {
			if (!pack.hasResource(location)) {
				continue;
			}

			InputStream metadata = null;
			if (!metadataResolved) {
				metadataLocation = getMetadataLocation(location);
				metadataResolved = true;
			}

			if (pack.hasResource(metadataLocation)) {
				metadata = lomka$open(pack, metadataLocation);
			}

			InputStream stream = lomka$open(pack, location);

//$ if >=1.8
			resources.add(new SimpleResource(pack.getName(), location, stream, metadata,
					this.metadataSerializers));
//$ else
			resources.add(new SimpleResource(location, stream, metadata, this.metadataSerializers));
//$ end
		}

		if (resources.isEmpty()) {
			throw new FileNotFoundException(location.toString());
		}

		return resources;
	}

// 1.8 added the wrapResource helper, which 1.12 also routes through a debug-logging wrapper.
	// Below that the stream comes straight off the pack, exactly as vanilla opens it.
	private InputStream lomka$open(ResourcePack pack, Identifier location) throws IOException {
//$ if >=1.8
		return this.wrapResource(location, pack);
//$ else
		return pack.getResource(location);
//$ end
	}
}