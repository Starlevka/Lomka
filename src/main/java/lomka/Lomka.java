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

//? if fabric {
package lomka;

import net.fabricmc.api.ModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class Lomka implements ModInitializer {
    public  static final String MOD_ID  = "lomka";
    public  static final String VERSION = /*$ mod_version */ "0.5.0";
    private static final Logger LOGGER  = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitialize() {
        init();
    }

    public static void init() {
        LOGGER.info("Lomka v" + VERSION + " - Initializing... 🌠 Initialized!");
    }
}
//?} else if forge {
/*package lomka;

import net.minecraftforge.fml.common.Mod;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Mod(Lomka.MOD_ID)
public final class Lomka {
    public  static final String MOD_ID  = "lomka";
    // NOTE: keep in sync with mod.version (the stonecutter swap lives in the fabric branch).
    public  static final String VERSION = "0.5.0";
    private static final Logger LOGGER  = LoggerFactory.getLogger(MOD_ID);

    public Lomka() {
        init();
    }

    public static void init() {
        LOGGER.info("Lomka v" + VERSION + " - Initializing... 🌠 Initialized!");
    }
}
*///?} else if neoforge {
/*package lomka;

import net.neoforged.fml.common.Mod;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Mod(Lomka.MOD_ID)
public final class Lomka {
    public  static final String MOD_ID  = "lomka";
    // NOTE: keep in sync with mod.version (the stonecutter swap lives in the fabric branch).
    public  static final String VERSION = "0.5.0";
    private static final Logger LOGGER  = LoggerFactory.getLogger(MOD_ID);

    public Lomka() {
        init();
    }

    public static void init() {
        LOGGER.info("Lomka v" + VERSION + " - Initializing... 🌠 Initialized!");
    }
}
*///?}
