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

package lomka.starl.mixins.net.minecraft.util;

//? if >=1.21.11 {
import net.minecraft.util.Util;
//?} else {
/*import net.minecraft.Util;
*///?}
import com.google.common.collect.Maps;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectLists;
import it.unimi.dsi.fastutil.objects.Reference2IntOpenHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.ToIntFunction;
import java.util.stream.IntStream;
import net.minecraft.CharPredicate;
import net.minecraft.util.RandomSource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Unique;

@Mixin(Util.class)
public abstract class MixinUtil {

    @Unique private static final Predicate<?> lomka$ALWAYS_TRUE  = object -> true;
    @Unique private static final Predicate<?> lomka$ALWAYS_FALSE = object -> false;

    /**
     * @author Starlev
     * @reason Replaces Instant.now().toEpochMilli() with intrinsic System.currentTimeMillis(); eliminates heap allocation.
     */
    @Overwrite
    public static long getEpochMillis() {
        return System.currentTimeMillis();
    }

    /**
     * @author Starlev
     * @reason Keeps vanilla's exact pipeline semantics - {@code toLowerCase(Locale.ROOT)} runs on the whole
     *         string first (it can change length, e.g. U+0130 lowercases to two chars) and the predicate then
     *         tests every resulting char - but drops the boxed {@code chars().mapToObj()} stream: one scan
     *         finds the first rejected char, and only a containing case copies to a char[] and patches the
     *         substitutes in place. All-valid input returns the lowercased string directly.
     */
    @Overwrite
    public static String sanitizeName(String s, CharPredicate charpredicate) {
        String lower = s.toLowerCase(Locale.ROOT);
        int len = lower.length();
        int firstInvalid = -1;
        for (int i = 0; i < len; ++i) {
            if (!charpredicate.test(lower.charAt(i))) {
                firstInvalid = i;
                break;
            }
        }
        if (firstInvalid < 0) {
            return lower;
        }
        char[] chars = lower.toCharArray();
        chars[firstInvalid] = '_';
        for (int i = firstInvalid + 1; i < len; ++i) {
            if (!charpredicate.test(chars[i])) {
                chars[i] = '_';
            }
        }
        return new String(chars);
    }

    /**
     * @author Starlev
     * @reason Branch-efficient 32-bit integer arithmetic without 64-bit promotion or library calls.
     */
    //? if >=1.21.6 {
    @Overwrite
    public static int growByHalf(int i, int j) {
        int grown = i + (i >> 1);
        if (grown > 2147483639 || grown < 0) {
            grown = 2147483639;
        }
        return grown < j ? j : grown;
    }
    //?}

    /**
     * @author Starlev
     * @reason Specializes 2x2 and 3x3 recipe symmetry checks; replaces inner multiplication with stepped pointers.
     */
    //? if >=1.21 {
    @Overwrite
    public static <T> boolean isSymmetrical(int i, int j, List<T> list) {
        if (i <= 1) {
            return true;
        }
        if (i == 2) {
            for (int l = 0, row = 0; l < j; ++l, row += 2) {
                if (!Objects.equals(list.get(row), list.get(row + 1))) {
                    return false;
                }
            }
            return true;
        }
        if (i == 3) {
            for (int l = 0, row = 0; l < j; ++l, row += 3) {
                if (!Objects.equals(list.get(row), list.get(row + 2))) {
                    return false;
                }
            }
            return true;
        }
        int k = i >> 1;
        for (int l = 0, row = 0; l < j; ++l, row += i) {
            for (int left = row, right = row + i - 1; left < row + k; ++left, --right) {
                if (!Objects.equals(list.get(left), list.get(right))) {
                    return false;
                }
            }
        }
        return true;
    }
    //?}

