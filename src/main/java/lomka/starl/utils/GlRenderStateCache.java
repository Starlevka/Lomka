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

package lomka.starl.utils;

import java.util.Arrays;

/**
 * Backing store for the GL viewport/scissor/polygon/fbo dedup in MixinGlStateManager. Lives outside the
 * mixin so both the injected handlers and the window-resize reset operate on one shared copy
 * (a mixin class and its merged duplicate keep separate statics).
 *
 * Indices 0-3: viewport x/y/w/h. Indices 4-7: scissor box x/y/w/h.
 * Indices 8-9: polygon mode face/mode. Indices 10-11: bound read/write framebuffer ids.
 * MIN_VALUE means unknown.
 */
public final class GlRenderStateCache {

    private GlRenderStateCache() {}

    public static final int VIEWPORT_X   = 0;
    public static final int VIEWPORT_Y   = 1;
    public static final int VIEWPORT_W   = 2;
    public static final int VIEWPORT_H   = 3;
    public static final int SCISSOR_X    = 4;
    public static final int SCISSOR_Y    = 5;
    public static final int SCISSOR_W    = 6;
    public static final int SCISSOR_H    = 7;
    public static final int POLYGON_FACE = 8;
    public static final int POLYGON_MODE = 9;
    public static final int FBO_READ     = 10;
    public static final int FBO_WRITE    = 11;

    private static final int[] STATE = new int[12];

    static {
        reset();
    }

    public static void reset() {
        Arrays.fill(STATE, Integer.MIN_VALUE);
    }

    public static int get(int index) {
        return STATE[index];
    }

    public static void set(int index, int value) {
        STATE[index] = value;
    }
}


