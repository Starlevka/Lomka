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

package lomka.starl.mixins.net.minecraft.network;

import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;
import java.util.Collection;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import net.minecraft.network.FriendlyByteBuf;
//? if >=1.21 {
import net.minecraft.network.codec.StreamEncoder;
//?}
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;

@Mixin(FriendlyByteBuf.class)
public abstract class MixinFriendlyByteBuf {

    @Shadow public abstract int readVarInt();
    @Shadow public abstract FriendlyByteBuf writeVarInt(int i);
    //? if >=1.21 {
    @Shadow public abstract FriendlyByteBuf writeByte(int i);
    //?}
    @Shadow public abstract byte readByte();

    @Unique private final LomkaMapHelper lomka$mapHelper = new LomkaMapHelper();

    @Unique
    private static final class LomkaMapHelper<K, V> implements BiConsumer<K, V> {
        FriendlyByteBuf buf;
        //? if >=1.21 {
        StreamEncoder<? super FriendlyByteBuf, K> keyEnc;
        StreamEncoder<? super FriendlyByteBuf, V> valEnc;
        @Override public void accept(K k, V v) {
            this.keyEnc.encode(this.buf, k);
            this.valEnc.encode(this.buf, v);
        }
        //?} else {
        /*FriendlyByteBuf.Writer<K> keyEnc;
        FriendlyByteBuf.Writer<V> valEnc;
        @Override public void accept(K k, V v) { this.keyEnc.accept(this.buf, k); this.valEnc.accept(this.buf, v); }*/
        //?}
    }

    /**
     * @author Starlev
     * @reason Avoids Iterator allocation for List+RandomAccess collections
     *         (the common case: ArrayList-backed item/entity lists in most packets)
     *         by using indexed access instead. Non-RandomAccess collections fall
     *         back to the same iterator-based mechanism vanilla always uses.
     */
    @Overwrite
    //? if >=1.21 {
    public <T> void writeCollection(Collection<T> collection, StreamEncoder<? super FriendlyByteBuf, T> streamencoder) {
    //?} else {
    /*public <T> void writeCollection(Collection<T> collection, FriendlyByteBuf.Writer<T> streamencoder) {*/
    //?}
        this.writeVarInt(collection.size());
        if (collection instanceof List && collection instanceof java.util.RandomAccess) {
            List<?> list = (List<?>) collection;
            int size = list.size();
            for (int i = 0; i < size; ++i) {
                @SuppressWarnings("unchecked")
                T element = (T) list.get(i);
                //? if >=1.21 {
                streamencoder.encode((FriendlyByteBuf) (Object) this, element);
                //?} else {
                /*streamencoder.accept((FriendlyByteBuf) (Object) this, element);*/
                //?}
            }
        } else {
            for (T object : collection) {
                //? if >=1.21 {
                streamencoder.encode((FriendlyByteBuf) (Object) this, object);
                //?} else {
                /*streamencoder.accept((FriendlyByteBuf) (Object) this, object);*/
                //?}
            }
        }
    }

    /**
     * @author Starlev
     * @reason Replaces per-call capturing lambda with a cached BiConsumer that
     *         still uses HashMap.forEach's internal table walk (no Entry iterator).
     *         For non-HashMap maps falls back to entrySet loop. Saves one lambda
     *         allocation per packet while keeping the fast internal iteration.
     */
    @Overwrite
    //? if >=1.21 {
    public <K, V> void writeMap(Map<K, V> map, StreamEncoder<? super FriendlyByteBuf, K> streamencoder, StreamEncoder<? super FriendlyByteBuf, V> streamencoder1) {
    //?} else {
    /*public <K, V> void writeMap(Map<K, V> map, FriendlyByteBuf.Writer<K> streamencoder, FriendlyByteBuf.Writer<V> streamencoder1) {*/
    //?}
        this.writeVarInt(map.size());
        if (map instanceof HashMap) {
            @SuppressWarnings("unchecked")
            LomkaMapHelper<K, V> h = (LomkaMapHelper<K, V>) this.lomka$mapHelper;
            h.buf    = (FriendlyByteBuf) (Object) this;
            h.keyEnc = streamencoder;
            h.valEnc = streamencoder1;
            //? if >=1.21 {
            ((HashMap<K, V>) map).forEach(h);
            //?} else {
            /*((HashMap<K, V>) map).forEach(h);*/
            //?}
            h.buf    = null;
            h.keyEnc = null;
            h.valEnc = null;
        } else {
            for (Map.Entry<K, V> entry : map.entrySet()) {
                //? if >=1.21 {
                streamencoder.encode((FriendlyByteBuf) (Object) this, entry.getKey());
                //?} else {
                /*streamencoder.accept((FriendlyByteBuf) (Object) this, entry.getKey());*/
                //?}
                //? if >=1.21 {
                streamencoder1.encode((FriendlyByteBuf) (Object) this, entry.getValue());
                //?} else {
                /*streamencoder1.accept((FriendlyByteBuf) (Object) this, entry.getValue());*/
                //?}
            }
        }
    }

