package set.starl;

import java.lang.management.GarbageCollectorMXBean;
import java.lang.management.ManagementFactory;
import java.util.List;
import set.starl.config.LomkaConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LomkaCore {
    public static final String MOD_ID = "lomka";
    public static final LomkaConfig CONFIG = LomkaConfig.get();
    public static final int JAVA_FEATURE_VERSION = Runtime.version().feature();
    public static final boolean JAVA_25_PLUS = JAVA_FEATURE_VERSION >= 25;
    private static final Logger LOGGER = LoggerFactory.getLogger("Lomka");

    public static void init() {
        LOGGER.info("Lomka core initialized!");
        lomka$logOptionalJvmOptimizations();
        lomka$logJavaOptimizations();
    }

    private static void lomka$logOptionalJvmOptimizations() {
        if (CONFIG.java.java25Optimizations && JAVA_25_PLUS) {
            LOGGER.info("Java 25 optimizations ENABLED in config.");
        } else if (JAVA_25_PLUS) {
            LOGGER.info("Java 25 optimizations DISABLED in config.");
        }
        if (JAVA_FEATURE_VERSION >= 24 && lomka$hasG1Gc()) {
            LOGGER.info("G1 GC detected on Java 24+ (includes JEP 475 barriers).");
        }
    }

    private static void lomka$logAotCacheStatus() {
        List<String> jvmArgs = ManagementFactory.getRuntimeMXBean().getInputArguments();
        boolean hasAotCache = false;
        for (String arg : jvmArgs) {
            if (arg.startsWith("-XX:AOTCacheOutput=") || arg.startsWith("-XX:AOTCache=") || arg.startsWith("-XX:AOTCacheInput=")) {
                hasAotCache = true;
                break;
            }
        }
        if (hasAotCache) {
            LOGGER.info("  AOT cache: ENABLED — faster startup and reduced JIT warmup.");
        } else {
            LOGGER.info("  AOT cache: not enabled. Tip: Add -XX:AOTCache=<path> to JVM args.");
        }
    }

    private static void lomka$logCompactHeadersStatus() {
        List<String> jvmArgs = ManagementFactory.getRuntimeMXBean().getInputArguments();
        boolean hasCompact = false;
        for (String arg : jvmArgs) {
            if (arg.equals("-XX:+UseCompactObjectHeaders")) {
                hasCompact = true;
                break;
            }
        }
        if (hasCompact) {
            LOGGER.info("  Compact object headers: ENABLED — 4 bytes saved per object, better cache locality.");
        } else {
            LOGGER.info("  Compact object headers: not enabled. Tip: Add -XX:+UseCompactObjectHeaders to JVM args.");
        }
    }

    private static void lomka$logJavaOptimizations() {
        LOGGER.info("Java {} optimizations:", JAVA_FEATURE_VERSION);
        LOGGER.info("  Math.fma: ENABLED (lerp, vector math, distSq)");
        LOGGER.info("  Generation-based cache: ENABLED (no clear() per frame)");
        if (JAVA_FEATURE_VERSION >= 24) {
            LOGGER.info("  JEP 475 G1 barriers: ENABLED (if G1 GC active)");
        }
        if (JAVA_25_PLUS) {
            LOGGER.info("  String.hash @Stable: ENABLED (automatic, faster Map.get)");
            LOGGER.info("  Section visibility cache: Java 25+ optimized path");
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

        public static double distSq(final double x1, final double y1, final double z1,
                                    final double x2, final double y2, final double z2) {
            double dx = x1 - x2;
            double dy = y1 - y2;
            double dz = z1 - z2;
            return Math.fma(dx, dx, Math.fma(dy, dy, dz * dz));
        }

        public static int hash3(final int x, final int y, final int z) {
            int h = x * 73856093 ^ y * 19349663 ^ z * 83492791;
            h ^= (h >>> 16);
            h *= 0x45d9f3b;
            h ^= (h >>> 16);
            return h;
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