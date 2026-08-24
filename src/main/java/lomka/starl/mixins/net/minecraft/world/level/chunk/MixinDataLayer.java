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

package lomka.starl.mixins.net.minecraft.world.level.chunk;

import net.minecraft.world.level.chunk.DataLayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(DataLayer.class)
public abstract class MixinDataLayer {

    @Shadow protected byte[] data;
    @Shadow private int defaultValue;

    @Shadow public abstract byte[] getData();

    /**
     * @author Starlev
     * @reason Inlines the public->private->getByteIndex/getNibbleIndex
     *         delegation chain (4 method calls in vanilla) into a single flat
     *         expression. Called on every light/biome lookup during chunk meshing
     *         and lighting propagation, making this one of the hottest small
     *         methods in the chunk pipeline.
     */
    @Overwrite
    public int get(int i, int j, int k) {
        if (this.data == null) {
            return this.defaultValue;
        }
        int idx = (j << 8) | (k << 4) | i;
        return (this.data[idx >> 1] >> ((idx & 1) << 2)) & 15;
    }

    /**
     * @author Starlev
     * @reason Same inlining as get(i,j,k), applied to the write path.
     */
    @Overwrite
    public void set(int i, int j, int k, int l) {
        byte[] abyte = this.getData();
        int idx = (j << 8) | (k << 4) | i;
        int byteIdx = idx >> 1;
        int shift = (idx & 1) << 2;
        abyte[byteIdx] = (byte) ((abyte[byteIdx] & ~(15 << shift)) | ((l & 15) << shift));
    }

    /**
     * @author Starlev
     * @reason Inlines getByteIndex/getNibbleIndex directly. Kept as its own
     *         method (not merged into get(i,j,k)) because vanilla's toString() and
     *         layerToString() call this single-arg overload directly.
     */
    @Overwrite
    private int get(int i) {
        if (this.data == null) {
            return this.defaultValue;
        }
        return (this.data[i >> 1] >> ((i & 1) << 2)) & 15;
    }

    /**
     * @author Starlev
     * @reason Same inlining as the read-side private get(i).
     */
    @Overwrite
    private void set(int i, int j) {
        byte[] abyte = this.getData();
        int byteIdx = i >> 1;
        int shift = (i & 1) << 2;
        abyte[byteIdx] = (byte) ((abyte[byteIdx] & ~(15 << shift)) | ((j & 15) << shift));
    }

    /**
     * @author Starlev
     * @reason Vanilla's loop (for j=4; j<8; j+=4) only ever executes once,
     *         making it dead code left over from a more general N-bit packing
     *         scheme. Collapsed to a single OR; verified equivalent to vanilla for
     *         every possible int input (not just the expected 0-15 nibble range),
     *         since OR-then-truncate-to-byte is invariant to extra high bits present
     *         in either operand before the final cast.
     */
    @Overwrite
    private static byte packFilled(int i) {
        return (byte) (i | (i << 4));
    }
}