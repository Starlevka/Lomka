package set.starl.injection.mixins.network;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.Semaphore;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.nbt.NbtIo;
import set.starl.util.NbtCacheEntry;
import com.mojang.blaze3d.systems.RenderSystem;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(NbtIo.class)
public class NbtIoThrottleMixin {
    @Unique
    private static final int LOMKA$PARALLELISM = set.starl.LomkaCore.CONFIG.nbt.ioParallelism;

    @Unique
    private static final Semaphore LOMKA$GUARD = LOMKA$PARALLELISM > 0 ? new Semaphore(Math.min(64, Math.max(1, LOMKA$PARALLELISM))) : null;

    @Unique
    private static final boolean LOMKA$READ_COMPRESSED_PATH_CACHE = set.starl.LomkaCore.CONFIG.nbt.cache.readCompressedPath;

    @Unique
    private static final int LOMKA$READ_COMPRESSED_PATH_CACHE_MAX_ENTRIES = set.starl.LomkaCore.CONFIG.nbt.cache.maxEntries;

    @Unique
    private static final long LOMKA$READ_COMPRESSED_PATH_CACHE_MAX_BYTES = set.starl.LomkaCore.CONFIG.nbt.cache.maxBytes;

    /**
     * LRU cache backed by LinkedHashMap with accessOrder=true.
     * Wrapped in Collections.synchronizedMap for thread safety.
     * Eviction is O(1) — just remove the eldest entry (the iterator head).
     */
    @Unique
    private static final Map<String, NbtCacheEntry> LOMKA$CACHE = lomka$buildCache();

    @Unique
    private static Map<String, NbtCacheEntry> lomka$buildCache() {
        int capacity = Math.max(16, LOMKA$READ_COMPRESSED_PATH_CACHE_MAX_ENTRIES > 0 ? LOMKA$READ_COMPRESSED_PATH_CACHE_MAX_ENTRIES : 512);
        // accessOrder=true → access moves entry to tail, eldest entry is at head
        LinkedHashMap<String, NbtCacheEntry> lru = new LinkedHashMap<>(capacity, 0.75f, true);
        return Collections.synchronizedMap(lru);
    }

	@Shadow
	private static DataInputStream createDecompressorStream(final InputStream in) throws IOException {
		throw new AssertionError();
	}

	@Shadow
	private static DataOutputStream createCompressorStream(final OutputStream out) throws IOException {
		throw new AssertionError();
	}

	@Shadow
	public static CompoundTag read(final java.io.DataInput input, final NbtAccounter accounter) throws IOException {
		throw new AssertionError();
	}

	@Shadow
	public static void write(final CompoundTag tag, final java.io.DataOutput output) throws IOException {
		throw new AssertionError();
	}

    @Inject(
        method = "readCompressed(Ljava/io/InputStream;Lnet/minecraft/nbt/NbtAccounter;)Lnet/minecraft/nbt/CompoundTag;",
        at = @At("HEAD"),
        cancellable = true
    )
    private static void lomka$readCompressedThrottled(final InputStream in, final NbtAccounter accounter, final CallbackInfoReturnable<CompoundTag> cir) throws IOException {
        Semaphore guard = LOMKA$GUARD;
        if (guard == null || RenderSystem.isOnRenderThread() || Thread.currentThread() instanceof io.netty.util.concurrent.FastThreadLocalThread) {
            return;
        }

        guard.acquireUninterruptibly();
        try {
            try (DataInputStream dis = createDecompressorStream(in)) {
                cir.setReturnValue(read(dis, accounter));
                cir.cancel();
            }
        } finally {
            guard.release();
        }
    }

