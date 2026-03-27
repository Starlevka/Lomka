package set.starl.util;

import java.nio.file.Path;

public interface PlatformHelper {
    Path getConfigDir();
    boolean isModLoaded(String modId);
    boolean isDevelopmentEnvironment();
}
