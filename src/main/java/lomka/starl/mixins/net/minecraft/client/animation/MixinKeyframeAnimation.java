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

import java.util.List;
import lomka.starl.duck.IKeyframeAnimationEntry;
import net.minecraft.client.animation.KeyframeAnimation;
import org.joml.Vector3f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;

/*
 * The baked Bedrock animation path (1.21.6+): bake() resolves bone names to ModelParts once
 * and interpolation writes into a caller-provided Vector3f, so the per-frame cost is meant to
 * be arithmetic only. It is not, in one place: 1.21.9 dropped the scratch vector field and
 * `apply(long, float)` now allocates `new Vector3f()` per call, and that object escapes into
 * the Interpolation and Target interfaces, so C2 cannot scalar-replace it. scripts/bench
 * AnimationAllocBenchRunner measures it at 24 B/frame in both an interface and a direct call
 * shape, and 0.00 B/frame with the vector reused - this overwrite is what removes it.
 *
 * Everything else in this method is left as vanilla computes it. The list iterator, the
 * AnimationState#ifStarted capturing Consumer and the search's capturing IntPredicate all
 * measure 0.00 B/frame (they are inlined and scalar-replaced), which is why the state overload
 * is not overwritten any more and MixinEntry no longer reimplements the search. The added
 * zero-scale early return is not an allocation argument: with targetScale == 0 both vanilla
 * interpolations multiply their result by 0 and all three targets only offset by it, so the
 * pass cannot change the pose, and skipping it saves the whole 8-16 channel walk for a model
 * whose animation speed is zero. The fuzz in AnimationBenchRunner proves both invariants and
 * pins the only two bit-level exceptions of the skip: vanilla's +0.0F write normalizes a pose
 * component holding -0.0F (value-identical, sign of zero differs), and degenerate content with
 * duplicate head keyframe timestamps hit exactly on them lets vanilla poison the pose with NaN
 * where the skip does not - both unreachable on well-formed animations.
 * (Figures: scripts/bench.)
 *
 * Only for the versions where the vanilla allocation exists: vanilla used a field up to
 * 1.21.8, so this mixin's own field replaces it on 1.21.9+ and shadows it below.
 */
@Mixin(KeyframeAnimation.class)
public abstract class MixinKeyframeAnimation {

    @Shadow private List<IKeyframeAnimationEntry> entries;

    @Shadow protected abstract float getElapsedSeconds(long millisSinceStart);

    /*
     * A KeyframeAnimation belongs to one model and setupAnim only runs on the render thread,
     * with entities animated one after another, so a per-instance scratch vector is safe.
     * Vanilla kept its own field until 1.21.9 and dropped it; reuse it where it exists and
     * supply a copy where it does not.
     */
    //? if >=1.21.9 {
    @Unique private final Vector3f lomka$scratchVector = new Vector3f();
    //?} else {
    /*@Shadow private Vector3f scratchVector;
    *///?}

    /**
     * @author Starlev
     * @reason Reuses the scratch vector (vanilla's own field before 1.21.9) and walks the
     *         immutable entry list by index instead of allocating an iterator. The zero-scale
     *         guard is exact, not approximate: both interpolations multiply their result by the
     *         scale and the three vanilla targets only offset by that result, so a clip applied
     *         with scale 0.0F cannot change the pose. Tiny non-zero scales are still applied.
     */
    @Overwrite
    public void apply(long millisSinceStart, float targetScale) {
        if (targetScale == 0.0F) {
            return;
        }

        float secondsSinceStart = this.getElapsedSeconds(millisSinceStart);
        //? if >=1.21.9 {
        Vector3f scratch = this.lomka$scratchVector;
        //?} else {
        /*Vector3f scratch = this.scratchVector;
        *///?}
        List<IKeyframeAnimationEntry> entries = this.entries;

        for (int i = 0, size = entries.size(); i < size; i++) {
            entries.get(i).lomka$apply(secondsSinceStart, targetScale, scratch);
        }
    }
}
