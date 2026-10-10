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
import net.minecraft.world.phys.shapes.EntityCollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(targets = "net.minecraft.world.level.block.state.BlockBehaviour$BlockStateBase")
public abstract class MixinBlockStateBase {

    //? if <1.19 {
    /*@Shadow protected BlockBehaviour.BlockStateBase.@Nullable Cache cache;
    *///?} else {
    @Shadow private BlockBehaviour.BlockStateBase.@Nullable Cache cache;
    //?}

    //? if >=1.20 {
    @Shadow private @Nullable FluidState fluidState;
    //?} else {
    /*@Shadow public abstract FluidState getFluidState();
    *///?}

    @Shadow public    abstract Block      getBlock();
    @Shadow protected abstract BlockState asState();

    /**
     * @author Starlev
     * @reason Replaces polymorphic Block.getCollisionShape dispatch with the per-state shape
     *         BlockStateBase.Cache already holds - but only for queries that cannot observe
     *         anything the cached shape was not built from. The cache is filled once, in the
     *         Cache constructor, from (EmptyBlockGetter.INSTANCE, BlockPos.ZERO,
     *         CollisionContext.empty()), and vanilla only ever reads it from the two-argument
     *         BlockState#getCollisionShape(BlockGetter, BlockPos), which supplies exactly that
     *         context. The three-argument overload used by entity collision delegates
     *         unconditionally instead, so serving the cache for an entity context deletes any
     *         block whose collision shape varies per entity - and modded blocks break in ways
     *         that look unrelated to collision: Create's CrushingWheelControllerBlock returns an
     *         empty shape for the entity it is currently crushing, and with a solid controller
     *         the item can never be shoved past the intake threshold, so the wheel spins and
     *         crushes nothing. Twenty-one Create blocks override getCollisionShape and none of
     *         them declares hasDynamicShape, so the cache exists for all of them.
     *
     *         The gate is the one predicate that is exact on every version we build: the empty
     *         sentinels are all EntityCollisionContext with a null entity (EntityCollisionContext.EMPTY,
     *         and Empty.WITHOUT/WITH_FLUID_COLLISIONS from 1.21.11), CollisionContext.of and
     *         MinecartCollisionContext carry one, and the 26.2+ PositionCollisionContext does not
     *         extend EntityCollisionContext at all - so it lands on the dispatch path, which is what
     *         a position-sensitive shape needs. Entity collision therefore always dispatches now;
     *         the fluidState carve-out stays because LiquidBlock overrides getCollisionShape
     *         without declaring hasDynamicShape either.
     */
    @Overwrite
    public VoxelShape getCollisionShape(BlockGetter level, BlockPos pos, CollisionContext context) {
        boolean contextFree = !(context instanceof EntityCollisionContext entityContext)
                || entityContext.getEntity() == null;

//? if >=1.20 {
    boolean fluidFree = this.fluidState.isEmpty();
    //? } else {
    /*boolean fluidFree = this.getFluidState().isEmpty();
    *///?}
        return this.cache != null && fluidFree && contextFree
                ? this.cache.collisionShape
                : ((InvokerBlockBehaviour) (Object) this.getBlock()).invokeGetCollisionShape(this.asState(), level, pos, context);
    }
}
