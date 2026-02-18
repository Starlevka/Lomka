package set.starl.mixin.cache;

import com.mojang.blaze3d.platform.NativeImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.FileTime;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Stream;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.MipmapStrategy;
import net.minecraft.client.renderer.texture.TextureContents;
import net.minecraft.client.resources.metadata.texture.TextureMetadataSection;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import set.starl.Lomka;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(TextureContents.class)
public class TextureContentsDiskCacheMixin {
	@Unique
	private static final long LOMKA$MAX_ENTRY_BYTES = Long.parseLong(System.getProperty("lomka.textures.cache.maxEntryBytes", "16777216"));

	@Unique
	private static final boolean LOMKA$ENABLE = !"false".equalsIgnoreCase(System.getProperty("lomka.textures.cache.simple", "true"));

	@Unique
	private static final ThreadLocal<byte[]> LOMKA$IO_BUFFER = ThreadLocal.withInitial(() -> new byte[64 * 1024]);

	@Unique
	private static final boolean LOMKA$WRITE_ASYNC = Lomka.JAVA_22_25_OPTIMIZATIONS
		&& !"false".equalsIgnoreCase(System.getProperty("lomka.textures.cache.writeAsync", "true"));

	@Unique
	private static final int LOMKA$WRITE_PARALLELISM = Integer.parseInt(System.getProperty("lomka.textures.cache.write.parallelism", "1"));

	@Unique
	private static final Semaphore LOMKA$WRITE_GUARD = (LOMKA$WRITE_ASYNC && LOMKA$WRITE_PARALLELISM > 0)
		? new Semaphore(Math.max(1, Math.min(8, LOMKA$WRITE_PARALLELISM)))
		: null;

	@Unique
	private static final long LOMKA$MAX_TOTAL_BYTES = Long.parseLong(System.getProperty("lomka.textures.cache.maxTotalBytes", "536870912"));

	@Unique
	private static final long LOMKA$TRIM_INTERVAL_MS = Long.parseLong(System.getProperty("lomka.textures.cache.trimIntervalMs", "30000"));

	@Unique
	private static final int LOMKA$MAX_DELETES_PER_TRIM = Integer.parseInt(System.getProperty("lomka.textures.cache.maxDeletesPerTrim", "64"));

	@Unique
	private static final AtomicLong LOMKA$LAST_TRIM_MS = new AtomicLong(0L);

	@Unique
	private static final AtomicBoolean LOMKA$TRIM_TASK_SCHEDULED = new AtomicBoolean(false);

	@Unique
	private static final ThreadLocal<MessageDigest> LOMKA$SHA1 = ThreadLocal.withInitial(() -> {
		try {
			return MessageDigest.getInstance("SHA-1");
		} catch (NoSuchAlgorithmException e) {
			throw new RuntimeException(e);
		}
	});

	@Inject(
		method = "load(Lnet/minecraft/server/packs/resources/ResourceManager;Lnet/minecraft/resources/Identifier;)Lnet/minecraft/client/renderer/texture/TextureContents;",
		at = @At("HEAD"),
		cancellable = true
	)
	private static void lomka$loadWithDiskCache(final ResourceManager resourceManager, final Identifier id, final CallbackInfoReturnable<TextureContents> cir) {
		if (!LOMKA$ENABLE) {
			return;
		}

		Resource resource;
		try {
			resource = resourceManager.getResource(id).orElse(null);
		} catch (Exception ignored) {
			return;
		}
		if (resource == null) {
			return;
		}

		String packId = Lomka.dedupString(resource.sourcePackId());
		Object known = resource.knownPackInfo().orElse(null);
		String packVersion = Lomka.dedupString(known == null ? "" : known.toString());
		Path cacheFile = lomka$cacheFile(packId, packVersion, String.valueOf(id), Lomka.TEXTURE_MAX_DIMENSION);
		NativeImage loaded;
		try {
			loaded = lomka$decodeOffThread(() -> lomka$loadImageWithDiskCache(resource, cacheFile));
		} catch (Exception ignored) {
			return;
		}
		if (loaded == null) {
			return;
		}

		TextureMetadataSection meta;
		try {
			meta = resource.metadata().getSection(TextureMetadataSection.TYPE).orElse(new TextureMetadataSection(false, false, MipmapStrategy.AUTO, 0.0F));
		} catch (Exception ignored) {
			meta = new TextureMetadataSection(false, false, MipmapStrategy.AUTO, 0.0F);
		}

		cir.setReturnValue(new TextureContents(loaded, meta));
	}

