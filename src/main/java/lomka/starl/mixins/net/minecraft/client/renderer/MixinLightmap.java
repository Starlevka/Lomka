package lomka.starl.mixins.net.minecraft.client.renderer;

//? if >=26.2 {
/*import com.mojang.blaze3d.buffers.GpuBufferSlice;
import java.util.Optional;
*///?} else {
import com.mojang.blaze3d.buffers.GpuBuffer;
import java.util.OptionalInt;
//?}
import com.mojang.blaze3d.systems.CommandEncoder;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.GpuTextureView;
import net.minecraft.client.renderer.Lightmap;
import net.minecraft.client.renderer.MappableRingBuffer;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.state.LightmapRenderState;
import org.joml.Vector3f;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

import java.nio.ByteBuffer;
import java.util.function.Supplier;

@Mixin(value = Lightmap.class, priority = 500)
public abstract class MixinLightmap {

    private static final Supplier<String> RENDER_PASS_LABEL = () -> "Update light";

    @Shadow @Final private MappableRingBuffer ubo;
    @Shadow @Final private GpuTextureView textureView;

    /**
     * @author Lomka
     * @reason Zero-allocation direct UBO writing for Lightmap update
     */
    @Overwrite
    public void render(final LightmapRenderState renderState) {
        if (!renderState.needsUpdate) {
            return;
        }

        CommandEncoder commandEncoder = RenderSystem.getDevice().createCommandEncoder();

        //? if >=26.2 {
        /*GpuBufferSlice.MappedView view = this.ubo.currentBuffer().map(false, true);
        *///?} else {
        GpuBuffer.MappedView view = commandEncoder.mapBuffer(this.ubo.currentBuffer(), false, true);
        //?}

        try {
            ByteBuffer buffer = view.data();

            buffer.putFloat(0, renderState.skyFactor);
            buffer.putFloat(4, renderState.blockFactor);
            buffer.putFloat(8, renderState.nightVisionEffectIntensity);
            buffer.putFloat(12, renderState.darknessEffectScale);
            buffer.putFloat(16, renderState.bossOverlayWorldDarkening);
            buffer.putFloat(20, renderState.brightness);

            buffer.putFloat(32, renderState.blockLightTint.x());
            buffer.putFloat(36, renderState.blockLightTint.y());
            buffer.putFloat(40, renderState.blockLightTint.z());

            buffer.putFloat(48, renderState.skyLightColor.x());
            buffer.putFloat(52, renderState.skyLightColor.y());
            buffer.putFloat(56, renderState.skyLightColor.z());

            buffer.putFloat(64, renderState.ambientColor.x());
            buffer.putFloat(68, renderState.ambientColor.y());
            buffer.putFloat(72, renderState.ambientColor.z());

            buffer.putFloat(80, renderState.nightVisionColor.x());
            buffer.putFloat(84, renderState.nightVisionColor.y());
            buffer.putFloat(88, renderState.nightVisionColor.z());
        } finally {
            if (view != null) {
                view.close();
            }
        }

        //? if >=26.2 {
        /*RenderPass renderPass = commandEncoder.createRenderPass(
                RENDER_PASS_LABEL,
                this.textureView,
                Optional.empty()
        );
        *///?} else {
        RenderPass renderPass = commandEncoder.createRenderPass(
                RENDER_PASS_LABEL,
                this.textureView,
                OptionalInt.empty()
        );
        //?}

        try {
            renderPass.setPipeline(RenderPipelines.LIGHTMAP);
            RenderSystem.bindDefaultUniforms(renderPass);
            renderPass.setUniform("LightmapInfo", this.ubo.currentBuffer());

            //? if >=26.2 {
            /*renderPass.draw(3, 1, 0, 0);
            *///?} else {
            renderPass.draw(0, 3);
            //?}
        } finally {
            if (renderPass != null) {
                renderPass.close();
            }
        }

        this.ubo.rotate();
    }
}