package lomka.starl.mixins.net.minecraft.client.renderer.texture;

import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.renderer.texture.SpriteContents;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import lomka.starl.utils.SpriteContentsHelper;

@Mixin(SpriteContents.class)
public class MixinSpriteContents implements SpriteContentsHelper {

    @Shadow NativeImage[] byMipLevel;

    /**
     * Closes all NativeImage mip levels above 0 after the texture upload,
     * freeing GPU-unused heap memory (~33% per mipmapped sprite).
     */
    @Unique
    @Override
    public void lomka$releaseUselessMipmaps() {
        if (this.byMipLevel != null && this.byMipLevel.length > 1) {
            
            for (int i = 1; i < this.byMipLevel.length; i++) {
                if (this.byMipLevel[i] != null) {
                    this.byMipLevel[i].close();
                    this.byMipLevel[i] = null;
                }
            }
            this.byMipLevel = new NativeImage[]{this.byMipLevel[0]};
        }
    }
}