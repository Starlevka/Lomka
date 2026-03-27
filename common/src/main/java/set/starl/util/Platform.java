package set.starl.util;

import java.util.ServiceLoader;

public class Platform {
    private static final PlatformHelper HELPER = ServiceLoader.load(PlatformHelper.class).findFirst()
            .orElseThrow(() -> new RuntimeException("Failed to find PlatformHelper implementation"));

    public static PlatformHelper get() {
        return HELPER;
    }
}
