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

package lomka.starl.mixins.com.mojang.blaze3d.vertex;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.vertex.VertexFormatElement;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(VertexFormatElement.class)
public abstract class MixinVertexFormatElement {

    @Shadow @Final private VertexFormatElement.Type  type;
    @Shadow @Final private VertexFormatElement.Usage usage;
    @Shadow @Final private int index;
    @Shadow @Final private int count;

    @Unique private int lomka$cachedHash;

    @Inject(method = "<init>", at = @At("RETURN"))
    private void lomka$cacheHash(int i, VertexFormatElement.Type type, VertexFormatElement.Usage usage, int j, CallbackInfo ci) {
        int h = this.type.hashCode();
        h = 31 * h + this.usage.hashCode();
        h = 31 * h + this.index;
        h = 31 * h + this.count;
        this.lomka$cachedHash = h;
    }

    /**
     * @author Starlev
     * @reason Returns precomputed hash code to bypass repeated enum and field hashing during format map lookups.
     */
    @Overwrite
    public int hashCode() {
        return this.lomka$cachedHash;
    }

    /**
     * @author Starlev
     * @reason Bypasses the megamorphic functional-interface dispatch in Usage with a direct enum switch,
     *         removing per-attribute lambda invocation during VAO setup on the 1.20.1 GL path.
     */
    @Overwrite
    public void setupBufferState(int elementIndex, long offset, int stride) {
        int glType = this.type.getGlType();
        int count = this.count;
        int attrIndex = this.index;

        switch (this.usage) {
            case POSITION -> {
                GlStateManager._enableVertexAttribArray(elementIndex);
                GlStateManager._vertexAttribPointer(elementIndex, count, glType, false, stride, offset);
            }
            case NORMAL -> {
                GlStateManager._enableVertexAttribArray(elementIndex);
                GlStateManager._vertexAttribPointer(elementIndex, count, glType, true, stride, offset);
            }
            case COLOR -> {
                GlStateManager._enableVertexAttribArray(elementIndex);
                GlStateManager._vertexAttribPointer(elementIndex, count, glType, true, stride, offset);
            }
            case UV -> {
                GlStateManager._enableVertexAttribArray(elementIndex);
                if (glType == 5126) {
                GlStateManager._vertexAttribPointer(elementIndex, count, glType, false, stride, offset);
                } else {
                GlStateManager._vertexAttribIPointer(elementIndex, count, glType, stride, offset);
                }
            }
            case GENERIC -> {
                GlStateManager._enableVertexAttribArray(elementIndex);
                GlStateManager._vertexAttribPointer(elementIndex, count, glType, false, stride, offset);
            }
            case PADDING -> {
            }
        }
    }

    /**
     * @author Starlev
     * @reason Direct dispatch to disable the vertex attribute array, bypassing the Usage lambda invocation.
     */
    @Overwrite
    public void clearBufferState(int elementIndex) {
        if (this.usage != VertexFormatElement.Usage.PADDING) {
            GlStateManager._disableVertexAttribArray(elementIndex);
        }
    }
}
