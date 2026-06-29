package lomka.starl.mixins.com.mojang.blaze3d.vertex;

import com.mojang.blaze3d.vertex.ByteBufferBuilder;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(ByteBufferBuilder.class)
public abstract class MixinByteBufferBuilder {

    @Shadow long pointer;
    //? if >=1.21.9 {
    @Shadow private long capacity;
    @Shadow private long writeOffset;

    @Shadow protected abstract void ensureCapacity(long i);
    //?} else {
    /*@Shadow private int capacity;
    @Shadow private int writeOffset;

    @Shadow protected abstract void ensureCapacity(int i);*/
    //?}

    /**
     * @author Starlev
     * @reason Removes Math.addExact() overhead (branching/exception checks) in the hottest path of the renderer.
     *         A buffer size or pointer will never exceed Long.MAX_VALUE (9 Exabytes), making overflow checks useless.
     *         Also manually inlines the capacity check to avoid method call overhead on the fast path.
     */
    @Overwrite
    public long reserve(int i) {
        //? if >=1.21.9 {
        long currentOffset = this.writeOffset;
        long requiredCapacity = currentOffset + i;
        //?} else {
        /*int currentOffset = this.writeOffset;
        int requiredCapacity = currentOffset + i;*/
        //?}

        if (requiredCapacity > this.capacity) {
            this.ensureCapacity(requiredCapacity);
        }

        this.writeOffset = requiredCapacity;
        
        return this.pointer + currentOffset;
    }
}