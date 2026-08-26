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
//? if fabric {
import net.fabricmc.api.ModInitializer;
//?} else if neoforge {
/*import net.neoforged.fml.common.Mod;*/
//?} else if forge {
/*import net.minecraftforge.fml.common.Mod;*/
//?}

public final class Lomka {
    public static final String MOD_ID  = "lomka";
    public static final String VERSION = /*$ mod_version */ "0.5.1";
    private static final Logger LOGGER  = LoggerFactory.getLogger(MOD_ID);

    private Lomka() {
    }

    public static void init() {
        LOGGER.info("Lomka v" + VERSION + " - Initializing... 🌠 Initialized!");
    }

    //? if fabric {
    public static final class Fabric implements ModInitializer {

        @Override
        public void onInitialize() {
            Lomka.init();
        }
    }
    //?} else if forge {
    /*@Mod(Lomka.MOD_ID)
    public static final class Forge {

        public Forge() {
            Lomka.init();
        }
    }*/
    //?} else if neoforge {
    /*@Mod(Lomka.MOD_ID)
    public static final class NeoForge {

        public NeoForge() {
            Lomka.init();
        }
    }*/
    //?}
}
