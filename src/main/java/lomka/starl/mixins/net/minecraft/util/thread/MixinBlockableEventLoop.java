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
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * Event loop task zone, suppressed by folding the guard its name argument hangs on.
 *
 * <p>{@code doRunTask} wraps every task drained from the main loop in a profiler zone:
 * {@code TracyClient.beginZone("Task", SharedConstants.IS_RUNNING_IN_IDE)}. The flag is the second
 * argument of a library call rather than a branch of its own, and {@code Zone} is null-checked twice
 * afterwards, so the library is documented by its own call sites to return {@code null} when the
 * flag is false. The guard exists to keep the flag read out of the task path; redirecting it to a
 * constant is the whole patch.
 *
 * <p><b>Gate: {@code >=1.21.4}.</b> The read does not exist below that - {@code BlockableEventLoop}
 * has zero {@code IS_RUNNING_IN_IDE} references in 1.19.2, 1.20.1 and 1.21.1, because Tracy support
 * was not in the game yet. So this covers seven of the ten MC versions, fourteen of the twenty
 * variants, and the mixin is excluded from the other six by
 * {@code excludeUnlistedMixins()} rather than by a source-level conditional.
 *
 * <p><b>Common, not client.</b> {@code BlockableEventLoop} is the base of both the client loop and
 * {@code MinecraftServer}'s loop, and the guarded field read is on {@code SharedConstants} rather
 * than on the profiler library, so the anchor resolves on a dedicated server too. This belongs in
 * the {@code mixins} list; putting it in {@code client} would apply it nowhere and, per the 0.3.0 -
 * 0.5.5 key-spelling bug, an entry in the wrong list is dropped silently rather than loudly.
 *
 * <p><b>Why {@code FIELD} and not {@code GETSTATIC}.</b> {@code GETSTATIC} is not an injection point
 * specifier: the built-in list is {@code HEAD}, {@code RETURN}, {@code TAIL}, {@code INVOKE},
 * {@code INVOKE_ASSIGN}, {@code FIELD}, {@code NEW}, {@code INVOKE_STRING}, {@code JUMP},
 * {@code CONSTANT}, and {@code GETSTATIC} is only a valid value of the {@code opcode} attribute that
 * narrows {@code FIELD}. Spelled as the specifier it fails at {@code Parse Injection Points} and,
 * with {@code "required": true}, aborts the mixin at boot. {@code RedirectInjector} routes a
 * {@code FIELD} anchor to {@code injectAtFieldAccess}, which is what substitutes the constant.
 *
 * <p><b>Competitor check</b>, per the pre-overwrite rule: no checked mod patches
 * {@code doRunTask}. Axiom calls {@code BlockableEventLoop#schedule}, ModernFix holds a reference to
 * the type in {@code ChunkMapMixin} and its {@code MinecraftMixin} is a local stub extending the
 * class rather than a patch of it. So there is no foreign injector anchored in the method body and
 * this redirect cannot collide.
 *
 * <p><b>What it costs.</b> The removed work is one read of a mutable non-final static per executed
 * task - small, and in proportion to how many tasks the loop drains.
 *
 * <p><b>What is lost.</b> Tracy can no longer record per-task zones. Enabling the profiler flag now
 * produces no task breakdown from this site, which is the cost that makes this worth revisiting
 * rather than accumulating: the saving is real but small, and the diagnostic is the only thing a
 * profiler user would notice.
 */
@Mixin(BlockableEventLoop.class)
public class MixinBlockableEventLoop {

    @Redirect(
            method = "doRunTask",
            at = @At(
                    value = "FIELD",
                    target = "Lnet/minecraft/SharedConstants;IS_RUNNING_IN_IDE:Z",
                    opcode = Opcodes.GETSTATIC
            )
    )
    private static boolean lomka$noIdeTaskZone() {
        return false;
    }
}