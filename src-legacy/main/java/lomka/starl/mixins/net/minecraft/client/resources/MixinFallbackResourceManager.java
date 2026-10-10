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

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import com.google.common.collect.Lists;
import net.minecraft.client.resources.FallbackResourceManager;
import net.minecraft.client.resources.IResource;
import net.minecraft.client.resources.IResourcePack;
import net.minecraft.client.resources.SimpleResource;
import net.minecraft.client.resources.data.MetadataSerializer;
import net.minecraft.util.ResourceLocation;

/**
 * Defers the {@code .mcmeta} lookup in 1.12.2's resource manager.
 *
 * <p>Vanilla builds the metadata location eagerly, at the top of both lookups:
 * {@code getLocationMcmeta(location)} allocates a new {@link ResourceLocation} whose path is
 * {@code location.getPath() + ".mcmeta"} - one string concat plus one object per call, before a
 * single pack has been consulted. Most resources have no metadata sidecar at all, so on a
 * resource-heavy load this is an allocation and a map probe spent on nothing.
 *
 * <p>Here the sidecar is only materialised once a pack has actually answered
 * {@code resourceExists(metadataLocation)} true, and the resulting location is reused for the
 * stream open. That is the same shape as the modern build's fast-path metadata presence check,
 * inverted to match this version's per-pack fallback walk.
 *
 * <p>Iteration order, the highest-priority-pack-wins selection, the debug-logging wrapper in
 * {@code getInputStream}, and the {@link FileNotFoundException} on a total miss are all
 * unchanged. {@code checkResourcePath} is still called first in both methods, so the
 * {@code ".."} rejection happens before anything else, exactly as before.
 */
@Mixin(FallbackResourceManager.class)
public abstract class MixinFallbackResourceManager {

	@Shadow @Final protected List<IResourcePack> resourcePacks;

	@Shadow @Final private MetadataSerializer frmMetadataSerializer;

	@Shadow
	protected abstract InputStream getInputStream(ResourceLocation location, IResourcePack pack)
			throws IOException;

	@Shadow
	private void checkResourcePath(ResourceLocation location) throws IOException {
		throw new AssertionError();
	}

	@Shadow
	private static ResourceLocation getLocationMcmeta(ResourceLocation location) {
		throw new AssertionError();
	}

	/**
	 * @author Starlev
	 * @reason Builds the {@code .mcmeta} sidecar location lazily instead of on every call, so a
	 *         resource without metadata no longer pays a string concat and a ResourceLocation
	 *         allocation before the pack walk even starts.
	 */
	@Overwrite
	public IResource getResource(ResourceLocation location) throws IOException {
		this.checkResourcePath(location);

		IResourcePack metadataPack = null;
		ResourceLocation metadataLocation = null;

		for (int i = this.resourcePacks.size() - 1; i >= 0; --i) {
			IResourcePack pack = this.resourcePacks.get(i);

			// Resolved at most once, and only for a pack that reports having the sidecar.
			if (metadataPack == null) {
				ResourceLocation candidate = getLocationMcmeta(location);
				if (pack.resourceExists(candidate)) {
					metadataPack = pack;
					metadataLocation = candidate;
				}
			}

			if (!pack.resourceExists(location)) {
				continue;
			}

			InputStream metadata = metadataPack != null
					? this.getInputStream(metadataLocation, metadataPack)
					: null;

			return new SimpleResource(pack.getPackName(), location, this.getInputStream(location, pack), metadata,
					this.frmMetadataSerializer);
		}

		throw new FileNotFoundException(location.toString());
	}

	/**
	 * @author Starlev
	 * @reason Same lazy sidecar resolution as {@code getResource}, applied to the multi-pack walk.
	 *         The sidecar is built once and reused across every pack in the list.
	 */
	@Overwrite
	public List<IResource> getAllResources(ResourceLocation location) throws IOException {
		this.checkResourcePath(location);

		List<IResource> resources = Lists.newArrayList();
		ResourceLocation metadataLocation = null;
		boolean metadataResolved = false;

		for (IResourcePack pack : this.resourcePacks) {
			if (!pack.resourceExists(location)) {
				continue;
			}

			InputStream metadata = null;
			if (!metadataResolved) {
				metadataLocation = getLocationMcmeta(location);
				metadataResolved = true;
			}

			if (pack.resourceExists(metadataLocation)) {
				metadata = this.getInputStream(metadataLocation, pack);
			}

			resources.add(new SimpleResource(pack.getPackName(), location, this.getInputStream(location, pack),
					metadata, this.frmMetadataSerializer));
		}

		if (resources.isEmpty()) {
			throw new FileNotFoundException(location.toString());
		}

		return resources;
	}
}