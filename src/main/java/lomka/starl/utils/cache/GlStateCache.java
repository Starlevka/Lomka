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

package lomka.starl.utils.cache;

/**
 * Last values pushed to the driver for the GL viewport/scissor/polygon/fbo dedup in
 * MixinGlStateManager. Lives outside the mixin so the injected handlers and the window-resize
 * reset in MixinWindow address one shared copy (a mixin class and its merged duplicate keep
 * separate statics).
 *
 * {@link #UNKNOWN} means "not pushed yet", so the first call of a session always reaches the driver.
 */
public final class GlStateCache {

    private GlStateCache() {}

    public static final int UNKNOWN = Integer.MIN_VALUE;

    public static int viewportX   = UNKNOWN;
    public static int viewportY   = UNKNOWN;
    public static int viewportW   = UNKNOWN;
    public static int viewportH   = UNKNOWN;
    public static int scissorX    = UNKNOWN;
    public static int scissorY    = UNKNOWN;
    public static int scissorW    = UNKNOWN;
    public static int scissorH    = UNKNOWN;
    public static int polygonFace = UNKNOWN;
    public static int polygonMode = UNKNOWN;
    public static int fboRead     = UNKNOWN;
    public static int fboWrite    = UNKNOWN;

    /**
     * Forgets every cached value, so the next call of each state reaches the driver again.
     */
    public static void reset() {
        viewportX = viewportY = viewportW = viewportH = UNKNOWN;
        scissorX = scissorY = scissorW = scissorH = UNKNOWN;
        polygonFace = polygonMode = UNKNOWN;
        fboRead = fboWrite = UNKNOWN;
    }
}