    /**
     * @author Starlev
     * @reason Replaces stream pipeline and collector overhead with direct entrySet iteration and pre-sized map.
     */
    //? if >=1.21.6 {
    @Overwrite
    public static <K, V1, V2> Map<K, V2> mapValues(Map<K, V1> map, Function<? super V1, V2> function) {
        int size = map.size();
        if (size == 0) {
            return Map.of();
        }
        Map<K, V2> result = Maps.newHashMapWithExpectedSize(size);
        for (Map.Entry<K, V1> entry : map.entrySet()) {
            result.put(entry.getKey(), function.apply(entry.getValue()));
        }
        return result;
    }
    //?}

    /**
     * @author Starlev
     * @reason Eliminates ReferenceImmutableList wrapper; specializes 0, 1, and 2-element lookups into direct comparisons.
     */
    //? if >=1.21 {
    @Overwrite
    public static <T> ToIntFunction<T> createIndexIdentityLookup(List<T> list) {
        int size = list.size();
        if (size == 0) {
            return t -> -1;
        }
        if (size == 1) {
            T val0 = list.get(0);
            return t -> t == val0 ? 0 : -1;
        }
        if (size == 2) {
            T val0 = list.get(0);
            T val1 = list.get(1);
            return t -> t == val0 ? 0 : (t == val1 ? 1 : -1);
        }
        if (size < 8) {
            Object[] array = list.toArray();
            return t -> {
                for (int j = 0; j < array.length; ++j) {
                    if (array[j] == t) {
                        return j;
                    }
                }
                return -1;
            };
        }
        Reference2IntOpenHashMap<T> map = new Reference2IntOpenHashMap<>(size);
        map.defaultReturnValue(-1);
        for (int j = 0; j < size; ++j) {
            map.put(list.get(j), j);
        }
        return map;
    }
    //?}

    /**
     * @author Starlev
     * @reason Specializes 0, 1, and 2-element equality lookups; avoids method reference allocation and list traversal.
     */
    @Overwrite
    public static <T> ToIntFunction<T> createIndexLookup(List<T> list) {
        int size = list.size();
        if (size == 0) {
            return t -> -1;
        }
        if (size == 1) {
            T val0 = list.get(0);
            return t -> Objects.equals(t, val0) ? 0 : -1;
        }
        if (size == 2) {
            T val0 = list.get(0);
            T val1 = list.get(1);
            return t -> Objects.equals(t, val0) ? 0 : (Objects.equals(t, val1) ? 1 : -1);
        }
        if (size < 8) {
            Objects.requireNonNull(list);
            return list::indexOf;
        }
        Object2IntOpenHashMap<T> map = new Object2IntOpenHashMap<>(size);
        map.defaultReturnValue(-1);
        for (int j = 0; j < size; ++j) {
            map.put(list.get(j), j);
        }
        return map;
    }

    /**
     * @author Starlev
     * @reason Eliminates stream/lambda collector in sequence future; fast-paths already completed futures.
     */
    @Overwrite
    public static <V> CompletableFuture<List<V>> sequence(List<? extends CompletableFuture<V>> list) {
        int size = list.size();
        if (size == 0) {
            return CompletableFuture.completedFuture(List.of());
        }
        if (size == 1) {
            //? if >=1.21.11 {
            return list.get(0).thenApply(ObjectLists::singleton);
            //?} else {
            /*return ((CompletableFuture<V>) list.get(0)).thenApply(List::of);
            *///?}
        }
        boolean allDone = true;
        for (int i = 0; i < size; ++i) {
            if (!list.get(i).isDone()) {
                allDone = false;
                break;
            }
        }
        if (allDone) {
            ObjectArrayList<V> result = new ObjectArrayList<>(size);
            for (int i = 0; i < size; ++i) {
                result.add(list.get(i).join());
            }
            return CompletableFuture.completedFuture(result);
        }
        CompletableFuture<?>[] array = list.toArray(new CompletableFuture[size]);
        return CompletableFuture.allOf(array).thenApply(v -> {
            ObjectArrayList<V> result = new ObjectArrayList<>(size);
            for (int i = 0; i < size; ++i) {
                result.add(list.get(i).join());
            }
            return result;
        });
    }

