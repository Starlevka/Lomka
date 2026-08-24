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

package lomka.starl.mixins.net.minecraft.client.sounds;

import net.minecraft.client.Camera;
import net.minecraft.client.sounds.SoundEngine;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(SoundEngine.class)
public abstract class MixinSoundEngine {

    @Shadow private boolean loaded;

    @Unique private Vec3   lomka$lastPos  = Vec3.ZERO;
    @Unique private float  lomka$lastXRot = Float.NaN;
    @Unique private float  lomka$lastYRot = Float.NaN;

    /**
     * @author Starlev
     * @reason Skips redundant listener updates when the camera state has not changed,
     *         avoiding needless native OpenAL work on the sound update path.
     */
    @Inject(method = "updateSource", at = @At("HEAD"), cancellable = true)
    private void lomka$skipRedundantCameraUpdates(Camera camera, CallbackInfo ci) {
        if (!this.loaded) return;

        //? if >=1.21.11 {
        if (!camera.isInitialized()) return;

        Vec3  currentPos  = camera.position();
        float currentXRot = camera.xRot();
        float currentYRot = camera.yRot();
        //?} else {
        /*if (!camera.isInitialized()) return;

        Vec3  currentPos  = camera.getPosition();
        float currentXRot = camera.getXRot();
        float currentYRot = camera.getYRot();
        *///?}

        if (currentPos.equals(this.lomka$lastPos)
                && currentXRot == this.lomka$lastXRot
                && currentYRot == this.lomka$lastYRot) {
            ci.cancel();
            return;
        }

        this.lomka$lastPos  = currentPos;
        this.lomka$lastXRot = currentXRot;
        this.lomka$lastYRot = currentYRot;
    }
}
