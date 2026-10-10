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

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import net.minecraft.client.Minecraft;

/**
 * Drops the per-frame {@code Thread.yield()} from {@code Minecraft#runGameLoop} on 1.12.2.
 *
 * <p>It is a redirect rather than an overwrite on purpose. {@code runGameLoop} is one of the most
 * heavily patched methods on 1.12.2 - every rendering mod touches it - and an overwrite would
 * delete the method node those mixins anchor into, which is the documented way to break a game
 * with an otherwise correct patch. A redirect replaces exactly one instruction and leaves every
 * other injector in the method able to resolve.
 *
 * <p>Measured cost of the call on this machine: ~90-100 ns, once per frame, which is ~0.001% of a
 * 16.7 ms frame. This is a vanilla wart, not a Lomka optimization, and the figure is recorded so
 * the trade is visible rather than assumed: it is a scheduler hint that lets helper threads (skin
 * and resource downloads) run sooner, and the JDK schedules them anyway. 1.19.2 through 1.21.11
 * keep the same call in the same place; Mojang removed it in 26.x. (Figures: scripts/bench.)
 */
@Mixin(Minecraft.class)
public abstract class MixinMinecraft {

	// Replaces the spin hint with nothing; C2 folds the empty body away entirely.
	@Redirect(method = "runGameLoop", at = @At(value = "INVOKE", target = "Ljava/lang/Thread;yield()V"))
	private static void lomka$skipFrameYield() {
	}
}
