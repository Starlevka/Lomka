// ignored due to lazily work with that \\

// package lomka.starl.mixins.net.minecraft.util;

// import net.minecraft.core.BlockPos;
// import net.minecraft.util.LightCoordsUtil;
// import net.minecraft.world.level.BlockAndLightGetter;
// import net.minecraft.world.level.LightLayer;
// import net.minecraft.world.level.block.state.BlockState;
// import org.spongepowered.asm.mixin.Mixin;
// import org.spongepowered.asm.mixin.Overwrite;

// @Mixin(LightCoordsUtil.class)
// public class MixinLightCoordsUtil {

//     /**
//      * @author Starlev
//      * @reason Undefined optimization
//      */
//     @Overwrite
//     public static int addSmoothBlockEmission(final int lightCoords, float blockLightEmission) {
//         if (blockLightEmission <= 0.0F) {
//             int currentBlock = lightCoords & 255;
//             if (currentBlock <= 240) {
//                 return lightCoords & 16711935;
//             }
//             return lightCoords & 16711680 | 240;
//         }
//         float clamped = blockLightEmission > 1.0F ? 1.0F : blockLightEmission;
//         int emittedBlock = (int) (clamped * 240.0F);
//         int currentBlock = lightCoords & 255;
//         int block = currentBlock + emittedBlock;
//         if (block > 240) {
//             block = 240;
//         }
//         return lightCoords & 16711680 | block;
//     }

//     /**
//      * @author Starlev
//      * @reason Undefined optimization
//      */
//     @Overwrite
//     public static int max(final int coords1, final int coords2) {
//         if (coords1 == coords2) {
//             return coords1;
//         }
//         int block1 = coords1 >> 4 & 15;
//         int block2 = coords2 >> 4 & 15;
//         int sky1 = coords1 >> 20 & 15;
//         int sky2 = coords2 >> 20 & 15;
//         int block = block1 > block2 ? block1 : block2;
//         int sky = sky1 > sky2 ? sky1 : sky2;
//         return block << 4 | sky << 20;
//     }

//     /**
//      * @author Starlev
//      * @reason Undefined optimization
//      */
//     @Overwrite
//     public static int lightCoordsWithEmission(final int lightCoords, final int emission) {
//         if (emission <= 0) {
//             return lightCoords;
//         }
//         if (emission >= 15) {
//             return LightCoordsUtil.FULL_BRIGHT;
//         }
//         int sky = lightCoords >> 20 & 15;
//         int block = lightCoords >> 4 & 15;
//         if (sky < emission) {
//             sky = emission;
//         }
//         if (block < emission) {
//             block = emission;
//         }
//         return block << 4 | sky << 20;
//     }

//     /**
//      * @author Starlev
//      * @reason Undefined optimization
//      */
//     @Overwrite
//     public static int smoothBlend(int neighbor1, int neighbor2, int neighbor3, final int center) {
//         if ((center & 15728640) > 2097152 || (center & 240) > 32) {
//             if (neighbor1 == 0) {
//                 neighbor1 = center;
//             } else if ((neighbor1 & 15728640) == 0) {
//                 neighbor1 |= center & 16711680;
//             }

//             if (neighbor2 == 0) {
//                 neighbor2 = center;
//             } else if ((neighbor2 & 15728640) == 0) {
//                 neighbor2 |= center & 16711680;
//             }

//             if (neighbor3 == 0) {
//                 neighbor3 = center;
//             } else if ((neighbor3 & 15728640) == 0) {
//                 neighbor3 |= center & 16711680;
//             }
//         }

//         return neighbor1 + neighbor2 + neighbor3 + center >> 2 & 16711935;
//     }

//     /**
//      * @author Starlev
//      * @reason Undefined optimization
//      */
//     @Overwrite
//     public static int smoothWeightedBlend(final int coords1, final int coords2, final int coords3, final int coords4, final float weight1, final float weight2, final float weight3, final float weight4) {
//         int sky = (int) ((coords1 >> 16 & 255) * weight1 + (coords2 >> 16 & 255) * weight2 + (coords3 >> 16 & 255) * weight3 + (coords4 >> 16 & 255) * weight4);
//         int block = (int) ((coords1 & 255) * weight1 + (coords2 & 255) * weight2 + (coords3 & 255) * weight3 + (coords4 & 255) * weight4);

//         return (block & 255) | ((sky & 255) << 16);
//     }

//     /**
//      * @author Starlev
//      * @reason Undefined optimization
//      */
//     @Overwrite
//     public static int getLightCoords(final BlockAndLightGetter level, final BlockPos pos) {
//         BlockState state = level.getBlockState(pos);
//         if (state.emissiveRendering()) {
//             return LightCoordsUtil.FULL_BRIGHT;
//         }
//         int sky = level.getBrightness(LightLayer.SKY, pos);
//         int block = level.getBrightness(LightLayer.BLOCK, pos);
//         int packedBrightness = block << 4 | sky << 20;
//         int blockSelfEmission = state.getLightEmission();
//         if (blockSelfEmission > 0 && block < blockSelfEmission) {
//             return packedBrightness & 16711680 | blockSelfEmission << 4;
//         }
//         return packedBrightness;
//     }

//     /**
//      * @author Starlev
//      * @reason Undefined optimization
//      */
//     @Overwrite
//     public static int getLightCoords(final LightCoordsUtil.BrightnessGetter brightnessGetter, final BlockAndLightGetter level, final BlockState state, final BlockPos pos) {
//         if (state.emissiveRendering()) {
//             return LightCoordsUtil.FULL_BRIGHT;
//         }
//         int packedBrightness = brightnessGetter.packedBrightness(level, pos);
//         int blockSelfEmission = state.getLightEmission();
//         if (blockSelfEmission > 0) {
//             int block = packedBrightness >> 4 & 15;
//             if (block < blockSelfEmission) {
//                 return packedBrightness & 16711680 | blockSelfEmission << 4;
//             }
//         }
//         return packedBrightness;
//     }
// }