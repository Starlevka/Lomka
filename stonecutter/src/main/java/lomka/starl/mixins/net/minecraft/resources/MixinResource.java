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
    @Unique private int lomka$hashCode;

    /**
     * @author Starlev
     * @reason Caches the string representation to avoid repeated namespace:path concatenation.
     */
    @Overwrite
    public String toString() {
        String s = this.lomka$cachedString;
        if (s == null) {
            s = this.namespace + ":" + this.path;
            this.lomka$cachedString = s;
        }
        return s;
    }

    /**
     * @author Starlev
     * @reason Lazily computes and caches the hash code to avoid redundant namespace+path hashing.
     */
    @Overwrite
    public int hashCode() {
        int h = this.lomka$hashCode;
        if (h == 0) {
            int result = 31 * this.namespace.hashCode() + this.path.hashCode();
            h = result;
            if (h == 0) h = 1;
            this.lomka$hashCode = h;
        }
        return h;
    }
}