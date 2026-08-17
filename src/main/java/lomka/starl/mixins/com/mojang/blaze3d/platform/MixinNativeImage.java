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

    /**
     * @author Starlev
     * @reason Direct native memory manipulation via MemoryUtil.memPutInt.
     *         Converts ARGB to ABGR once upfront and fills contiguous memory blocks directly,
     *         bypassing per-pixel method dispatch, bounds checks, and allocation checks.
     */
    @Overwrite
    public void fillRect(int x, int y, int width, int height, int color) {
        this.checkAllocated();

        if (this.format != NativeImage.Format.RGBA) {
            throw new IllegalArgumentException("fillRect only works on RGBA images; have " + this.format);
        }

        int startX = Math.max(0, x);
        int startY = Math.max(0, y);
        int endX = Math.min(this.width, x + width);
        int endY = Math.min(this.height, y + height);

        if (startX >= endX || startY >= endY) {
            return;
        }

        int abgrColor = ARGB.toABGR(color);
        int rowWidth = endX - startX;
        long basePixels = this.pixels;
        int imgWidth = this.width;

        if (startX == 0 && endX == imgWidth) {
            long startAddress = basePixels + ((long) startY * imgWidth) * 4L;
            long endAddress = startAddress + ((long) (endY - startY) * imgWidth) * 4L;
            for (long addr = startAddress; addr < endAddress; addr += 4L) {
                MemoryUtil.memPutInt(addr, abgrColor);
            }
        } else {
            long rowBytes = (long) rowWidth * 4L;
            for (int currentY = startY; currentY < endY; currentY++) {
                long rowStart = basePixels + ((long) currentY * imgWidth + startX) * 4L;
                long rowEnd = rowStart + rowBytes;
                for (long addr = rowStart; addr < rowEnd; addr += 4L) {
                    MemoryUtil.memPutInt(addr, abgrColor);
                }
            }
        }
    }

    /**
     * @author Starlev
     * @reason Replaces per-pixel get/set loops with SIMD/Unsafe MemoryUtil.memCopy for bulk row
     *         and whole-image transfers, and uses direct pointer offset addressing for flipped regions,
     *         completely eliminating virtual dispatch and per-pixel bounds validation on hot paths.
     */
    @Overwrite
    public void copyRect(NativeImage dest, int srcX, int srcY, int destX, int destY, int copyWidth, int copyHeight, boolean flipX, boolean flipY) {
        this.checkAllocated();

        MixinNativeImage destMixin = (MixinNativeImage) (Object) dest;
        destMixin.checkAllocated();

        if (this.format != NativeImage.Format.RGBA || dest.format() != NativeImage.Format.RGBA) {
            throw new UnsupportedOperationException("Can only call copyRect with RGBA format");
        }

        if (srcX < 0 || srcY < 0 || srcX + copyWidth > this.width || srcY + copyHeight > this.height
            || destX < 0 || destY < 0 || destX + copyWidth > dest.getWidth() || destY + copyHeight > dest.getHeight()) {
            throw new IllegalArgumentException("Coordinates outside of image bounds");
        }

        if (copyWidth <= 0 || copyHeight <= 0) {
            return;
        }

        long srcBase = this.pixels;
        long destBase = destMixin.pixels;
        int srcW = this.width;
        int destW = dest.getWidth();

        if (!flipX && !flipY) {
            if (srcW == destW && copyWidth == srcW && srcX == 0 && destX == 0) {
                long srcAddress = srcBase + ((long) srcY * srcW) * 4L;
                long destAddress = destBase + ((long) destY * destW) * 4L;
                MemoryUtil.memCopy(srcAddress, destAddress, (long) copyWidth * copyHeight * 4L);
            } else {
                long bytesPerRow = (long) copyWidth * 4L;
                for (int row = 0; row < copyHeight; row++) {
                    long srcRowAddress = srcBase + (((long) (srcY + row) * srcW) + srcX) * 4L;
                    long destRowAddress = destBase + (((long) (destY + row) * destW) + destX) * 4L;
                    MemoryUtil.memCopy(srcRowAddress, destRowAddress, bytesPerRow);
                }
            }
        } else {
            for (int y = 0; y < copyHeight; y++) {
                int srcOffsetY = srcY + y;
                int dstOffsetY = destY + (flipY ? copyHeight - 1 - y : y);
                long srcRowBase = srcBase + (((long) srcOffsetY * srcW) + srcX) * 4L;
                long dstRowBase = destBase + (((long) dstOffsetY * destW) + destX) * 4L;

                for (int x = 0; x < copyWidth; x++) {
                    int dstOffsetX = flipX ? copyWidth - 1 - x : x;
                    int pixel = MemoryUtil.memGetInt(srcRowBase + ((long) x * 4L));
                    MemoryUtil.memPutInt(dstRowBase + ((long) dstOffsetX * 4L), pixel);
                }
            }
        }
    }

}
//?}
//? if <1.21.4 {
/*package lomka.starl.mixins.com.mojang.blaze3d.platform;

import com.mojang.blaze3d.platform.NativeImage;
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

        if (this.format != NativeImage.Format.RGBA) {
            throw new IllegalArgumentException("fillRect only works on RGBA images; have " + this.format);
        }

        int startX = Math.max(0, x);
        int startY = Math.max(0, y);
        int endX = Math.min(this.width, x + width);
        int endY = Math.min(this.height, y + height);

        if (startX >= endX || startY >= endY) {
            return;
        }

        int rowWidth = endX - startX;
        long basePixels = this.pixels;
        int imgWidth = this.width;

        if (startX == 0 && endX == imgWidth) {
            long startAddress = basePixels + ((long) startY * imgWidth) * 4L;
            long endAddress = startAddress + ((long) (endY - startY) * imgWidth) * 4L;
            for (long addr = startAddress; addr < endAddress; addr += 4L) {
                MemoryUtil.memPutInt(addr, color);
            }
        } else {
            long rowBytes = (long) rowWidth * 4L;
            for (int currentY = startY; currentY < endY; currentY++) {
                long rowStart = basePixels + ((long) currentY * imgWidth + startX) * 4L;
                long rowEnd = rowStart + rowBytes;
                for (long addr = rowStart; addr < rowEnd; addr += 4L) {
                    MemoryUtil.memPutInt(addr, color);
                }
            }
        }
    }

    @Overwrite
    public void copyRect(NativeImage dest, int srcX, int srcY, int destX, int destY, int copyWidth, int copyHeight, boolean flipX, boolean flipY) {
        this.checkAllocated();

        MixinNativeImage destMixin = (MixinNativeImage) (Object) dest;
        destMixin.checkAllocated();

        if (this.format != NativeImage.Format.RGBA || dest.format() != NativeImage.Format.RGBA) {
            throw new UnsupportedOperationException("Can only call copyRect with RGBA format");
        }

        if (srcX < 0 || srcY < 0 || srcX + copyWidth > this.width || srcY + copyHeight > this.height
            || destX < 0 || destY < 0 || destX + copyWidth > dest.getWidth() || destY + copyHeight > dest.getHeight()) {
            throw new IllegalArgumentException("Coordinates outside of image bounds");
        }

        if (copyWidth <= 0 || copyHeight <= 0) {
            return;
        }

        long srcBase = this.pixels;
        long destBase = destMixin.pixels;
        int srcW = this.width;
        int destW = dest.getWidth();

        if (!flipX && !flipY) {
            if (srcW == destW && copyWidth == srcW && srcX == 0 && destX == 0) {
                long srcAddress = srcBase + ((long) srcY * srcW) * 4L;
                long destAddress = destBase + ((long) destY * destW) * 4L;
                MemoryUtil.memCopy(srcAddress, destAddress, (long) copyWidth * copyHeight * 4L);
            } else {
                long bytesPerRow = (long) copyWidth * 4L;
                for (int row = 0; row < copyHeight; row++) {
                    long srcRowAddress = srcBase + (((long) (srcY + row) * srcW) + srcX) * 4L;
                    long destRowAddress = destBase + (((long) (destY + row) * destW) + destX) * 4L;
                    MemoryUtil.memCopy(srcRowAddress, destRowAddress, bytesPerRow);
                }
            }
        } else {
            for (int y = 0; y < copyHeight; y++) {
                int srcOffsetY = srcY + y;
                int dstOffsetY = destY + (flipY ? copyHeight - 1 - y : y);
                long srcRowBase = srcBase + (((long) srcOffsetY * srcW) + srcX) * 4L;
                long dstRowBase = destBase + (((long) dstOffsetY * destW) + destX) * 4L;

                for (int x = 0; x < copyWidth; x++) {
                    int dstOffsetX = flipX ? copyWidth - 1 - x : x;
                    int pixel = MemoryUtil.memGetInt(srcRowBase + ((long) x * 4L));
                    MemoryUtil.memPutInt(dstRowBase + ((long) dstOffsetX * 4L), pixel);
                }
            }
        }
    }
}
*///?}