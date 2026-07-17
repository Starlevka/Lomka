package lomka.starl.mixins.net.minecraft.client.resources.model;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ImmutableList;
import lomka.starl.mixins.accessor.InvokerBuilder;
//? if >=26.1 {
/*import net.minecraft.client.resources.model.geometry.BakedQuad;
import net.minecraft.client.resources.model.geometry.QuadCollection;*/
//?} else {
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.resources.model.QuadCollection;
//?}
import net.minecraft.core.Direction;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.ArrayList;
import java.util.List;

@Mixin(QuadCollection.Builder.class)
public class MixinBuilder {

    @Shadow @Final private ImmutableList.Builder<BakedQuad> unculledFaces;

    @Unique
    private List<BakedQuad>[] lomka$fastCulledFaces;

    /**
     * @author Starlev
     * @reason culledFaces is never read after addCulledFace/build are overwritten below —
     * skip the eager HashMap allocation Guava performs inside ArrayListMultimap.create()
     * on every Builder construction, which otherwise happens unconditionally regardless
     * of the array-based fast path replacing it.
     */
    @Redirect(
        method = "<init>()V",
        at = @At(value = "INVOKE", target = "Lcom/google/common/collect/ArrayListMultimap;create()Lcom/google/common/collect/ArrayListMultimap;")
    )
    private ArrayListMultimap<Direction, BakedQuad> lomka$skipDeadMultimapAlloc() {
        return null;
    }

    /**
     * @author Starlev
     * @reason Replaces slow Guava ArrayListMultimap with a fast array-based bucket list.
     * Drastically reduces garbage allocation and map lookups during block model baking.
     */
    @SuppressWarnings("unchecked")
    @Overwrite
    public QuadCollection.Builder addCulledFace(Direction direction, BakedQuad bakedquad) {
        if (this.lomka$fastCulledFaces == null) {
            this.lomka$fastCulledFaces = new List[6];
        }

        int idx = direction.get3DDataValue();
        if (this.lomka$fastCulledFaces[idx] == null) {
            this.lomka$fastCulledFaces[idx] = new ArrayList<>(4);
        }

        this.lomka$fastCulledFaces[idx].add(bakedquad);
        return (QuadCollection.Builder) (Object) this;
    }

    /**
     * @author Starlev
     * @reason Assembling the QuadCollection using the fast array instead of iterating
     * over Multimap collections. Order strictly matches Mojang's switch format.
     * Empty-culled branch mirrors vanilla's direct constructor call (unculled reused
     * as both 'all' and 'unculled' by reference) instead of going through
     * createFromSublists' subList(0, size) path, which happens to alias on current
     * Guava internals but isn't a documented List.subList() guarantee.
     */
    @Overwrite
    public QuadCollection build() {
        ImmutableList<BakedQuad> unculled = this.unculledFaces.build();

        if (this.lomka$fastCulledFaces == null) {
            return unculled.isEmpty() ? QuadCollection.EMPTY
                : InvokerBuilder.invokeCreateFromSublists(unculled, unculled.size(), 0, 0, 0, 0, 0, 0);
        }

        ImmutableList.Builder<BakedQuad> allQuads = ImmutableList.builder();
        allQuads.addAll(unculled);

        List<BakedQuad> north = lomka$getList(Direction.NORTH);
        allQuads.addAll(north);

        List<BakedQuad> south = lomka$getList(Direction.SOUTH);
        allQuads.addAll(south);

        List<BakedQuad> east = lomka$getList(Direction.EAST);
        allQuads.addAll(east);

        List<BakedQuad> west = lomka$getList(Direction.WEST);
        allQuads.addAll(west);

        List<BakedQuad> up = lomka$getList(Direction.UP);
        allQuads.addAll(up);

        List<BakedQuad> down = lomka$getList(Direction.DOWN);
        allQuads.addAll(down);

        return InvokerBuilder.invokeCreateFromSublists(
            allQuads.build(),
            unculled.size(),
            north.size(),
            south.size(),
            east.size(),
            west.size(),
            up.size(),
            down.size()
        );
    }

    @Inject(method = "addAll", at = @At("HEAD"), cancellable = true, require = 0)
    private void lomka$addAll(QuadCollection quadCollection, CallbackInfoReturnable<QuadCollection.Builder> cir) {
        for (BakedQuad quad : quadCollection.getQuads(null)) {
            this.unculledFaces.add(quad);
        }
        for (Direction dir : Direction.values()) {
            for (BakedQuad quad : quadCollection.getQuads(dir)) {
                this.addCulledFace(dir, quad);
            }
        }
        cir.setReturnValue((QuadCollection.Builder) (Object) this);
    }

    @Unique
    private List<BakedQuad> lomka$getList(Direction dir) {
        List<BakedQuad> list = this.lomka$fastCulledFaces[dir.get3DDataValue()];
        return list != null ? list : List.of();
    }
}