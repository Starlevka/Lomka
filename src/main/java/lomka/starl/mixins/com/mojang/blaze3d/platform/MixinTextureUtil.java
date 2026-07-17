//? if >=1.21.11 {
package lomka.starl.mixins.com.mojang.blaze3d.platform;

import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.platform.TextureUtil;
import java.util.Arrays;
import lomka.starl.utils.TextureUtilState;
import net.minecraft.util.ARGB;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;

@Mixin(TextureUtil.class)
public class MixinTextureUtil {

    @Shadow @Final
    private static int[][] DIRECTIONS;

    @Unique
    private static final int lomka$MAX_POOLED_PIXELS = 8192 * 8192;

    @Unique
    private static final ThreadLocal<TextureUtilState> lomka$STATE = ThreadLocal.withInitial(TextureUtilState::new);

    /**
     * @author Starlev
     * @reason Replace ArrayList-based BFS with flat arrays and a bounded ThreadLocal pool to
     * eliminate object allocations and GC pressure during texture solidification, without
     * leaking stale color data between calls or letting one oversized texture permanently
     * inflate every worker thread's memory footprint.
     */
    @Overwrite
    public static void solidify(NativeImage nativeimage) {
        int width = nativeimage.getWidth();
        int height = nativeimage.getHeight();
        int totalPixels = width * height;

        int[] colorBuffer;
        int[] distanceBuffer;
        int[] queue;

        if (totalPixels <= lomka$MAX_POOLED_PIXELS) {
            TextureUtilState state = lomka$STATE.get();
            state.ensureCapacity(totalPixels);
            colorBuffer = state.colorBuffer;
            distanceBuffer = state.distanceBuffer;
            queue = state.queue;
        } else {
            colorBuffer = new int[totalPixels];
            distanceBuffer = new int[totalPixels];
            queue = new int[totalPixels];
        }

        Arrays.fill(distanceBuffer, 0, totalPixels, Integer.MAX_VALUE);

        int head = 0;
        int tail = 0;

        for (int x = 0; x < width; ++x) {
            for (int y = 0; y < height; ++y) {
                int color = nativeimage.getPixel(x, y);
                if (ARGB.alpha(color) != 0) {
                    int packedIndex = x + y * width;

                    distanceBuffer[packedIndex] = 0;
                    colorBuffer[packedIndex] = color;

                    queue[tail++] = (y << 16) | x;
                }
            }
        }

        if (tail == 0) {
            for (int x = 0; x < width; ++x) {
                for (int y = 0; y < height; ++y) {
                    nativeimage.setPixel(x, y, 0);
                }
            }
            return;
        }

        int[][] directions = DIRECTIONS;
        int dirCount = directions.length;

        while (head < tail) {
            int val = queue[head++];
            int x = val & 0xFFFF;
            int y = val >> 16;
            int packedIndex = x + y * width;

            for (int i = 0; i < dirCount; ++i) {
                int[] dir = directions[i];
                int nx = x + dir[0];
                int ny = y + dir[1];

                if (nx >= 0 && ny >= 0 && nx < width && ny < height) {
                    int nPackedIndex = nx + ny * width;
                    if (distanceBuffer[nPackedIndex] > distanceBuffer[packedIndex] + 1) {
                        distanceBuffer[nPackedIndex] = distanceBuffer[packedIndex] + 1;
                        colorBuffer[nPackedIndex] = colorBuffer[packedIndex];
                        queue[tail++] = (ny << 16) | nx;
                    }
                }
            }
        }

        for (int x = 0; x < width; ++x) {
            for (int y = 0; y < height; ++y) {
                int color = nativeimage.getPixel(x, y);
                if (ARGB.alpha(color) == 0) {
                    nativeimage.setPixel(x, y, ARGB.color(0, colorBuffer[x + y * width]));
                }
            }
        }
    }
}
//?}