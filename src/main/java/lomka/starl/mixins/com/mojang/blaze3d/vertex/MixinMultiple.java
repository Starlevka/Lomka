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

//? if >=1.21 {
package lomka.starl.mixins.com.mojang.blaze3d.vertex;

import com.mojang.blaze3d.vertex.VertexConsumer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

/**
 * @author Starlev
 * Replaces the per-call lambda allocation + Consumer.accept() dispatch of
 * VertexMultiConsumer.Multiple#forEach with direct indexed array loops. The
 * fan-out forwards every vertex mutation to all delegates; the lambdas
 * are hot (per-vertex) and unique per call site, so this avoids GC churn.
 * Covers 1.21 -> 26.1 (class removed/reworked in 26.2 Vulkan pipeline).
 */
@Mixin(targets = "com.mojang.blaze3d.vertex.VertexMultiConsumer$Multiple")
public abstract class MixinMultiple implements VertexConsumer {

    @Shadow private VertexConsumer[] delegates;

    @Overwrite
    public VertexConsumer addVertex(float f, float f1, float f2) {
        VertexConsumer[] arr = this.delegates;
        for (int i = 0, len = arr.length; i < len; i++) {
            arr[i].addVertex(f, f1, f2);
        }
        return this;
    }

    @Overwrite
    public VertexConsumer setColor(int i, int j, int k, int l) {
        VertexConsumer[] arr = this.delegates;
        for (int x = 0, len = arr.length; x < len; x++) {
            arr[x].setColor(i, j, k, l);
        }
        return this;
    }

    @Overwrite
    public VertexConsumer setUv(float f, float f1) {
        VertexConsumer[] arr = this.delegates;
        for (int i = 0, len = arr.length; i < len; i++) {
            arr[i].setUv(f, f1);
        }
        return this;
    }

    @Overwrite
    public VertexConsumer setUv1(int i, int j) {
        VertexConsumer[] arr = this.delegates;
        for (int x = 0, len = arr.length; x < len; x++) {
            arr[x].setUv1(i, j);
        }
        return this;
    }

    @Overwrite
    public VertexConsumer setUv2(int i, int j) {
        VertexConsumer[] arr = this.delegates;
        for (int x = 0, len = arr.length; x < len; x++) {
            arr[x].setUv2(i, j);
        }
        return this;
    }

    @Overwrite
    public VertexConsumer setNormal(float f, float f1, float f2) {
        VertexConsumer[] arr = this.delegates;
        for (int i = 0, len = arr.length; i < len; i++) {
            arr[i].setNormal(f, f1, f2);
        }
        return this;
    }

    @Overwrite
    public void addVertex(float f, float f1, float f2, int i, float f3, float f4, int j, int k, float f5, float f6, float f7) {
        VertexConsumer[] arr = this.delegates;
        for (int x = 0, len = arr.length; x < len; x++) {
            arr[x].addVertex(f, f1, f2, i, f3, f4, j, k, f5, f6, f7);
        }
    }

    //? if >=1.21.11 {
    @Overwrite
    public VertexConsumer setColor(int i) {
        VertexConsumer[] arr = this.delegates;
        for (int x = 0, len = arr.length; x < len; x++) {
            arr[x].setColor(i);
        }
        return this;
    }

    @Overwrite
    public VertexConsumer setLineWidth(float f) {
        VertexConsumer[] arr = this.delegates;
        for (int x = 0, len = arr.length; x < len; x++) {
            arr[x].setLineWidth(f);
        }
        return this;
    }
    //?}
}
//?} else {
/*package lomka.starl.mixins.com.mojang.blaze3d.vertex;

import com.mojang.blaze3d.vertex.VertexConsumer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

// Replaces the per-call lambda allocation + Consumer.accept() dispatch of
// VertexMultiConsumer.Multiple#forEach with direct indexed array loops.
// Legacy vertex format API.
@Mixin(targets = "com.mojang.blaze3d.vertex.VertexMultiConsumer$Multiple")
public abstract class MixinMultiple implements VertexConsumer {

    @Shadow private VertexConsumer[] delegates;

    @Overwrite
    public VertexConsumer vertex(double d0, double d1, double d2) {
        VertexConsumer[] arr = this.delegates;
        for (int i = 0, len = arr.length; i < len; i++) {
            arr[i].vertex(d0, d1, d2);
        }
        return this;
    }

    @Overwrite
    public VertexConsumer color(int i, int j, int k, int l) {
        VertexConsumer[] arr = this.delegates;
        for (int x = 0, len = arr.length; x < len; x++) {
            arr[x].color(i, j, k, l);
        }
        return this;
    }

    @Overwrite
    public VertexConsumer uv(float f, float f1) {
        VertexConsumer[] arr = this.delegates;
        for (int i = 0, len = arr.length; i < len; i++) {
            arr[i].uv(f, f1);
        }
        return this;
    }

    @Overwrite
    public VertexConsumer overlayCoords(int i, int j) {
        VertexConsumer[] arr = this.delegates;
        for (int x = 0, len = arr.length; x < len; x++) {
            arr[x].overlayCoords(i, j);
        }
        return this;
    }

    @Overwrite
    public VertexConsumer uv2(int i, int j) {
        VertexConsumer[] arr = this.delegates;
        for (int x = 0, len = arr.length; x < len; x++) {
            arr[x].uv2(i, j);
        }
        return this;
    }

    @Overwrite
    public VertexConsumer normal(float f, float f1, float f2) {
        VertexConsumer[] arr = this.delegates;
        for (int i = 0, len = arr.length; i < len; i++) {
            arr[i].normal(f, f1, f2);
        }
        return this;
    }

    @Overwrite
    public void vertex(float f, float f1, float f2, float f3, float f4, float f5, float f6, float f7, float f8, int i, int j, float f9, float f10, float f11) {
        VertexConsumer[] arr = this.delegates;
        for (int x = 0, len = arr.length; x < len; x++) {
            arr[x].vertex(f, f1, f2, f3, f4, f5, f6, f7, f8, i, j, f9, f10, f11);
        }
    }

    @Overwrite
    public void endVertex() {
        VertexConsumer[] arr = this.delegates;
        for (int i = 0, len = arr.length; i < len; i++) {
            arr[i].endVertex();
        }
    }

    @Overwrite
    public void defaultColor(int i, int j, int k, int l) {
        VertexConsumer[] arr = this.delegates;
        for (int x = 0, len = arr.length; x < len; x++) {
            arr[x].defaultColor(i, j, k, l);
        }
    }

    @Overwrite
    public void unsetDefaultColor() {
        VertexConsumer[] arr = this.delegates;
        for (int i = 0, len = arr.length; i < len; i++) {
            arr[i].unsetDefaultColor();
        }
    }
}
*///?}