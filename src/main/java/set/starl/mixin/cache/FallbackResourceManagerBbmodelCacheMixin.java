package set.starl.mixin.cache;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.SequenceInputStream;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.PackResources;
import net.minecraft.server.packs.resources.FallbackResourceManager;
import net.minecraft.server.packs.resources.IoSupplier;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceMetadata;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.gen.Invoker;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(FallbackResourceManager.class)
public class FallbackResourceManagerBbmodelCacheMixin {
	@Unique
	private static final boolean LOMKA$BBMODEL_ENABLE = !"false".equalsIgnoreCase(System.getProperty("lomka.bbmodel.cache.enable", "true"));

	@Unique
	private static final long LOMKA$BBMODEL_MAX_TOTAL_BYTES = Long.parseLong(System.getProperty("lomka.bbmodel.cache.maxTotalBytes", "67108864"));

	@Unique
	private static final int LOMKA$BBMODEL_MAX_ENTRY_BYTES = Integer.parseInt(System.getProperty("lomka.bbmodel.cache.maxEntryBytes", "8388608"));

	@Unique
	private static final boolean LOMKA$SMALL_JSON_ENABLE = !"false".equalsIgnoreCase(System.getProperty("lomka.resources.smallCache.enable", "true"));

	@Unique
	private static final boolean LOMKA$SMALL_CACHE_SHADERS = !"false".equalsIgnoreCase(System.getProperty("lomka.resources.smallCache.shaders", "true"));

	@Unique
	private static final long LOMKA$SMALL_JSON_MAX_TOTAL_BYTES = Long.parseLong(System.getProperty("lomka.resources.smallCache.maxTotalBytes", "67108864"));

	@Unique
	private static final int LOMKA$SMALL_JSON_MAX_ENTRY_BYTES = Integer.parseInt(System.getProperty("lomka.resources.smallCache.maxEntryBytes", "2097152"));

	@Unique
	private static final Object LOMKA$BBMODEL_LOCK = new Object();

	@Unique
	private static final LinkedHashMap<String, byte[]> LOMKA$BBMODEL_CACHE = new LinkedHashMap<>(32, 0.75F, true);

	@Unique
	private static long LOMKA$BBMODEL_TOTAL_BYTES = 0L;

	@Unique
	private static final Object LOMKA$SMALL_JSON_LOCK = new Object();

	@Unique
	private static final LinkedHashMap<String, byte[]> LOMKA$SMALL_JSON_CACHE = new LinkedHashMap<>(64, 0.75F, true);

	@Unique
	private static long LOMKA$SMALL_JSON_TOTAL_BYTES = 0L;

	@Invoker("wrapForDebug")
	private static IoSupplier<InputStream> lomka$wrapForDebug(final Identifier location, final PackResources source, final IoSupplier<InputStream> resource) {
		throw new AssertionError();
	}

	@Inject(method = "createResource", at = @At("HEAD"), cancellable = true)
	private static void lomka$createResourceWithBbmodelCache(
		final PackResources source,
		final Identifier location,
		final IoSupplier<InputStream> resource,
		final IoSupplier<ResourceMetadata> metadata,
		final CallbackInfoReturnable<Resource> cir
	) {
		String path = location.getPath();
		if (LOMKA$BBMODEL_ENABLE && lomka$endsWithIgnoreCase(path, ".bbmodel")) {
			IoSupplier<InputStream> upstream = lomka$wrapForDebug(location, source, resource);
			String key = lomka$key(source, location);
			IoSupplier<InputStream> cached = () -> lomka$openCachedBbmodel(key, upstream);
			cir.setReturnValue(new Resource(source, cached, metadata));
			return;
		}

		if (LOMKA$SMALL_JSON_ENABLE && lomka$isSmallCachedCandidate(path)) {
			IoSupplier<InputStream> upstream = lomka$wrapForDebug(location, source, resource);
			String key = lomka$key(source, location);
			IoSupplier<InputStream> cached = () -> lomka$openCachedSmallJson(key, upstream);
			cir.setReturnValue(new Resource(source, cached, metadata));
		}
	}

