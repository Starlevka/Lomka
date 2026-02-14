package set.starl.mixin.shader;

import com.mojang.blaze3d.opengl.GlProgram;
import com.mojang.blaze3d.opengl.GlShaderModule;
import com.mojang.blaze3d.opengl.GlStateManager;
import com.mojang.blaze3d.shaders.ShaderSource;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.VertexFormat;
import java.lang.reflect.Constructor;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.FileTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Stream;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.ShaderDefines;
import net.minecraft.client.renderer.ShaderManager;
import net.minecraft.util.Util;
import com.mojang.blaze3d.opengl.GlDevice;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.logging.LogUtils;
import set.starl.Lomka;
import org.slf4j.Logger;
import org.lwjgl.opengl.GL;
import org.lwjgl.opengl.GL11C;
import org.lwjgl.opengl.GL20C;
import org.lwjgl.opengl.GL41C;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(GlDevice.class)
public class GlDeviceProgramBinaryCacheMixin {
	@Unique
	private static final Logger LOMKA$LOGGER = LogUtils.getLogger();

	@Unique
	private static final boolean LOMKA$ENABLE = Lomka.JAVA_22_25_OPTIMIZATIONS
		&& !"false".equalsIgnoreCase(System.getProperty("lomka.shaders.programBinaryCache", "true"));

	@Unique
	private static final boolean LOMKA$READ = !"false".equalsIgnoreCase(System.getProperty("lomka.shaders.programBinaryCache.read", "true"));

	@Unique
	private static final boolean LOMKA$WRITE = !"false".equalsIgnoreCase(System.getProperty("lomka.shaders.programBinaryCache.write", "true"));

	@Unique
	private static final boolean LOMKA$DRIVER_SALT = !"false".equalsIgnoreCase(System.getProperty("lomka.shaders.programBinaryCache.driverSalt", "true"));

	@Unique
	private static final long LOMKA$MAX_ENTRY_BYTES = Long.parseLong(System.getProperty("lomka.shaders.programBinaryCache.maxEntryBytes", "8388608"));

	@Unique
	private static final long LOMKA$MAX_TOTAL_BYTES = Long.parseLong(System.getProperty("lomka.shaders.programBinaryCache.maxTotalBytes", "268435456"));

	@Unique
	private static final long LOMKA$TRIM_INTERVAL_MS = Long.parseLong(System.getProperty("lomka.shaders.programBinaryCache.trimIntervalMs", "30000"));

	@Unique
	private static final int LOMKA$MAX_DELETES_PER_TRIM = Integer.parseInt(System.getProperty("lomka.shaders.programBinaryCache.maxDeletesPerTrim", "32"));

	@Unique
	private static final boolean LOMKA$TRIM_ASYNC = !"false".equalsIgnoreCase(System.getProperty("lomka.shaders.programBinaryCache.trimAsync", "true"));

	@Unique
	private static final AtomicLong LOMKA$LAST_TRIM_MS = new AtomicLong(0L);

	@Unique
	private static final int LOMKA$MAGIC = 0x4C4F4D4B;

	@Unique
	private static final int LOMKA$FORMAT_VERSION = 1;

	@Unique
	private static volatile Constructor<GlProgram> LOMKA$GLPROGRAM_CTOR;

	@Unique
	private static volatile String LOMKA$DRIVER_KEY_PREFIX;

	@Redirect(
		method = "compileProgram",
		at = @At(
			value = "INVOKE",
			target = "Lcom/mojang/blaze3d/opengl/GlProgram;link(Lcom/mojang/blaze3d/opengl/GlShaderModule;Lcom/mojang/blaze3d/opengl/GlShaderModule;Lcom/mojang/blaze3d/vertex/VertexFormat;Ljava/lang/String;)Lcom/mojang/blaze3d/opengl/GlProgram;"
		)
	)
	private GlProgram lomka$linkWithProgramBinaryCache(
		final GlShaderModule vertexShader,
		final GlShaderModule fragmentShader,
		final VertexFormat vertexFormat,
		final String debugLabel,
		final RenderPipeline pipeline,
		final ShaderSource shaderSource
	) throws ShaderManager.CompilationException {
		if (!LOMKA$ENABLE) {
			return GlProgram.link(vertexShader, fragmentShader, vertexFormat, debugLabel);
		}

		if (!lomka$canUseProgramBinary()) {
			return GlProgram.link(vertexShader, fragmentShader, vertexFormat, debugLabel);
		}

		Path cacheFile = lomka$cacheFile(pipeline, vertexShader, fragmentShader, vertexFormat);

		if (LOMKA$READ) {
			GlProgram cached = lomka$tryLoadProgram(cacheFile, vertexFormat, debugLabel);
			if (cached != null) {
				lomka$touch(cacheFile);
				return cached;
			}
		}

		GlProgram linked = LOMKA$WRITE
			? lomka$linkProgramRetrievable(vertexShader, fragmentShader, vertexFormat, debugLabel)
			: GlProgram.link(vertexShader, fragmentShader, vertexFormat, debugLabel);
		if (LOMKA$WRITE) {
			lomka$tryStoreProgram(cacheFile, linked);
		}
		return linked;
	}