	@Inject(
		method = "writeCompressed(Lnet/minecraft/nbt/CompoundTag;Ljava/io/OutputStream;)V",
		at = @At("HEAD"),
		cancellable = true
	)
	private static void lomka$writeCompressedThrottled(final CompoundTag tag, final OutputStream out, final CallbackInfo ci) throws IOException {
		Semaphore guard = LOMKA$GUARD;
		if (guard == null || RenderSystem.isOnRenderThread() || Thread.currentThread() instanceof io.netty.util.concurrent.FastThreadLocalThread) {
			return;
		}

		guard.acquireUninterruptibly();
		try {
			try (DataOutputStream dos = createCompressorStream(out)) {
				write(tag, (java.io.DataOutput)dos);
			}
		} finally {
			guard.release();
		}
		ci.cancel();
	}

	@Inject(
		method = "readCompressed(Ljava/nio/file/Path;Lnet/minecraft/nbt/NbtAccounter;)Lnet/minecraft/nbt/CompoundTag;",
		at = @At("HEAD"),
		cancellable = true
	)
    private static void lomka$readCompressedPathCached(final Path file, final NbtAccounter accounter, final CallbackInfoReturnable<CompoundTag> cir) {
        if (!LOMKA$READ_COMPRESSED_PATH_CACHE) {
            return;
        }
        if (LOMKA$READ_COMPRESSED_PATH_CACHE_MAX_ENTRIES <= 0 || LOMKA$READ_COMPRESSED_PATH_CACHE_MAX_BYTES == 0L) {
            return;
        }

        String key;
        long size;
        long mtime;
        try {
            key = file.toAbsolutePath().normalize().toString();
            size = Files.size(file);
            mtime = Files.getLastModifiedTime(file).toMillis();
        } catch (Exception ignored) {
            return;
        }

        NbtCacheEntry cached = LOMKA$CACHE.get(key);
        if (cached == null) {
            return;
        }

        // File changed on disk — evict stale entry
        if (cached.mtime() != mtime || cached.size() != size) {
            LOMKA$CACHE.remove(key);
            return;
        }

        cir.setReturnValue(cached.tag().copy());
        cir.cancel();
    }

    @Inject(
        method = "readCompressed(Ljava/nio/file/Path;Lnet/minecraft/nbt/NbtAccounter;)Lnet/minecraft/nbt/CompoundTag;",
        at = @At("RETURN")
    )
    private static void lomka$storeReadCompressedPathCached(final Path file, final NbtAccounter accounter, final CallbackInfoReturnable<CompoundTag> cir) {
        if (!LOMKA$READ_COMPRESSED_PATH_CACHE) {
            return;
        }
        if (LOMKA$READ_COMPRESSED_PATH_CACHE_MAX_ENTRIES <= 0 || LOMKA$READ_COMPRESSED_PATH_CACHE_MAX_BYTES == 0L) {
            return;
        }

        CompoundTag tag = cir.getReturnValue();
        if (tag == null) {
            return;
        }

        String key;
        long size;
        long mtime;
        try {
            key = file.toAbsolutePath().normalize().toString();
            size = Files.size(file);
            mtime = Files.getLastModifiedTime(file).toMillis();
        } catch (Exception ignored) {
            return;
        }

        if (size <= 0L) {
            return;
        }
        if (LOMKA$READ_COMPRESSED_PATH_CACHE_MAX_BYTES > 0L && size > LOMKA$READ_COMPRESSED_PATH_CACHE_MAX_BYTES) {
            return;
        }

        // LRU eviction: remove eldest entries if cache is full — O(1) per eviction
        Map<String, NbtCacheEntry> cache = LOMKA$CACHE;
        synchronized (cache) {
            while (LOMKA$READ_COMPRESSED_PATH_CACHE_MAX_ENTRIES > 0 && cache.size() >= LOMKA$READ_COMPRESSED_PATH_CACHE_MAX_ENTRIES) {
                Iterator<String> it = cache.keySet().iterator();
                if (it.hasNext()) {
                    it.next();
                    it.remove();
                } else {
                    break;
                }
            }
            cache.put(key, new NbtCacheEntry(tag.copy(), mtime, size));
        }
    }
}
