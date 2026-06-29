package lomka.starl.mixins.net.minecraft.util;

import java.util.NoSuchElementException;
import java.util.Objects;

import net.minecraft.util.ArrayListDeque;
import org.spongepowered.asm.mixin.Dynamic;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(ArrayListDeque.class)
public class MixinArrayListDeque<T> {

    @Shadow private Object[] contents;
    @Shadow private int head;
    @Shadow private int size;

    @Shadow(remap = false) @Dynamic private transient int modCount;

    @Unique
    private void lomka$incModCount() {
        this.modCount++;
    }

    /**
     * Rounds the initial capacity up to the nearest power of two.
     * This is a mandatory invariant: bitwise AND masking only works correctly
     * when the backing array length is a power of two.
     */
    @ModifyVariable(method = "<init>(I)V", at = @At("HEAD"), argsOnly = true)
    private int lomka$roundToPowerOfTwo(int capacity) {
        return capacity <= 1 ? 1 : Integer.highestOneBit(capacity - 1) << 1;
    }

    /**
     * @author Starlev
     * @reason Replace modulo with a single bitwise AND operation.
     * Requires contents.length to be a power of two.
     */
    @Overwrite
    private int getIndex(int i) {
        return (i + this.head) & (this.contents.length - 1);
    }

    /**
     * @author Starlev
     * @reason Double capacity instead of 1.5x growth to preserve the power-of-two invariant.
     * Uses System.arraycopy for bulk data migration instead of per-element virtual dispatch.
     *
     * Defensive head normalization: vanilla remove(int) at i==0 increments head without
     * masking, which can push it exactly to contents.length. Applying (head & (oldCap - 1))
     * corrects this without branching and prevents NegativeArraySizeException in arraycopy.
     */
    @Overwrite
    private void grow() {
        int oldCap = this.contents.length;
        int newCap = oldCap << 1;
        if (newCap < 0) throw new IllegalStateException("Deque too big");

        Object[] newArr = new Object[newCap];
        int h = this.head & (oldCap - 1);
        int rightLen = oldCap - h;
        int copyCount = Math.min(this.size, rightLen);

        System.arraycopy(this.contents, h, newArr, 0, copyCount);
        if (this.size > copyCount) {
            System.arraycopy(this.contents, 0, newArr, copyCount, this.size - copyCount);
        }

        this.head = 0;
        this.contents = newArr;
    }

    /**
     * @author Starlev
     * @reason O(1) prepend. Bitwise AND correctly wraps a negative head index.
     */
    @Overwrite
    public void addFirst(T t0) {
        Objects.requireNonNull(t0);
        if (this.size == this.contents.length) this.grow();
        this.head = (this.head - 1) & (this.contents.length - 1);
        this.contents[this.head] = t0;
        ++this.size;
        lomka$incModCount();
    }

    /**
     * @author Starlev
     * @reason O(1) append. Skips the branching overhead of the generic add() path.
     */
    @Overwrite
    public void addLast(T t0) {
        Objects.requireNonNull(t0);
        if (this.size == this.contents.length) this.grow();
        this.contents[(this.head + this.size) & (this.contents.length - 1)] = t0;
        ++this.size;
        lomka$incModCount();
    }

    /**
     * @author Starlev
     * @reason O(1) removal from front. Properly masks head after increment.
     */
    @SuppressWarnings("unchecked")
    @Overwrite
    public T removeFirst() {
        if (this.size == 0) throw new NoSuchElementException();
        int h = this.head;
        T obj = (T) this.contents[h];
        this.contents[h] = null;
        this.head = (h + 1) & (this.contents.length - 1);
        --this.size;
        lomka$incModCount();
        return obj;
    }

    /**
     * @author Starlev
     * @reason O(1) removal from back. Direct index calculation without delegating to remove().
     */
    @SuppressWarnings("unchecked")
    @Overwrite
    public T removeLast() {
        if (this.size == 0) throw new NoSuchElementException();
        int idx = (this.head + this.size - 1) & (this.contents.length - 1);
        T obj = (T) this.contents[idx];
        this.contents[idx] = null;
        --this.size;
        lomka$incModCount();
        return obj;
    }

    /**
     * @author Starlev
     * @reason Fixes a bug in the vanilla implementation: at i==0 vanilla does ++head without
     * masking, which breaks the power-of-two invariant and eventually causes
     * NegativeArraySizeException inside grow() when the deque fills up again.
     * Also eliminates the redundant get() call in the shift loop, replacing it with
     * direct contents access via getIndex() to reduce virtual dispatch overhead.
     */
    @SuppressWarnings("unchecked")
    @Overwrite
    public T remove(int i) {
        if (i < 0 || i >= this.size) throw new IndexOutOfBoundsException(i);
        int j = this.getIndex(i);
        T obj = (T) this.contents[j];

        if (i == 0) {
            this.contents[j] = null;
            this.head = (this.head + 1) & (this.contents.length - 1);
        } else if (i == this.size - 1) {
            this.contents[j] = null;
        } else {
            for (int k = i + 1; k < this.size; ++k) {
                this.contents[this.getIndex(k - 1)] = this.contents[this.getIndex(k)];
            }
            this.contents[this.getIndex(this.size - 1)] = null;
        }

        lomka$incModCount();
        --this.size;
        return obj;
    }
}