	@Unique
	private static String lomka$key(final PackResources source, final Identifier location) {
		String packId = source.packId();
		Object known = source.knownPackInfo().orElse(null);
		String packInfo = known == null ? "" : known.toString();
		return packId + "|" + packInfo + "|" + location;
	}

	@Unique
	private static boolean lomka$endsWithIgnoreCase(final String value, final String suffix) {
		int valueLen = value.length();
		int suffixLen = suffix.length();
		if (suffixLen > valueLen) {
			return false;
		}
		return value.regionMatches(true, valueLen - suffixLen, suffix, 0, suffixLen);
	}

	@Unique
	private static boolean lomka$isSmallCachedCandidate(final String path) {
		if (!lomka$endsWithIgnoreCase(path, ".json")) {
			if (!LOMKA$SMALL_CACHE_SHADERS) {
				return false;
			}
			return lomka$endsWithIgnoreCase(path, ".vsh")
				|| lomka$endsWithIgnoreCase(path, ".fsh")
				|| lomka$endsWithIgnoreCase(path, ".glsl");
		}
		if (lomka$endsWithIgnoreCase(path, ".geo.json")
			|| lomka$endsWithIgnoreCase(path, ".animation.json")
			|| lomka$endsWithIgnoreCase(path, ".animations.json")
			|| lomka$endsWithIgnoreCase(path, ".animation_controllers.json")
			|| lomka$endsWithIgnoreCase(path, ".emissive.json")) {
			return true;
		}
		return path.contains("/geo/")
			|| path.contains("/animations/")
			|| path.contains("/animation/")
			|| path.contains("/emissive/");
	}

	@Unique
	private static InputStream lomka$openCachedBbmodel(final String key, final IoSupplier<InputStream> upstream) throws IOException {
		byte[] cached;
		synchronized (LOMKA$BBMODEL_LOCK) {
			cached = LOMKA$BBMODEL_CACHE.get(key);
		}
		if (cached != null) {
			return new ByteArrayInputStream(cached);
		}

		InputStream in = upstream.get();
		ByteArrayOutputStream buffer = new ByteArrayOutputStream(Math.min(8192, LOMKA$BBMODEL_MAX_ENTRY_BYTES));
		byte[] tmp = new byte[8192];
		int total = 0;

		while (true) {
			int read = in.read(tmp);
			if (read < 0) {
				break;
			}

			int nextTotal = total + read;
			if (nextTotal <= LOMKA$BBMODEL_MAX_ENTRY_BYTES) {
				buffer.write(tmp, 0, read);
				total = nextTotal;
				continue;
			}

			int keep = Math.max(0, LOMKA$BBMODEL_MAX_ENTRY_BYTES - total);
			if (keep > 0) {
				buffer.write(tmp, 0, keep);
			}

			byte[] prefix = buffer.toByteArray();
			InputStream suffix = new SequenceInputStream(new ByteArrayInputStream(tmp, keep, read - keep), in);
			return new SequenceInputStream(new ByteArrayInputStream(prefix), suffix);
		}

		in.close();
		byte[] bytes = buffer.toByteArray();
		lomka$putBbmodel(key, bytes);
		return new ByteArrayInputStream(bytes);
	}

	@Unique
	private static InputStream lomka$openCachedSmallJson(final String key, final IoSupplier<InputStream> upstream) throws IOException {
		byte[] cached;
		synchronized (LOMKA$SMALL_JSON_LOCK) {
			cached = LOMKA$SMALL_JSON_CACHE.get(key);
		}
		if (cached != null) {
			return new ByteArrayInputStream(cached);
		}

		InputStream in = upstream.get();
		ByteArrayOutputStream buffer = new ByteArrayOutputStream(Math.min(8192, LOMKA$SMALL_JSON_MAX_ENTRY_BYTES));
		byte[] tmp = new byte[8192];
		int total = 0;

		while (true) {
			int read = in.read(tmp);
			if (read < 0) {
				break;
			}

			int nextTotal = total + read;
			if (nextTotal <= LOMKA$SMALL_JSON_MAX_ENTRY_BYTES) {
				buffer.write(tmp, 0, read);
				total = nextTotal;
				continue;
			}

			int keep = Math.max(0, LOMKA$SMALL_JSON_MAX_ENTRY_BYTES - total);
			if (keep > 0) {
				buffer.write(tmp, 0, keep);
			}

			byte[] prefix = buffer.toByteArray();
			InputStream suffix = new SequenceInputStream(new ByteArrayInputStream(tmp, keep, read - keep), in);
			return new SequenceInputStream(new ByteArrayInputStream(prefix), suffix);
		}

		in.close();
		byte[] bytes = buffer.toByteArray();
		lomka$putSmallJson(key, bytes);
		return new ByteArrayInputStream(bytes);
	}