	@Unique
	private static boolean lomka$canUseProgramBinary() {
		RenderSystem.assertOnRenderThread();
		try {
			var caps = GL.getCapabilities();
			return caps != null && caps.glGetProgramBinary != 0L && caps.glProgramBinary != 0L && caps.glProgramParameteri != 0L;
		} catch (Throwable ignored) {
			return false;
		}
	}

	@Unique
	private static GlProgram lomka$linkProgramRetrievable(
		final GlShaderModule vertexShader,
		final GlShaderModule fragmentShader,
		final VertexFormat vertexFormat,
		final String debugLabel
	) throws ShaderManager.CompilationException {
		RenderSystem.assertOnRenderThread();

		int programId = GlStateManager.glCreateProgram();
		if (programId <= 0) {
			throw new ShaderManager.CompilationException("Could not create shader program (returned program ID " + programId + ")");
		}

		int attributeLocation = 0;
		for (String attributeName : vertexFormat.getElementAttributeNames()) {
			GlStateManager._glBindAttribLocation(programId, attributeLocation, attributeName);
			attributeLocation++;
		}

		GlStateManager.glAttachShader(programId, vertexShader.getShaderId());
		GlStateManager.glAttachShader(programId, fragmentShader.getShaderId());
		try {
			GL41C.glProgramParameteri(programId, GL41C.GL_PROGRAM_BINARY_RETRIEVABLE_HINT, GL11C.GL_TRUE);
		} catch (Throwable ignored) {
		}
		GlStateManager.glLinkProgram(programId);
		int linkStatus = GlStateManager.glGetProgrami(programId, GL20C.GL_LINK_STATUS);
		String linkMessage = GlStateManager.glGetProgramInfoLog(programId, 32768);
		if (linkStatus != 0 && !linkMessage.contains("Failed for unknown reason")) {
			if (!linkMessage.isEmpty()) {
				LOMKA$LOGGER.info(
					"Info log when linking program containing VS {} and FS {}. Log output: {}",
					vertexShader.getId(),
					fragmentShader.getId(),
					linkMessage
				);
			}
			return lomka$newProgram(programId, debugLabel);
		}

		try {
			GlStateManager.glDeleteProgram(programId);
		} catch (Exception ignored) {
		}
		throw new ShaderManager.CompilationException(
			"Error encountered when linking program containing VS " + vertexShader.getId() + " and FS " + fragmentShader.getId() + ". Log output: " + linkMessage
		);
	}

	@Unique
	private static GlProgram lomka$tryLoadProgram(final Path cacheFile, final VertexFormat vertexFormat, final String debugLabel) {
		long size;
		try {
			size = Files.size(cacheFile);
		} catch (NoSuchFileException ignored) {
			return null;
		} catch (Exception ignored) {
			return null;
		}

		if (size <= 0L || (LOMKA$MAX_ENTRY_BYTES > 0L && size > LOMKA$MAX_ENTRY_BYTES) || size > (long)Integer.MAX_VALUE) {
			return null;
		}

		byte[] bytes;
		try {
			bytes = Files.readAllBytes(cacheFile);
		} catch (Exception ignored) {
			return null;
		}

		if (bytes.length < 16) {
			return null;
		}

		ByteBuffer bb = ByteBuffer.wrap(bytes).order(ByteOrder.BIG_ENDIAN);
		int magic = bb.getInt();
		int version = bb.getInt();
		if (magic != LOMKA$MAGIC || version != LOMKA$FORMAT_VERSION) {
			return null;
		}

		int format = bb.getInt();
		int len = bb.getInt();
		if (len <= 0 || len != bb.remaining()) {
			return null;
		}

		int programId = GlStateManager.glCreateProgram();
		if (programId <= 0) {
			return null;
		}

		int attributeLocation = 0;
		for (String attributeName : vertexFormat.getElementAttributeNames()) {
			GlStateManager._glBindAttribLocation(programId, attributeLocation, attributeName);
			attributeLocation++;
		}

		ByteBuffer bin = null;
		try {
			bin = MemoryUtil.memAlloc(len);
			bin.put(bytes, 16, len);
			bin.flip();
			GL41C.glProgramBinary(programId, format, bin);
			int linkStatus = GlStateManager.glGetProgrami(programId, GL20C.GL_LINK_STATUS);
			if (linkStatus == 0) {
				GlStateManager.glDeleteProgram(programId);
				return null;
			}
			return lomka$newProgram(programId, debugLabel);
		} catch (Throwable ignored) {
			try {
				GlStateManager.glDeleteProgram(programId);
			} catch (Exception ignored2) {
			}
			return null;
		} finally {
			if (bin != null) {
				MemoryUtil.memFree(bin);
			}
		}
	}

