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

package lomka.starl.mixins.net.minecraft.client.resources.model;

import com.google.common.collect.ImmutableList;
//? if >=26.1 {
/*import net.minecraft.client.resources.model.geometry.BakedQuad;
import net.minecraft.client.resources.model.geometry.QuadCollection;*/
//?} else {
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.resources.model.QuadCollection;
//?}
import net.minecraft.core.Direction;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;

import java.util.ArrayList;
import java.util.List;

@Mixin(QuadCollection.Builder.class)
public class MixinBuilder {

    @Shadow @Final private ImmutableList.Builder<BakedQuad> unculledFaces;

    @Shadow
    private static QuadCollection createFromSublists(List<BakedQuad> all, int unculledCount, int northCount, int southCount, int eastCount, int westCount, int upCount, int downCount) {
        throw new AssertionError();
    }

    @Unique private List<BakedQuad>[] lomka$fastCulledFaces;

    /**
     * @author Starlev
     * @reason Replaces slow Guava ArrayListMultimap with a fast array-based bucket list.
     *         Drastically reduces garbage allocation and map lookups during block model baking.
     */
    @SuppressWarnings("unchecked")
    @Overwrite
    public QuadCollection.Builder addCulledFace(Direction direction, BakedQuad bakedquad) {
        if (this.lomka$fastCulledFaces == null) {
            this.lomka$fastCulledFaces = new List[6];
        }

        int idx = direction.get3DDataValue();
        List<BakedQuad> list = this.lomka$fastCulledFaces[idx];
        if (list == null) {
            this.lomka$fastCulledFaces[idx] = list = new ArrayList<>(4);
        }

        list.add(bakedquad);
        return (QuadCollection.Builder) (Object) this;
    }

    /**
     * @author Starlev
     * @reason Assembling the QuadCollection using the fast array instead of iterating
     *         over Multimap collections. Order strictly matches Mojang's internal switch layout.
     */
    @Overwrite
    public QuadCollection build() {
        ImmutableList<BakedQuad> unculled = this.unculledFaces.build();

        if (this.lomka$fastCulledFaces == null) {
            return unculled.isEmpty() ? QuadCollection.EMPTY
                : createFromSublists(unculled, unculled.size(), 0, 0, 0, 0, 0, 0);
        }

        List<BakedQuad> north = lomka$getList(Direction.NORTH);
        List<BakedQuad> south = lomka$getList(Direction.SOUTH);
        List<BakedQuad> east  = lomka$getList(Direction.EAST);
        List<BakedQuad> west  = lomka$getList(Direction.WEST);
        List<BakedQuad> up    = lomka$getList(Direction.UP);
        List<BakedQuad> down  = lomka$getList(Direction.DOWN);

        ImmutableList.Builder<BakedQuad> allQuads = ImmutableList.builder();
        allQuads.addAll(unculled);
        allQuads.addAll(north);
        allQuads.addAll(south);
        allQuads.addAll(east);
        allQuads.addAll(west);
        allQuads.addAll(up);
        allQuads.addAll(down);

        return createFromSublists(
            allQuads.build(),
            unculled.size(),
            north.size(),
            south.size(),
            east.size(),
            west.size(),
            up.size(),
            down.size()
        );
    }
    /**
     * @author Starlev
     * @reason Bulk transfers quads directly into bucketed lists without Multimap allocation or intermediate collections.
     */

    //? if >=26.1 {
    /*@SuppressWarnings("unchecked")
    @Overwrite
    public QuadCollection.Builder addAll(QuadCollection quadCollection) {
        this.unculledFaces.addAll(quadCollection.getQuads(null));

        lomka$addFaceList(Direction.NORTH, quadCollection.getQuads(Direction.NORTH));
        lomka$addFaceList(Direction.SOUTH, quadCollection.getQuads(Direction.SOUTH));
        lomka$addFaceList(Direction.EAST,  quadCollection.getQuads(Direction.EAST));
        lomka$addFaceList(Direction.WEST,  quadCollection.getQuads(Direction.WEST));
        lomka$addFaceList(Direction.UP,    quadCollection.getQuads(Direction.UP));
        lomka$addFaceList(Direction.DOWN,  quadCollection.getQuads(Direction.DOWN));

        return (QuadCollection.Builder) (Object) this;
    }

    @Unique
    private void lomka$addFaceList(Direction dir, List<BakedQuad> incoming) {
        if (incoming == null || incoming.isEmpty()) return;

        if (this.lomka$fastCulledFaces == null) {
            this.lomka$fastCulledFaces = new List[6];
        }

        int idx = dir.get3DDataValue();
        List<BakedQuad> list = this.lomka$fastCulledFaces[idx];
        if (list == null) {
            this.lomka$fastCulledFaces[idx] = list = new ArrayList<>(incoming.size());
        }
        list.addAll(incoming);
    }*/
    //?}

    @Unique
    private List<BakedQuad> lomka$getList(Direction dir) {
        if (this.lomka$fastCulledFaces == null) return List.of();
        List<BakedQuad> list = this.lomka$fastCulledFaces[dir.get3DDataValue()];
        return list != null ? list : List.of();
    }
}