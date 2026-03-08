package set.starl.injection.mixins.shader;

import com.mojang.blaze3d.opengl.GlProgram;
import com.mojang.blaze3d.opengl.GlShaderModule;
import com.mojang.blaze3d.opengl.GlStateManager;
import com.mojang.blaze3d.shaders.ShaderSource;
import com.mojang.blaze3d.shaders.ShaderType;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.VertexFormat;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.FileTime;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Stream;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.ShaderDefines;
import net.minecraft.client.renderer.ShaderManager;
import net.minecraft.resources.Identifier;
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
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(GlDevice.class)
public class GlDeviceBinaryCacheMixin {
	@Unique
	private static final Logger LOMKA$LOGGER = LogUtils.getLogger();

	@Unique
	private static final boolean LOMKA$ENABLE = Lomka.JAVA_22_OPTIMIZATIONS
		&& Lomka.CONFIG.shaders.programBinaryCache.enable;

	@Unique
	private static final boolean LOMKA$READ = true;

	@Unique
	private static final boolean LOMKA$WRITE = true;

	@Unique
	private static final boolean LOMKA$DRIVER_SALT = true;

	@Unique
	private static final long LOMKA$MAX_ENTRY_BYTES = Lomka.CONFIG.shaders.programBinaryCache.maxEntryBytes;

	@Unique
	private static final long LOMKA$MAX_TOTAL_BYTES = Lomka.CONFIG.shaders.programBinaryCache.maxTotalBytes;

	@Unique
	private static final long LOMKA$TRIM_INTERVAL_MS = 60_000L; // Increase to 60s to reduce frequency

	@Unique
	private static final int LOMKA$MAX_DELETES_PER_TRIM = 32;

	@Unique
	private static final boolean LOMKA$TRIM_ASYNC = true;

	@Unique
	private static final boolean LOMKA$DISABLE_TRIM = false; // Set to true if trimming causes freezes

	@Unique
	private static final AtomicLong LOMKA$LAST_TRIM_MS = new AtomicLong(0L);

	@Unique
	private static final int LOMKA$MAGIC = 0x4C4F4D4B;

	@Unique
	private static final int LOMKA$FORMAT_VERSION = 1;

	@Unique
	private static volatile MethodHandle LOMKA$GLPROGRAM_CTOR;

	@Unique
	private static volatile byte[] LOMKA$DRIVER_SALT_BYTES;

	@Unique
	private static final ThreadLocal<ByteBuffer> LOMKA$BIN_BUFFER = new ThreadLocal<>();

	@Unique
	private static final ThreadLocal<ByteBuffer> LOMKA$FILE_BUFFER = new ThreadLocal<>();

	@Unique
	private static final ThreadLocal<ByteBuffer> LOMKA$HEADER_BUFFER = new ThreadLocal<>();

	@Unique
	private static final ThreadLocal<byte[]> LOMKA$SCRATCH_BYTES = ThreadLocal.withInitial(() -> new byte[1024]);

	@Unique
	private static final ThreadLocal<MessageDigest> LOMKA$SHA1 = new ThreadLocal<>();

	@Unique
	private static final char[] LOMKA$HEX = "0123456789abcdef".toCharArray();

	@Unique
	private static final Map<GlShaderModule, byte[]> LOMKA$MODULE_HASHES = Collections.synchronizedMap(new WeakHashMap<>());

	@Unique
	private static final Executor LOMKA$IO_WORKER = Executors.newSingleThreadExecutor(r -> {
		Thread t = new Thread(r, "Lomka Shader Cache Worker");
		t.setDaemon(true);
		t.setPriority(Thread.MIN_PRIORITY);
		return t;
	});

	@Unique
	private static final long LOMKA$TOUCH_THRESHOLD_MS = 3_600_000L; // 1 hour

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

		Path cacheFile = lomka$cacheFile(pipeline, vertexShader, fragmentShader, vertexFormat, shaderSource);

