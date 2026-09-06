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

package lomka.starl.mixins.net.minecraft.util.thread;

import net.minecraft.util.thread.BlockableEventLoop;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(BlockableEventLoop.class)
public abstract class MixinBlockableEventLoop {

    @Shadow protected abstract boolean scheduleExecutables();

    /**
     * @author Starlev
     * @reason Uses leaner runAsync(runnable, this) instead of vanilla's supplyAsync(supplier, this),
     *         skipping the extra capturing-lambda allocation on the async submission path. Internal
     *         vanilla call sites that block on submitAsync(...).join() route through this overwrite
     *         too, so behavior stays uniform.
     */
    @Overwrite
    private CompletableFuture<Void> submitAsync(Runnable runnable) {
        return CompletableFuture.runAsync(runnable, (Executor) this);
    }
}