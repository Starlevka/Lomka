package set.starl.injection.mixins.gl;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.HashMap;
import java.util.Set;

@Mixin(targets = "com.mojang.blaze3d.opengl.GlRenderPass", remap = false)
public class GlRenderPassReuseMixin {

   @Shadow
   @Final
   protected HashMap uniforms;

   @Shadow
   @Final
   protected HashMap samplers;

   @Shadow
   @Final
   protected Set dirtyUniforms;

   /**
      * Clear collections after close() finishes its cleanup.
      * GlRenderPass is created per render pass — clearing on close
      * helps the GC collect the backing arrays sooner, reducing
      * memory pressure during rapid render pass cycling.
      *
      * Injected at TAIL to ensure original close() logic
      * (pushedDebugGroups check, closed=true, finishRenderPass) completes first.
      */
   @Inject(method = "close", at = @At("TAIL"))
   private void lomka$clearCollections(CallbackInfo ci) {
         this.uniforms.clear();
         this.samplers.clear();
         this.dirtyUniforms.clear();
   }
}