    //? if >=1.19.3 {
    /**
     * @author Starlev
     * @reason Replaces BitSet + toByteArray() with sparse-aware packing: iterate
     *         only over present elements (size) instead of all enum constants (len).
     *         For DyeColor/Direction sets (1-4 elements of 16) this is 4-8x fewer
     *         contains checks. Same LSB-first convention, verified round-trip.
     */
    @Overwrite
    public <E extends Enum<E>> void writeEnumSet(EnumSet<E> enumset, Class<E> oclass) {
        int len       = oclass.getEnumConstants().length;
        int byteCount = (len + 7) >> 3;
        if (enumset.isEmpty()) {
            for (int i = 0; i < byteCount; ++i) {
                //? if >=1.21 {
                this.writeByte(0);
                //?} else {
                /*((io.netty.buffer.ByteBuf) (Object) this).writeByte(0);*/
                //?}
            }
            return;
        }
        byte[] out = new byte[byteCount];
        for (E e : enumset) {
            int ord = e.ordinal();
            out[ord >> 3] |= (byte) (1 << (ord & 7));
        }
        for (int i = 0; i < byteCount; ++i) {
            //? if >=1.21 {
            this.writeByte(out[i] & 255);
            //?} else {
            /*((io.netty.buffer.ByteBuf) (Object) this).writeByte(out[i] & 255);*/
            //?}
        }
    }

    /**
     * @author Starlev
     * @reason Sparse-aware unpack: iterate only set bits via trailingZeros
     *         instead of all 8 per byte. Avoids BitSet.valueOf() alloc and
     *         is O(set size) not O(len).
     */
    @Overwrite
    public <E extends Enum<E>> EnumSet<E> readEnumSet(Class<E> oclass) {
        E[] aenum = oclass.getEnumConstants();
        int len       = aenum.length;
        int byteCount = (len + 7) >> 3;
        EnumSet<E> enumset = EnumSet.noneOf(oclass);
        for (int i = 0; i < byteCount; ++i) {
            int b     = this.readByte() & 255;
            int start = i << 3;
            while (b != 0) {
                int bit = Integer.numberOfTrailingZeros(b);
                int ord = start + bit;
                if (ord < len) enumset.add(aenum[ord]);
                b &= b - 1;
            }
        }
        return enumset;
    }
    //?}

    /**
     * @author Starlev
     * @reason Pre-sizes the backing array to the declared element count to
     *         avoid geometric-growth reallocations. Capped at 65536 regardless of
     *         the declared count: that count comes straight from an unvalidated
     *         network VarInt, and a single 5-byte VarInt can claim up to
     *         Integer.MAX_VALUE elements independent of the packet's actual size.
     *         Without this cap, a malicious or corrupt packet could trigger an
     *         immediate multi-gigabyte allocation attempt before the read loop ever
     *         gets a chance to fail on insufficient remaining bytes. The list still
     *         grows incrementally past the cap if a legitimately large count is
     *         ever sent, so this only changes the up-front allocation, not behavior.
     */
    @Overwrite
    public IntList readIntIdList() {
        int i = this.readVarInt();
        IntArrayList intarraylist = new IntArrayList(Math.max(0, Math.min(i, 1 << 16)));
        for (int j = 0; j < i; ++j) {
            intarraylist.add(this.readVarInt());
        }
        return intarraylist;
    }

    /**
     * @author Starlev
     * @reason Indexed getInt(i) instead of forEach(this::writeVarInt);
     *         IntArrayList.getInt is a direct array access, avoiding the method
     *         reference dispatch layer.
     */
    @Overwrite
    public void writeIntIdList(IntList intlist) {
        int size = intlist.size();
        this.writeVarInt(size);
        for (int i = 0; i < size; ++i) {
            this.writeVarInt(intlist.getInt(i));
        }
    }
}