    /**
     * @author Starlev
     * @reason Direct primitive array swapping during shuffle for ObjectArrayList; bypasses virtual get/set and bounds checking (legacy 1.20.1).
     */
    //? if <1.21 {
    @Overwrite
    public static <T> void shuffle(ObjectArrayList<T> list, RandomSource randomsource) {
        int size = list.size();
        if (size <= 1) {
            return;
        }
        Object[] elements = list.elements();
        for (int j = size; j > 1; --j) {
            int k = randomsource.nextInt(j);
            Object tmp = elements[j - 1];
            elements[j - 1] = elements[k];
            elements[k] = tmp;
        }
    }
    //?}
    //? if >=1.21 {
    @Overwrite
    public static <T> void shuffle(List<T> list, RandomSource randomsource) {
        int size = list.size();
        if (size <= 1) {
            return;
        }
        if (list instanceof ObjectArrayList<T> oal) {
            Object[] elements = oal.elements();
            for (int j = size; j > 1; --j) {
                int k = randomsource.nextInt(j);
                Object tmp = elements[j - 1];
                elements[j - 1] = elements[k];
                elements[k] = tmp;
            }
            return;
        }
        for (int j = size; j > 1; --j) {
            int k = randomsource.nextInt(j);
            list.set(j - 1, list.set(k, list.get(j - 1)));
        }
    }
    //?}

    /**
     * @author Starlev
     * @reason Direct primitive int array swapping during shuffle; bypasses virtual get/set and bounds checking.
     */
    @Overwrite
    public static IntArrayList toShuffledList(IntStream intstream, RandomSource randomsource) {
        int[] array = intstream.toArray();
        int size = array.length;
        for (int j = size; j > 1; --j) {
            int k = randomsource.nextInt(j);
            int tmp = array[j - 1];
            array[j - 1] = array[k];
            array[k] = tmp;
        }
        return IntArrayList.wrap(array);
    }

    /**
     * @author Starlev
     * @reason Specializes single cursor steps (j == 1 or -1) in text fields; avoids loop overhead for non-surrogate text.
     */
    @Overwrite
    public static int offsetByCodepoints(String s, int i, int j) {
        int len = s.length();
        if (j == 0) {
            return i;
        }
        if (j == 1) {
            if (i >= len) {
                return i;
            }
            char c = s.charAt(i++);
            if (Character.isHighSurrogate(c) && i < len && Character.isLowSurrogate(s.charAt(i))) {
                ++i;
            }
            return i;
        }
        if (j == -1) {
            if (i <= 0) {
                return i;
            }
            char c = s.charAt(--i);
            if (Character.isLowSurrogate(c) && i > 0 && Character.isHighSurrogate(s.charAt(i - 1))) {
                --i;
            }
            return i;
        }
        if (j > 0) {
            for (int l = 0; i < len && l < j; ++l) {
                if (Character.isHighSurrogate(s.charAt(i++)) && i < len && Character.isLowSurrogate(s.charAt(i))) {
                    ++i;
                }
            }
        } else {
            for (int l = j; i > 0 && l < 0; ++l) {
                --i;
                if (Character.isLowSurrogate(s.charAt(i)) && i > 0 && Character.isHighSurrogate(s.charAt(i - 1))) {
                    --i;
                }
            }
        }
        return i;
    }

    /**
     * @author Starlev
     * @reason Returns cached constants singleton predicate; eliminates lambda allocation.
     */
    //? if >=1.21.4 {
    @SuppressWarnings("unchecked")
    @Overwrite
    public static <T> Predicate<T> allOf() {
        return (Predicate<T>) lomka$ALWAYS_TRUE;
    }

    @SuppressWarnings("unchecked")
    @Overwrite
    public static <T> Predicate<T> anyOf() {
        return (Predicate<T>) lomka$ALWAYS_FALSE;
    }
    //?}
}
