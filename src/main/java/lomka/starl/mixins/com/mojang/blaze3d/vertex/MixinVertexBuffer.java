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

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.VertexBuffer;
import com.mojang.blaze3d.vertex.VertexFormat;
//? if >=1.21 {
import com.mojang.blaze3d.vertex.MeshData;
//?} else {
/*import com.mojang.blaze3d.vertex.BufferBuilder;
*///?}
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = VertexBuffer.class, priority = 900) // Mojang overwrite first
public abstract class MixinVertexBuffer {

    @Shadow private int indexCount;
    @Shadow private VertexFormat.Mode mode;
    @Shadow private RenderSystem.AutoStorageIndexBuffer sequentialIndices;

    @Unique private int lomka$cachedGlMode;

    @Shadow
    private VertexFormat.IndexType getIndexType() {
        return null;
    }

    /**
     * Respect the enum's asGLMode as the cache source of truth for mode.
     */
    @Unique
    private void lomka$updateCachedMode() {
        this.lomka$cachedGlMode = this.mode != null ? this.mode.asGLMode : 0;
    }

    /**
     * Cache mode once the geometry is uploaded (mode never changes between uploads on this VBO).
     */
    //? if >=1.21 {
    @Inject(method = "upload(Lcom/mojang/blaze3d/vertex/MeshData;)V", at = @At("RETURN"))
    private void lomka$onUpload(MeshData meshData, CallbackInfo ci) {
        this.lomka$updateCachedMode();
    }
    //?} else {
    /*@Inject(method = "upload(Lcom/mojang/blaze3d/vertex/BufferBuilder$RenderedBuffer;)V", at = @At("RETURN"))
    private void lomka$onUploadLegacy(BufferBuilder.RenderedBuffer buffer, CallbackInfo ci) {
        this.lomka$updateCachedMode();
    }
    *///?}

    /**
     * @author Starlev
     * @reason Zero-alloc draw call: read cached GL primitive mode + live index type.
     */
    @Overwrite
    public void draw() {
        RenderSystem.drawElements(this.lomka$cachedGlMode, this.indexCount, this.getIndexType().asGLType);
    }

    /**
     * Replaces deep VertexFormat.equals() with reference equality. The formats compared in
     * uploadVertexBuffer are the compiled format singletons held by VertexBuffer.format, which
     * are identity-stable per mesh owner; == short-circuits a list/array walk.
     */
    @Redirect(
        method = "uploadVertexBuffer",
        at = @At(value = "INVOKE", target = "Ljava/lang/Object;equals(Ljava/lang/Object;)Z"),
        require = 0
    )
    private boolean lomka$fastFormatCheck(Object formatA, Object formatB) {
        return formatA == formatB;
    }
}