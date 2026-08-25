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

package lomka.starl.config;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Properties;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

/**
 * Runtime switchboard for Lomka's mixins: on first launch the shipped
 * {@code config/lomka-mixins.properties} starts empty except for a notice, and each
 * line a user adds forces one patch on or off:
 * <pre>
 *   net.minecraft.core.MixinCursor3D = false
 * </pre>
 * Keys are fully qualified mixin names exactly as listed in {@code lomka.mixins.json};
 * missing keys always mean enabled, so new mixins added by an update work without any
 * file migration. Descriptions and the full patch list live in the wiki - long before
 * any game class exists, so this class must stay free of Minecraft imports.
 */
public final class LomkaMixinPlugin implements IMixinConfigPlugin {

    private static final Logger LOGGER = LoggerFactory.getLogger("Lomka/MixinConfig");
    private static final String WIKI_URL = " https://github.com/Starlevka/Lomka/wiki/Configuration";
    private static final String MIXIN_PREFIX = "lomka.starl.mixins.";
    private static volatile Properties options = new Properties();

    @Override
    public boolean shouldApplyMixin(String targetClassName, String mixinClassName) {
        String value = lomka$lookup(mixinClassName);
        return value == null || value.equalsIgnoreCase("true") || value.equals("1");
    }

    /**
     * Resolves a mixin's enable/disable flag tolerating the several name forms Mixin
     * may hand to shouldApplyMixin: the config-declared short name.
     */
    private static String lomka$lookup(String mixinClassName) {
        String v = options.getProperty(mixinClassName);
        if (v != null) return v;
        String stripped = mixinClassName.startsWith(MIXIN_PREFIX)
                ? mixinClassName.substring(MIXIN_PREFIX.length()) : mixinClassName;
        v = options.getProperty(stripped);
        if (v != null) return v;
        String simple = mixinClassName.substring(mixinClassName.lastIndexOf('.') + 1);
        return options.getProperty(simple);
    }

    /** Both mixin generations route here - see the overload note at the bottom of this class. */
    private static void lomka$loadOptions() {
        Path file = Path.of("config", "lomka-mixins.properties");

        if (!Files.isReadable(file)) {
            try {
                Files.createDirectories(file.getParent());
                Files.writeString(file, lomka$defaultConfig());
                LOGGER.info("Created default {}", file);
            } catch (Exception e) {
                LOGGER.info("Could not create {} - all mixins stay enabled", file, e);
                return;
            }
        }

        try {
            Properties loaded = new Properties();
            loaded.load(Files.newInputStream(file));
            options = loaded;

            Set<String> packaged = new java.util.HashSet<>(lomka$packagedMixins());
            for (String p : new java.util.HashSet<>(packaged)) {
                packaged.add(p.substring(p.lastIndexOf('.') + 1));
            }
            List<String> disabled = new ArrayList<>();
            List<String> stale = new ArrayList<>();

            for (String key : loaded.stringPropertyNames()) {
                String raw = loaded.getProperty(key);

                String norm = key.startsWith(MIXIN_PREFIX) ? key.substring(MIXIN_PREFIX.length()) : key;
                String simple = key.substring(key.lastIndexOf('.') + 1);
                if (!packaged.contains(key) && !packaged.contains(norm) && !packaged.contains(simple)) {
                    stale.add(key);
                    continue;
                }

                if (!(raw.equalsIgnoreCase("true") || raw.equals("1")
                        || raw.equalsIgnoreCase("false") || raw.equals("0"))) {
                    LOGGER.warn("Value '{}' for '{}' is not true/false - treating it as false", raw, key);
                }

                if (!(raw.equalsIgnoreCase("true") || raw.equals("1"))) {
                    disabled.add(key.substring(key.lastIndexOf('.') + 1));
                }
            }

            if (!stale.isEmpty()) {
                LOGGER.warn(
                        "{} unknown key(s) ignored (not present in this build - wrong version or typo?): {}",
                        stale.size(), String.join(", ", stale));
            }

            if (!disabled.isEmpty()) {
                LOGGER.warn("Disabled {} mixin(s): {}", disabled.size(), String.join(", ", disabled));
            }
        } catch (Exception e) {
            LOGGER.error("Failed to read {}, falling back to all-enabled", file, e);
            options = new Properties();
        }
    }

    private static String lomka$defaultConfig() {
        return """
                # Mixin configuration file for Lomka mod.
                #
                # Each line overrides a single optimization patch at runtime:
                #   <fully.qualified.MixinName> = true|false
                #
                # Missing keys stay enabled, so this file only needs entries for
                # patches you want to turn OFF. Note that disabling patches reduces
                # performance and may change behavior; this file is intended for
                # debugging and mod-compatibility triage.
                #
                # The full list of mixins with descriptions lives here:
                # """ + WIKI_URL + "\n#\n# By default, this file is empty except for this notice.\n";
    }

    /** Reads the packaged, version-resolved mixin list from our own config resource. */
    private static List<String> lomka$packagedMixins() {
        try (var in = LomkaMixinPlugin.class.getResourceAsStream("/lomka.mixins.json")) {
            if (in == null) {
                LOGGER.warn("Packaged lomka.mixins.json not found - key validation skipped");
                return Collections.emptyList();
            }

            var root = com.google.gson.JsonParser.parseReader(
                    new java.io.InputStreamReader(in, StandardCharsets.UTF_8)).getAsJsonObject();
            List<String> out = new ArrayList<>();

            for (String section : new String[] {"client", "main"}) {
                var array = root.getAsJsonArray(section);
                if (array != null) {
                    array.forEach(entry -> out.add(entry.getAsString()));
                }
            }
            return out;
        } catch (Exception e) {
            LOGGER.warn("Failed to read packaged mixin list - key validation skipped", e);
            return Collections.emptyList();
        }
    }

    @Override public String getRefMapperConfig() { return null; }
    @Override public List<String> getMixins() { return Collections.emptyList(); }
    @Override public void acceptTargets(Set<String> myTargets, Set<String> otherTargets) {}

    @Override public void onLoad(String configPath) { lomka$loadOptions(); }
    @Override public void preApply(String targetClass, org.objectweb.asm.tree.ClassNode classNode, String mixinClass, IMixinInfo mixinInfo) {}
    @Override public void postApply(String targetClass, org.objectweb.asm.tree.ClassNode classNode, String mixinClass, IMixinInfo mixinInfo) {}
}
