//? if >=1.21.11 {
package lomka.starl.mixins.com.mojang.blaze3d.platform;

import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.platform.TextureUtil;
import it.unimi.dsi.fastutil.ints.IntArrayFIFOQueue;
import java.util.Arrays;
import net.minecraft.util.ARGB;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(TextureUtil.class)
public class MixinTextureUtil {

    @Shadow @Final
    private static int[][] DIRECTIONS;

    /**
     * @author Starlev
     * @reason Replace ArrayList-based BFS with IntArrayFIFOQueue and packed int coordinates to eliminate object allocations during texture solidification.
     */
    @Overwrite
    public static void solidify(NativeImage nativeimage) {
        int width = nativeimage.getWidth();
        int height = nativeimage.getHeight();
        int totalPixels = width * height;

        int[] colorBuffer = new int[totalPixels];
        int[] distanceBuffer = new int[totalPixels];

        Arrays.fill(distanceBuffer, Integer.MAX_VALUE);
        IntArrayFIFOQueue queue = new IntArrayFIFOQueue();

        for (int x = 0; x < width; ++x) {
            for (int y = 0; y < height; ++y) {
                int color = nativeimage.getPixel(x, y);
                if (ARGB.alpha(color) != 0) {
                    int packedIndex = x + y * width;

                    distanceBuffer[packedIndex] = 0;
                    colorBuffer[packedIndex] = color;
                    
                    queue.enqueue((y << 16) | x);
                }
            }
        }

        int[][] directions = DIRECTIONS;
        int dirCount = directions.length;

        while (!queue.isEmpty()) {
            int val = queue.dequeueInt();
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
                        queue.enqueue((ny << 16) | nx);
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