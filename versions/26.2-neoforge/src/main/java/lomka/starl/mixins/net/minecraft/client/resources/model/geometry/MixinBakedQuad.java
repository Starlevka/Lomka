package lomka.starl.mixins.net.minecraft.client.resources.model.geometry;

import net.minecraft.client.resources.model.geometry.BakedQuad;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

@Mixin(BakedQuad.class)
public class MixinBakedQuad {

    /**
     * @author Starlev
     * @reason The default Java record hashCode() uses invokedynamic and bootstrap methods,
     * which introduces a slight overhead. Overwriting this with a manually unrolled,
     * completely allocation-free hash calculation avoids object boxing and provides
     * the best performance for chunk building and model rendering.
     */
    @Overwrite
    public int hashCode() {
        BakedQuad self = (BakedQuad) (Object) this;

        int result = self.position0().hashCode();
        result = 31 * result + self.position1().hashCode();
        result = 31 * result + self.position2().hashCode();
        result = 31 * result + self.position3().hashCode();

        long uv0 = self.packedUV0();
            result = 31 * result + (int) (uv0 ^ (uv0 >>> 32));
        long uv1 = self.packedUV1();
            result = 31 * result + (int) (uv1 ^ (uv1 >>> 32));
        long uv2 = self.packedUV2();
            result = 31 * result + (int) (uv2 ^ (uv2 >>> 32));
        long uv3 = self.packedUV3();
            result = 31 * result + (int) (uv3 ^ (uv3 >>> 32));

        result = 31 * result + self.direction().hashCode();
        result = 31 * result + self.materialInfo().hashCode();

        return result;
    }
}