	@Unique
	private static void lomka$tryStoreProgram(final Path cacheFile, final GlProgram program) {
		int programId = program.getProgramId();
		int len;
		try {
			len = GlStateManager.glGetProgrami(programId, GL41C.GL_PROGRAM_BINARY_LENGTH);
		} catch (Throwable ignored) {
			return;
		}

		if (len <= 0) {
			return;
		}
		if (LOMKA$MAX_ENTRY_BYTES > 0L && (long)len + 16L > LOMKA$MAX_ENTRY_BYTES) {
			return;
		}

		ByteBuffer bin = null;
		try {
			bin = MemoryUtil.memAlloc(len);
			int actualLen;
			int format;
			try (MemoryStack stack = MemoryStack.stackPush()) {
				var lengthOut = stack.mallocInt(1);
				var formatOut = stack.mallocInt(1);
				GL41C.glGetProgramBinary(programId, lengthOut, formatOut, bin);
				actualLen = lengthOut.get(0);
				format = formatOut.get(0);
			}
			if (actualLen <= 0 || actualLen > len) {
				return;
			}
			if (LOMKA$MAX_ENTRY_BYTES > 0L && (long)actualLen + 16L > LOMKA$MAX_ENTRY_BYTES) {
				return;
			}

			byte[] out = new byte[16 + actualLen];
			ByteBuffer header = ByteBuffer.wrap(out).order(ByteOrder.BIG_ENDIAN);
			header.putInt(LOMKA$MAGIC);
			header.putInt(LOMKA$FORMAT_VERSION);
			header.putInt(format);
			header.putInt(actualLen);
			bin.flip();
			if (bin.remaining() < actualLen) {
				return;
			}
			bin.get(out, 16, actualLen);

			try {
				Files.createDirectories(cacheFile.getParent());
			} catch (Exception ignored) {
				return;
			}

			try {
				Path temp = cacheFile.resolveSibling(cacheFile.getFileName().toString() + "." + Thread.currentThread().threadId() + ".tmp");
				Files.write(temp, out);
				try {
					Files.move(temp, cacheFile, StandardCopyOption.ATOMIC_MOVE);
				} catch (Exception ignored) {
					try {
						Files.move(temp, cacheFile, StandardCopyOption.REPLACE_EXISTING);
					} catch (Exception ignored2) {
					}
				}
				lomka$touch(cacheFile);
				lomka$maybeTrim();
			} catch (Exception ignored) {
			}
		} catch (Throwable ignored) {
		} finally {
			if (bin != null) {
				MemoryUtil.memFree(bin);
			}
		}
	}

