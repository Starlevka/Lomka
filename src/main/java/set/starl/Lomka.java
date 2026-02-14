package set.starl;

import net.fabricmc.api.ModInitializer;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicLong;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Lomka implements ModInitializer {
	public static final String MOD_ID = "lomka";
	public static final int JAVA_FEATURE_VERSION = Runtime.version().feature();
	public static final boolean JAVA_22_PLUS = JAVA_FEATURE_VERSION >= 22;
	public static final boolean JAVA_22_25_OPTIMIZATIONS = JAVA_22_PLUS
		&& "true".equalsIgnoreCase(System.getProperty("lomka.java22_25.optimizations", "false"));
	public static final int TEXTURE_MAX_DIMENSION = Integer.parseInt(System.getProperty("lomka.textures.maxDimension", "0"));
	public static final boolean TEXTURE_DECODE_OFF_THREAD = !"false".equalsIgnoreCase(System.getProperty("lomka.textures.decode.offThread", "true"));
	public static final int TEXTURE_DECODE_PARALLELISM = Integer.parseInt(System.getProperty("lomka.textures.decode.parallelism", "2"));
	public static final Semaphore TEXTURE_DECODE_GUARD = new Semaphore(Math.max(1, Math.min(64, TEXTURE_DECODE_PARALLELISM)));
	public static final ThreadLocal<Boolean> IN_TEXTURE_DECODE = ThreadLocal.withInitial(() -> Boolean.FALSE);
	public static final Executor TEXTURE_DECODE_EXECUTOR = lomka$createTextureDecodeExecutor();
	public static final AtomicLong RESOURCE_RELOAD_SALT = new AtomicLong(0L);
	public static final ThreadLocal<MessageDigest> SHA1 = ThreadLocal.withInitial(() -> {
		try {
			return MessageDigest.getInstance("SHA-1");
		} catch (NoSuchAlgorithmException e) {
			throw new RuntimeException(e);
		}
	});

	// This logger is used to write text to the console and the log file.
	// It is considered best practice to use your mod id as the logger's name.
	// That way, it's clear which mod wrote info, warnings, and errors.
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	@Override
	public void onInitialize() {
		// This code runs as soon as Minecraft is in a mod-load-ready state.
		// However, some things (like resources) may still be uninitialized.
		// Proceed with mild caution.

		LOGGER.info("lomka initialized!");
	}

	private static Executor lomka$createTextureDecodeExecutor() {
		String mode = System.getProperty("lomka.textures.decode.executor", "virtual");
		if (mode == null) {
			mode = "virtual";
		}
		mode = mode.toLowerCase();

		if ("common".equals(mode)) {
			return ForkJoinPool.commonPool();
		}

		if ("fixed".equals(mode) || "platform".equals(mode)) {
			int threads = Math.max(1, Math.min(64, TEXTURE_DECODE_PARALLELISM));
			return Executors.newFixedThreadPool(threads, (r) -> {
				Thread t = new Thread(r, "lomka-texture-decode-" + System.nanoTime());
				t.setDaemon(true);
				return t;
			});
		}

		ExecutorService v = Executors.newVirtualThreadPerTaskExecutor();
		return v;
	}

	public static long nextResourceReloadSalt() {
		return RESOURCE_RELOAD_SALT.incrementAndGet();
	}

	public static long getResourceReloadSalt() {
		return RESOURCE_RELOAD_SALT.get();
	}

	public static String sha1Hex(final String s) {
		MessageDigest md = SHA1.get();
		md.reset();
		byte[] digest = md.digest(s.getBytes(StandardCharsets.UTF_8));
		char[] hex = new char[digest.length * 2];
		for (int i = 0; i < digest.length; i++) {
			int v = digest[i] & 0xff;
			hex[i * 2] = Character.forDigit(v >>> 4, 16);
			hex[i * 2 + 1] = Character.forDigit(v & 0xf, 16);
		}
		return new String(hex);
	}

	public static final class UploadSizedRunnable implements Runnable {
		public final long bytes;
		private final Runnable delegate;

		public UploadSizedRunnable(final long bytes, final Runnable delegate) {
			this.bytes = bytes;
			this.delegate = delegate;
		}

		@Override
		public void run() {
			this.delegate.run();
		}
	}
}
