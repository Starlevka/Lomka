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
package lomka;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.ornithemc.osl.entrypoints.api.ModInitializer;

/**
 * Entrypoint for the Ornithe variants.
 *
 * <p>The interface is OSL's, not {@code net.fabricmc.api.ModInitializer}: on legacy Minecraft the
 * Fabric API classes do not exist, so OSL supplies backports and the loader's language adapter
 * routes the {@code init} entrypoint to this one. The method is therefore {@code init()}, not
 * {@code onInitialize()}.
 */
public final class Lomka implements ModInitializer {

	public static final String MOD_ID = "lomka";
	public static final String VERSION = "0.7.0";

	private static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	@Override
	public void init() {
		init0();
	}

	public static void init0() {
		LOGGER.info("Lomka v{} - Initializing... * Initialized!", VERSION);
	}

	public static String version() {
		return VERSION;
	}
}
