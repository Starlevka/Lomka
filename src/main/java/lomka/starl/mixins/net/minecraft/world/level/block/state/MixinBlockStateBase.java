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

package lomka.starl.mixins.net.minecraft.world.level.block.state;

import lomka.starl.mixins.accessor.InvokerBlockBehaviour;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(targets = "net.minecraft.world.level.block.state.BlockBehaviour$BlockStateBase")
public abstract class MixinBlockStateBase {

    @Shadow private BlockBehaviour.BlockStateBase.@Nullable Cache cache;

    @Shadow private @Nullable FluidState fluidState;

    @Shadow public    abstract Block      getBlock();
    @Shadow protected abstract BlockState asState();

    /**
     * @author Starlev
     * @reason Bypasses virtual Block.getCollisionShape dispatch for all non-dynamic blocks.
     *         Vanilla's 2-argument getCollisionShape(level, pos) already checks `this.cache != null ? cache.collisionShape : ...`,
     *         but the 3-argument overload used by entity collisions (BlockCollisions / EntityCollisionContext)
     *         unconditionally delegates to `this.getBlock().getCollisionShape(...)`. For non-dynamic blocks
     *         (hasDynamicShape == false) this.cache is non-null and holds the pre-computed collision shape
     *         evaluated with CollisionContext.empty(). Fluid blocks (LiquidBlock: water/lava) are not marked
     *         dynamicShape yet override getCollisionShape with context-dependent shapes, so they must keep
     *         the vanilla dispatch path.
     */
    @Overwrite
    public VoxelShape getCollisionShape(BlockGetter level, BlockPos pos, CollisionContext context) {
        return this.cache != null && this.fluidState.isEmpty()
                ? this.cache.collisionShape
                : ((InvokerBlockBehaviour) (Object) this.getBlock()).invokeGetCollisionShape(this.asState(), level, pos, context);
    }
}
