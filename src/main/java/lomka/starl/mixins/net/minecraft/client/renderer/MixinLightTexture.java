package lomka.starl.mixins.net.minecraft.client.renderer;

//? if >=1.21.6 {
import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.buffers.Std140Builder;
import com.mojang.blaze3d.systems.CommandEncoder;
import com.mojang.blaze3d.systems.RenderPass;
import net.minecraft.client.renderer.RenderPipelines;
import com.mojang.blaze3d.textures.GpuTextureView;
import net.minecraft.client.renderer.MappableRingBuffer;
//? }
//? if < 1.21.6 {
/*import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.renderer.texture.DynamicTexture;*/
//? }
import com.mojang.blaze3d.systems.RenderSystem;
import java.util.OptionalInt;
import java.util.function.Supplier;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.LightTexture;
//? if >=1.21.6 {
import net.minecraft.util.profiling.Profiler;
//?}
import net.minecraft.util.profiling.ProfilerFiller;
//? if >=1.21.11 {
import net.minecraft.world.attribute.EnvironmentAttributes;
import net.minecraft.client.renderer.EndFlashState;
//?}
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import org.joml.Vector3f;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;

@Mixin(value = LightTexture.class, priority = 500)
public abstract class MixinLightTexture {

    //? if < 1.21.6 {
    /*@Shadow @Final private Minecraft minecraft;
    @Shadow @Final private GameRenderer renderer;
    @Shadow @Final private DynamicTexture lightTexture;
    @Shadow @Final private NativeImage lightPixels;
    @Shadow private boolean updateLightTexture;
    @Shadow private float blockLightRedFlicker;

    @Shadow
    private float calculateDarknessScale(LivingEntity entity, float factor, float partialTick) {
        throw new AssertionError();
    }

    @Shadow
    protected abstract float getDarknessGamma(float partialTick);

    @Unique
    private final float[] skyLight = new float[16];
    @Unique
    private final float[] blockR = new float[16];
    @Unique
    private final float[] blockG = new float[16];
    @Unique
    private final float[] blockB = new float[16];

    @Overwrite
    public void updateLightTexture(float partialTick) {
        if (!this.updateLightTexture) {
            return;
        }

        this.updateLightTexture = false;
        this.minecraft.getProfiler().push("lightTex");
        ClientLevel clientLevel = this.minecraft.level;

        if (clientLevel != null) {
            float skyDarken = clientLevel.getSkyDarken(1.0F);
            float skyFactor = clientLevel.getSkyFlashTime() > 0 ? 1.0F : skyDarken * 0.95F + 0.05F;

            float darknessScale = ((Double) this.minecraft.options.darknessEffectScale().get()).floatValue();
            float darknessGamma = this.getDarknessGamma(partialTick) * darknessScale;
            float darknessScaleVal = this.calculateDarknessScale(this.minecraft.player, darknessGamma, partialTick) * darknessScale;

            float waterVision = this.minecraft.player.getWaterVision();
            float nightVisionScale;

            if (this.minecraft.player.hasEffect(MobEffects.NIGHT_VISION)) {
                nightVisionScale = GameRenderer.getNightVisionScale(this.minecraft.player, partialTick);
            } else if (waterVision > 0.0F && this.minecraft.player.hasEffect(MobEffects.CONDUIT_POWER)) {
                nightVisionScale = waterVision;
            } else {
                nightVisionScale = 0.0F;
            }

            float skyR = skyDarken * 0.65F + 0.35F;
            float skyG = skyR;
            float skyB = 1.0F;

            float flickerModifier = this.blockLightRedFlicker + 1.5F;
            boolean forceBright = clientLevel.effects().forceBrightLightmap();
            float darkenWorld = this.renderer.getDarkenWorldAmount(partialTick);
            float gamma = ((Double) this.minecraft.options.gamma().get()).floatValue();
            float gammaFactor = Math.max(0.0F, gamma - darknessGamma);

            var dimensionType = clientLevel.dimensionType();

            for (int i = 0; i < 16; ++i) {
                this.skyLight[i] = LightTexture.getBrightness(dimensionType, i) * skyFactor;

                float brightness = LightTexture.getBrightness(dimensionType, i) * flickerModifier;
                this.blockR[i] = brightness;
                this.blockG[i] = brightness * ((brightness * 0.6F + 0.4F) * 0.6F + 0.4F);
                this.blockB[i] = brightness * (brightness * brightness * 0.6F + 0.4F);
            }

            for (int i = 0; i < 16; ++i) {
                float currentSkyLight = this.skyLight[i];
                float skyValR = skyR * currentSkyLight;
                float skyValG = skyG * currentSkyLight;
                float skyValB = skyB * currentSkyLight;

                for (int j = 0; j < 16; ++j) {
                    float r = this.blockR[j];
                    float g = this.blockG[j];
                    float b = this.blockB[j];

                    if (forceBright) {
                        r += (0.99F - r) * 0.25F;
                        g += (1.12F - g) * 0.25F;
                        b += (1.00F - b) * 0.25F;

                        r = r < 0.0F ? 0.0F : (r > 1.0F ? 1.0F : r);
                        g = g < 0.0F ? 0.0F : (g > 1.0F ? 1.0F : g);
                        b = b < 0.0F ? 0.0F : (b > 1.0F ? 1.0F : b);
                    } else {
                        r += skyValR;
                        g += skyValG;
                        b += skyValB;

                        r += (0.75F - r) * 0.04F;
                        g += (0.75F - g) * 0.04F;
                        b += (0.75F - b) * 0.04F;

                        if (darkenWorld > 0.0F) {
                            r += (r * 0.7F - r) * darkenWorld;
                            g += (g * 0.6F - g) * darkenWorld;
                            b += (b * 0.6F - b) * darkenWorld;
                        }
                    }

                    if (nightVisionScale > 0.0F) {
                        float maxComp = r > g ? (r > b ? r : b) : (g > b ? g : b);
                        if (maxComp < 1.0F) {
                            float invMax = 1.0F / maxComp;
                            r += (r * invMax - r) * nightVisionScale;
                            g += (g * invMax - g) * nightVisionScale;
                            b += (b * invMax - b) * nightVisionScale;
                        }
                    }

                    if (!forceBright) {
                        if (darknessScaleVal > 0.0F) {
                            r -= darknessScaleVal;
                            g -= darknessScaleVal;
                            b -= darknessScaleVal;
                        }
                        r = r < 0.0F ? 0.0F : (r > 1.0F ? 1.0F : r);
                        g = g < 0.0F ? 0.0F : (g > 1.0F ? 1.0F : g);
                        b = b < 0.0F ? 0.0F : (b > 1.0F ? 1.0F : b);
                    }

                    float invR = 1.0F - r;
                    float invG = 1.0F - g;
                    float invB = 1.0F - b;

                    float notGammaR = 1.0F - invR * invR * invR * invR;
                    float notGammaG = 1.0F - invG * invG * invG * invG;
                    float notGammaB = 1.0F - invB * invB * invB * invB;

                    r += (notGammaR - r) * gammaFactor;
                    g += (notGammaG - g) * gammaFactor;
                    b += (notGammaB - b) * gammaFactor;

                    r += (0.75F - r) * 0.04F;
                    g += (0.75F - g) * 0.04F;
                    b += (0.75F - b) * 0.04F;

                    r = r < 0.0F ? 0.0F : (r > 1.0F ? 1.0F : r);
                    g = g < 0.0F ? 0.0F : (g > 1.0F ? 1.0F : g);
                    b = b < 0.0F ? 0.0F : (b > 1.0F ? 1.0F : b);

                    int ir = (int) (r * 255.0F);
                    int ig = (int) (g * 255.0F);
                    int ib = (int) (b * 255.0F);

                    this.lightPixels.setPixelRGBA(j, i, 0xFF000000 | (ib << 16) | (ig << 8) | ir);
                }
            }

            this.lightTexture.upload();
            this.minecraft.getProfiler().pop();
        }
    }*/
    //? } else {
    @Shadow private boolean updateLightTexture;
    @Shadow @Final private Minecraft minecraft;
    @Shadow private float blockLightRedFlicker;
    @Shadow @Final private GameRenderer renderer;
    @Shadow @Final private MappableRingBuffer ubo;
    //? if >=1.21.9 {
    @Shadow @Final private GpuTextureView textureView;
    //?} else {
    /*@Shadow @Final private GpuTextureView textureView;*/
    //?}

