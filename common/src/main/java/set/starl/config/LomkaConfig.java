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
import java.util.Properties;
import set.starl.util.Platform;
import org.slf4j.Logger;

public final class LomkaConfig {
	private static final Logger LOGGER = LogUtils.getLogger();
	private static volatile LomkaConfig INSTANCE;

    public Culling culling = new Culling();
    public Entities entities = new Entities();
    public Render render = new Render();
    public Chunks chunks = new Chunks();
    public Nbt nbt = new Nbt();
    public Network network = new Network();
    public Java java = new Java();

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
		return Platform.get().getConfigDir().resolve("lomka.properties");
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
        if (culling == null) culling = new Culling();
        if (culling.frustumCameraCubeMode == null) culling.frustumCameraCubeMode = "vanilla";
        if (entities == null) entities = new Entities();
        if (chunks == null) chunks = new Chunks();
        if (nbt == null) nbt = new Nbt();
        if (nbt.cache == null) nbt.cache = new Nbt.Cache();
        if (network == null) network = new Network();
        if (java == null) java = new Java();
    }

	private static int lomka$clamp(final int v, final int min, final int max) {
		return Math.max(min, Math.min(v, max));
	}

	private static long lomka$nonNegative(final long v) {
		return Math.max(0L, v);
	}

    public static final class Culling {
        public boolean enable = true;
        public boolean frustumCullAll = true;
        public int frustumHysteresisTicks = 5;
        public boolean sectionOcclusionCull = true;
        public boolean blockEntityCulling = true;
        public String frustumCameraCubeMode = "vanilla";
        public boolean smartLeavesCulling = true;
    }

    public static final class Entities {
        public boolean enable = true;
        public boolean clientTickCull = true;
        public double clientTickCullMinDistance = 128.0;
        public boolean clientTickCullIncludeDisplay = true;
    }

    public static final class Render {
        public boolean openglDebugDisable = true;
        public boolean translucencyResortThrottle = true;
    }

    public static final class Nbt {
        public int ioParallelism = 1;
        public int codecParallelism = 1;
        public Cache cache = new Cache();
        public static final class Cache {
            public boolean readCompressedPath = true;
            public int maxEntries = 512;
            public long maxBytes = 33554432L;
        }
    }

    public static final class Network {
    }

    public static final class Java {
        public boolean java25Optimizations = true;
    }

    public static final class Chunks {
        public boolean enable = true;
        public boolean silenceIgnoreOutOfRangeLog = true;
    }
}