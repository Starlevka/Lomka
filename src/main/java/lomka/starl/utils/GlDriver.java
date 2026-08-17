//? if >=1.21.6 {
package lomka.starl.utils;

import com.mojang.blaze3d.opengl.GlStateManager;
import org.lwjgl.opengl.GL11;

/**
 * Lazily-cached GPU vendor/renderer detection used to gate driver-specific
 * workarounds. GL string queries are only valid while a GL context is current,
 * so the first call must happen during GlDevice construction; results are then
 * frozen for the process lifetime.
 */
public final class GlDriver {

    private static volatile Boolean nvidia;
    private static volatile Boolean intel;

    private GlDriver() {
    }

    public static boolean isNVIDIA() {
        if (nvidia == null) {
            nvidia = getString(GL11.GL_VENDOR).contains("NVIDIA");
        }
        return nvidia;
    }

    public static boolean isIntel() {
        if (intel == null) {
            String renderer = getString(GL11.GL_RENDERER);
            intel = renderer.contains("HD Graphics 4000")
                || renderer.contains("HD Graphics 2500")
                || renderer.contains("Ivy Bridge");
        }
        return intel;
    }

    private static String getString(int id) {
        String s = GlStateManager._getString(id);
        return s == null ? "" : s;
    }
}
//? }