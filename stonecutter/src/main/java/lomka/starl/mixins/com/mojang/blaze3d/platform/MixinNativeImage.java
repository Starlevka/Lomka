package lomka.starl.mixins.com.mojang.blaze3d.platform;

import com.mojang.blaze3d.platform.NativeImage;
import org.lwjgl.system.MemoryUtil;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;


@Mixin(NativeImage.class)
public abstract class MixinNativeImage {

    @Shadow private NativeImage.Format format;
    @Shadow private int width;
    @Shadow private int height;
    @Shadow private long pixels;

    @Shadow protected abstract void checkAllocated();

    /**
     * @author Starlev
     * @reason Replaces BufferedImage-style per-pixel setRGB with direct
     * MemoryUtil.memPutInt writes, skipping color space conversion overhead.
     */
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

        for (int currentY = startY; currentY < endY; currentY++) {
            long rowStart = this.pixels + (currentY * (long) this.width + startX) * 4L;
            int rowWidth = endX - startX;

            for (int currentX = 0; currentX < rowWidth; currentX++) {
                MemoryUtil.memPutInt(rowStart + (currentX * 4L), color);
            }
        }
    }

    //? if >=1.21.9 {
    /*@Overwrite
    public void copyRect(NativeImage dest, int srcX, int srcY, int destX, int destY, int copyWidth, int copyHeight, boolean flipX, boolean flipY) {
        this.checkAllocated();

        if (!flipX && !flipY
            && this.format == NativeImage.Format.RGBA
            && dest.format() == NativeImage.Format.RGBA
            && dest.getPointer() != 0L
            && srcX >= 0 && srcY >= 0
            && destX >= 0 && destY >= 0
            && srcX + copyWidth <= this.width
            && srcY + copyHeight <= this.height
            && destX + copyWidth <= dest.getWidth()
            && destY + copyHeight <= dest.getHeight()) {

            long srcBase = this.pixels;
            long destBase = dest.getPointer();
            long bytesPerRow = copyWidth * 4L;

            for (int row = 0; row < copyHeight; row++) {
                long srcRowAddress = srcBase + ((srcY + row) * (long) this.width + srcX) * 4L;
                long destRowAddress = destBase + ((destY + row) * (long) dest.getWidth() + destX) * 4L;

                MemoryUtil.memCopy(srcRowAddress, destRowAddress, bytesPerRow);
            }
        } else {
            for (int y = 0; y < copyHeight; y++) {
                int srcOffsetY = srcY + y;
                int dstOffsetY = destY + (flipY ? copyHeight - 1 - y : y);
                long srcRowBase = this.pixels + ((srcOffsetY * (long) this.width + srcX) * 4L);
                long dstRowBase = dest.getPointer() + ((dstOffsetY * (long) dest.getWidth() + destX) * 4L);

                for (int x = 0; x < copyWidth; x++) {
                    int dstOffsetX = flipX ? copyWidth - 1 - x : x;
                    int pixel = MemoryUtil.memGetInt(srcRowBase + x * 4L);
                    MemoryUtil.memPutInt(dstRowBase + dstOffsetX * 4L, pixel);
                }
            }
        }
    }*/
    //?}
}