    @Shadow
    private float calculateDarknessScale(LivingEntity livingentity, float f, float f1) {
        throw new AssertionError();
    }

    @Unique
    private static final Vector3f LOMKA$FLASH_COLOR = new Vector3f(0.99F, 1.12F, 1.0F);
    @Unique
    private static final Vector3f LOMKA$DEFAULT_COLOR = new Vector3f(1.0F, 1.0F, 1.0F);
    @Unique
    private static final Supplier<String> LOMKA$RENDER_PASS_LABEL = () -> "Update light";

    @Unique
    private final Vector3f lomka$skyLightColorVec = new Vector3f();

    @Overwrite
    public void updateLightTexture(float f) {
        if (!this.updateLightTexture) {
            return;
        }

        ClientLevel clientlevel = this.minecraft.level;
        if (clientlevel == null) {
            return;
        }

        this.updateLightTexture = false;
        ProfilerFiller profilerfiller = Profiler.get();
        profilerfiller.push("lightTex");

        Camera camera = this.minecraft.gameRenderer.getMainCamera();
        LocalPlayer player = this.minecraft.player;

        //? if >=1.21.11 {
        float ambientLight = clientlevel.dimensionType().ambientLight();
        int skyLightColorInt = (Integer) camera.attributeProbe().getValue(EnvironmentAttributes.SKY_LIGHT_COLOR, f);
        float skyLightFactor = (Float) camera.attributeProbe().getValue(EnvironmentAttributes.SKY_LIGHT_FACTOR, f);

        EndFlashState endflashstate = clientlevel.endFlashState();
        Vector3f flashOrDefaultColor;

        if (endflashstate != null) {
            flashOrDefaultColor = LOMKA$FLASH_COLOR;
            if (!(Boolean) this.minecraft.options.hideLightningFlash().get()) {
                float intensity = endflashstate.getIntensity(f);
                if (this.minecraft.gui.getBossOverlay().shouldCreateWorldFog()) {
                    skyLightFactor += intensity / 3.0F;
                } else {
                    skyLightFactor += intensity;
                }
            }
        } else {
            flashOrDefaultColor = LOMKA$DEFAULT_COLOR;
        }

        this.lomka$skyLightColorVec.set(
            (float) (skyLightColorInt >> 16 & 255) / 255.0F,
            (float) (skyLightColorInt >> 8 & 255) / 255.0F,
            (float) (skyLightColorInt & 255) / 255.0F
        );
        //?} else {
        /*float f1 = clientlevel.getSkyDarken(1.0F);
        float skyLightFactor;
        if (clientlevel.getSkyFlashTime() > 0) {
            skyLightFactor = 1.0F;
        } else {
            skyLightFactor = f1 * 0.95F + 0.05F;
        }
        float ambientLight = clientlevel.dimensionType().ambientLight();
        Vector3f flashOrDefaultColor = LOMKA$DEFAULT_COLOR;
        this.lomka$skyLightColorVec.set(f1, f1, 1.0F);
        this.lomka$skyLightColorVec.lerp(LOMKA$DEFAULT_COLOR, 0.35F);*/
        //?}

        float darknessEffectScale = ((Double) this.minecraft.options.darknessEffectScale().get()).floatValue();
        float darknessFactor = player.getEffectBlendFactor(MobEffects.DARKNESS, f) * darknessEffectScale;
        float darknessScale = this.calculateDarknessScale(player, darknessFactor, f) * darknessEffectScale;
        float waterVision = player.getWaterVision();

        float nightVisionScale;
        if (player.hasEffect(MobEffects.NIGHT_VISION)) {
            nightVisionScale = GameRenderer.getNightVisionScale(player, f);
        } else if (waterVision > 0.0F && player.hasEffect(MobEffects.CONDUIT_POWER)) {
            nightVisionScale = waterVision;
        } else {
            nightVisionScale = 0.0F;
        }

        float blockFlicker = this.blockLightRedFlicker + 1.5F;
        float gamma = ((Double) this.minecraft.options.gamma().get()).floatValue();
        float effectiveGamma = Math.max(0.0F, gamma - darknessFactor);

        CommandEncoder commandencoder = RenderSystem.getDevice().createCommandEncoder();

        //? if >=1.21.9 {
        try (GpuBuffer.MappedView mappedView = commandencoder.mapBuffer(this.ubo.currentBuffer(), false, true)) {
            Std140Builder.intoBuffer(mappedView.data())
                .putFloat(ambientLight)
                .putFloat(skyLightFactor)
                .putFloat(blockFlicker)
                .putFloat(nightVisionScale)
                .putFloat(darknessScale)
                .putFloat(this.renderer.getDarkenWorldAmount(f))
                .putFloat(effectiveGamma)
                .putVec3(this.lomka$skyLightColorVec)
                .putVec3(flashOrDefaultColor);
        }

        try (RenderPass renderpass = commandencoder.createRenderPass(LOMKA$RENDER_PASS_LABEL, this.textureView, OptionalInt.empty())) {
            renderpass.setPipeline(RenderPipelines.LIGHTMAP);
            RenderSystem.bindDefaultUniforms(renderpass);
            renderpass.setUniform("LightmapInfo", this.ubo.currentBuffer());
            renderpass.draw(0, 3);
        }
        //?} else {
        /*boolean useBright = clientlevel.effects().forceBrightLightmap();
        GpuBuffer.MappedView mappedView = commandencoder.mapBuffer(this.ubo.currentBuffer(), false, true);
        try {
            Std140Builder.intoBuffer(mappedView.data())
                .putFloat(ambientLight)
                .putFloat(skyLightFactor)
                .putFloat(blockFlicker)
                .putInt(useBright ? 1 : 0)
                .putFloat(nightVisionScale)
                .putFloat(darknessScale)
                .putFloat(this.renderer.getDarkenWorldAmount(f))
                .putFloat(effectiveGamma)
                .putVec3(this.lomka$skyLightColorVec);
        } finally {
            mappedView.close();
        }

        RenderSystem.AutoStorageIndexBuffer indexBuffer = RenderSystem.getSequentialBuffer(com.mojang.blaze3d.vertex.VertexFormat.Mode.QUADS);
        GpuBuffer indexGpuBuffer = indexBuffer.getBuffer(6);
        try (RenderPass renderpass = commandencoder.createRenderPass(LOMKA$RENDER_PASS_LABEL, this.textureView, OptionalInt.empty())) {
            renderpass.setPipeline(RenderPipelines.LIGHTMAP);
            RenderSystem.bindDefaultUniforms(renderpass);
            renderpass.setUniform("LightmapInfo", this.ubo.currentBuffer());
            renderpass.setVertexBuffer(0, RenderSystem.getQuadVertexBuffer());
            renderpass.setIndexBuffer(indexGpuBuffer, indexBuffer.type());
            renderpass.drawIndexed(0, 0, 6, 1);
        }*/
        //?}

        this.ubo.rotate();
        profilerfiller.pop();
    }
    //? }
}
