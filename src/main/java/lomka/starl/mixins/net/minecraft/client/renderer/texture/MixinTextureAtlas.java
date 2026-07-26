package lomka.starl.mixins.net.minecraft.client.renderer.texture;

//? if >=1.21.6 {
import com.mojang.blaze3d.systems.CommandEncoder;
import com.mojang.blaze3d.systems.GpuDevice;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.GpuTexture;
import com.mojang.blaze3d.textures.GpuTextureView;
//? }
import lomka.starl.utils.SpriteContentsHelper;
import net.minecraft.client.renderer.texture.SpriteContents;
//? if < 1.21.11 {
/*import net.minecraft.resources.ResourceLocation;*/
//? } else {
import net.minecraft.resources.Identifier;
//? }
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
//? if < 1.21.6 {
/*import net.minecraft.client.renderer.texture.DynamicTexture;*/
//? }
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;
import java.util.OptionalInt;
import java.util.function.Supplier;

@Mixin(value = TextureAtlas.class, priority = 500)
public abstract class MixinTextureAtlas {

    //? if < 1.21.6 {
    /*@Shadow private List<TextureAtlasSprite.Ticker> animatedTextures;
    @Shadow @Nullable private DynamicTexture texture;

    @Overwrite
    public void cycleAnimationFrames() {
        DynamicTexture tex = this.texture;
        if (tex != null) {
            List<TextureAtlasSprite.Ticker> list = this.animatedTextures;
            for (int i = 0, n = list.size(); i < n; i++) {
                list.get(i).tickAndUpload(tex);
            }
        }
    }*/
    //? }

    //? if >=1.21.6 {
    //? if < 1.21.11 {
    //? } else {
    @Shadow private List<SpriteContents.AnimationState> animatedTexturesStates;
    @Shadow private GpuTextureView[] mipViews;
    @Shadow private int maxMipLevel;
    @Shadow private List<TextureAtlasSprite> sprites;
    @Shadow private Identifier location;

    @Unique private GpuDevice lomka$device;
    @Unique private @Nullable Supplier<String> lomka$animateLabelSupplier;

    @Inject(method = "<init>", at = @At("RETURN"))
    private void lomka$initDevice(Identifier location, CallbackInfo ci) {
        this.lomka$device = RenderSystem.getDevice();
    }

    @Overwrite
    public void cycleAnimationFrames() {
        List<SpriteContents.AnimationState> states = this.animatedTexturesStates;
        for (int i = 0, n = states.size(); i < n; i++) {
            states.get(i).tick();
        }
        this.uploadAnimationFrames();
    }

    @Overwrite
    private void uploadAnimationFrames() {
        List<SpriteContents.AnimationState> states = this.animatedTexturesStates;
        int count = states.size();

        boolean needsDraw = false;
        for (int i = 0; i < count; i++) {
            if (states.get(i).needsToDraw()) {
                needsDraw = true;
                break;
            }
        }
        if (!needsDraw) return;

        Supplier<String> labelSupplier = this.lomka$animateLabelSupplier;
        if (labelSupplier == null) {
            String passLabel = "Animate " + this.location;
            labelSupplier = () -> passLabel;
            this.lomka$animateLabelSupplier = labelSupplier;
        }

        CommandEncoder encoder = this.lomka$device.createCommandEncoder();

        for (int mip = 0; mip <= this.maxMipLevel; mip++) {
            //? if >=26.2 {
            /*RenderPass renderpass = encoder.createRenderPass(labelSupplier, this.mipViews[mip], java.util.Optional.empty());*/
            //? } else {
            RenderPass renderpass = encoder.createRenderPass(labelSupplier, this.mipViews[mip], OptionalInt.empty());
            //? }

            try {
                //? if >=26.2 {
                RenderSystem.bindDefaultUniforms(renderpass);
                //?}
                for (int i = 0; i < count; i++) {
                    SpriteContents.AnimationState state = states.get(i);
                    if (state.needsToDraw()) {
                        state.drawToAtlas(renderpass, state.getDrawUbo(mip));
                    }
                }
            } catch (Throwable t) {
                try { renderpass.close(); } catch (Throwable t2) { t.addSuppressed(t2); }
                throw t;
            }
            renderpass.close();
        }
    }

    @Inject(at = @At("RETURN"), method = "uploadInitialContents")
    private void lomka$releaseMipmaps(CallbackInfo ci) {
        for (int i = 0, n = this.sprites.size(); i < n; i++) {
            SpriteContents contents = this.sprites.get(i).contents();
            ((SpriteContentsHelper) contents).lomka$releaseUselessMipmaps();
        }
    }
    //? }
    //? }
}
