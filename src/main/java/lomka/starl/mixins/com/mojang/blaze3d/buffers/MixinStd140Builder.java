package lomka.starl.mixins.com.mojang.blaze3d.buffers;

import com.mojang.blaze3d.buffers.Std140Builder;
import java.nio.ByteBuffer;
import org.joml.Matrix4fc;
import org.joml.Vector2fc;
import org.joml.Vector2ic;
import org.joml.Vector3fc;
import org.joml.Vector3ic;
import org.joml.Vector4fc;
import org.joml.Vector4ic;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

/**
 * Optimizes std140 uniform buffer serialization by replacing division-based
 * alignments with single-cycle power-of-two bitwise masks and batching multiple
 * relative position mutations into single absolute-indexed direct writes.
 */
@Mixin(Std140Builder.class)
public abstract class MixinStd140Builder {

    @Shadow @Final private ByteBuffer buffer;
    @Shadow @Final private int start;

    @Overwrite
    public Std140Builder align(int align) {
        int currentPos = this.buffer.position();
        int offset = currentPos - this.start;
        int alignedOffset = (offset + (align - 1)) & -align;

        if (alignedOffset != offset) {
            this.buffer.position(this.start + alignedOffset);
        }
        return (Std140Builder) (Object) this;
    }

    @Overwrite
    public Std140Builder putFloat(float f) {
        int pos = this.buffer.position();
        int offset = pos - this.start;
        int target = this.start + ((offset + 3) & -4);
        this.buffer.putFloat(target, f);
        this.buffer.position(target + 4);
        return (Std140Builder) (Object) this;
    }

    @Overwrite
    public Std140Builder putInt(int i) {
        int pos = this.buffer.position();
        int offset = pos - this.start;
        int target = this.start + ((offset + 3) & -4);
        this.buffer.putInt(target, i);
        this.buffer.position(target + 4);
        return (Std140Builder) (Object) this;
    }

    @Overwrite
    public Std140Builder putVec2(float f, float f1) {
        int pos = this.buffer.position();
        int offset = pos - this.start;
        int target = this.start + ((offset + 7) & -8);
        this.buffer.putFloat(target, f);
        this.buffer.putFloat(target + 4, f1);
        this.buffer.position(target + 8);
        return (Std140Builder) (Object) this;
    }

    @Overwrite
    public Std140Builder putVec2(Vector2fc vec) {
        int pos = this.buffer.position();
        int offset = pos - this.start;
        int target = this.start + ((offset + 7) & -8);
        vec.get(target, this.buffer);
        this.buffer.position(target + 8);
        return (Std140Builder) (Object) this;
    }

    @Overwrite
    public Std140Builder putIVec2(int i, int j) {
        int pos = this.buffer.position();
        int offset = pos - this.start;
        int target = this.start + ((offset + 7) & -8);
        this.buffer.putInt(target, i);
        this.buffer.putInt(target + 4, j);
        this.buffer.position(target + 8);
        return (Std140Builder) (Object) this;
    }

    @Overwrite
    public Std140Builder putIVec2(Vector2ic vec) {
        int pos = this.buffer.position();
        int offset = pos - this.start;
        int target = this.start + ((offset + 7) & -8);
        vec.get(target, this.buffer);
        this.buffer.position(target + 8);
        return (Std140Builder) (Object) this;
    }

    @Overwrite
    public Std140Builder putVec3(float f, float f1, float f2) {
        int pos = this.buffer.position();
        int offset = pos - this.start;
        int target = this.start + ((offset + 15) & -16);
        this.buffer.putFloat(target, f);
        this.buffer.putFloat(target + 4, f1);
        this.buffer.putFloat(target + 8, f2);
        this.buffer.position(target + 16);
        return (Std140Builder) (Object) this;
    }

    @Overwrite
    public Std140Builder putVec3(Vector3fc vec) {
        int pos = this.buffer.position();
        int offset = pos - this.start;
        int target = this.start + ((offset + 15) & -16);
        vec.get(target, this.buffer);
        this.buffer.position(target + 16);
        return (Std140Builder) (Object) this;
    }

    @Overwrite
    public Std140Builder putIVec3(int i, int j, int k) {
        int pos = this.buffer.position();
        int offset = pos - this.start;
        int target = this.start + ((offset + 15) & -16);
        this.buffer.putInt(target, i);
        this.buffer.putInt(target + 4, j);
        this.buffer.putInt(target + 8, k);
        this.buffer.position(target + 16);
        return (Std140Builder) (Object) this;
    }

    @Overwrite
    public Std140Builder putIVec3(Vector3ic vec) {
        int pos = this.buffer.position();
        int offset = pos - this.start;
        int target = this.start + ((offset + 15) & -16);
        vec.get(target, this.buffer);
        this.buffer.position(target + 16);
        return (Std140Builder) (Object) this;
    }

    @Overwrite
    public Std140Builder putVec4(float f, float f1, float f2, float f3) {
        int pos = this.buffer.position();
        int offset = pos - this.start;
        int target = this.start + ((offset + 15) & -16);
        this.buffer.putFloat(target, f);
        this.buffer.putFloat(target + 4, f1);
        this.buffer.putFloat(target + 8, f2);
        this.buffer.putFloat(target + 12, f3);
        this.buffer.position(target + 16);
        return (Std140Builder) (Object) this;
    }

    @Overwrite
    public Std140Builder putVec4(Vector4fc vec) {
        int pos = this.buffer.position();
        int offset = pos - this.start;
        int target = this.start + ((offset + 15) & -16);
        vec.get(target, this.buffer);
        this.buffer.position(target + 16);
        return (Std140Builder) (Object) this;
    }

    @Overwrite
    public Std140Builder putIVec4(int i, int j, int k, int l) {
        int pos = this.buffer.position();
        int offset = pos - this.start;
        int target = this.start + ((offset + 15) & -16);
        this.buffer.putInt(target, i);
        this.buffer.putInt(target + 4, j);
        this.buffer.putInt(target + 8, k);
        this.buffer.putInt(target + 12, l);
        this.buffer.position(target + 16);
        return (Std140Builder) (Object) this;
    }

    @Overwrite
    public Std140Builder putIVec4(Vector4ic vec) {
        int pos = this.buffer.position();
        int offset = pos - this.start;
        int target = this.start + ((offset + 15) & -16);
        vec.get(target, this.buffer);
        this.buffer.position(target + 16);
        return (Std140Builder) (Object) this;
    }

    @Overwrite
    public Std140Builder putMat4f(Matrix4fc mat) {
        int pos = this.buffer.position();
        int offset = pos - this.start;
        int target = this.start + ((offset + 15) & -16);
        mat.get(target, this.buffer);
        this.buffer.position(target + 64);
        return (Std140Builder) (Object) this;
    }
}