package lomka.starl.mixins.net.minecraft.client.renderer;

import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.LightmapRenderStateExtractor;
import net.minecraft.client.renderer.EndFlashState;
import net.minecraft.client.renderer.state.LightmapRenderState;
import net.minecraft.util.ARGB;
import net.minecraft.util.profiling.Profiler;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.attribute.EnvironmentAttributes;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import org.joml.Vector3f;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = LightmapRenderStateExtractor.class, priority = 500)
public abstract class MixinLightmapRenderStateExtractor {

    @Shadow @Final private GameRenderer renderer;
    @Shadow @Final private Minecraft minecraft;
    @Shadow private boolean needsUpdate;
    @Shadow private float blockLightFlicker;

    @Unique private final Vector3f lomka$blockLightTint   = new Vector3f();
    @Unique private final Vector3f lomka$skyLightColor    = new Vector3f();
    @Unique private final Vector3f lomka$ambientColor     = new Vector3f();
    @Unique private final Vector3f lomka$nightVisionColor = new Vector3f();

    /**
     * Short-circuits the vanilla extractor and keeps the lightmap render-state
     * refresh on the custom fast path instead of falling back to the heavier pipeline.
     */
    @Inject(method = "extract", at = @At("HEAD"), cancellable = true)
    private void lomka$extract(LightmapRenderState renderState, float partialTicks, CallbackInfo ci) {
        renderState.needsUpdate = this.needsUpdate;
        if (this.needsUpdate) {
            ClientLevel level = this.minecraft.level;
            LocalPlayer player = this.minecraft.player;

            if (level != null && player != null) {
                ProfilerFiller profiler = Profiler.get();

                profiler.push("lightmap");
                //? if >=26.2 {
                /*Camera camera = this.renderer.mainCamera();
                *///?} else {
                Camera camera = this.renderer.getMainCamera();
                //?}

                renderState.blockFactor = this.blockLightFlicker + 1.4F;

                int blockTint = (Integer) camera.attributeProbe().getValue(EnvironmentAttributes.BLOCK_LIGHT_TINT, partialTicks);
                lomka$setVector3fFromRGB24(this.lomka$blockLightTint, blockTint);
                renderState.blockLightTint = this.lomka$blockLightTint;

                renderState.skyFactor = (Float) camera.attributeProbe().getValue(EnvironmentAttributes.SKY_LIGHT_FACTOR, partialTicks);

                int skyColor = (Integer) camera.attributeProbe().getValue(EnvironmentAttributes.SKY_LIGHT_COLOR, partialTicks);
                lomka$setVector3fFromRGB24(this.lomka$skyLightColor, skyColor);
                renderState.skyLightColor = this.lomka$skyLightColor;

                EndFlashState endFlashState = level.endFlashState();

                if (endFlashState != null && !(Boolean) this.minecraft.options.hideLightningFlash().get()) {
                    float intensity = endFlashState.getIntensity(partialTicks);
                    //? if >=26.2 {
                    /*if (this.minecraft.gui.hud.getBossOverlay().shouldCreateWorldFog()) {
                    *///?} else {
                    if (this.minecraft.gui.getBossOverlay().shouldCreateWorldFog()) {
                    //?}
                        renderState.skyFactor += intensity / 3.0F;
                    } else {
                        renderState.skyFactor += intensity;
                    }
                }

                int ambient = (Integer) camera.attributeProbe().getValue(EnvironmentAttributes.AMBIENT_LIGHT_COLOR, partialTicks);
                lomka$setVector3fFromRGB24(this.lomka$ambientColor, ambient);
                renderState.ambientColor = this.lomka$ambientColor;

                float gamma = ((Double) this.minecraft.options.gamma().get()).floatValue();
                float darknessEffectScaleOption = ((Double) this.minecraft.options.darknessEffectScale().get()).floatValue();
                float darknessEffectBrightnessModifier = player.getEffectBlendFactor(MobEffects.DARKNESS, partialTicks) * darknessEffectScaleOption;

                renderState.brightness = Math.max(0.0F, gamma - darknessEffectBrightnessModifier);
                renderState.darknessEffectScale = this.calculateDarknessScale(player, darknessEffectBrightnessModifier, partialTicks) * darknessEffectScaleOption;

                float waterVision = player.getWaterVision();

                if (player.hasEffect(MobEffects.NIGHT_VISION)) {
                    //? if >=26.2 {
                    /*renderState.nightVisionEffectIntensity = GameRenderer.nightVisionScale(player, partialTicks);
                    *///?} else {
                    renderState.nightVisionEffectIntensity = GameRenderer.getNightVisionScale(player, partialTicks);
                    //?}
                } else if (waterVision > 0.0F && player.hasEffect(MobEffects.CONDUIT_POWER)) {
                    renderState.nightVisionEffectIntensity = waterVision;
                } else {
                    renderState.nightVisionEffectIntensity = 0.0F;
                }

                int nightColor = (Integer) camera.attributeProbe().getValue(EnvironmentAttributes.NIGHT_VISION_COLOR, partialTicks);
                lomka$setVector3fFromRGB24(this.lomka$nightVisionColor, nightColor);
                renderState.nightVisionColor = this.lomka$nightVisionColor;

                //? if >=26.2 {
                /*renderState.bossOverlayWorldDarkening = this.renderer.bossOverlayWorldDarkening(partialTicks);
                *///?} else {
                renderState.bossOverlayWorldDarkening = this.renderer.getBossOverlayWorldDarkening(partialTicks);
                //?}

                profiler.pop();
                this.needsUpdate = false;
                ci.cancel();
            }
        }
    }

    @Shadow
    private float calculateDarknessScale(LivingEntity livingEntity, float f, float f1) {
        throw new AssertionError();
    }

    @Unique
    private static void lomka$setVector3fFromRGB24(Vector3f target, int color) {
        target.set(
            (float) (color >> 16 & 255) / 255.0F,
            (float) (color >> 8 & 255) / 255.0F,
            (float) (color & 255) / 255.0F
        );
    }
}
