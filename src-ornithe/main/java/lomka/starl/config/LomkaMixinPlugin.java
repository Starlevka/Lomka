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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

/**
 * Runtime switchboard: on first launch the shipped {@code config/lomka-mixins.properties} starts
 * empty except for a notice, and each line a user adds forces one patch on or off:
 * <pre>
 *   net.minecraft.util.math.MixinDirection = false
 * </pre>
 * Missing keys always mean enabled, so patches added by an update need no file migration.
 *
 * <p>Separate class from the modern one on purpose, not by accident: this tree is a third source
 * root compiled against the Feather namespace, and the modern class targets 1.19.2+.
 *
 * <p>Two divergences from the modern plugin, both forced by the target rather than chosen:
 * <ul>
 *   <li><b>No JOML system properties.</b> 1.12.2 uses Mojang math, so {@code Quaternionf} never
 *       exists and the {@code joml.sinLookup} tuning has nothing to apply to.</li>
 *   <li><b>No Gson.</b> The packaged mixin list is scanned as quoted tokens instead of parsed, so
 *       the plugin cannot fail on an older Gson's missing static overloads.</li>
 * </ul>
 *
 * <p>Logging is SLF4J here, not Log4j: the Forge legacy builds log through Log4j because SLF4J
 * does not exist on 1.7.10-1.12.2, but Ornithe runs the Fabric loader, which always provides it.
 *
 * <p>This class must stay free of Minecraft imports - {@code onLoad} runs long before any game
 * class is transformable.
 */
public final class LomkaMixinPlugin implements IMixinConfigPlugin {

	private static final Logger LOGGER = LoggerFactory.getLogger("Lomka/MixinConfig");
	private static final String CONFIG_PATH = "lomka-mixins.properties";
	private static final String WIKI_URL = " https://github.com/Starlevka/Lomka/wiki/Mixin-Configuration-Legacy";
	private static final String MIXIN_PREFIX = "lomka.starl.mixins.";

	private static volatile Properties options = new Properties();

	@Override
	public boolean shouldApplyMixin(String targetClassName, String mixinClassName) {
		String value = lomka$lookup(mixinClassName);
		return value == null || value.equalsIgnoreCase("true") || value.equals("1");
	}

	/**
	 * Resolves a mixin's flag tolerating the several name forms Mixin may hand over: the
	 * fully qualified name as declared in the config, the same name without our package prefix, or
	 * the simple class name.
	 */
	private static String lomka$lookup(String mixinClassName) {
		String v = options.getProperty(mixinClassName);
		if (v != null) {
			return v;
		}
		String stripped = mixinClassName.startsWith(MIXIN_PREFIX)
				? mixinClassName.substring(MIXIN_PREFIX.length()) : mixinClassName;
		v = options.getProperty(stripped);
		if (v != null) {
			return v;
		}
		return options.getProperty(mixinClassName.substring(mixinClassName.lastIndexOf('.') + 1));
	}

	private static void lomka$loadOptions() {
		Path file = Paths.get("config", CONFIG_PATH);

		if (!Files.isReadable(file)) {
			try {
				Files.createDirectories(file.getParent());
				Files.write(file, lomka$defaultConfig().getBytes(StandardCharsets.UTF_8));
				LOGGER.info("Created default {}", file);
			} catch (Exception e) {
				LOGGER.info("Could not create {} - all mixins stay enabled", file, e);
				return;
			}
		}

		InputStream in = null;
		try {
			Properties loaded = new Properties();
			in = Files.newInputStream(file);
			loaded.load(new InputStreamReader(in, StandardCharsets.UTF_8));
			options = loaded;

			Set<String> packaged = new HashSet<String>(lomka$packagedMixins());
			for (String p : new HashSet<String>(packaged)) {
				packaged.add(p.substring(p.lastIndexOf('.') + 1));
			}

			List<String> disabled = new ArrayList<String>();
			List<String> stale = new ArrayList<String>();

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
					disabled.add(simple);
				}
			}

