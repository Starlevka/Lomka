package set.starl.injection.mixins.network;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.concurrent.Semaphore;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.nbt.NbtIo;
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
	private static final int LOMKA$PARALLELISM = set.starl.Lomka.CONFIG.nbt.ioParallelism;

	@Unique
	private static final Semaphore LOMKA$GUARD = LOMKA$PARALLELISM > 0 ? new Semaphore(Math.min(64, Math.max(1, LOMKA$PARALLELISM))) : null;

	@Unique
	private static final boolean LOMKA$READ_COMPRESSED_PATH_CACHE = set.starl.Lomka.CONFIG.nbt.cache.readCompressedPath;

	@Unique
	private static final int LOMKA$READ_COMPRESSED_PATH_CACHE_MAX_ENTRIES = set.starl.Lomka.CONFIG.nbt.cache.maxEntries;

	@Unique
	private static final long LOMKA$READ_COMPRESSED_PATH_CACHE_MAX_BYTES = set.starl.Lomka.CONFIG.nbt.cache.maxBytes;

	@Unique
	private static final LinkedHashMap<String, Object[]> LOMKA$READ_COMPRESSED_PATH_CACHE_MAP = new LinkedHashMap<>(128, 0.75f, true);

	@Unique
	private static long LOMKA$READ_COMPRESSED_PATH_CACHE_BYTES = 0L;

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
		if (guard == null || RenderSystem.isOnRenderThread()) {
			return; // Let vanilla run synchronously without blocking the render thread.
		}

		guard.acquireUninterruptibly();
		try {
			try (DataInputStream dis = createDecompressorStream(in)) {
				cir.setReturnValue(read(dis, accounter));
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
		if (guard == null || RenderSystem.isOnRenderThread()) {
			return; // Bypass write throttle if on render thread
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

		Object[] cached;
		synchronized (LOMKA$READ_COMPRESSED_PATH_CACHE_MAP) {
			cached = LOMKA$READ_COMPRESSED_PATH_CACHE_MAP.get(key);
			if (cached == null) {
				return;
			}
			long cachedMtime = ((Long)cached[1]).longValue();
			long cachedSize = ((Long)cached[2]).longValue();
			if (cachedMtime != mtime || cachedSize != size) {
				LOMKA$READ_COMPRESSED_PATH_CACHE_MAP.remove(key);
				LOMKA$READ_COMPRESSED_PATH_CACHE_BYTES -= cachedSize;
				return;
			}
		}

		CompoundTag tag = (CompoundTag)cached[0];
		cir.setReturnValue(tag.copy());
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

		CompoundTag cachedTag = tag.copy();
		synchronized (LOMKA$READ_COMPRESSED_PATH_CACHE_MAP) {
			Object[] existing = LOMKA$READ_COMPRESSED_PATH_CACHE_MAP.put(key, new Object[] { cachedTag, Long.valueOf(mtime), Long.valueOf(size) });
			if (existing != null) {
				LOMKA$READ_COMPRESSED_PATH_CACHE_BYTES -= ((Long)existing[2]).longValue();
			}
			LOMKA$READ_COMPRESSED_PATH_CACHE_BYTES += size;
			lomka$evictReadCompressedPathCache();
		}
	}

	@Unique
	private static void lomka$evictReadCompressedPathCache() {
		int maxEntries = LOMKA$READ_COMPRESSED_PATH_CACHE_MAX_ENTRIES;
		long maxBytes = LOMKA$READ_COMPRESSED_PATH_CACHE_MAX_BYTES;
		if ((maxEntries > 0 && LOMKA$READ_COMPRESSED_PATH_CACHE_MAP.size() <= maxEntries) && (maxBytes <= 0L || LOMKA$READ_COMPRESSED_PATH_CACHE_BYTES <= maxBytes)) {
			return;
		}

		Iterator<Object[]> it = LOMKA$READ_COMPRESSED_PATH_CACHE_MAP.values().iterator();
		while (it.hasNext() && ((maxEntries > 0 && LOMKA$READ_COMPRESSED_PATH_CACHE_MAP.size() > maxEntries) || (maxBytes > 0L && LOMKA$READ_COMPRESSED_PATH_CACHE_BYTES > maxBytes))) {
			Object[] entry = it.next();
			it.remove();
			LOMKA$READ_COMPRESSED_PATH_CACHE_BYTES -= ((Long)entry[2]).longValue();
		}
	}
}
