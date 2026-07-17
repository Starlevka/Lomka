//? if >=26.1 {
// package lomka.starl.mixins.accessor;

// import net.minecraft.client.resources.model.geometry.BakedQuad;
// import net.minecraft.client.resources.model.geometry.QuadCollection;
// import org.spongepowered.asm.mixin.Mixin;
// import org.spongepowered.asm.mixin.gen.Accessor;

// import java.util.List;

// @Mixin(QuadCollection.class)
// public interface AccessorQuadCollection {
//     @Accessor("unculled")
//     List<BakedQuad> lomka$getUnculled();

//     @Accessor("up")
//     List<BakedQuad> lomka$getUp();

//     @Accessor("down")
//     List<BakedQuad> lomka$getDown();

//     @Accessor("north")
//     List<BakedQuad> lomka$getNorth();

//     @Accessor("south")
//     List<BakedQuad> lomka$getSouth();

//     @Accessor("east")
//     List<BakedQuad> lomka$getEast();

//     @Accessor("west")
//     List<BakedQuad> lomka$getWest();
// }
//?}