package set.starl;

import net.fabricmc.api.ModInitializer;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Iterator;
import java.util.LinkedHashMap;
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
	public static final ThreadLocal<byte[]> SHA1_ASCII_BUF = ThreadLocal.withInitial(() -> new byte[4096]);
	public static final boolean STRING_DEDUP_ENABLE = !"false".equalsIgnoreCase(System.getProperty("lomka.strings.dedup", "true"));
	public static final int STRING_DEDUP_MAX_ENTRIES = Integer.parseInt(System.getProperty("lomka.strings.dedup.maxEntries", "8192"));
	public static final int STRING_DEDUP_MAX_LEN = Integer.parseInt(System.getProperty("lomka.strings.dedup.maxLen", "256"));
	private static final LinkedHashMap<String, String> STRING_DEDUP_CACHE = new LinkedHashMap<>(8192, 0.75f, true);
	public static final ThreadLocal<byte[]> PNG_HEADER_32 = ThreadLocal.withInitial(() -> new byte[32]);
	public static volatile float RENDER_BUDGET_SCALE = 1.0f;
	public static volatile double RENDER_LEVEL_MS_EMA = -1.0;
	public static final double RENDER_BUDGET_TARGET_MS = Double.parseDouble(System.getProperty("lomka.frameBudget.targetMs", "16.6667"));
	public static final double RENDER_BUDGET_EMA_ALPHA = Double.parseDouble(System.getProperty("lomka.frameBudget.emaAlpha", "0.15"));
	public static final float RENDER_BUDGET_MIN_SCALE = Float.parseFloat(System.getProperty("lomka.frameBudget.minScale", "0.25"));
	public static final float RENDER_BUDGET_MAX_SCALE = Float.parseFloat(System.getProperty("lomka.frameBudget.maxScale", "2.0"));

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
		if (s == null || s.isEmpty()) {
			byte[] digest = md.digest();
			return lomka$toHex(digest);
		}

		byte[] buf = SHA1_ASCII_BUF.get();
		int off = 0;
		for (int i = 0; i < s.length(); i++) {
			char c = s.charAt(i);
			if (c > 0x7F) {
				byte[] digest = md.digest(s.getBytes(StandardCharsets.UTF_8));
				return lomka$toHex(digest);
			}
			buf[off++] = (byte)c;
			if (off == buf.length) {
				md.update(buf, 0, off);
				off = 0;
			}
		}
		if (off > 0) {
			md.update(buf, 0, off);
		}
		byte[] digest = md.digest();
		return lomka$toHex(digest);
	}

	private static String lomka$toHex(final byte[] digest) {
		char[] hex = new char[digest.length * 2];
		for (int i = 0; i < digest.length; i++) {
			int v = digest[i] & 0xff;
			hex[i * 2] = Character.forDigit(v >>> 4, 16);
			hex[i * 2 + 1] = Character.forDigit(v & 0xf, 16);
		}
		return new String(hex);
	}

	public static void recordRenderLevelNanos(final long nanos) {
		if (nanos <= 0L) {
			return;
		}
		double ms = (double)nanos * 1.0e-6;
		if (!(ms > 0.0)) {
			return;
		}
		double alpha = RENDER_BUDGET_EMA_ALPHA;
		if (!(alpha > 0.0) || alpha > 1.0) {
			alpha = 0.15;
		}
		double ema = RENDER_LEVEL_MS_EMA;
		if (!(ema > 0.0)) {
			ema = ms;
		} else {
			ema += (ms - ema) * alpha;
		}
		RENDER_LEVEL_MS_EMA = ema;

		double target = RENDER_BUDGET_TARGET_MS;
		if (!(target > 0.0)) {
			target = 16.6667;
		}
		float scale = (float)(target / ema);
		float min = RENDER_BUDGET_MIN_SCALE;
		float max = RENDER_BUDGET_MAX_SCALE;
		if (min <= 0.0f) {
			min = 0.25f;
		}
		if (max < min) {
			max = min;
		}
		if (scale < min) {
			scale = min;
		} else if (scale > max) {
			scale = max;
		}
		RENDER_BUDGET_SCALE = scale;
	}

	public static float getRenderBudgetScale() {
		return RENDER_BUDGET_SCALE;
	}

	public static String dedupString(final String s) {
		if (!STRING_DEDUP_ENABLE || s == null) {
			return s;
		}
		int maxEntries = STRING_DEDUP_MAX_ENTRIES;
		if (maxEntries <= 0) {
			return s;
		}
		int maxLen = STRING_DEDUP_MAX_LEN;
		if (maxLen > 0 && s.length() > maxLen) {
			return s;
		}
		synchronized (STRING_DEDUP_CACHE) {
			String existing = STRING_DEDUP_CACHE.get(s);
			if (existing != null) {
				return existing;
			}
			STRING_DEDUP_CACHE.put(s, s);
			if (STRING_DEDUP_CACHE.size() > maxEntries) {
				Iterator<String> it = STRING_DEDUP_CACHE.keySet().iterator();
				if (it.hasNext()) {
					it.next();
					it.remove();
				}
			}
			return s;
		}
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
