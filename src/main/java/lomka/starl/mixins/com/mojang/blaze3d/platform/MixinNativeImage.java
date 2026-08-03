//? if >=1.21.4 {
package lomka.starl.mixins.com.mojang.blaze3d.platform;

import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.util.ARGB;
import org.lwjgl.system.MemoryUtil;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(NativeImage.class)
public abstract class MixinNativeImage {

    @Shadow
    private NativeImage.Format format;
    @Shadow
    private int width;
    @Shadow
    private int height;
    @Shadow
    private long pixels;

    @Shadow
    protected abstract void checkAllocated();

    @Overwrite
    public void fillRect(int x, int y, int width, int height, int color) {
        this.checkAllocated();

        int startX = Math.max(0, x);
        int startY = Math.max(0, y);
        int endX = Math.min(this.width, x + width);
        int endY = Math.min(this.height, y + height);

        if (startX >= endX || startY >= endY) {
            return;
        }

        int abgrColor = ARGB.toABGR(color);

        for (int currentY = startY; currentY < endY; currentY++) {
            long rowStart = this.pixels + (currentY * (long) this.width + startX) * 4L;
            int rowWidth = endX - startX;

            for (int currentX = 0; currentX < rowWidth; currentX++) {
                MemoryUtil.memPutInt(rowStart + (currentX * 4L), abgrColor);
            }
        }
    }

    @Overwrite
    public void copyRect(NativeImage dest, int srcX, int srcY, int destX, int destY, int copyWidth, int copyHeight, boolean flipX, boolean flipY) {
        this.checkAllocated();

        MixinNativeImage destMixin = (MixinNativeImage) (Object) dest;
        destMixin.checkAllocated();

        if (!flipX && !flipY
            && this.format == NativeImage.Format.RGBA
            && dest.format() == NativeImage.Format.RGBA
            && srcX >= 0 && srcY >= 0
            && destX >= 0 && destY >= 0
            && srcX + copyWidth <= this.width
            && srcY + copyHeight <= this.height
            && destX + copyWidth <= dest.getWidth()
            && destY + copyHeight <= dest.getHeight()) {

            long srcBase = this.pixels;
            long destBase = destMixin.pixels;
            long bytesPerRow = copyWidth * 4L;

            for (int row = 0; row < copyHeight; row++) {
                long srcRowAddress = srcBase + ((srcY + row) * (long) this.width + srcX) * 4L;
                long destRowAddress = destBase + ((destY + row) * (long) dest.getWidth() + destX) * 4L;

                MemoryUtil.memCopy(srcRowAddress, destRowAddress, bytesPerRow);
            }
        } else {
            long srcBase = this.pixels;
            long destBase = destMixin.pixels;

            for (int y = 0; y < copyHeight; y++) {
                int srcOffsetY = srcY + y;
                int dstOffsetY = destY + (flipY ? copyHeight - 1 - y : y);
                long srcRowBase = srcBase + ((srcOffsetY * (long) this.width + srcX) * 4L);
                long dstRowBase = destBase + ((dstOffsetY * (long) dest.getWidth() + destX) * 4L);

                for (int x = 0; x < copyWidth; x++) {
                    int dstOffsetX = flipX ? copyWidth - 1 - x : x;
                    int pixel = MemoryUtil.memGetInt(srcRowBase + x * 4L);
                    MemoryUtil.memPutInt(dstRowBase + dstOffsetX * 4L, pixel);
                }
            }
        }
    }

}
//?}
//? if <1.21.4 {
/*package lomka.starl.mixins.com.mojang.blaze3d.platform;

import com.mojang.blaze3d.platform.NativeImage;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(NativeImage.class)
public abstract class MixinNativeImage {

    @Shadow
    private int width;
    @Shadow
    private int height;
    @Shadow
    private long pixels;

    @Shadow
    protected abstract void checkAllocated();
    @Shadow
    public abstract int getPixelRGBA(int i, int j);
    @Shadow
    public abstract void setPixelRGBA(int i, int j, int k);

    @Overwrite
    public void fillRect(int x, int y, int width, int height, int color) {
        this.checkAllocated();

        int startX = Math.max(0, x);
        int startY = Math.max(0, y);
        int endX = Math.min(this.width, x + width);
        int endY = Math.min(this.height, y + height);

        if (startX >= endX || startY >= endY) {
            return;
        }

        for (int row = startY; row < endY; row++) {
            for (int col = startX; col < endX; col++) {
                this.setPixelRGBA(col, row, color);
            }
        }
    }

    @Overwrite
    public void copyRect(NativeImage dest, int srcX, int srcY, int destX, int destY, int copyWidth, int copyHeight, boolean flipX, boolean flipY) {
        for (int row = 0; row < copyHeight; row++) {
            int srcRow = srcY + row;
            int dstRow = destY + (flipY ? copyHeight - 1 - row : row);

            for (int col = 0; col < copyWidth; col++) {
                int srcCol = srcX + col;
                int dstCol = destX + (flipX ? copyWidth - 1 - col : col);
                int pixel = this.getPixelRGBA(srcCol, srcRow);
                dest.setPixelRGBA(dstCol, dstRow, pixel);
            }
        }
    }
}*/
//?}
