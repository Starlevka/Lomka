package lomka.starl.mixins.com.mojang.blaze3d.opengl;

import com.mojang.blaze3d.opengl.GlProgram;
import com.mojang.blaze3d.opengl.Uniform;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import java.util.Map;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GlProgram.class)
public abstract class MixinGlProgram {

    @Shadow private Map<String, Uniform> uniformsByName;

    /**
     * @author Starlev
     * @reason Swap the vanilla per-program HashMap for a dense fastutil open-addressing map: lower
     *         memory overhead (no per-entry nodes) and faster get() on the per-draw uniform lookup
     *         hot path (GlCommandEncoder). The field is made non-final via AW/AT and replaced after
     *         the initializer runs, so vanilla setupUniforms() populates the fastutil map directly
     *         and getUniform() resolves through it with no copy and no @Overwrite.
     */
    @Inject(method = "<init>", at = @At("RETURN"))
    private void lomka$useFastUtilMap(CallbackInfo ci) {
        this.uniformsByName = new Object2ObjectOpenHashMap<>();
    }
}