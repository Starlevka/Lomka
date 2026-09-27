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

package lomka.starl.utils.pack;

import net.minecraft.server.packs.PackResources;

/**
 * One resolved pack entry: the pack it came from, a supplier for its bytes, and the pack's index
 * in the fallback list.
 *
 * <p>Lives outside {@code lomka.starl.mixins} because ModLauncher's MixinLaunchPluginLegacy
 * refuses to load classes inside a configured mixin package, so a record nested in
 * {@code MixinFallbackResourceManager} aborts every Forge and NeoForge launch with
 * {@code IllegalClassLoadError}. Plain public fields rather than a record, because the call sites
 * read {@code entry.source} directly and a record's components are private to the record.
 *
 * <p>The supplier is a type parameter because {@code IoSupplier} does not exist before 1.19.3,
 * while this file is compiled for every variant.
 */
public final class ResourceEntry<S> {

    public final PackResources source;
    public final S resource;
    public final int packIndex;

    public ResourceEntry(PackResources source, S resource, int packIndex) {
        this.source = source;
        this.resource = resource;
        this.packIndex = packIndex;
    }
}
