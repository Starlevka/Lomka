package lomka.starl.mixins.net.minecraft.resources;

//? if >=1.21.11 {
import net.minecraft.resources.Identifier;
//?} else {
/*import net.minecraft.resources.ResourceLocation;*/
//?}
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;

@Mixin(
    //? if >=1.21.11 {
    Identifier.class
    //?} else {
    /*ResourceLocation.class*/
    //?}
)
public class MixinResource {

    @Shadow @Final private String namespace;
    @Shadow @Final private String path;

    @Unique private String lomka$cachedString;
    @Unique private boolean lomka$hashComputed;
    @Unique private int lomka$hashCode;

    @Overwrite
    public String toString() {
        String s = this.lomka$cachedString;
        if (s == null) {
            this.lomka$cachedString = s = this.namespace + ":" + this.path;
        }
        return s;
    }

    @Overwrite
    public int hashCode() {
        if (!this.lomka$hashComputed) {
            this.lomka$hashCode = 31 * this.namespace.hashCode() + this.path.hashCode();
            this.lomka$hashComputed = true;
        }
        return this.lomka$hashCode;
    }
}