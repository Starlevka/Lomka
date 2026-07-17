package lomka.starl.mixins.net.minecraft.client.renderer.texture;

import com.mojang.blaze3d.systems.CommandEncoder;
import com.mojang.blaze3d.systems.GpuDevice;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.GpuTextureView;
import lomka.starl.utils.SpriteContentsHelper;
import net.minecraft.client.renderer.texture.SpriteContents;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.resources.Identifier;
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

    @Shadow private List<SpriteContents.AnimationState> animatedTexturesStates;
    @Shadow private GpuTextureView[] mipViews;
    @Shadow private int maxMipLevel;
    @Shadow private Identifier location;
    @Shadow private List<TextureAtlasSprite> sprites;

    @Unique private GpuDevice lomka$device;
    @Unique private @Nullable Supplier<String> lomka$animateLabelSupplier;

    @Inject(method = "<init>", at = @At("RETURN"))
    private void lomka$initDevice(Identifier location, CallbackInfo ci) {
        this.lomka$device = RenderSystem.getDevice();
    }

    /**
     * @author Starlev
     * @reason Replaces the per-tick Iterator allocation with a zero-allocation indexed loop.
     */
    @Overwrite
    public void cycleAnimationFrames() {
        List<SpriteContents.AnimationState> states = this.animatedTexturesStates;
        for (int i = 0, n = states.size(); i < n; i++) {
            states.get(i).tick();
        }
        this.uploadAnimationFrames();
    }

    /**
     * @author Starlev
     * @reason Eliminates Stream, Iterator, string and lambda allocations on every tick.
     *         Reuses a single CommandEncoder instance across all mip-level render passes.
     */
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
}