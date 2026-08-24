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

package lomka.starl.mixins.com.mojang.blaze3d.audio;

import com.mojang.blaze3d.audio.Listener;
import com.mojang.blaze3d.audio.ListenerTransform;
import net.minecraft.world.phys.Vec3;
import org.lwjgl.openal.AL10;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;

@Mixin(Listener.class)
public abstract class MixinListener {

    @Shadow private ListenerTransform transform;

    @Unique private final float[] lomka$orientationCache = new float[6];

    /**
     * @author Starlev
     * @reason Avoids allocating a fresh orientation array each frame and reuses a cached
     *         buffer so listener updates stay allocation-light on the audio hot path.
     */
    @Overwrite
    public void setTransform(ListenerTransform listenertransform) {
        this.transform = listenertransform;

        Vec3 pos     = listenertransform.position();
        Vec3 forward = listenertransform.forward();
        Vec3 up      = listenertransform.up();

        AL10.alListener3f(4100, (float) pos.x, (float) pos.y, (float) pos.z);

        lomka$orientationCache[0] = (float) forward.x;
        lomka$orientationCache[1] = (float) forward.y;
        lomka$orientationCache[2] = (float) forward.z;
        lomka$orientationCache[3] = (float) up.x;
        lomka$orientationCache[4] = (float) up.y;
        lomka$orientationCache[5] = (float) up.z;

        AL10.alListenerfv(4111, lomka$orientationCache);
    }
}