	@Unique
	private static void lomka$putBbmodel(final String key, final byte[] bytes) {
		if (bytes.length <= 0 || bytes.length > LOMKA$BBMODEL_MAX_ENTRY_BYTES) {
			return;
		}

		synchronized (LOMKA$BBMODEL_LOCK) {
			byte[] previous = LOMKA$BBMODEL_CACHE.remove(key);
			if (previous != null) {
				LOMKA$BBMODEL_TOTAL_BYTES -= previous.length;
			}

			LOMKA$BBMODEL_CACHE.put(key, bytes);
			LOMKA$BBMODEL_TOTAL_BYTES += bytes.length;

			if (LOMKA$BBMODEL_MAX_TOTAL_BYTES <= 0L) {
				LOMKA$BBMODEL_CACHE.clear();
				LOMKA$BBMODEL_TOTAL_BYTES = 0L;
				return;
			}

			if (LOMKA$BBMODEL_TOTAL_BYTES <= LOMKA$BBMODEL_MAX_TOTAL_BYTES) {
				return;
			}

			Iterator<Map.Entry<String, byte[]>> it = LOMKA$BBMODEL_CACHE.entrySet().iterator();
			while (LOMKA$BBMODEL_TOTAL_BYTES > LOMKA$BBMODEL_MAX_TOTAL_BYTES && it.hasNext()) {
				Map.Entry<String, byte[]> e = it.next();
				byte[] v = e.getValue();
				if (v != null) {
					LOMKA$BBMODEL_TOTAL_BYTES -= v.length;
				}
				it.remove();
			}
		}
	}

	@Unique
	private static void lomka$putSmallJson(final String key, final byte[] bytes) {
		if (bytes.length <= 0 || bytes.length > LOMKA$SMALL_JSON_MAX_ENTRY_BYTES) {
			return;
		}

		synchronized (LOMKA$SMALL_JSON_LOCK) {
			byte[] previous = LOMKA$SMALL_JSON_CACHE.remove(key);
			if (previous != null) {
				LOMKA$SMALL_JSON_TOTAL_BYTES -= previous.length;
			}

			LOMKA$SMALL_JSON_CACHE.put(key, bytes);
			LOMKA$SMALL_JSON_TOTAL_BYTES += bytes.length;

			if (LOMKA$SMALL_JSON_MAX_TOTAL_BYTES <= 0L) {
				LOMKA$SMALL_JSON_CACHE.clear();
				LOMKA$SMALL_JSON_TOTAL_BYTES = 0L;
				return;
			}

			if (LOMKA$SMALL_JSON_TOTAL_BYTES <= LOMKA$SMALL_JSON_MAX_TOTAL_BYTES) {
				return;
			}

			Iterator<Map.Entry<String, byte[]>> it = LOMKA$SMALL_JSON_CACHE.entrySet().iterator();
			while (LOMKA$SMALL_JSON_TOTAL_BYTES > LOMKA$SMALL_JSON_MAX_TOTAL_BYTES && it.hasNext()) {
				Map.Entry<String, byte[]> e = it.next();
				byte[] v = e.getValue();
				if (v != null) {
					LOMKA$SMALL_JSON_TOTAL_BYTES -= v.length;
				}
				it.remove();
			}
		}
	}

}

