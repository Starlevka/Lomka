package lomka.starl.mixins.net.minecraft.client.renderer.block.model;

import net.minecraft.client.renderer.block.model.BakedQuad;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

@Mixin(BakedQuad.class)
public class MixinBakedQuad {

    /**
     * @author Starlev
     * @reason Manual hash calculation using directly-accessed record components,
     * avoiding Objects.hash() varargs allocation and boxing overhead.
     */
    @Overwrite
    public int hashCode() {
        BakedQuad self = (BakedQuad) (Object) this;

        int result = 31 + (self.position0() != null ? self.position0().hashCode() : 0);
        result = 31 * result + (self.position1() != null ? self.position1().hashCode() : 0);
        result = 31 * result + (self.position2() != null ? self.position2().hashCode() : 0);
        result = 31 * result + (self.position3() != null ? self.position3().hashCode() : 0);

        long uv0 = self.packedUV0(); result = 31 * result + (int)(uv0 ^ (uv0 >>> 32));
        long uv1 = self.packedUV1(); result = 31 * result + (int)(uv1 ^ (uv1 >>> 32));
        long uv2 = self.packedUV2(); result = 31 * result + (int)(uv2 ^ (uv2 >>> 32));
        long uv3 = self.packedUV3(); result = 31 * result + (int)(uv3 ^ (uv3 >>> 32));

        result = 31 * result + self.tintIndex();
        result = 31 * result + (self.direction() != null ? self.direction().hashCode() : 0);
        result = 31 * result + (self.sprite()    != null ? self.sprite().hashCode()    : 0);
        result = 31 * result + (self.shade() ? 1231 : 1237);
        result = 31 * result + self.lightEmission();

        return result;
    }
}