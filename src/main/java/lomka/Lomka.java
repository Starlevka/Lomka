package lomka;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class Lomka {
    public static final String MOD_ID = "lomka";
    public static final String VERSION = /*$ mod_version */ "0.4.0";
    private static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    public static void init() {
        LOGGER.info("Lomka v" + VERSION + " - Initializing... 🌠 Initialized!");
    }
}
