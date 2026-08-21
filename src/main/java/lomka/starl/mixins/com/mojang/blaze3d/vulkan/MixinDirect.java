package lomka.starl.mixins.com.mojang.blaze3d.vulkan;

import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.vulkan.VulkanDevice;
import com.mojang.blaze3d.vulkan.VulkanGpuBuffer;
import com.mojang.blaze3d.vulkan.VulkanUtils;
import java.nio.ByteBuffer;
import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;
import org.lwjgl.util.vma.Vma;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(VulkanGpuBuffer.Direct.class)
public abstract class MixinDirect extends VulkanGpuBuffer {

    @Shadow @Final protected VulkanDevice device;
    @Shadow private long    vmaAllocation;
    @Shadow private int     mappingRefCount;
    @Shadow private boolean closed;

    @Unique private static final Runnable lomka$NO_OP = () -> {};
    @Unique private long lomka$persistentAddress;

    public MixinDirect(long vkBuffer, int usage, long size) {
        super(vkBuffer, usage, size);
    }

    /**
     * @author Starlev
     * @reason Establish persistent mapping for host-visible buffers at creation time.
     *         Vanilla sets VMA_ALLOCATION_CREATE_MAPPED_BIT (flags | 2048) but still
     *         calls vmaMapMemory on every map() call. We cache the mapped address once
     *         here so subsequent map() calls skip the driver round-trip entirely.
     *         Only applies when usage contains READ or WRITE bits (host-visible).
     */
    @Inject(method = "<init>", at = @At("TAIL"))
    private void lomka$setupPersistentMapping(
            VulkanDevice device, Object label, int usage,
            long size, boolean forceHostVisibleAllocation, CallbackInfo ci) {
        if (VulkanUtils.hasAnyBit(usage, 3)) {
            MemoryStack stack = MemoryStack.stackPush();
            try {
                PointerBuffer pointer = stack.callocPointer(1);
                if (Vma.vmaMapMemory(device.vma(), this.vmaAllocation, pointer) == 0) {
                    this.lomka$persistentAddress = pointer.get(0);
                }
            } finally {
                stack.close();
            }
        }
    }

    /**
     * @author Starlev
     * @reason Zero-allocation persistent-mapped fast path. When persistentAddress != 0,
     *         we return a direct memory slice with a static no-op Runnable — no heap
     *         allocation, no vmaMapMemory/vmaUnmapMemory driver calls. Falls back to
     *         lambda-based vanilla path for non-host-visible buffers, replacing the
     *         anonymous class (which had an instance initializer calling
     *         Objects.requireNonNull(Direct.this)) with a simpler lambda.
     */
    @Overwrite
    public GpuBufferSlice.MappedView map(long offset, long length, boolean read, boolean write) {
        if (this.closed) {
            throw new IllegalStateException("Buffer already closed");
        }
        if (!read && !write) {
            throw new IllegalArgumentException("At least read or write must be true");
        }
        if (read && (this.usage() & 1) == 0) {
            throw new IllegalStateException("Buffer is not readable");
        }
        if (write && (this.usage() & 2) == 0) {
            throw new IllegalStateException("Buffer is not writable");
        }
        if (offset + length > this.size()) {
            throw new IllegalArgumentException("Cannot map more data than this buffer can hold (attempting to map "
                    + length + " bytes at offset " + offset + " from " + this.size() + " size buffer)");
        }
        if (length > Integer.MAX_VALUE) {
            throw new IllegalArgumentException("Mapping buffer slice larger than 2GB is not supported");
        }
        if (offset < 0L || length < 0L) {
            throw new IllegalArgumentException("Offset or length must be positive integer values");
        }

        if (this.lomka$persistentAddress != 0L) {
            ByteBuffer byteBuffer = MemoryUtil.memByteBuffer(this.lomka$persistentAddress + offset, (int) length);
            return new GpuBufferSlice.MappedView(this.slice(offset, length), byteBuffer, lomka$NO_OP);
        }

        ++this.mappingRefCount;
        MemoryStack stack = MemoryStack.stackPush();
        try {
            PointerBuffer pointer = stack.callocPointer(1);
            VulkanUtils.crashIfFailure(this.device,
                    Vma.vmaMapMemory(this.device.vma(), this.vmaAllocation, pointer),
                    "Failed to map buffer");
            ByteBuffer byteBuffer = MemoryUtil.memByteBuffer(pointer.get(0) + offset, (int) length);

            Runnable closeAction = () -> {
                --this.mappingRefCount;
                Vma.vmaUnmapMemory(this.device.vma(), this.vmaAllocation);
            };
            return new GpuBufferSlice.MappedView(this.slice(offset, length), byteBuffer, closeAction);
        } finally {
            stack.close();
        }
    }

    /**
     * @author Starlev
     * @reason Explicitly unmap persistent memory before vmaDestroyBuffer to avoid
     *         VMA validation layer warnings. Vanilla relies on implicit unmap inside
     *         vmaDestroyBuffer, but explicit cleanup is safer for debug/validation builds.
     */
    @Inject(method = "destroy", at = @At("HEAD"))
    private void lomka$unmapPersistent(CallbackInfo ci) {
        if (this.lomka$persistentAddress != 0L) {
            Vma.vmaUnmapMemory(this.device.vma(), this.vmaAllocation);
            this.lomka$persistentAddress = 0L;
        }
    }
}