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

package lomka.starl.mixins.net.minecraft.client.animation;

import lomka.starl.duck.IKeyframeAnimationEntry;
import org.joml.Vector3f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;

/*
 * Bridge to KeyframeAnimation$Entry, nothing else (1.21.6+).
 *
 * Entry is a private nested record, so MixinKeyframeAnimation cannot name it; the duck
 * interface gives it a typed handle and lets it hand the entry its own scratch vector
 * instead of vanilla's per-call `new Vector3f()` (24 B per apply on 1.21.9+, the only
 * allocation in this path that survives C2 - see below). The body itself is vanilla's.
 *
 * The search is deliberately NOT reimplemented here any more. It was, to drop the capturing
 * IntPredicate that `Mth#binarySearch(0, len, seconds <= timestamp)` builds per channel:
 * scripts/bench AnimationAllocBenchRunner measures that object at 0.00 B/frame through both
 * an interface and a direct call site - `Mth#binarySearch` is a 10-line static method, so C2
 * inlines it and scalar-replaces the callback, while the scratch Vector3f (passed on into the
 * Interpolation/Target interfaces) keeps escaping. The inlined replica was also the slower of
 * the two, 5.067 vs 4.654 ns per search in AnimationBenchRunner#benchSearch. So reimplementing
 * it bought nothing measurable and cost a permanent parity obligation against vanilla's
 * insertion index - through an @Invoker/@Shadow call the vanilla search stays authoritative.
 *
 * This is where COO (scripts/COMPABILITY owns Collections of Optimizations) stops: it rewrites
 * KeyframeAnimations#animate, which only exists before the 1.21.6 bake rework, and that mixin
 * is `require = 0` on newer versions. No competitor touches Entry#apply.
 */
@Mixin(targets = "net.minecraft.client.animation.KeyframeAnimation$Entry")
public abstract class MixinEntry implements IKeyframeAnimationEntry {

    @Shadow public abstract void apply(float secondsSinceStart, float targetScale, Vector3f scratch);

    /**
     * @author Starlev
     * @reason Hands the call through to vanilla's own Entry#apply with the caller's scratch
     *         vector, so MixinKeyframeAnimation can reuse one Vector3f per animation and the
     *         frame search, its clamp and its interpolation stay exactly vanilla's.
     */
    @Override
    @Unique
    public void lomka$apply(float secondsSinceStart, float targetScale, Vector3f scratch) {
        this.apply(secondsSinceStart, targetScale, scratch);
    }
}
