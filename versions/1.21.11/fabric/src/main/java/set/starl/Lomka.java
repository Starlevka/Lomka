package set.starl;

import net.fabricmc.api.ModInitializer;
import java.lang.management.GarbageCollectorMXBean;
import java.lang.management.ManagementFactory;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.Semaphore;
import set.starl.config.LomkaConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Lomka implements ModInitializer {
	public static final String MOD_ID = "lomka";
	public static final LomkaConfig CONFIG = LomkaConfig.get();
	public static final int JAVA_FEATURE_VERSION = Runtime.version().feature();
	public static final boolean JAVA_22_PLUS = JAVA_FEATURE_VERSION >= 22;
	public static final boolean JAVA_24_PLUS = JAVA_FEATURE_VERSION >= 24;
	public static final boolean JAVA_25_PLUS = JAVA_FEATURE_VERSION >= 25;
	public static final boolean JAVA_22_OPTIMIZATIONS = JAVA_22_PLUS
		&& (JAVA_25_PLUS || CONFIG.java22Optimizations);
	public static final boolean TEXTURE_DECODE_OFF_THREAD = CONFIG.textures.decode.offThread;
	public static final int TEXTURE_DECODE_PARALLELISM = CONFIG.textures.decode.parallelism;
	public static final Semaphore TEXTURE_DECODE_GUARD = new Semaphore(Math.max(1, Math.min(64, TEXTURE_DECODE_PARALLELISM)));
	public static final ThreadLocal<Boolean> IN_TEXTURE_DECODE = ThreadLocal.withInitial(() -> Boolean.FALSE);
	public static final Executor TEXTURE_DECODE_EXECUTOR = lomka$createTextureDecodeExecutor();


	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	@Override
	public void onInitialize() {

		LOGGER.info("lomka initialized!");
		lomka$logOptionalJvmOptimizations();
	}

	private static Executor lomka$createTextureDecodeExecutor() {
		return Executors.newVirtualThreadPerTaskExecutor();
	}

	private static void lomka$logOptionalJvmOptimizations() {
		List<String> args = ManagementFactory.getRuntimeMXBean().getInputArguments();
		boolean aotCacheFlag = false;
		for (int i = 0; i < args.size(); i++) {
			String arg = args.get(i);
			if (arg.startsWith("-XX:AOTCacheOutput=") || arg.startsWith("-XX:AOTCache=") || arg.startsWith("-XX:AOTCacheInput=")) {
				aotCacheFlag = true;
				break;
			}
		}
		if (JAVA_25_PLUS && aotCacheFlag) {
			LOGGER.info("Java 25 AOT cache is enabled.");
		}
		if (JAVA_24_PLUS && lomka$hasG1Gc()) {
			LOGGER.info("G1 GC detected on Java 24+ (includes JEP 475 barriers).");
		}
	}

	private static boolean lomka$hasG1Gc() {
		for (GarbageCollectorMXBean bean : ManagementFactory.getGarbageCollectorMXBeans()) {
			String name = bean.getName();
			if (name != null && name.contains("G1")) {
				return true;
			}
		}
		return false;
	}



	public static final class MathHelper {
		private MathHelper() {
		}

		public static int floorToInt(final double x) {
			int i = (int)x;
			if (i == Integer.MIN_VALUE) {
				return i;
			}
			return x < (double)i ? i - 1 : i;
		}

		public static int ceilToInt(final double x) {
			int i = (int)x;
			if (i == Integer.MAX_VALUE) {
				return i;
			}
			return x > (double)i ? i + 1 : i;
		}

		public static int clamp(final int v, final int min, final int max) {
			return v < min ? min : (v > max ? max : v);
		}

		public static float clamp(final float v, final float min, final float max) {
			return v < min ? min : (v > max ? max : v);
		}

		public static double clamp(final double v, final double min, final double max) {
			return v < min ? min : (v > max ? max : v);
		}

		public static float lerp(final float t, final float a, final float b) {
			return Math.fma(t, b - a, a);
		}

		public static double lerp(final double t, final double a, final double b) {
			return Math.fma(t, b - a, a);
		}

		public static float degToRad(final float deg) {
			return deg * 0.017453292F;
		}

		public static double degToRad(final double deg) {
			return deg * 0.017453292519943295;
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
