package lomka.starl.mixins.com.mojang.blaze3d.vertex;

import com.mojang.blaze3d.vertex.VertexFormat;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;

@Mixin(VertexFormat.class)
public abstract class MixinVertexFormat {

    @Shadow @Final private int elementsMask;
    @Shadow @Final private int[] offsetsByElement;

    @Unique private int lomka$hashCode;

    /**
     * @author Starlev
     * @reason Lazy-caches the VertexFormat hash code to avoid looping
     * through the elements array on every call.
     */
    @Overwrite
    public int hashCode() {
        int h = this.lomka$hashCode;
        if (h == 0) {
            int result = this.elementsMask * 31 + java.util.Arrays.hashCode(this.offsetsByElement);
            h = result;
            if (h == 0) h = 1;
            this.lomka$hashCode = h;
        }
        return h;
    }
}