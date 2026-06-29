//? if >=1.21.11 {
package lomka.starl.mixins.net.minecraft.client.gui.render;

import com.mojang.blaze3d.textures.GpuSampler;
import com.mojang.blaze3d.textures.GpuTextureView;
import net.minecraft.client.gui.render.TextureSetup;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

import java.util.Objects;

@Mixin(TextureSetup.class)
public abstract class MixinTextureSetup {

    @Shadow @Final private @Nullable GpuTextureView texure0;
    @Shadow @Final private @Nullable GpuTextureView texure1;
    @Shadow @Final private @Nullable GpuTextureView texure2;
    @Shadow @Final private @Nullable GpuSampler sampler0;
    @Shadow @Final private @Nullable GpuSampler sampler1;
    @Shadow @Final private @Nullable GpuSampler sampler2;

    /**
     * @author Starlev
     * @reason Bypasses Java Record's slow default invokedynamic hashCode calculation
     * with an allocation-free manual calculation.
     */
    @Overwrite
    public int hashCode() {
        int result = this.texure0 != null ? this.texure0.hashCode() : 0;
        result = 31 * result + (this.texure1 != null ? this.texure1.hashCode() : 0);
        result = 31 * result + (this.texure2 != null ? this.texure2.hashCode() : 0);
        result = 31 * result + (this.sampler0 != null ? this.sampler0.hashCode() : 0);
        result = 31 * result + (this.sampler1 != null ? this.sampler1.hashCode() : 0);
        result = 31 * result + (this.sampler2 != null ? this.sampler2.hashCode() : 0);
        return result;
    }

    /**
     * @author Starlev
     * @reason Bypasses record's invokedynamic equals() using fast direct field comparisons
     * and native record accessors.
     */
    @Overwrite
    public boolean equals(Object object) {
        if (this == object) {
            return true;
        } 
        
        if (!(object instanceof TextureSetup other)) {
            return false;
        } 

        return Objects.equals(this.texure0, other.texure0())
            && Objects.equals(this.texure1, other.texure1())
            && Objects.equals(this.texure2, other.texure2())
            && Objects.equals(this.sampler0, other.sampler0())
            && Objects.equals(this.sampler1, other.sampler1())
            && Objects.equals(this.sampler2, other.sampler2());
    }
}
//?}