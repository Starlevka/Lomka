package lomka.starl.mixins.net.minecraft.client.renderer.texture;

import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.renderer.texture.SpriteContents;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import lomka.starl.utils.SpriteContentsHelper;

@Mixin(SpriteContents.class)
public class MixinSpriteContents implements SpriteContentsHelper {

    @Shadow NativeImage[] byMipLevel;

    @Unique
    @Override
    public void lomka$releaseUselessMipmaps() {
        NativeImage[] mipmaps = this.byMipLevel;
        if (mipmaps != null && mipmaps.length > 1) {
            for (int i = 1; i < mipmaps.length; i++) {
                NativeImage nativeImage = mipmaps[i];
                if (nativeImage != null) {
                    nativeImage.close();
                    mipmaps[i] = null;
                }
            }
        }
    }

    /**
     * @author Starlev
     * @reason Overwrite to support null-safe closing of released mipmap images,
     *         eliminating the need to re-allocate resized arrays.
     */
    @Overwrite
    public void close() {
        NativeImage[] mipmaps = this.byMipLevel;
        if (mipmaps != null) {
            for (int i = 0; i < mipmaps.length; i++) {
                NativeImage nativeImage = mipmaps[i];
                if (nativeImage != null) {
                    nativeImage.close();
                }
            }
        }
    }
}