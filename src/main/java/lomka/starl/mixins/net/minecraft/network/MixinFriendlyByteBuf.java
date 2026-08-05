package lomka.starl.mixins.net.minecraft.network;

import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;
import java.util.Collection;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import net.minecraft.network.FriendlyByteBuf;
//? if >=1.21 {
import net.minecraft.network.codec.StreamEncoder;
//?}
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(FriendlyByteBuf.class)
public abstract class MixinFriendlyByteBuf {

    @Shadow public abstract int readVarInt();
    @Shadow public abstract FriendlyByteBuf writeVarInt(int i);
    @Shadow public abstract FriendlyByteBuf writeByte(int i);
    @Shadow public abstract byte readByte();

    /**
     * @author Starlev
     * @reason Avoids Iterator allocation for List+RandomAccess collections
     * (the common case: ArrayList-backed item/entity lists in most packets)
     * by using indexed access instead. Non-RandomAccess collections fall
     * back to the same iterator-based mechanism vanilla always uses.
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
     * @reason Replaces map.forEach(capturingLambda) with a direct entrySet
     * loop. Vanilla's lambda captures `this` and both encoders, so a fresh
     * instance is allocated on every writeMap call; this removes that
     * allocation entirely.
     */
    @Overwrite
    //? if >=1.21 {
    public <K, V> void writeMap(Map<K, V> map, StreamEncoder<? super FriendlyByteBuf, K> streamencoder, StreamEncoder<? super FriendlyByteBuf, V> streamencoder1) {
    //?} else {
    /*public <K, V> void writeMap(Map<K, V> map, FriendlyByteBuf.Writer<K> streamencoder, FriendlyByteBuf.Writer<V> streamencoder1) {*/
    //?}
        this.writeVarInt(map.size());
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

    /**
     * @author Starlev
     * @reason Replaces BitSet + toByteArray()/writeFixedBitSet with direct
     * manual bit packing, avoiding the BitSet allocation and its
     * intermediate byte[]. Uses the same LSB-first-within-byte convention as
     * BitSet.toByteArray(); verified via round-trip testing against random
     * enum sets at many lengths, including non-multiple-of-8 boundaries.
     */
    @Overwrite
    public <E extends Enum<E>> void writeEnumSet(EnumSet<E> enumset, Class<E> oclass) {
        E[] aenum = oclass.getEnumConstants();
        int len = aenum.length;
        int byteCount = (len + 7) >> 3;
        for (int i = 0; i < byteCount; ++i) {
            int b = 0;
            int start = i << 3;
            int end = Math.min(start + 8, len);
            for (int j = start; j < end; ++j) {
                if (enumset.contains(aenum[j])) {
                    b |= (1 << (j - start));
                }
            }
            this.writeByte(b);
        }
    }

    /**
     * @author Starlev
     * @reason Mirrors writeEnumSet's manual bit packing, avoiding
     * readFixedBitSet's BitSet.valueOf() allocation on the read side.
     */
    @Overwrite
    public <E extends Enum<E>> EnumSet<E> readEnumSet(Class<E> oclass) {
        E[] aenum = oclass.getEnumConstants();
        int len = aenum.length;
        int byteCount = (len + 7) >> 3;
        EnumSet<E> enumset = EnumSet.noneOf(oclass);
        for (int i = 0; i < byteCount; ++i) {
            int b = this.readByte() & 255;
            int start = i << 3;
            int end = Math.min(start + 8, len);
            for (int j = start; j < end; ++j) {
                if ((b & (1 << (j - start))) != 0) {
                    enumset.add(aenum[j]);
                }
            }
        }
        return enumset;
    }

    /**
     * @author Starlev
     * @reason Pre-sizes the backing array to the declared element count to
     * avoid geometric-growth reallocations. Capped at 65536 regardless of
     * the declared count: that count comes straight from an unvalidated
     * network VarInt, and a single 5-byte VarInt can claim up to
     * Integer.MAX_VALUE elements independent of the packet's actual size.
     * Without this cap, a malicious or corrupt packet could trigger an
     * immediate multi-gigabyte allocation attempt before the read loop ever
     * gets a chance to fail on insufficient remaining bytes. The list still
     * grows incrementally past the cap if a legitimately large count is
     * ever sent, so this only changes the up-front allocation, not behavior.
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
     * IntArrayList.getInt is a direct array access, avoiding the method
     * reference dispatch layer.
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