	@Unique
	private static NativeImage lomka$decodeOffThread(final Callable<NativeImage> job) throws Exception {
		if (!Lomka.TEXTURE_DECODE_OFF_THREAD) {
			return job.call();
		}
		if (Lomka.IN_TEXTURE_DECODE.get().booleanValue()) {
			return job.call();
		}
		return CompletableFuture.supplyAsync(() -> {
			Lomka.IN_TEXTURE_DECODE.set(Boolean.TRUE);
			boolean acquired = false;
			try {
				Lomka.TEXTURE_DECODE_GUARD.acquire();
				acquired = true;
				return job.call();
			} catch (Exception e) {
				throw new RuntimeException(e);
			} finally {
				if (acquired) {
					Lomka.TEXTURE_DECODE_GUARD.release();
				}
				Lomka.IN_TEXTURE_DECODE.set(Boolean.FALSE);
			}
		}, Lomka.TEXTURE_DECODE_EXECUTOR).join();
	}

	@Unique
	private static NativeImage lomka$loadImageWithDiskCache(final Resource resource, final Path cacheFile) throws Exception {
		NativeImage loaded = null;
		try {
			long size = Files.size(cacheFile);
			if (size > 0L && size <= LOMKA$MAX_ENTRY_BYTES) {
					if (Lomka.JAVA_22_25_OPTIMIZATIONS && size <= (long) Integer.MAX_VALUE) {
					byte[] bytes = Files.readAllBytes(cacheFile);
					if (bytes.length > 0) {
						loaded = NativeImage.read(bytes);
					}
				} else {
					try (InputStream in = Files.newInputStream(cacheFile)) {
						loaded = NativeImage.read(in);
					}
				}
			}
		} catch (NoSuchFileException ignored) {
		} catch (Exception ignored) {
		}

		if (loaded == null) {
			Path rawTemp = null;
			byte[] rawBytes = null;
			boolean rawTempExists = false;
			long rawSize = 0L;
			try {
				Files.createDirectories(cacheFile.getParent());
				rawTemp = cacheFile.resolveSibling(cacheFile.getFileName().toString() + "." + Thread.currentThread().threadId() + ".rawtmp");
				try (InputStream stream = resource.open()) {
					long[] total = new long[1];
					rawBytes = lomka$readToMemoryOrSpillToFile(stream, rawTemp, LOMKA$MAX_ENTRY_BYTES, total);
					rawSize = total[0];
				}
				if (rawBytes != null) {
					loaded = NativeImage.read(rawBytes);
				} else {
					rawTempExists = true;
					try (InputStream in = Files.newInputStream(rawTemp)) {
						loaded = NativeImage.read(in);
					}
				}
			} catch (Exception ignored) {
				if (rawTemp != null) {
					try {
						Files.deleteIfExists(rawTemp);
					} catch (Exception ignored2) {
					}
				}
				return null;
			}

			boolean shouldWriteScaled = Lomka.TEXTURE_MAX_DIMENSION > 0 && Math.max(loaded.getWidth(), loaded.getHeight()) > Lomka.TEXTURE_MAX_DIMENSION;
			loaded = lomka$downscaleIfNeeded(loaded);

			if (shouldWriteScaled) {
				try {
					if (rawTempExists) {
						Files.deleteIfExists(rawTemp);
					}
				} catch (Exception ignored) {
				}
				try {
					Path temp = cacheFile.resolveSibling(cacheFile.getFileName().toString() + "." + Thread.currentThread().threadId() + ".tmp");
					loaded.writeToFile(temp);
					long size = Files.size(temp);
					boolean written = size > 0L && size <= LOMKA$MAX_ENTRY_BYTES;
					if (written) {
						try {
							Files.move(temp, cacheFile, StandardCopyOption.ATOMIC_MOVE);
						} catch (Exception ignored) {
							try {
								Files.move(temp, cacheFile, StandardCopyOption.REPLACE_EXISTING);
							} catch (Exception ignored2) {
							}
						}
						lomka$touch(cacheFile);
						lomka$scheduleTrimCache();
					} else {
						try {
							Files.deleteIfExists(temp);
						} catch (Exception ignored) {
						}
					}
				} catch (Exception ignored) {
				}
			} else {
				boolean written = rawSize > 0L && rawSize <= LOMKA$MAX_ENTRY_BYTES;
				if (written) {
					long tempTid = Thread.currentThread().threadId();
					if (rawBytes != null) {
						byte[] rawBytesFinal = rawBytes;
						lomka$runWriteJob(() -> {
							try {
								Path temp = cacheFile.resolveSibling(cacheFile.getFileName().toString() + "." + tempTid + ".tmp");
								try (OutputStream os = Files.newOutputStream(temp)) {
									os.write(rawBytesFinal);
								}
								try {
									Files.move(temp, cacheFile, StandardCopyOption.ATOMIC_MOVE);
								} catch (Exception ignored) {
									try {
										Files.move(temp, cacheFile, StandardCopyOption.REPLACE_EXISTING);
									} catch (Exception ignored2) {
									}
								}
								lomka$touch(cacheFile);
								lomka$scheduleTrimCache();
							} catch (Exception ignored) {
							}
						});
					} else {
						Path rawTempFinal = rawTemp;
						lomka$runWriteJob(() -> {
							try {
								try {
									Files.move(rawTempFinal, cacheFile, StandardCopyOption.ATOMIC_MOVE);
								} catch (Exception ignored) {
									try {
										Files.move(rawTempFinal, cacheFile, StandardCopyOption.REPLACE_EXISTING);
									} catch (Exception ignored2) {
									}
								}
								lomka$touch(cacheFile);
								lomka$scheduleTrimCache();
							} catch (Exception ignored) {
								try {
									Files.deleteIfExists(rawTempFinal);
								} catch (Exception ignored2) {
								}
							}
						});
					}
				} else {
					try {
						if (rawTempExists) {
							Files.deleteIfExists(rawTemp);
						}
					} catch (Exception ignored) {
					}
				}
			}
		} else {
			if (Lomka.JAVA_22_25_OPTIMIZATIONS) {
				CompletableFuture.runAsync(() -> lomka$touch(cacheFile), Lomka.TEXTURE_DECODE_EXECUTOR);
			} else {
				lomka$touch(cacheFile);
			}
		}

		return lomka$downscaleIfNeeded(loaded);
	}

