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

import java.util.Queue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;

import com.mojang.blaze3d.audio.Library;
import net.minecraft.client.sounds.ChannelAccess;
import net.minecraft.client.sounds.ChannelAccess.ChannelHandle;
import net.minecraft.client.sounds.SoundEngine;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * @author Starlev
 * Keeps a small pool of channel handles the sound executor already acquired, so playing a sound
 * no longer waits for the sound thread.
 *
 * <p>{@code play} asks for its handle through {@code createHandle(...).join()}, but vanilla only
 * <em>submits</em> that acquisition: the client thread then blocks on a thread hand-off plus an
 * OpenAL source allocation, queued behind {@code scheduleTick()} - the single task that streams
 * every live channel. Only the acquisition is redirected, so the rest of the play path
 * (volume, pitch, attenuation, looping, {@code PlayResult}) stays exactly vanilla.
 *
 * <p>Pooled handles are ordinary handles vanilla has already registered in its channel set. The
 * client thread only ever takes one out and the sound thread remains the only thread that creates
 * or releases them, so the threading contract of the channel set is unchanged. Because a
 * pre-acquired handle has no stream attached and its source is still in the initial state, the
 * per-tick streaming pass treats it as an idle channel.
 *
 * <p>What this does and does not buy: the client thread stops waiting for the sound thread while
 * the pool keeps up, which is a large win at moderate sound rates. It does not remove the wait
 * unconditionally - see the regime limit on {@code lomka$acquireChannelHandle}.
 */
@Mixin(SoundEngine.class)
public abstract class MixinSoundEngine {

    /**
     * Vanilla derives its channel pool limits from the device source count, and the streaming
     * pool never exceeds {@code clamp(sqrt(sources), 2, 8)} sources. Idle streaming handles must
     * therefore stay a small fraction of that budget or music loses sources, while the static
     * pool allows up to 255.
     */
    @Unique @Final private static final int LOMKA$STREAMING_CAP = 3;
    @Unique @Final private static final int LOMKA$STATIC_CAP    = 16;
    @Unique @Final private static final int LOMKA$STREAMING     = 1;

    @Unique @Final private Queue<ChannelHandle>[] lomka$pooled    = lomka$queues();
    @Unique @Final private AtomicInteger[] lomka$acquiring        = lomka$counters();
    @Unique @Final private AtomicInteger[] lomka$pooledCount      = lomka$sizes();

    @Redirect(method = "play", at = @At(value = "INVOKE", target =
            "Lnet/minecraft/client/sounds/ChannelAccess;"
          + "createHandle(Lcom/mojang/blaze3d/audio/Library$Pool;)"
          + "Ljava/util/concurrent/CompletableFuture;"))
    private CompletableFuture<ChannelHandle> lomka$acquireChannelHandle(
            ChannelAccess channelAccess, Library.Pool pool) {
        int index = pool == Library.Pool.STREAMING ? LOMKA$STREAMING : 0;
        ChannelHandle pooled = this.lomka$poll(index);

        /*
         * Replenish on hits too: refilling only on misses leaves the next burst waiting for the
         * executor again. A hit avoids that wait while the executor can replenish the pool
         * faster than playback drains it. Idle pooled handles still occupy device sources and
         * participate in scheduleTick's state queries. At source saturation, unsuccessful
         * refill requests add executor work before the ordinary acquisition, so this policy
         * does not guarantee lower latency than vanilla in that regime.
         */
        this.lomka$topUp(channelAccess, pool, index);

        if (pooled != null) {
            return CompletableFuture.completedFuture(pooled);
        }

        CompletableFuture<ChannelHandle> requested = channelAccess.createHandle(pool);

        return requested.thenApply(handle -> handle != null ? handle : this.lomka$poll(index));
    }

    /*
     * Drops the pooled handles: stopAll() has already released every channel they point at, so a
     * leftover entry would hand out a dead handle and silently drop the sound. The in-flight
     * counters are reset too, because stopAll() shuts the executor down - and with it every
     * pending acquisition - before the channels are released, so their continuations never run.
     */
    @Inject(method = "stopAll", at = @At("TAIL"))
    private void lomka$dropPooledHandles(CallbackInfo ci) {
        for (Queue<ChannelHandle> queue : this.lomka$pooled) {
            queue.clear();
        }

        for (AtomicInteger acquiring : this.lomka$acquiring) {
            acquiring.set(0);
        }

        for (AtomicInteger size : this.lomka$pooledCount) {
            size.set(0);
        }
    }

    /**
     * Schedules at most one acquisition while the pool has drained past half its cap, and never
     * while {@code cap} acquisitions are already in flight. Half the cap rather than the full cap
     * is what keeps this from degenerating into a top-up loop: the pool is refilled ahead of the
     * next burst instead of being topped up on every call.
     */
    @Unique
    private void lomka$topUp(ChannelAccess channelAccess, Library.Pool pool, int index) {
        int cap = index == LOMKA$STREAMING ? LOMKA$STREAMING_CAP : LOMKA$STATIC_CAP;

        if (this.lomka$pooledCount[index].get() * 2 >= cap) {
            return;
        }

        if (this.lomka$acquiring[index].get() < cap) {
            this.lomka$acquiring[index].incrementAndGet();
            this.lomka$refill(channelAccess, pool, index);
        }
    }

    @Unique
    private ChannelHandle lomka$poll(int index) {
        ChannelHandle handle = this.lomka$pooled[index].poll();

        if (handle != null) {
            this.lomka$pooledCount[index].decrementAndGet();
        }

        return handle;
    }

    @Unique
    private void lomka$refill(ChannelAccess channelAccess, Library.Pool pool, int index) {
        @SuppressWarnings("unchecked")
        CompletableFuture<ChannelHandle> pending = channelAccess.createHandle(pool);

        pending.thenAccept(handle -> {
            this.lomka$acquiring[index].decrementAndGet();

            if (handle != null) {
                this.lomka$pooled[index].offer(handle);
                this.lomka$pooledCount[index].incrementAndGet();
            }
        });
    }

    @Unique
    @SuppressWarnings("unchecked")
    private static Queue<ChannelHandle>[] lomka$queues() {
        return new Queue[]{new ConcurrentLinkedQueue<>(), new ConcurrentLinkedQueue<>()};
    }

    /*
     * Built by a helper rather than as an array literal: the legacy Forge pipeline merges @Unique
     * field initialisers into the target initialiser, and an array literal puts an AASTORE opcode
     * in there, which its Mixin version cannot handle.
     */
    @Unique
    private static AtomicInteger[] lomka$counters() {
        return new AtomicInteger[]{new AtomicInteger(), new AtomicInteger()};
    }

    @Unique
    private static AtomicInteger[] lomka$sizes() {
        return new AtomicInteger[]{new AtomicInteger(), new AtomicInteger()};
    }
}
