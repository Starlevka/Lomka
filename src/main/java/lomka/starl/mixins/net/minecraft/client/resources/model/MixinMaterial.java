package lomka.starl.mixins.net.minecraft.client.resources.model;

//? if >=26.1 {
/*import net.minecraft.client.resources.model.sprite.Material;
import net.minecraft.resources.Identifier;*/
//?} else if >=1.21.11 {
import net.minecraft.client.resources.model.Material;
import net.minecraft.resources.Identifier;
//?} else {
/*import net.minecraft.client.resources.model.Material;
import net.minecraft.resources.ResourceLocation;*/
//?}
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;

@Mixin(Material.class)
public abstract class MixinMaterial {

    //? if >=26.1 {
    /*@Shadow @Final private Identifier sprite;
    @Shadow @Final private boolean forceTranslucent;*/
    //?} else if >=1.21.11 {
    @Shadow @Final private Identifier atlasLocation;
    @Shadow @Final private Identifier texture;
    //?} else {
    /*@Shadow @Final private ResourceLocation atlasLocation;
    @Shadow @Final private ResourceLocation texture;*/
    //?}

    @Unique private int lomka$hashCode;
    @Unique private boolean lomka$hashCodeCached;

    @Overwrite
    public int hashCode() {
        if (!this.lomka$hashCodeCached) {
            //? if >=26.1 {
            /*int h = 31 + this.sprite.hashCode();
            h = 31 * h + Boolean.hashCode(this.forceTranslucent);*/
            //?} else if >=1.21.11 {
            int h = 31 + this.atlasLocation.hashCode();
            h = 31 * h + this.texture.hashCode();
            //?} else {
            /*int h = 31 + this.atlasLocation.hashCode();
            h = 31 * h + this.texture.hashCode();*/
            //?}
            this.lomka$hashCode = h;
            this.lomka$hashCodeCached = true;
        }
        return this.lomka$hashCode;
    }
}
