package set.starl.injection.mixins.render;

import com.mojang.blaze3d.opengl.GlRenderPass;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.HashMap;
import java.util.Set;

@Mixin(GlRenderPass.class)
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
      * Clear collections instead of letting them be GC'd.
      * GlRenderPass is created per render pass — clearing on close
      * reuses the backing arrays, saving 3 object allocations per pass.
      */
   @Inject(method = "close", at = @At("HEAD"))
   private void lomka$clearCollections(CallbackInfo ci) {
         this.uniforms.clear();
         this.samplers.clear();
         this.dirtyUniforms.clear();
   }
}