			if (!stale.isEmpty()) {
				LOGGER.warn("{} unknown key(s) ignored (not present in this build - wrong version or typo?): {}",
						stale.size(), String.join(", ", stale));
			}
			if (!disabled.isEmpty()) {
				LOGGER.warn("Disabled {} mixin(s): {}", disabled.size(), String.join(", ", disabled));
			}
		} catch (Exception e) {
			LOGGER.error("Failed to read {}, falling back to all-enabled", file, e);
			options = new Properties();
		} finally {
			if (in != null) {
				try {
					in.close();
				} catch (IOException ignored) {
					// Nothing useful to do about a failure to close a read-only stream.
				}
			}
		}
	}

	private static String lomka$defaultConfig() {
		return "# Mixin configuration file for Lomka mod.\n"
				+ "#\n"
				+ "# Each line overrides a single optimization patch at runtime:\n"
				+ "#   <fully.qualified.MixinName> = true|false\n"
				+ "#\n"
				+ "# Missing keys stay enabled, so this file only needs entries for\n"
				+ "# patches you want to turn OFF. Note that disabling patches reduces\n"
				+ "# performance and may change behavior; this file is intended for\n"
				+ "# debugging and mod-compatibility triage.\n"
				+ "#\n"
				+ "# Patches come in groups whose members depend on each other; disabling\n"
				+ "# one half of a group throws at runtime rather than degrading.\n"
				+ "#\n"
				+ "# The full list of mixins with descriptions lives here:\n"
				+ "# " + WIKI_URL + "\n"
				+ "#\n"
				+ "# By default, this file is empty except for this notice.\n";
	}

	/**
	 * Reads the packaged, version-resolved mixin list out of our own {@code lomka.mixins.json}.
	 *
	 * <p>Deliberately not Gson: parsing would tie the plugin to whatever Gson's static overloads
	 * happen to exist on the target. This scans quoted tokens inside arrays instead, which is exact
	 * for this file - {@code client}, {@code mixins} and {@code server} are its only arrays of
	 * strings. Failures degrade to "key validation skipped", never to a broken boot.
	 */
	private static List<String> lomka$packagedMixins() {
		List<String> out = new ArrayList<String>();
		InputStream in = LomkaMixinPlugin.class.getResourceAsStream("/lomka.mixins.json");
		if (in == null) {
			LOGGER.warn("Packaged lomka.mixins.json not found - key validation skipped");
			return out;
		}

		try {
			ByteArrayOutputStream buffer = new ByteArrayOutputStream();
			byte[] chunk = new byte[4096];
			int read;
			while ((read = in.read(chunk)) > 0) {
				buffer.write(chunk, 0, read);
			}

			String text = new String(buffer.toByteArray(), StandardCharsets.UTF_8);
			int i = 0;
			while (i < text.length()) {
				int open = text.indexOf('[', i);
				if (open < 0) {
					break;
				}
				int close = text.indexOf(']', open);
				if (close < 0) {
					break;
				}
				String array = text.substring(open + 1, close);
				int quote = array.indexOf('"');
				while (quote >= 0) {
					int end = array.indexOf('"', quote + 1);
					if (end < 0) {
						break;
					}
					out.add(array.substring(quote + 1, end));
					quote = array.indexOf('"', end + 1);
				}
				i = close + 1;
			}
		} catch (Exception e) {
			LOGGER.warn("Failed to read packaged mixin list - key validation skipped", e);
			return Collections.emptyList();
		} finally {
			try {
				in.close();
			} catch (IOException ignored) {
				// Nothing useful to do about a failure to close a read-only stream.
			}
		}
		return out;
	}

	@Override
	public String getRefMapperConfig() {
		return null;
	}

	@Override
	public List<String> getMixins() {
		return Collections.emptyList();
	}

	@Override
	public void acceptTargets(Set<String> myTargets, Set<String> otherTargets) {
	}

	@Override
	public void onLoad(String configPath) {
		lomka$loadOptions();
	}

	@Override
	public void preApply(String targetClass, org.objectweb.asm.tree.ClassNode classNode, String mixinClass,
			IMixinInfo mixinInfo) {
	}

	@Override
	public void postApply(String targetClass, org.objectweb.asm.tree.ClassNode classNode, String mixinClass,
			IMixinInfo mixinInfo) {
	}
}
