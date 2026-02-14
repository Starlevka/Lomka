package set.starl.mixin.cache;

import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.platform.TextureUtil;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import net.minecraft.client.renderer.texture.MipmapGenerator;
import net.minecraft.client.renderer.texture.MipmapStrategy;
import net.minecraft.resources.Identifier;
import org.lwjgl.system.MemoryUtil;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(MipmapGenerator.class)
public class MipmapGeneratorMixin {
	@Unique
	private static final Object LOMKA$LOCK = new Object();

	@Unique
	private static final long LOMKA$MAX_CACHE_BYTES = Long.parseLong(System.getProperty("lomka.mipmaps.cache.maxBytes", "67108864"));

	@Unique
	private static final int LOMKA$MAX_BASE_AREA = Integer.parseInt(System.getProperty("lomka.mipmaps.cache.maxBaseArea", "65536"));

	@Unique
	private static final boolean LOMKA$ENABLE = !"false".equalsIgnoreCase(System.getProperty("lomka.mipmaps.cache.enable", "true"));

	@Unique
	private static final LinkedHashMap<String, Object[]> LOMKA$CACHE = new LinkedHashMap<>(256, 0.75f, true);

	@Unique
	private static long LOMKA$CACHE_BYTES = 0L;

	@Unique
	private static final ThreadLocal<String> LOMKA$LAST_KEY = new ThreadLocal<>();

	@Inject(method = "generateMipLevels", at = @At("HEAD"), cancellable = true)
	private static void lomka$tryGetFromCache(
		final Identifier name,
		final NativeImage[] currentMips,
		final int newMipLevel,
		MipmapStrategy mipmapStrategy,
		final float alphaCutoffBias,
		final CallbackInfoReturnable<NativeImage[]> cir
	) {
		if (!LOMKA$ENABLE || LOMKA$MAX_CACHE_BYTES <= 0L) {
			return;
		}
		if (newMipLevel + 1 <= currentMips.length) {
			return;
		}

		int baseArea = currentMips[0].getWidth() * currentMips[0].getHeight();
		if (baseArea > LOMKA$MAX_BASE_AREA) {
			return;
		}

		if (mipmapStrategy == MipmapStrategy.AUTO) {
			mipmapStrategy = lomka$hasTransparentPixel(currentMips[0]) ? MipmapStrategy.CUTOUT : MipmapStrategy.MEAN;
		}

		long baseHash = lomka$hashImage(currentMips[0]);
		String key = lomka$key(name, baseHash, mipmapStrategy, Float.floatToIntBits(alphaCutoffBias), newMipLevel);
		LOMKA$LAST_KEY.set(key);

		Object[] entry;
		synchronized (LOMKA$LOCK) {
			entry = LOMKA$CACHE.get(key);
			if (entry != null) {
				((long[]) entry[1])[1] += 2L;
			}
		}

		if (entry == null) {
			return;
		}

		if (currentMips.length == 1 && !name.getPath().startsWith("item/")) {
			if (mipmapStrategy == MipmapStrategy.DARK_CUTOUT) {
				TextureUtil.fillEmptyAreasWithDarkColor(currentMips[0]);
			} else if (mipmapStrategy == MipmapStrategy.CUTOUT || mipmapStrategy == MipmapStrategy.STRICT_CUTOUT) {
				TextureUtil.solidify(currentMips[0]);
			}
		}

		NativeImage[] result = new NativeImage[newMipLevel + 1];
		result[0] = currentMips[0];

		try {
			NativeImage[] cachedLevels = (NativeImage[]) entry[0];
			for (int level = 1; level <= newMipLevel; level++) {
				if (level < currentMips.length) {
					result[level] = currentMips[level];
				} else {
					NativeImage cachedLevel = cachedLevels[level - 1];
					NativeImage copy = new NativeImage(cachedLevel.format(), cachedLevel.getWidth(), cachedLevel.getHeight(), false);
					copy.copyFrom(cachedLevel);
					result[level] = copy;
				}
			}
		} finally {
			synchronized (LOMKA$LOCK) {
				long[] meta = (long[]) entry[1];
				meta[1] -= 2L;
				if ((meta[1] >>> 1) <= 0L && (meta[1] & 1L) != 0L) {
					lomka$closeLevels((NativeImage[]) entry[0]);
				}
			}
		}

		LOMKA$LAST_KEY.remove();
		cir.setReturnValue(result);
	}

