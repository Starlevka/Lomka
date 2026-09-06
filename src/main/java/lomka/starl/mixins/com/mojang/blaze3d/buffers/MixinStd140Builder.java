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

package lomka.starl.mixins.com.mojang.blaze3d.buffers;

import com.mojang.blaze3d.buffers.Std140Builder;
import java.nio.ByteBuffer;
import net.minecraft.util.Mth;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(Std140Builder.class)
public abstract class MixinStd140Builder {

    @Shadow @Final private ByteBuffer buffer;
    @Shadow @Final private int start;

    /**
     * @author Starlev
     * @reason Speeds up std140 alignment on the warm per-frame UBO write paths
     *         (Lighting.updateLevel, DynamicUniformStorage.writeTransform, LightTexture)
     *         by resolving power-of-two alignments - the only kind std140 layouts ever
     *         produce - with a single AND mask instead of Mth.roundToward's ceilDiv chain.
     */
    @Overwrite
    public Std140Builder align(int align) {
        if (align > 0 && (align & (align - 1)) == 0) {
            int offset = this.buffer.position() - this.start;
            int aligned = (offset + (align - 1)) & -align;
            if (aligned != offset) {
                this.buffer.position(this.start + aligned);
            }
        } else {
            int j = this.buffer.position();
            this.buffer.position(this.start + Mth.roundToward(j - this.start, align));
        }
        return (Std140Builder) (Object) this;
    }
}
