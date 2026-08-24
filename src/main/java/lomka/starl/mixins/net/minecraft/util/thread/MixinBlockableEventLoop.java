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

    private static final CompletableFuture<Void> COMPLETED_FUTURE =
            CompletableFuture.completedFuture(null);

    /**
     * @author Starlev
     * @reason Uses leaner runAsync(runnable, this) instead of vanilla's supplyAsync(supplier, this),
     *         skipping an extra captured-lambda allocation on the async submission path. Internal
     *         vanilla call sites that block on submitAsync(...).join() route through this overwrite
     *         too, so behavior stays uniform.
     */
    @Overwrite
    private CompletableFuture<Void> submitAsync(Runnable runnable) {
        return CompletableFuture.runAsync(runnable, (Executor) this);
    }

    /**
     * @author Starlev
     * @reason Inline path runs the task directly and returns a shared already-completed future
     *         instead of allocating a fresh one each call. Sharing is safe because a completed
     *         future is immutable through the public completion API — complete()/cancel()/
     *         completeExceptionally() are silent no-ops returning false, exactly as on vanilla's
     *         own freshly-created instance. Only obtrudeValue()/obtrudeResult() could mutate it,
     *         and no vanilla or modded caller uses those. The async branch reuses submitAsync.
     */
    @Overwrite
    public CompletableFuture<Void> submit(Runnable runnable) {
        if (this.scheduleExecutables()) {
            return this.submitAsync(runnable);
        }
        runnable.run();
        return COMPLETED_FUTURE;
    }
}