	@Inject(method = "generateMipLevels", at = @At("RETURN"))
	private static void lomka$storeToCache(
		final Identifier name,
		final NativeImage[] currentMips,
		final int newMipLevel,
		final MipmapStrategy mipmapStrategy,
		final float alphaCutoffBias,
		final CallbackInfoReturnable<NativeImage[]> cir
	) {
		if (!LOMKA$ENABLE || LOMKA$MAX_CACHE_BYTES <= 0L) {
			return;
		}
		String key = LOMKA$LAST_KEY.get();
		LOMKA$LAST_KEY.remove();
		if (key == null) {
			return;
		}

		NativeImage[] result = cir.getReturnValue();
		if (result == null || result.length < newMipLevel + 1) {
			return;
		}

		int baseArea = currentMips[0].getWidth() * currentMips[0].getHeight();
		if (baseArea > LOMKA$MAX_BASE_AREA) {
			return;
		}

		long bytes = 0L;
		NativeImage[] levels = new NativeImage[newMipLevel];
		for (int level = 1; level <= newMipLevel; level++) {
			NativeImage src = result[level];
			NativeImage copy = new NativeImage(src.format(), src.getWidth(), src.getHeight(), false);
			copy.copyFrom(src);
			levels[level - 1] = copy;
			bytes += (long) copy.getWidth() * (long) copy.getHeight() * (long) copy.format().components();
			if (bytes > LOMKA$MAX_CACHE_BYTES / 2L) {
				lomka$closeLevels(levels);
				return;
			}
		}

		synchronized (LOMKA$LOCK) {
			Object[] existing = LOMKA$CACHE.remove(key);
			if (existing != null) {
				long[] existingMeta = (long[]) existing[1];
				LOMKA$CACHE_BYTES -= existingMeta[0];
				if ((existingMeta[1] >>> 1) <= 0L) {
					lomka$closeLevels((NativeImage[]) existing[0]);
				} else {
					existingMeta[1] |= 1L;
				}
			}

			LOMKA$CACHE.put(key, new Object[] { levels, new long[] { bytes, 0L } });
			LOMKA$CACHE_BYTES += bytes;
			lomka$evictToSize();
		}
	}

	@Unique
	private static void lomka$evictToSize() {
		Iterator<Map.Entry<String, Object[]>> it = LOMKA$CACHE.entrySet().iterator();
		while (LOMKA$CACHE_BYTES > LOMKA$MAX_CACHE_BYTES && it.hasNext()) {
			Map.Entry<String, Object[]> eldest = it.next();
			Object[] entry = eldest.getValue();
			it.remove();
			long[] meta = (long[]) entry[1];
			LOMKA$CACHE_BYTES -= meta[0];
			if ((meta[1] >>> 1) <= 0L) {
				lomka$closeLevels((NativeImage[]) entry[0]);
			} else {
				meta[1] |= 1L;
			}
		}
	}

	@Unique
	private static void lomka$closeLevels(final NativeImage[] levels) {
		for (NativeImage level : levels) {
			if (level != null) {
				level.close();
			}
		}
	}

	@Unique
	private static long lomka$hashImage(final NativeImage image) {
		long hash = 1469598103934665603L;
		hash ^= image.getWidth();
		hash *= 1099511628211L;
		hash ^= image.getHeight();
		hash *= 1099511628211L;
		hash ^= image.format().components();
		hash *= 1099511628211L;

		if (image.format() != NativeImage.Format.RGBA) {
			return hash;
		}

		long ptr = image.getPointer();
		int pixelCount = image.getWidth() * image.getHeight();
		for (int i = 0; i < pixelCount; i++) {
			int v = MemoryUtil.memGetInt(ptr + (long) i * 4L);
			hash ^= (long) v & 0xffffffffL;
			hash *= 1099511628211L;
		}
		return hash;
	}

	@Unique
	private static boolean lomka$hasTransparentPixel(final NativeImage image) {
		if (image.format() != NativeImage.Format.RGBA) {
			for (int y = 0; y < image.getHeight(); y++) {
				for (int x = 0; x < image.getWidth(); x++) {
					if ((image.getPixel(x, y) >>> 24) == 0) {
						return true;
					}
				}
			}
			return false;
		}

		long ptr = image.getPointer();
		int pixelCount = image.getWidth() * image.getHeight();
		for (int i = 0; i < pixelCount; i++) {
			int abgr = MemoryUtil.memGetInt(ptr + (long) i * 4L);
			if ((abgr >>> 24) == 0) {
				return true;
			}
		}
		return false;
	}

	@Unique
	private static String lomka$key(
		final Identifier name,
		final long baseHash,
		final MipmapStrategy strategy,
		final int alphaCutoffBiasBits,
		final int mipLevel
	) {
		StringBuilder sb = new StringBuilder(name.toString().length() + 64);
		sb.append(name);
		sb.append('|');
		sb.append(baseHash);
		sb.append('|');
		sb.append(strategy.ordinal());
		sb.append('|');
		sb.append(alphaCutoffBiasBits);
		sb.append('|');
		sb.append(mipLevel);
		return sb.toString();
	}
}
