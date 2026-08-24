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

package lomka.starl.mixins.net.minecraft.client.renderer;

import net.minecraft.client.Camera;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.state.CameraRenderState;
import net.minecraft.client.renderer.state.LevelRenderState;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(GameRenderer.class)
public abstract class MixinGameRenderer {

    @Shadow @Final private Camera mainCamera;

    @Shadow @Final private LevelRenderState levelRenderState;

    /**
     * @author Starlev
     * @reason Reuse the camera render state quaternion instead of allocating a
     *         new Quaternionf every frame (CameraRenderState initializes it in the
     *         constructor, and every consumer only copies its values, never retaining
     *         the reference).
     */
    @Overwrite
    private void extractCamera(float f) {
        CameraRenderState state = this.levelRenderState.cameraRenderState;
        state.initialized = this.mainCamera.isInitialized();
        state.pos = this.mainCamera.position();
        //? if >=1.21.11 {
        state.blockPos = this.mainCamera.blockPosition();
        state.entityPos = this.mainCamera.entity().getPosition(f);
        //?} else {
        /*state.blockPos = this.mainCamera.getBlockPosition();
        state.entityPos = this.mainCamera.getEntity().getPosition(f);
        *///?}

        state.orientation.set(this.mainCamera.rotation());
    }
}