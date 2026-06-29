package lomka.starl.mixins.accessor;

//? if >=26.1 {
/*import net.minecraft.client.resources.model.geometry.BakedQuad;
import net.minecraft.client.resources.model.geometry.QuadCollection;*/
//?} else {
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.resources.model.QuadCollection;
//?}
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;
import java.util.List;

@Mixin(QuadCollection.Builder.class)
public interface InvokerQuadCollectionBuilder {
    @Invoker("createFromSublists")
    static QuadCollection invokeCreateFromSublists(List<BakedQuad> list, int unculled, int north, int south, int east, int west, int up, int down) {
        throw new AssertionError();
    }
}