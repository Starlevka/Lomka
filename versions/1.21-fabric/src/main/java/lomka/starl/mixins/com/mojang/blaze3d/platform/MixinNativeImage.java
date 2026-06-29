package lomka.starl.mixins.com.mojang.blaze3d.platform;

import com.mojang.blaze3d.platform.NativeImage;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(NativeImage.class)
public abstract class MixinNativeImage {

    @Shadow private int width;
    @Shadow private int height;
    @Shadow private long pixels;

    @Shadow protected abstract void checkAllocated();
    @Shadow public abstract int getPixelRGBA(int i, int j);
    @Shadow public abstract void setPixelRGBA(int i, int j, int k);

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
}