		if (LOMKA$READ) {
			GlProgram cached = lomka$tryLoadProgram(cacheFile, vertexFormat, debugLabel);
			if (cached != null) {
				LOMKA$IO_WORKER.execute(() -> lomka$touch(cacheFile));
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
			GlProgram wrapped = lomka$newProgram(programId, debugLabel);
			if (wrapped != null) {
				return wrapped;
			}
			try {
				GlStateManager.glDeleteProgram(programId);
			} catch (Exception ignored) {
			}
			return GlProgram.link(vertexShader, fragmentShader, vertexFormat, debugLabel);
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

		int sizeI = (int)size;
		if (sizeI < 16) {
			return null;
		}

		ByteBuffer file;
		try {
			file = lomka$getFileBuffer(sizeI);
			file.clear();
			file.limit(sizeI);
			try (var ch = Files.newByteChannel(cacheFile, StandardOpenOption.READ)) {
				while (file.hasRemaining()) {
					if (ch.read(file) < 0) {
						return null;
					}
				}
			}
			file.flip();
		} catch (Exception ignored) {
			return null;
		}

		file.order(ByteOrder.BIG_ENDIAN);
		int magic = file.getInt();
		int version = file.getInt();
		if (magic != LOMKA$MAGIC || version != LOMKA$FORMAT_VERSION) {
			return null;
		}

		int format = file.getInt();
		int len = file.getInt();
		if (len <= 0 || len != file.remaining()) {
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

		try {
			ByteBuffer bin = file.slice();
			bin.limit(len);
			GL41C.glProgramBinary(programId, format, bin);
			int linkStatus = GlStateManager.glGetProgrami(programId, GL20C.GL_LINK_STATUS);
			if (linkStatus == 0) {
				GlStateManager.glDeleteProgram(programId);
				return null;
			}
			GlProgram wrapped = lomka$newProgram(programId, debugLabel);
			if (wrapped != null) {
				return wrapped;
			}
			GlStateManager.glDeleteProgram(programId);
			return null;
		} catch (Throwable ignored) {
			try {
				GlStateManager.glDeleteProgram(programId);
			} catch (Exception ignored2) {
			}
			return null;
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

		try {
			ByteBuffer bin = lomka$getBinBuffer(len);
			bin.clear();
			bin.limit(len);
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
			
			// Copy to heap to avoid concurrency issues with ThreadLocal buffer
			byte[] data = new byte[actualLen];
			bin.position(0);
			bin.get(data);
			
			final int finalFormat = format;
			LOMKA$IO_WORKER.execute(() -> {
				try {
					Files.createDirectories(cacheFile.getParent());
					Path temp = cacheFile.resolveSibling(cacheFile.getFileName().toString() + "." + Thread.currentThread().threadId() + ".tmp");
					try (FileChannel fc = FileChannel.open(temp, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.WRITE)) {
						ByteBuffer header = ByteBuffer.allocate(16).order(ByteOrder.BIG_ENDIAN);
						header.putInt(LOMKA$MAGIC);
						header.putInt(LOMKA$FORMAT_VERSION);
						header.putInt(finalFormat);
						header.putInt(data.length);
						header.flip();
						
						ByteBuffer binBuf = ByteBuffer.wrap(data);
						while (header.hasRemaining() || binBuf.hasRemaining()) {
							fc.write(new ByteBuffer[] { header, binBuf });
						}
					}
					try {
						Files.move(temp, cacheFile, StandardCopyOption.ATOMIC_MOVE);
					} catch (Exception ignored) {
						Files.move(temp, cacheFile, StandardCopyOption.REPLACE_EXISTING);
					}
					lomka$touch(cacheFile);
					lomka$maybeTrim();
				} catch (Exception ignored) {
				}
			});
		} catch (Throwable ignored) {
		}
	}

	@Unique
	private static ByteBuffer lomka$getBinBuffer(final int capacity) {
		ByteBuffer buf = LOMKA$BIN_BUFFER.get();
		if (buf == null || buf.capacity() < capacity) {
			int newCap = lomka$nextPow2AtLeast(Math.max(1, capacity));
			if (LOMKA$MAX_ENTRY_BYTES > 0L && (long)newCap > LOMKA$MAX_ENTRY_BYTES) {
				newCap = capacity;
			}
			buf = ByteBuffer.allocateDirect(newCap);
			LOMKA$BIN_BUFFER.set(buf);
		}
		return buf;
	}

	@Unique
	private static ByteBuffer lomka$getFileBuffer(final int capacity) {
		ByteBuffer buf = LOMKA$FILE_BUFFER.get();
		if (buf == null || buf.capacity() < capacity) {
			int newCap = lomka$nextPow2AtLeast(Math.max(1, capacity));
			if (LOMKA$MAX_ENTRY_BYTES > 0L && (long)newCap > LOMKA$MAX_ENTRY_BYTES) {
				newCap = capacity;
			}
			buf = ByteBuffer.allocateDirect(newCap);
			LOMKA$FILE_BUFFER.set(buf);
		}
		return buf;
	}

	@Unique
	private static ByteBuffer lomka$getHeaderBuffer() {
		ByteBuffer buf = LOMKA$HEADER_BUFFER.get();
		if (buf == null) {
			buf = ByteBuffer.allocate(16);
			buf.order(ByteOrder.BIG_ENDIAN);
			LOMKA$HEADER_BUFFER.set(buf);
		}
		return buf;
	}

	@Unique
	private static int lomka$nextPow2AtLeast(final int x) {
		int v = x - 1;
		v |= v >>> 1;
		v |= v >>> 2;
		v |= v >>> 4;
		v |= v >>> 8;
		v |= v >>> 16;
		return v + 1;
	}

	@Unique
	private static GlProgram lomka$newProgram(final int programId, final String debugLabel) {
		try {
			MethodHandle ctor = LOMKA$GLPROGRAM_CTOR;
			if (ctor == null) {
				MethodHandles.Lookup lookup = MethodHandles.lookup();
				ctor = lookup.findConstructor(GlProgram.class, MethodType.methodType(void.class, int.class, String.class));
				LOMKA$GLPROGRAM_CTOR = ctor;
			}
			return (GlProgram) ctor.invokeExact(programId, debugLabel);
		} catch (Throwable ignored) {
			return null;
		}
	}

	@Unique
	private static Path lomka$cacheFile(final RenderPipeline pipeline, final GlShaderModule vs, final GlShaderModule fs, final VertexFormat vf, final ShaderSource shaderSource) {
		Path root = Minecraft.getInstance().gameDirectory.toPath().resolve("cache").resolve("lomka").resolve("shaderbin");
		String hash = lomka$hashKey(pipeline, vs, fs, vf, shaderSource);
		return root.resolve(hash.substring(0, 2)).resolve(hash + ".bin");
	}

	@Unique
	private static String lomka$hashKey(final RenderPipeline pipeline, final GlShaderModule vs, final GlShaderModule fs, final VertexFormat vf, final ShaderSource shaderSource) {
		MessageDigest md = LOMKA$SHA1.get();
		if (md == null) {
			try {
				md = MessageDigest.getInstance("SHA-1");
				LOMKA$SHA1.set(md);
			} catch (Exception ignored) {
				return "error";
			}
		}
		md.reset();
		
		final MessageDigest finalMd = md;

		if (LOMKA$DRIVER_SALT) {
			byte[] salt = LOMKA$DRIVER_SALT_BYTES;
			if (salt == null) {
				String vendor = "";
				String renderer = "";
				String version = "";
				try {
					vendor = String.valueOf(GlStateManager._getString(GL11C.GL_VENDOR));
					renderer = String.valueOf(GlStateManager._getString(GL11C.GL_RENDERER));
					version = String.valueOf(GlStateManager._getString(GL11C.GL_VERSION));
				} catch (Exception ignored) {
				}
				String prefix = vendor + "|" + renderer + "|" + version + "|";
				salt = prefix.getBytes(StandardCharsets.UTF_8);
				LOMKA$DRIVER_SALT_BYTES = salt;
			}
			md.update(salt);
		}

		lomka$mdUpdate(md, "gen:0|");
		lomka$mdUpdate(md, pipeline.getLocation().toString());
		lomka$mdUpdate(md, "|");
		lomka$mdUpdate(md, vs.getId().toString());
		lomka$mdUpdate(md, "|");
		lomka$mdUpdate(md, fs.getId().toString());
		lomka$mdUpdate(md, "|");
		lomka$mdUpdate(md, shaderSource.getClass().getName());
		lomka$mdUpdate(md, "|");
		
		lomka$updateShaderSourceHash(md, shaderSource, vs);
		lomka$mdUpdate(md, "|");
		lomka$updateShaderSourceHash(md, shaderSource, fs);
		lomka$mdUpdate(md, "|");

		for (String a : vf.getElementAttributeNames()) {
			lomka$mdUpdate(md, a);
			lomka$mdUpdate(md, ",");
		}
		lomka$mdUpdate(md, "|");

		ShaderDefines d = pipeline.getShaderDefines();
		if (d != null && !d.isEmpty()) {
			d.values().keySet().stream().sorted().forEach(k -> {
				lomka$mdUpdate(finalMd, k);
				lomka$mdUpdate(finalMd, "=");
				Object v = d.values().get(k);
				lomka$mdUpdate(finalMd, v == null ? "" : v.toString());
				lomka$mdUpdate(finalMd, ";");
			});
			lomka$mdUpdate(finalMd, "|");
			d.flags().stream().sorted().forEach(f -> {
				lomka$mdUpdate(finalMd, f);
				lomka$mdUpdate(finalMd, ";");
			});
		}

		byte[] digest = md.digest();
		char[] out = new char[digest.length * 2];
		for (int i = 0; i < digest.length; i++) {
			int v = digest[i] & 0xFF;
			out[i * 2] = LOMKA$HEX[v >>> 4];
			out[i * 2 + 1] = LOMKA$HEX[v & 0x0F];
		}
		return new String(out);
	}

	@Unique
	private static void lomka$mdUpdate(MessageDigest md, String s) {
		if (s == null || s.isEmpty()) return;
		int len = s.length();
		byte[] scratch = LOMKA$SCRATCH_BYTES.get();
		if (scratch.length < len * 3) {
			scratch = new byte[len * 3];
			LOMKA$SCRATCH_BYTES.set(scratch);
		}
		// Basic UTF-8 encoding into scratch to avoid String.getBytes() allocation
		int bIdx = 0;
		for (int i = 0; i < len; i++) {
			char c = s.charAt(i);
			if (c < 128) {
				scratch[bIdx++] = (byte) c;
			} else if (c < 2048) {
				scratch[bIdx++] = (byte) (192 | (c >> 6));
				scratch[bIdx++] = (byte) (128 | (c & 63));
			} else {
				scratch[bIdx++] = (byte) (224 | (c >> 12));
				scratch[bIdx++] = (byte) (128 | ((c >> 6) & 63));
				scratch[bIdx++] = (byte) (128 | (c & 63));
			}
		}
		md.update(scratch, 0, bIdx);
	}

	@Unique
	private static void lomka$updateShaderSourceHash(final MessageDigest md, final ShaderSource shaderSource, final GlShaderModule module) {
		byte[] hash = LOMKA$MODULE_HASHES.get(module);
		if (hash == null) {
			try {
				String source = shaderSource.get(module.getId(), module.getId().toString().endsWith(".vsh") ? ShaderType.VERTEX : ShaderType.FRAGMENT);
				if (source == null) {
					hash = new byte[0];
				} else {
					MessageDigest sourceMd = MessageDigest.getInstance("SHA-1");
					sourceMd.update(source.getBytes(StandardCharsets.UTF_8));
					hash = sourceMd.digest();
				}
			} catch (Exception e) {
				hash = new byte[0];
			}
			LOMKA$MODULE_HASHES.put(module, hash);
		}
		
		if (hash.length > 0) {
			md.update(hash);
		} else {
			lomka$mdUpdate(md, "null");
		}
	}

	@Unique
	private static void lomka$touch(final Path file) {
		try {
			long now = Util.getMillis();
			long last = Files.getLastModifiedTime(file).toMillis();
			if (now - last > LOMKA$TOUCH_THRESHOLD_MS) {
				Files.setLastModifiedTime(file, FileTime.fromMillis(now));
			}
		} catch (Exception ignored) {
		}
	}

	@Unique
	private static void lomka$maybeTrim() {
		if (LOMKA$DISABLE_TRIM) {
			return;
		}
		
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
			LOMKA$IO_WORKER.execute(() -> lomka$trimNow(root, maxTotal));
			return;
		}

		lomka$trimNow(root, maxTotal);
	}

	@Unique
	private static void lomka$trimNow(final Path root, final long maxTotal) {
		long total = 0L;
		java.util.List<Object[]> entries = new java.util.ArrayList<>(2048);
		try (var ds = Files.newDirectoryStream(root)) {
			for (Path sub : ds) {
				if (!Files.isDirectory(sub)) continue;
				try (var ds2 = Files.newDirectoryStream(sub)) {
					for (Path p : ds2) {
						if (!p.toString().endsWith(".bin")) continue;
						try {
							long size = Files.size(p);
							total += size;
							long time = Files.getLastModifiedTime(p).toMillis();
							entries.add(new Object[] { p, Long.valueOf(size), Long.valueOf(time) });
						} catch (Exception ignored) {}
					}
				} catch (Exception ignored) {}
			}
		} catch (Exception ignored) {
			return;
		}

		// When we trim, we aim for 90% of maxTotal to provide some "breathing room"
		// This prevents trimming every 30 seconds if we're exactly around the limit.
		long target = (long) (maxTotal * 0.9);
		if (total <= maxTotal) {
			return;
		}

		entries.sort(Comparator.comparingLong(e -> ((Long) e[2]).longValue()));
		for (Object[] e : entries) {
			if (total <= target) {
				break;
			}
			try {
				Path p = (Path) e[0];
				long size = ((Long) e[1]).longValue();
				if (Files.deleteIfExists(p)) {
					total -= size;
				}
			} catch (Exception ignored) {
			}
		}
	}

	@Unique
	private static String lomka$sha1(final String s) {
		MessageDigest md = LOMKA$SHA1.get();
		if (md == null) {
			try {
				md = MessageDigest.getInstance("SHA-1");
				LOMKA$SHA1.set(md);
			} catch (Exception ignored) {
				return "";
			}
		}
		try {
			md.reset();
			md.update(s.getBytes(StandardCharsets.UTF_8));
			byte[] digest = md.digest();
			char[] out = new char[digest.length * 2];
			int j = 0;
			for (byte b : digest) {
				int v = b & 0xFF;
				out[j++] = LOMKA$HEX[v >>> 4];
				out[j++] = LOMKA$HEX[v & 0x0F];
			}
			return new String(out);
		} catch (Exception ignored) {
			return "";
		}
	}
}