	@Unique
	private static void lomka$scheduleTrimCache() {
		if (!Lomka.JAVA_22_25_OPTIMIZATIONS) {
			lomka$maybeTrimCache();
			return;
		}
		if (!LOMKA$TRIM_TASK_SCHEDULED.compareAndSet(false, true)) {
			return;
		}
		CompletableFuture.runAsync(() -> {
			try {
				lomka$maybeTrimCache();
			} finally {
				LOMKA$TRIM_TASK_SCHEDULED.set(false);
			}
		}, Lomka.TEXTURE_DECODE_EXECUTOR);
	}

	@Unique
	private static void lomka$runWriteJob(final Runnable job) {
		if (!LOMKA$WRITE_ASYNC) {
			job.run();
			return;
		}
		CompletableFuture.runAsync(() -> {
			Semaphore guard = LOMKA$WRITE_GUARD;
			if (guard == null) {
				job.run();
				return;
			}
			guard.acquireUninterruptibly();
			try {
				job.run();
			} finally {
				guard.release();
			}
		}, Lomka.TEXTURE_DECODE_EXECUTOR);
	}

	@Unique
	private static boolean lomka$writeToFileCapped(final InputStream in, final Path out, final long maxBytes) throws IOException {
		byte[] buffer = LOMKA$IO_BUFFER.get();
		long written = 0L;
		try (java.io.OutputStream os = Files.newOutputStream(out)) {
			while (true) {
				int read = in.read(buffer);
				if (read < 0) {
					break;
				}
				written += read;
				if (written > maxBytes) {
					return false;
				}
				os.write(buffer, 0, read);
			}
		}
		return written > 0L;
	}

