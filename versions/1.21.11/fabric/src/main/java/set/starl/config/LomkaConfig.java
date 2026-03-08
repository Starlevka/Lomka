package set.starl.config;

import com.mojang.logging.LogUtils;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import net.fabricmc.loader.api.FabricLoader;
import org.slf4j.Logger;

public final class LomkaConfig {
	private static final Logger LOGGER = LogUtils.getLogger();
	private static volatile LomkaConfig INSTANCE;

	// Global settings
	public boolean java22Optimizations = false;

	// Sections - removed Compat, merged into Chunks
	public Shaders shaders = new Shaders();
	public Entities entities = new Entities();
	public Textures textures = new Textures();
	public Particles particles = new Particles();
	public Render render = new Render();
	public Culling culling = new Culling();
	public Chunks chunks = new Chunks();
	public Nbt nbt = new Nbt();
	public Network network = new Network();

	public static LomkaConfig get() {
		LomkaConfig existing = INSTANCE;
		if (existing != null) {
			return existing;
		}
		synchronized (LomkaConfig.class) {
			existing = INSTANCE;
			if (existing != null) {
				return existing;
			}
			LomkaConfig loaded = lomka$loadOrCreate(configPath());
			INSTANCE = loaded;
			return loaded;
		}
	}

	public static Path configPath() {
		// Use .properties or .conf as requested for "simple config"
		return FabricLoader.getInstance().getConfigDir().resolve("lomka.properties");
	}

	private static LomkaConfig lomka$loadOrCreate(final Path path) {
		LomkaConfig config = new LomkaConfig();
		if (Files.exists(path)) {
			lomka$load(path, config);
		} else {
			lomka$save(path, config);
		}
		config.lomka$normalize();
		return config;
	}

	private static void lomka$load(final Path path, final LomkaConfig config) {
		try (BufferedReader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
			Properties props = new Properties();
			props.load(reader);
			
			// Map properties to fields
			populate(config, "", props);
			
		} catch (Exception e) {
			LOGGER.warn("Failed to read {}", path, e);
			lomka$tryBackup(path);
		}
	}

	private static void populate(Object obj, String prefix, Properties props) throws IllegalAccessException {
		for (Field field : obj.getClass().getFields()) {
			if (Modifier.isStatic(field.getModifiers())) continue;

			String fieldName = field.getName();
			String snakeCaseName = toSnakeCase(fieldName);
			String key = prefix.isEmpty() ? snakeCaseName : prefix + "." + snakeCaseName;

			Class<?> type = field.getType();

			if (isConfigSection(type)) {
				Object section = field.get(obj);
				if (section == null) {
					try {
						section = type.getConstructor().newInstance();
						field.set(obj, section);
					} catch (Exception e) {
						LOGGER.error("Failed to instantiate config section: " + fieldName, e);
						continue;
					}
				}
				populate(section, key, props);
			} else {
				if (props.containsKey(key)) {
					String value = props.getProperty(key);
					try {
						if (type == boolean.class) field.setBoolean(obj, Boolean.parseBoolean(value));
						else if (type == int.class) field.setInt(obj, Integer.parseInt(value));
						else if (type == long.class) field.setLong(obj, Long.parseLong(value));
						else if (type == double.class) field.setDouble(obj, Double.parseDouble(value));
						else if (type == String.class) field.set(obj, value);
					} catch (NumberFormatException e) {
						LOGGER.warn("Invalid value for key {}: {}", key, value);
					}
				}
			}
		}
	}

	private static boolean isConfigSection(Class<?> type) {
		return type.getDeclaringClass() == LomkaConfig.class || 
		       type.getEnclosingClass() == LomkaConfig.class || 
		       type.getName().startsWith("set.starl.config.LomkaConfig$");
	}

	private static String toSnakeCase(String str) {
		StringBuilder result = new StringBuilder();
		for (int i = 0; i < str.length(); i++) {
			char c = str.charAt(i);
			if (Character.isUpperCase(c)) {
				if (i > 0) result.append('_');
				result.append(Character.toLowerCase(c));
			} else {
				result.append(c);
			}
		}
		return result.toString();
	}

	private static void lomka$save(final Path path, final LomkaConfig config) {
		try {
			Files.createDirectories(path.getParent());
		} catch (Exception ignored) {
		}

		try (BufferedWriter writer = Files.newBufferedWriter(
			path,
			StandardCharsets.UTF_8,
			StandardOpenOption.CREATE,
			StandardOpenOption.TRUNCATE_EXISTING,
			StandardOpenOption.WRITE
		)) {
			writer.write("# Lomka Configuration File\n");
			writer.write("# Modify settings below. Use true/false for booleans.\n\n");
			
			saveRecursive(config, "", writer);
			
		} catch (Exception e) {
			LOGGER.warn("Failed to write {}", path, e);
		}
	}
	
