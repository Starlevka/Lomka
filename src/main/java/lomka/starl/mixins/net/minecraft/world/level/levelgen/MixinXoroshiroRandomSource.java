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

import net.minecraft.world.level.levelgen.MarsagliaPolarGaussian;
import net.minecraft.world.level.levelgen.RandomSupport;
import net.minecraft.world.level.levelgen.Xoroshiro128PlusPlus;
import net.minecraft.world.level.levelgen.XoroshiroRandomSource;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Reseeds the existing Xoroshiro generator in place instead of installing a fresh one.
 *
 * <p>Vanilla's {@code setSeed} discards the current {@link Xoroshiro128PlusPlus} and stores
 * {@code new Xoroshiro128PlusPlus(RandomSupport.upgradeSeedTo128bit(seed))}. The generator is the
 * only escaping allocation on that path - the two {@code Seed128bit} records built while mixing
 * the seed are read straight back out and get scalar-replaced, while the generator lands in a
 * non-final field and therefore always reaches the heap. Worldgen reseeds hundreds of times per
 * chunk: {@code ChunkGenerator#applyBiomeDecoration} wraps a {@code XoroshiroRandomSource} in a
 * {@code WorldgenRandom} and calls {@code setFeatureSeed} once per structure and once per placed
 * feature of every decoration step (the wrapper's {@code setSeed} forwards here), so this patch
 * trades one generator allocation per feature for two field writes.
 *
 * <p>Bit-identical: the mixing math is still vanilla's own {@code upgradeSeedTo128bit}, the
 * all-zero-seed guard uses the constants {@code Xoroshiro128PlusPlus(long, long)} applies, and
 * {@code gaussianSource.reset()} runs exactly as vanilla. The generator is fully described by its
 * two state words and nothing upstream compares its identity, so the drawn sequence, {@code fork},
 * {@code forkPositional} and the codec all yield the same values.
 *
 * <p>The generator's two state words are widened to public by {@code lomka.ct} /
 * {@code accesstransformer.ct}, so the reseed is a plain field write with no accessor bridge.
 *
 * <p>Implemented as a cancellable HEAD inject instead of an {@code @Overwrite} for the same reason
 * as {@link MixinLegacyRandomSource}: the vanilla body stays in the bytecode, so foreign
 * injections into {@code setSeed} remain structurally applicable. Only a mod that redirects the
 * generator allocation itself loses its target at runtime - disable this patch via
 * {@code net.minecraft.world.level.levelgen.MixinXoroshiroRandomSource=false} in
 * {@code config/lomka-mixins.properties} to restore vanilla behaviour.
 */
@Mixin(XoroshiroRandomSource.class) // Compatibility with C2ME OpenCL probably
public abstract class MixinXoroshiroRandomSource {

    /* Constants Xoroshiro128PlusPlus(long, long) substitutes for an all-zero seed; we bypass that constructor. */
    @Unique private static final long lomka$ZERO_GUARD_LO = -7046029254386353131L;
    @Unique private static final long lomka$ZERO_GUARD_HI =  7640891576956012809L;

    @Shadow private Xoroshiro128PlusPlus randomNumberGenerator;

    @Shadow @Final private MarsagliaPolarGaussian gaussianSource;

    /*
     * Writes vanilla's post-mixseed state into the live generator, leaving the object identity and
     * every other field of the source untouched. Reached before the vanilla body, which is skipped
     * via ci.cancel(); a null generator (never observed upstream, but honest) falls through.
     */
    @Inject(
            method = "setSeed",
            at = @At("HEAD"),
            cancellable = true
    )
    private void lomka$reseedInPlace(long seed, CallbackInfo ci) {
        Xoroshiro128PlusPlus generator = this.randomNumberGenerator;
        if (generator == null) {
            return;
        }

        RandomSupport.Seed128bit upgraded = RandomSupport.upgradeSeedTo128bit(seed);
        long seedLo = upgraded.seedLo();
        long seedHi = upgraded.seedHi();
        if ((seedLo | seedHi) == 0L) {
            seedLo  = lomka$ZERO_GUARD_LO;
            seedHi  = lomka$ZERO_GUARD_HI;
        }

        generator.seedLo = seedLo;
        generator.seedHi = seedHi;

        this.gaussianSource.reset();
        ci.cancel();
    }
}