	@Unique
	private static byte[] lomka$readToMemoryOrSpillToFile(
		final InputStream in,
		final Path out,
		final long maxInMemoryBytes,
		final long[] totalOut
	) throws IOException {
		byte[] buffer = LOMKA$IO_BUFFER.get();
		long total = 0L;
		boolean spilled = false;
		ByteArrayOutputStream baos = new ByteArrayOutputStream(64 * 1024);
		OutputStream os = null;
		try {
			while (true) {
				int read = in.read(buffer);
				if (read < 0) {
					break;
				}
				total += read;
				if (!spilled && total > maxInMemoryBytes) {
					os = Files.newOutputStream(out);
					baos.writeTo(os);
					spilled = true;
				}
				if (spilled) {
					os.write(buffer, 0, read);
				} else {
					baos.write(buffer, 0, read);
				}
			}
		} finally {
			totalOut[0] = total;
			if (os != null) {
				try {
					os.close();
				} catch (Exception ignored) {
				}
			}
		}

		if (spilled) {
			return null;
		}
		return baos.toByteArray();
	}

	@Unique
	private static long lomka$copyToFile(final InputStream in, final Path out) throws IOException {
		byte[] buffer = LOMKA$IO_BUFFER.get();
		long written = 0L;
		try (java.io.OutputStream os = Files.newOutputStream(out)) {
			while (true) {
				int read = in.read(buffer);
				if (read < 0) {
					break;
				}
				written += read;
				os.write(buffer, 0, read);
			}
		}
		return written;
	}

	@Unique
	private static NativeImage lomka$downscaleIfNeeded(final NativeImage image) {
		int maxDim = Lomka.TEXTURE_MAX_DIMENSION;
		if (maxDim <= 0) {
			return image;
		}

		int srcW = image.getWidth();
		int srcH = image.getHeight();
		int longest = Math.max(srcW, srcH);
		if (longest <= maxDim) {
			return image;
		}

		double scale = (double)longest / (double)maxDim;
		int dstW = Math.max(1, (int)Math.floor((double)srcW / scale));
		int dstH = Math.max(1, (int)Math.floor((double)srcH / scale));

		NativeImage out = new NativeImage(image.format(), dstW, dstH, false);
		image.resizeSubRectTo(0, 0, srcW, srcH, out);

		image.close();
		return out;
	}

	@Unique
	private static Path lomka$cacheFile(final String packId, final String packVersion, final String idString, final int maxDimension) {
		Path gameDir = Minecraft.getInstance().gameDirectory.toPath();
		String packDirName = Lomka.dedupString(lomka$sanitize(packId));
		String key = packId + "|" + packVersion + "|" + idString + "|gen:" + Lomka.getResourceReloadSalt();
		if (maxDimension > 0) {
			key = key + "|max:" + maxDimension;
		}
		String hash = lomka$sha1(key);
		return gameDir.resolve("cache").resolve("lomka").resolve(packDirName).resolve(hash.substring(0, 2)).resolve(hash + ".png");
	}

