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

package lomka.starl.mixins.net.minecraft.world.level.levelgen;

import net.minecraft.world.level.levelgen.LegacyRandomSource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Removes the {@code AtomicLong.compareAndSet} round-trip from the default RNG's hot path.
 *
 * <p>Vanilla's {@code next(int)} performs a volatile read plus a {@code lock cmpxchg} on every
 * call purely as a cross-thread misuse detector (throws via ThreadingDetector). The LCG itself
 * needs none of that: same multiplier/increment/mask as {@code SingleThreadedRandomSource}, whose
 * plain-field bodies are replicated here, so the produced sequences stay bit-identical and
 * worldgen parity is untouched.
 *
 * <p>{@code setSeed} deliberately keeps its vanilla implementation - including the CAS and the
 * threading exception - because seed installation is rare while genuine cross-thread misuse is
 * worth failing loudly on. A TAIL injection mirrors the value vanilla just stored into its
 * AtomicLong into a private plain field, so the constructor-seeded state is visible to the fast
 * path before any consumer can call {@code next}.
 *
 * <p>Implemented as a cancellable HEAD inject instead of an {@code @Overwrite}: the vanilla body
 * stays in the bytecode, so other mods patching these methods remain structurally applicable -
 * overwrite-based RNG patches (e.g. Async) and invocation redirects of the inner CAS (e.g.
 * AsyncParticles) neither lose their injection targets nor crash against us. At runtime our
 * handler decides every draw, so such mods' bodies simply stop executing while Lomka is enabled;
 * users who prefer foreign RNG semantics should disable this patch via
 * {@code net.minecraft.world.level.levelgen.MixinLegacyRandomSource=false} in
 * {@code config/lomka-mixins.properties}.
 *
 * <p>Trade-off: concurrent calls to {@code next} that previously crashed with a threading
 * exception now proceed racily (benign for an RNG - worst case duplicated/correlated draws).
 */
@Mixin(LegacyRandomSource.class)
public abstract class MixinLegacyRandomSource {

    @Unique private long lomka$seed;

    /**
     * Vanilla wraps every draw in a volatile read + compareAndSet solely to detect
     * cross-thread misuse. Plain field turns the hot path into three integer ops
     * (identical to SingleThreadedRandomSource); LCG constants are unchanged so the
     * sequence - and therefore worldgen parity - remains bit-for-bit identical.
     */
    @Inject(
            method = "next(I)I",
            at = @At("HEAD"),
            cancellable = true
    )
    private void lomka$fastNext(int i, CallbackInfoReturnable<Integer> cir) {
        long j = this.lomka$seed * 25214903917L + 11L & 281474976710655L;
        this.lomka$seed = j;
        cir.setReturnValue((int) (j >> 48 - i));
    }

    /**
     * Mirrors the seed value vanilla computed into its AtomicLong into the plain field
     * backing the fast {@code next}. Runs after the vanilla body, so the CAS-based misuse
     * detector on seed installation is fully preserved.
     */
    @Inject(
            method = "setSeed",
            at = @At("TAIL")
    )
    private void lomka$syncPlainSeed(long l, CallbackInfo ci) {
        this.lomka$seed = (l ^ 25214903917L) & 281474976710655L;
    }
}
