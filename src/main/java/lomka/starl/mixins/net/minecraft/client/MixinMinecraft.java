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

package lomka.starl.mixins.net.minecraft.client;

import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(value = Minecraft.class, priority = 500) // VulkanMod compability
public class MixinMinecraft {

    /**
     * @author Starlev
     * @reason Thread.yield() at the end of runTick gives up the thread timeslice on
     *         every frame; on Windows this can add up to a scheduler quantum (probably ~15ms) of
     *         latency. Removing it costs slightly more CPU while frames are uncapped, in
     *         exchange for lower input/render latency. Valid on 1.21-1.21.11: 26.x removed
     *         the call natively (excluded from 26.x code).
     */
    @Redirect(
        method = "runTick",
        at = @At(value = "INVOKE", target = "Ljava/lang/Thread;yield()V"),
        require = 0
    )
    private void removeThreadYield() {
    // Thread.yield();
    }
}