	@Unique
	private static Path lomka$cacheRoot() {
		return Minecraft.getInstance().gameDirectory.toPath().resolve("cache").resolve("lomka");
	}

	@Unique
	private static void lomka$touch(final Path file) {
		try {
			Files.setLastModifiedTime(file, FileTime.fromMillis(System.currentTimeMillis()));
		} catch (Exception ignored) {
		}
	}

	@Unique
	private static void lomka$maybeTrimCache() {
		long maxTotal = LOMKA$MAX_TOTAL_BYTES;
		if (maxTotal <= 0L) {
			return;
		}

		long now = System.currentTimeMillis();
		long last = LOMKA$LAST_TRIM_MS.get();
		if (now - last < LOMKA$TRIM_INTERVAL_MS) {
			return;
		}
		if (!LOMKA$LAST_TRIM_MS.compareAndSet(last, now)) {
			return;
		}

		Path root = lomka$cacheRoot();
		if (!Files.isDirectory(root)) {
			return;
		}

		long total = 0L;
		List<Object[]> entries = new ArrayList<>(2048);
		try (Stream<Path> stream = Files.walk(root)) {
			for (Path p : (Iterable<Path>) stream::iterator) {
				if (!p.toString().endsWith(".png")) {
					continue;
				}
				try {
					long size = Files.size(p);
					total += size;
					long time;
					try {
						time = Files.getLastModifiedTime(p).toMillis();
					} catch (Exception ignored) {
						time = 0L;
					}
					entries.add(new Object[] { p, Long.valueOf(size), Long.valueOf(time) });
				} catch (Exception ignored) {
				}
			}
		} catch (Exception ignored) {
			return;
		}

		if (total <= maxTotal) {
			return;
		}

		entries.sort(Comparator.comparingLong(e -> ((Long) e[2]).longValue()));
		int deleted = 0;
		for (Object[] e : entries) {
			if (total <= maxTotal || deleted >= LOMKA$MAX_DELETES_PER_TRIM) {
				break;
			}
			try {
				Path p = (Path) e[0];
				long size = ((Long) e[1]).longValue();
				if (Files.deleteIfExists(p)) {
					total -= size;
					deleted++;
					lomka$deleteEmptyParents(root, p.getParent());
				}
			} catch (Exception ignored) {
			}
		}
	}

	@Unique
	private static void lomka$deleteEmptyParents(final Path root, Path dir) {
		while (dir != null && !dir.equals(root)) {
			try (java.nio.file.DirectoryStream<Path> ds = Files.newDirectoryStream(dir)) {
				if (ds.iterator().hasNext()) {
					return;
				}
			} catch (Exception ignored) {
				return;
			}
			try {
				Files.deleteIfExists(dir);
			} catch (Exception ignored) {
				return;
			}
			dir = dir.getParent();
		}
	}

	@Unique
	private static String lomka$sanitize(final String s) {
		StringBuilder out = new StringBuilder(Math.min(s.length(), 96));
		for (int i = 0; i < s.length() && out.length() < 96; i++) {
			char c = s.charAt(i);
			if ((c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z') || (c >= '0' && c <= '9') || c == '.' || c == '-' || c == '_') {
				out.append(c);
			} else {
				out.append('_');
			}
		}
		if (out.isEmpty()) {
			return "unknown";
		}
		return out.toString();
	}

	@Unique
	private static String lomka$sha1(final String s) {
		MessageDigest md = LOMKA$SHA1.get();
		md.reset();

		byte[] digest = md.digest(s.getBytes(java.nio.charset.StandardCharsets.UTF_8));
		char[] hex = new char[digest.length * 2];
		for (int i = 0; i < digest.length; i++) {
			int v = digest[i] & 0xff;
			hex[i * 2] = Character.forDigit(v >>> 4, 16);
			hex[i * 2 + 1] = Character.forDigit(v & 0xf, 16);
		}
		return new String(hex);
	}
}
