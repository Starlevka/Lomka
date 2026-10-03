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

package lomka.starl.mixins.net.minecraft.client.renderer.entity;

import net.minecraft.client.renderer.entity.DisplayRenderer;
import org.joml.Quaternionf;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(DisplayRenderer.class)
public abstract class MixinDisplayRenderer {

    @Unique private static final ThreadLocal<Quaternionf> lomka$SCRATCH_ORIENTATION = ThreadLocal.withInitial(Quaternionf::new);

    /*
     * Serves the billboarding quaternion from a reusable scratch instead of allocating one per
     * display entity per frame. Vanilla passes new Quaternionf() into calculateOrientation purely
     * as a write target: every branch of that switch does quaternionf.rotationYXZ(...) and
     * returns `this`, and the returned object is only read by mulPose/rotate, which multiply
     * it into the pose and the normal matrix without storing it. rotationYXZ assigns rather
     * than accumulating, so no state carries between calls and a shared instance is sound.
     * Uncontested: the only vendored mod mixing into DisplayRenderer is Axiom, and it redirects
     * Display#renderState inside extractRenderState, a different method.
     *
     * The allocation moved between eras, so the redirected METHOD differs, not just its name:
     *
     * - 1.20.1 has no output parameter at all. `calculateOrientation(RenderState, T)` allocates
     *   the quaternion itself, in the three allocating branches of its switch (lines 76/79/82);
     *   the FIXED branch returns the entity's own stored `orientation` field and allocates
     *   nothing. A redirect with no `ordinal` matches all three NEW sites, which is correct
     *   because the switch executes exactly one of them per call. 1.19.2 is below the gate:
     *   neither DisplayRenderer nor the Display entity exists there.
     * - 1.21-1.21.8 hoisted the allocation into the caller: `render` passes `new Quaternionf()`
     *   into calculateOrientation as the write target (1.21.1 line 57).
     * - 1.21.9+ renamed the entry point to submit(); the caller-side allocation is unchanged
     *   (the vendored 1.21.9-1.21.10 snapshot still shows render, it predates the rename).
     */
    //? if <1.21 {
    /*@Redirect(
            method = "calculateOrientation",
            at = @At(value = "NEW", target = "org/joml/Quaternionf")
    )
    private Quaternionf lomka$reuseOrientation() {
        return lomka$SCRATCH_ORIENTATION.get();
    }
    *///? } else if <1.21.9 {
    /*@Redirect(
            method = "render",
            at = @At(value = "NEW", target = "org/joml/Quaternionf")
    )
    private Quaternionf lomka$reuseOrientation() {
        return lomka$SCRATCH_ORIENTATION.get();
    }
    *///? } else {
    @Redirect(
            method = "submit",
            at = @At(value = "NEW", target = "org/joml/Quaternionf")
    )
    private Quaternionf lomka$reuseOrientation() {
        return lomka$SCRATCH_ORIENTATION.get();
    }
    //?}
}