	/**
	 * Save the current config to disk. Called from ModMenu config screen.
	 */
	public static void save() {
		LomkaConfig instance = INSTANCE;
		if (instance != null) {
			lomka$save(configPath(), instance);
		}
	}

	private static void saveRecursive(Object obj, String prefix, BufferedWriter writer) throws Exception {
		for (Field field : obj.getClass().getFields()) {
			if (Modifier.isStatic(field.getModifiers())) continue;

			String fieldName = field.getName();
			String snakeCaseName = toSnakeCase(fieldName);
			String key = prefix.isEmpty() ? snakeCaseName : prefix + "." + snakeCaseName;
			
			Object value = field.get(obj);
			if (value == null) continue;

			if (isConfigSection(field.getType())) {
				writer.write("\n# Section: " + key + "\n");
				saveRecursive(value, key, writer);
			} else {
				writer.write(key + "=" + value + "\n");
			}
		}
	}

	private static void lomka$tryBackup(final Path path) {
		try {
			long ts = Instant.now().toEpochMilli();
			Path backup = path.resolveSibling(path.getFileName() + ".broken." + ts);
			Files.move(path, backup, StandardCopyOption.REPLACE_EXISTING);
		} catch (Exception ignored) {
		}
	}

	private void lomka$normalize() {
		if (shaders == null) shaders = new Shaders();
		if (shaders.programBinaryCache == null) shaders.programBinaryCache = new Shaders.ProgramBinaryCache();
		if (entities == null) entities = new Entities();
		if (entities.clientTickCull == null) entities.clientTickCull = new Entities.ClientTickCull();
		if (textures == null) textures = new Textures();
		if (textures.decode == null) textures.decode = new Textures.Decode();
		if (particles == null) particles = new Particles();
		if (render == null) render = new Render();
		if (culling == null) culling = new Culling();
		if (chunks == null) chunks = new Chunks();
		if (nbt == null) nbt = new Nbt();
		if (nbt.cache == null) nbt.cache = new Nbt.Cache();
		if (network == null) network = new Network();
		if (network.compressionDecoder == null) network.compressionDecoder = new Network.CompressionDecoder();

		textures.decode.parallelism = lomka$clamp(textures.decode.parallelism, 1, 64);
		shaders.programBinaryCache.maxEntryBytes = lomka$nonNegative(shaders.programBinaryCache.maxEntryBytes);
		shaders.programBinaryCache.maxTotalBytes = lomka$nonNegative(shaders.programBinaryCache.maxTotalBytes);
		nbt.cache.maxBytes = lomka$nonNegative(nbt.cache.maxBytes);
		network.compressionDecoder.directInputReuseMaxKeepBytes = lomka$clamp(network.compressionDecoder.directInputReuseMaxKeepBytes, 0, 256 * 1024 * 1024);
	}

	private static int lomka$clamp(final int v, final int min, final int max) {
		return Math.max(min, Math.min(v, max));
	}

	private static long lomka$nonNegative(final long v) {
		return Math.max(0L, v);
	}

	public static final class Shaders {
		public ProgramBinaryCache programBinaryCache = new ProgramBinaryCache();
		public static final class ProgramBinaryCache {
			public boolean enable = true;
			public long maxEntryBytes = 8_388_608L;
			public long maxTotalBytes = 1_073_741_824L;
		}
	}

	public static final class Entities {
		public boolean enable = true;
		public boolean cullInvisible = false;
		public boolean frustumCullAll = true;
		public int frustumHysteresisTicks = 0;
		public boolean sectionOcclusionCull = true;
		public double displayCullDistance = 160.0;
		public ClientTickCull clientTickCull = new ClientTickCull();
		public static final class ClientTickCull {
			public boolean enable = true;
			public boolean includeDisplay = true;
			public double minDistance = 160.0;
		}
	}

	public static final class Textures {
		public Decode decode = new Decode();
		public static final class Decode {
			public boolean offThread = true;
			public int parallelism = 2;
		}
	}



	public static final class Particles {
		public double maxDistance = 128.0;
	}

	public static final class Render {
		public boolean openglDebugDisable = true;
		public boolean translucencyResortThrottle = true;
	}

	public static final class Culling {
		public boolean enable = true;
		public String frustumCameraCubeMode = "vanilla";
		public boolean smartLeavesCulling = true;
		public boolean cullGlobalBlockEntities = true;
	}

	public static final class Nbt {
		public int ioParallelism = 1;
		public int codecParallelism = 1;
		public Cache cache = new Cache();
		public static final class Cache {
			public boolean readCompressedPath = true;
			public int maxEntries = 512;
			public long maxBytes = 33554432L; // 32 MB
		}
	}

	public static final class Network {
		public CompressionDecoder compressionDecoder = new CompressionDecoder();
		public static final class CompressionDecoder {
			public boolean directInputReuse = true;
			public int directInputReuseMaxKeepBytes = 4194304;
		}
	}

	public static final class Chunks {
		public boolean enable = true;
		public boolean silenceIgnoreOutOfRangeLog = true;
	}
}
