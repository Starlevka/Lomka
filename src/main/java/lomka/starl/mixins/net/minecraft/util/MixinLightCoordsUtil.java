/*
 * This file is part of Lomka (https://github.com/Starlevka/Lomka)
 * Copyright (C) 2026 Starlev (a.k.a. Starlevka) and contributors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, version 3 of the License only.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 *
 * SPDX-License-Identifier: LGPL-3.0-only
 */

package lomka.starl.mixins.net.minecraft.util;

import net.minecraft.core.BlockPos;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.world.level.BlockAndLightGetter;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

@Mixin(LightCoordsUtil.class)
public class MixinLightCoordsUtil {

    /**
     * @author Starlev
     * @reason Smooth-lighting hot path: quick exit for the common case where the block emits no
     *         light (emission <= 0), returning the coords unchanged or clamped to 240 instead of
     *         running the clamp/repack. Removes the extra clamp plus bitshift work per call.
     */
    @Overwrite
    public static int addSmoothBlockEmission(final int lightCoords, float blockLightEmission) {
        if (blockLightEmission <= 0.0F) {
            int currentBlock = lightCoords & 255;
            if (currentBlock <= 240) {
                return lightCoords & 16711935;
            }
            return lightCoords & 16711680 | 240;
        }
        float clamped = blockLightEmission > 1.0F ? 1.0F : blockLightEmission;
        int emittedBlock = (int) (clamped * 240.0F);
        int currentBlock = lightCoords & 255;
        int block = currentBlock + emittedBlock;
        if (block > 240) {
            block = 240;
        }
        return lightCoords & 16711680 | block;
    }

    /**
     * @author Starlev
     * @reason Per-component max on the light-packing hot path: adds an equality fast path so
     *         identical coordinates return immediately, and packs each nibble max with direct
     *         bit ops. Equivalent to vanilla, minus the branchlets for the common equal case.
     */
    @Overwrite
    public static int max(final int coords1, final int coords2) {
        if (coords1 == coords2) {
            return coords1;
        }
        int block1 = coords1 >> 4 & 15;
        int block2 = coords2 >> 4 & 15;
        int sky1 = coords1 >> 20 & 15;
        int sky2 = coords2 >> 20 & 15;
        int block = block1 > block2 ? block1 : block2;
        int sky = sky1 > sky2 ? sky1 : sky2;
        return block << 4 | sky << 20;
    }

    /**
     * @author Starlev
     * @reason Emissive-light fast path: short-circuits the two trivial cases (emission <= 0 returns
     *         coords unchanged, emission >= 15 returns FULL_BRIGHT) so the per-channel max is only
     *         computed for the in-rage emissive blocks.
     */
    @Overwrite
    public static int lightCoordsWithEmission(final int lightCoords, final int emission) {
        if (emission <= 0) {
            return lightCoords;
        }
        if (emission >= 15) {
            return LightCoordsUtil.FULL_BRIGHT;
        }
        int sky = lightCoords >> 20 & 15;
        int block = lightCoords >> 4 & 15;
        if (sky < emission) {
            sky = emission;
        }
        if (block < emission) {
            block = emission;
        }
        return block << 4 | sky << 20;
    }

    /**
     * @author Starlev
     * @reason Replaces vanilla's per-channel interpolation call with a single weighted accumulation
     *         for each of the sky/block channels and a combined mask, cutting the call overhead on
     *         the smooth-lighting interpolation hot path.
     */
    @Overwrite
    public static int smoothWeightedBlend(final int coords1, final int coords2, final int coords3, final int coords4, final float weight1, final float weight2, final float weight3, final float weight4) {
        int sky = (int) ((coords1 >> 16 & 255) * weight1 + (coords2 >> 16 & 255) * weight2 + (coords3 >> 16 & 255) * weight3 + (coords4 >> 16 & 255) * weight4);
        int block = (int) ((coords1 & 255) * weight1 + (coords2 & 255) * weight2 + (coords3 & 255) * weight3 + (coords4 & 255) * weight4);

        return (block & 255) | ((sky & 255) << 16);
    }

    //? if !=26.1 {
    /**
     * @author Starlev
     * @reason Inlines the brightness lookup and block self-emission handling of a block into a single
     *         pass, avoiding an intermediate packed-light round trip and an extra branch when the block
     *         is emissive.
     */
    @Overwrite
    public static int getLightCoords(final BlockAndLightGetter level, final BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        if (state.emissiveRendering()) {
            return LightCoordsUtil.FULL_BRIGHT;
        }
        int sky = level.getBrightness(LightLayer.SKY, pos);
        int block = level.getBrightness(LightLayer.BLOCK, pos);
        int packedBrightness = block << 4 | sky << 20;
        int blockSelfEmission = state.getLightEmission();
        if (blockSelfEmission > 0 && block < blockSelfEmission) {
            return packedBrightness & 16711680 | blockSelfEmission << 4;
        }
        return packedBrightness;
    }

    /**
     * @author Starlev
     * @reason Combines the precomputed packed brightness with the block self-emission in one pass, and
     *         short-circuits to FULL_BRIGHT for fully emissive blocks, skipping the extract-cmp-merge
     *         steps on the lighting path.
     */
    @Overwrite
    public static int getLightCoords(final LightCoordsUtil.BrightnessGetter brightnessGetter, final BlockAndLightGetter level, final BlockState state, final BlockPos pos) {
        if (state.emissiveRendering()) {
            return LightCoordsUtil.FULL_BRIGHT;
        }
        int packedBrightness = brightnessGetter.packedBrightness(level, pos);
        int blockSelfEmission = state.getLightEmission();
        if (blockSelfEmission > 0) {
            int block = packedBrightness >> 4 & 15;
            if (block < blockSelfEmission) {
                return packedBrightness & 16711680 | blockSelfEmission << 4;
            }
        }
        return packedBrightness;
    }
    //?}
}