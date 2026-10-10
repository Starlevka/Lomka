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
 * Drops the per-frame {@code Thread.yield()} on 1.12.2.
 *
 * It is a redirect rather than an overwrite on purpose. The game loop is one of the most heavily
 * patched methods on 1.12.2 - every rendering mod touches it - and an overwrite would delete the
 * method node those mixins anchor into, which is the documented way to break a game with an
 * otherwise correct patch. A redirect replaces exactly one instruction and leaves every other
 * injector in the method able to resolve.
 *
 * Feather splits the loop in two: {@code run()} is the outer loop and {@code runGame()} holds the
 * body, including the yield. The redirect has to name {@code runGame}, not {@code run}.
 */
@Mixin(Minecraft.class)
public abstract class MixinMinecraft {

	@Redirect(method = "runGame", at = @At(value = "INVOKE", target = "Ljava/lang/Thread;yield()V"))
	private void lomka$skipFrameYield() {
	}
}