	@Unique
	private static GlProgram lomka$newProgram(final int programId, final String debugLabel) {
		try {
			Constructor<GlProgram> ctor = LOMKA$GLPROGRAM_CTOR;
			if (ctor == null) {
				ctor = GlProgram.class.getDeclaredConstructor(int.class, String.class);
				ctor.setAccessible(true);
				LOMKA$GLPROGRAM_CTOR = ctor;
			}
			return ctor.newInstance(programId, debugLabel);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	@Unique
	private static Path lomka$cacheFile(final RenderPipeline pipeline, final GlShaderModule vs, final GlShaderModule fs, final VertexFormat vf) {
		Path root = Minecraft.getInstance().gameDirectory.toPath().resolve("cache").resolve("lomka").resolve("shaderbin");
		String key = lomka$key(pipeline, vs, fs, vf);
		String hash = lomka$sha1(key);
		return root.resolve(hash.substring(0, 2)).resolve(hash + ".bin");
	}

	@Unique
	private static String lomka$key(final RenderPipeline pipeline, final GlShaderModule vs, final GlShaderModule fs, final VertexFormat vf) {
		StringBuilder sb = new StringBuilder(512);
		if (LOMKA$DRIVER_SALT) {
			String prefix = LOMKA$DRIVER_KEY_PREFIX;
			if (prefix == null) {
				String vendor = "";
				String renderer = "";
				String version = "";
				try {
					vendor = String.valueOf(GlStateManager._getString(GL11C.GL_VENDOR));
					renderer = String.valueOf(GlStateManager._getString(GL11C.GL_RENDERER));
					version = String.valueOf(GlStateManager._getString(GL11C.GL_VERSION));
				} catch (Exception ignored) {
				}
				prefix = vendor + "|" + renderer + "|" + version + "|";
				LOMKA$DRIVER_KEY_PREFIX = prefix;
			}
			sb.append(prefix);
		} else {
			sb.append("|||");
		}

		sb.append("gen:").append(Lomka.getResourceReloadSalt()).append('|');
		sb.append(pipeline.getLocation()).append('|');
		sb.append(vs.getId()).append('|');
		sb.append(fs.getId()).append('|');

		for (String a : vf.getElementAttributeNames()) {
			sb.append(a).append(',');
		}
		sb.append('|');

		ShaderDefines d = pipeline.getShaderDefines();
		if (d != null && !d.isEmpty()) {
			List<String> keys = new ArrayList<>(d.values().keySet());
			keys.sort(String::compareTo);
			for (String k : keys) {
				Object v = d.values().get(k);
				sb.append(k).append('=').append(v == null ? "" : v.toString()).append(';');
			}
			sb.append('|');
			List<String> flags = new ArrayList<>(d.flags());
			flags.sort(String::compareTo);
			for (String f : flags) {
				sb.append(f).append(';');
			}
		}

		return sb.toString();
	}

	@Unique
	private static void lomka$touch(final Path file) {
		try {
			Files.setLastModifiedTime(file, FileTime.fromMillis(Util.getMillis()));
		} catch (Exception ignored) {
		}
	}

	@Unique
	private static void lomka$maybeTrim() {
		long maxTotal = LOMKA$MAX_TOTAL_BYTES;
		if (maxTotal <= 0L) {
			return;
		}

		long now = Util.getMillis();
		long last = LOMKA$LAST_TRIM_MS.get();
		if (now - last < LOMKA$TRIM_INTERVAL_MS) {
			return;
		}
		if (!LOMKA$LAST_TRIM_MS.compareAndSet(last, now)) {
			return;
		}

		Path root = Minecraft.getInstance().gameDirectory.toPath().resolve("cache").resolve("lomka").resolve("shaderbin");
		if (!Files.isDirectory(root)) {
			return;
		}

		if (LOMKA$TRIM_ASYNC) {
			Util.ioPool().execute(() -> lomka$trimNow(root, maxTotal));
			return;
		}

		lomka$trimNow(root, maxTotal);
	}

	@Unique
	private static void lomka$trimNow(final Path root, final long maxTotal) {
		long total = 0L;
		List<Object[]> entries = new ArrayList<>(2048);
		try (Stream<Path> stream = Files.walk(root)) {
			for (Path p : (Iterable<Path>) stream::iterator) {
				if (!p.toString().endsWith(".bin")) {
					continue;
				}
				try {
					long size = Files.size(p);
					total += size;
					long time;
					try {
						time = Files.getLastModifiedTime(p).toMillis();
					} catch (Exception ignored) {
						time = 0L;
					}
					entries.add(new Object[] { p, Long.valueOf(size), Long.valueOf(time) });
				} catch (Exception ignored) {
				}
			}
		} catch (Exception ignored) {
			return;
		}

		if (total <= maxTotal) {
			return;
		}

		entries.sort(Comparator.comparingLong(e -> ((Long) e[2]).longValue()));
		int deleted = 0;
		for (Object[] e : entries) {
			if (total <= maxTotal || deleted >= LOMKA$MAX_DELETES_PER_TRIM) {
				break;
			}
			try {
				Path p = (Path) e[0];
				long size = ((Long) e[1]).longValue();
				if (Files.deleteIfExists(p)) {
					total -= size;
					deleted++;
				}
			} catch (Exception ignored) {
			}
		}
	}

	@Unique
	private static String lomka$sha1(final String s) {
		return Lomka.sha1Hex(s);
	}
}
