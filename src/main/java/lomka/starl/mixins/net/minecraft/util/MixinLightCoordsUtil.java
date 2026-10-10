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
        int sky   = level.getBrightness(LightLayer.SKY,   pos);
        int block = level.getBrightness(LightLayer.BLOCK, pos);
        int packedBrightness  = block << 4 | sky << 20;
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
        int packedBrightness  = brightnessGetter.packedBrightness(level, pos);
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