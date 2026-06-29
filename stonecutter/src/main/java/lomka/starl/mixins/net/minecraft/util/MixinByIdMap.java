package lomka.starl.mixins.net.minecraft.util;

import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import java.util.function.IntFunction;
import java.util.function.ToIntFunction;
import java.lang.reflect.Array;
import net.minecraft.util.ByIdMap;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(ByIdMap.class)
public class MixinByIdMap {

    @Shadow private static <T> IntFunction<T> createMap(ToIntFunction<T> tointfunction, T[] at) {
        throw new AssertionError();
    }

    /**
     * @author Starlev
     * @reason Eliminates a lambda allocation and a null-check on every lookup.
     */
    @SuppressWarnings("unchecked")
    @Overwrite
    public static <T> IntFunction<T> sparse(ToIntFunction<T> tointfunction, T[] at, T t0) {
        Int2ObjectOpenHashMap<T> map = (Int2ObjectOpenHashMap<T>) createMap(tointfunction, at);
        map.defaultReturnValue(t0);
        return map;
    }

    /**
     * @author Starlev
     * @reason Avoids unnecessary array cloning and Arrays.fill during registry initialization.
     */
    @SuppressWarnings("unchecked")
    @Overwrite
    private static <T> T[] createSortedArray(ToIntFunction<T> tointfunction, T[] at) {
        int len = at.length;
        if (len == 0) {
            throw new IllegalArgumentException("Empty value list");
        }

        T[] sorted = (T[]) Array.newInstance(at.getClass().getComponentType(), len);

        for (T obj : at) {
            int id = tointfunction.applyAsInt(obj);
            if (id < 0 || id >= len) {
                throw new IllegalArgumentException("Values are not continuous, found index " + id + " for value " + obj);
            }
            if (sorted[id] != null) {
                throw new IllegalArgumentException("Duplicate entry on id " + id + ": current=" + obj + ", previous=" + sorted[id]);
            }
            sorted[id] = obj;
        }

        for (int i = 0; i < len; ++i) {
            if (sorted[i] == null) {
                throw new IllegalArgumentException("Missing value at index: " + i);
            }
        }

        return sorted;
    }

    /**
     * @author Starlev
     * @reason Optimizes hot-path lookups by inlining math, avoiding method calls, 
     * and using bitwise AND for WRAP strategy when length is a power of 2.
     */
    @Overwrite
    public static <T> IntFunction<T> continuous(ToIntFunction<T> tointfunction, T[] at, ByIdMap.OutOfBoundsStrategy strategy) {
        T[] sorted = createSortedArray(tointfunction, at);
        int len = sorted.length;

        switch (strategy) {
            case ZERO:
                T fallback = sorted[0];
                return (j) -> j >= 0 && j < len ? sorted[j] : fallback;
                
            case WRAP:
                if ((len & (len - 1)) == 0) {
                    int mask = len - 1;
                    return (j) -> sorted[j & mask];
                }
                return (j) -> {
                    int rem = j % len;
                    return sorted[rem < 0 ? rem + len : rem];
                };
                
            case CLAMP:
                int max = len - 1;
                return (j) -> sorted[j < 0 ? 0 : (j > max ? max : j)];
                
            default:
                throw new MatchException((String) null, (Throwable) null);
        }
    }
}