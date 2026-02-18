package set.starl.mixin.cache;

import com.mojang.blaze3d.platform.NativeImage;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import net.minecraft.client.renderer.texture.SpriteContents;
import net.minecraft.client.renderer.texture.atlas.SpriteResourceLoader;
import net.minecraft.client.resources.metadata.animation.AnimationMetadataSection;
import net.minecraft.client.resources.metadata.animation.FrameSize;
import net.minecraft.client.resources.metadata.texture.TextureMetadataSection;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.metadata.MetadataSectionType;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceMetadata;
import net.minecraft.util.Mth;
import set.starl.Lomka;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(SpriteResourceLoader.class)
public interface SpriteResourceLoaderMixin {

	@Inject(method = "create", at = @At("RETURN"), cancellable = true)
	private static void lomka$wrapCreate(final Set additionalMetadataSections, final CallbackInfoReturnable<SpriteResourceLoader> cir) {
		SpriteResourceLoader original = cir.getReturnValue();
		if (original == null) {
			return;
		}

		cir.setReturnValue((spriteLocation, resource) -> {
			Optional<AnimationMetadataSection> animationInfo;
			Optional<TextureMetadataSection> textureInfo;
			List<MetadataSectionType.WithValue<?>> additionalMetadata;
			try {
				ResourceMetadata metadata = resource.metadata();
				animationInfo = metadata.getSection(AnimationMetadataSection.TYPE);
				textureInfo = metadata.getSection(TextureMetadataSection.TYPE);
				additionalMetadata = metadata.getTypedSections(additionalMetadataSections);
			} catch (Exception e) {
				SpriteResourceLoader.LOGGER.error("Unable to parse metadata from {}", spriteLocation, e);
				return null;
			}

			int[] wh = lomka$tryReadPngDimensions(resource);
			NativeImage image = null;
			int srcW;
			int srcH;
			if (wh != null) {
				srcW = wh[0];
				srcH = wh[1];
			} else {
				try {
					image = lomka$decodeImageGuarded(resource);
				} catch (IOException e) {
					SpriteResourceLoader.LOGGER.error("Using missing texture, unable to load {}", spriteLocation, e);
					return null;
				}
				srcW = image.getWidth();
				srcH = image.getHeight();
			}

			FrameSize frameSize;
			if (animationInfo.isPresent()) {
				frameSize = animationInfo.get().calculateFrameSize(srcW, srcH);
				if (!Mth.isMultipleOf(srcW, frameSize.width()) || !Mth.isMultipleOf(srcH, frameSize.height())) {
					SpriteResourceLoader.LOGGER.error(
						"Image {} size {},{} is not multiple of frame size {},{}",
						spriteLocation,
						srcW,
						srcH,
						frameSize.width(),
						frameSize.height()
					);
					if (image != null) {
						image.close();
					}
					return null;
				}
			} else {
				frameSize = new FrameSize(srcW, srcH);
			}

			int scale = lomka$chooseAtlasScale(srcW, srcH, frameSize);
			if (scale > 1) {
				frameSize = new FrameSize(frameSize.width() / scale, frameSize.height() / scale);
			}

			try {
				image = lomka$loadSpriteImageWithDiskCache(resource, spriteLocation, scale, image);
			} catch (IOException e) {
				if (image != null) {
					image.close();
				}
				SpriteResourceLoader.LOGGER.error("Using missing texture, unable to load {}", spriteLocation, e);
				return null;
			}

			if (scale > 1) {
				srcW = image.getWidth();
				srcH = image.getHeight();
				if (srcW % scale != 0 || srcH % scale != 0) {
					image.close();
					SpriteResourceLoader.LOGGER.error("Image {} size {},{} is not divisible by scale {}", spriteLocation, srcW, srcH, scale);
					return null;
				}
			}

			return new SpriteContents(spriteLocation, frameSize, image, animationInfo, additionalMetadata, textureInfo);
		});
	}

	private static NativeImage lomka$loadSpriteImageWithDiskCache(
		final Resource resource,
		final Identifier spriteLocation,
		final int scale,
		final NativeImage alreadyDecoded
	) throws IOException {
		String packId = Lomka.dedupString(resource.sourcePackId());
		Object known = resource.knownPackInfo().orElse(null);
		String packVersion = Lomka.dedupString(known == null ? "" : known.toString());
		int maxDim = Lomka.TEXTURE_MAX_DIMENSION;
		Path cacheFile = lomka$cacheFile(packId, packVersion, String.valueOf(spriteLocation), maxDim, scale);
		Path baseCacheFile = scale == 1 ? cacheFile : lomka$cacheFile(packId, packVersion, String.valueOf(spriteLocation), maxDim, 1);

		if (alreadyDecoded == null) {
			NativeImage scaledCached = lomka$tryLoadCached(cacheFile);
			if (scaledCached != null) {
				return scaledCached;
			}

			if (scale > 1) {
				NativeImage baseCached = lomka$tryLoadCached(baseCacheFile);
				if (baseCached != null) {
					NativeImage scaled = lomka$scaleForAtlas(baseCached, scale);
					lomka$tryWriteCachedIfAbsent(cacheFile, scaled);
					return scaled;
				}
			}
		}

		NativeImage image = alreadyDecoded != null ? alreadyDecoded : lomka$decodeImageGuarded(resource);

		if (image == null) {
			return null;
		}

		if (scale <= 1) {
			lomka$tryWriteCachedIfAbsent(cacheFile, image);
			return image;
		}

		lomka$tryWriteCachedIfAbsent(baseCacheFile, image);
		NativeImage scaled = lomka$scaleForAtlas(image, scale);
		lomka$tryWriteCachedIfAbsent(cacheFile, scaled);
		return scaled;
	}

	private static NativeImage lomka$scaleForAtlas(final NativeImage image, final int scale) {
		int w = image.getWidth();
		int h = image.getHeight();
		if (scale <= 1 || w <= 0 || h <= 0 || w % scale != 0 || h % scale != 0) {
			return image;
		}

		NativeImage out = new NativeImage(image.format(), w / scale, h / scale, false);
		image.resizeSubRectTo(0, 0, w, h, out);
		image.close();
		return out;
	}

	private static NativeImage lomka$decodeImageGuarded(final Resource resource) throws IOException {
		if (!Lomka.TEXTURE_DECODE_OFF_THREAD) {
			return lomka$decodeImageUnguarded(resource);
		}
		if (Lomka.IN_TEXTURE_DECODE.get().booleanValue()) {
			return lomka$decodeImageUnguarded(resource);
		}

		boolean acquired = false;
		Lomka.IN_TEXTURE_DECODE.set(Boolean.TRUE);
		try {
			Lomka.TEXTURE_DECODE_GUARD.acquireUninterruptibly();
			acquired = true;
			return lomka$decodeImageUnguarded(resource);
		} finally {
			if (acquired) {
				Lomka.TEXTURE_DECODE_GUARD.release();
			}
			Lomka.IN_TEXTURE_DECODE.set(Boolean.FALSE);
		}
	}

	private static NativeImage lomka$decodeImageUnguarded(final Resource resource) throws IOException {
		try (InputStream is = resource.open()) {
			return NativeImage.read(is);
		}
	}

	private static NativeImage lomka$tryLoadCached(final Path cacheFile) {
		try {
			if (Files.size(cacheFile) <= 64L * 1024L * 1024L) {
				try (InputStream in = Files.newInputStream(cacheFile)) {
					return NativeImage.read(in);
				}
			}
		} catch (NoSuchFileException ignored) {
		} catch (Exception ignored) {
			try {
				Files.deleteIfExists(cacheFile);
			} catch (Exception ignored2) {
			}
		}
		return null;
	}

	private static void lomka$tryWriteCachedIfAbsent(final Path cacheFile, final NativeImage image) {
		if (image == null) {
			return;
		}
		try {
			if (Files.exists(cacheFile)) {
				return;
			}
		} catch (Exception ignored) {
			return;
		}
		lomka$tryWriteCached(cacheFile, image);
	}

	private static void lomka$tryWriteCached(final Path cacheFile, final NativeImage image) {
		if (image == null) {
			return;
		}
		try {
			Files.createDirectories(cacheFile.getParent());
		} catch (Exception ignored) {
			return;
		}

		try {
			Path temp = cacheFile.resolveSibling(cacheFile.getFileName().toString() + "." + Thread.currentThread().threadId() + ".tmp");
			image.writeToFile(temp);
			try {
				Files.move(temp, cacheFile, StandardCopyOption.ATOMIC_MOVE);
			} catch (Exception ignored) {
				try {
					Files.move(temp, cacheFile, StandardCopyOption.REPLACE_EXISTING);
				} catch (Exception ignored2) {
				}
			}
		} catch (Exception ignored) {
		}
	}

	private static Path lomka$cacheFile(final String packId, final String packVersion, final String spriteIdString, final int maxDim, final int scale) {
		Path gameDir = Minecraft.getInstance().gameDirectory.toPath();
		String packDirName = Lomka.dedupString(lomka$sanitize(packId));
		String key = "atlas|" + packId + "|" + packVersion + "|" + spriteIdString + "|gen:" + Lomka.getResourceReloadSalt() + "|max:" + maxDim + "|scale:" + scale;
		String hash = lomka$sha1(key);
		return gameDir.resolve("cache").resolve("lomka").resolve(packDirName).resolve(hash.substring(0, 2)).resolve(hash + ".png");
	}

	private static String lomka$sanitize(final String s) {
		if (s == null || s.isEmpty()) {
			return "unknown";
		}
		StringBuilder out = new StringBuilder(s.length());
		for (int i = 0; i < s.length(); i++) {
			char c = s.charAt(i);
			if ((c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z') || (c >= '0' && c <= '9') || c == '_' || c == '-' || c == '.') {
				out.append(c);
			} else {
				out.append('_');
			}
		}
		return out.toString();
	}

	private static String lomka$sha1(final String s) {
		return Lomka.sha1Hex(s);
	}

	private static int[] lomka$tryReadPngDimensions(final Resource resource) {
		try (InputStream is = resource.open()) {
			byte[] header = Lomka.PNG_HEADER_32.get();
			int off = 0;
			while (off < 32) {
				int r = is.read(header, off, 32 - off);
				if (r < 0) {
					break;
				}
				off += r;
			}
			if (off < 24) {
				return null;
			}
			if ((header[0] & 0xFF) != 0x89 || header[1] != 0x50 || header[2] != 0x4E || header[3] != 0x47) {
				return null;
			}
			if (header[12] != 0x49 || header[13] != 0x48 || header[14] != 0x44 || header[15] != 0x52) {
				return null;
			}
			int w = ((header[16] & 0xFF) << 24) | ((header[17] & 0xFF) << 16) | ((header[18] & 0xFF) << 8) | (header[19] & 0xFF);
			int h = ((header[20] & 0xFF) << 24) | ((header[21] & 0xFF) << 16) | ((header[22] & 0xFF) << 8) | (header[23] & 0xFF);
			if (w <= 0 || h <= 0) {
				return null;
			}
			return new int[] { w, h };
		} catch (Exception ignored) {
			return null;
		}
	}

	private static int lomka$chooseAtlasScale(final int width, final int height, final FrameSize frameSize) {
		int maxDim = Lomka.TEXTURE_MAX_DIMENSION;
		if (maxDim <= 0) {
			return 1;
		}

		int longest = Math.max(width, height);
		if (longest <= maxDim) {
			return 1;
		}

		int scale = 1;
		while (scale < 64 && longest / scale > maxDim) {
			scale <<= 1;
		}

		while (scale > 1) {
			if (width % scale == 0
				&& height % scale == 0
				&& frameSize.width() >= scale
				&& frameSize.height() >= scale
				&& frameSize.width() % scale == 0
				&& frameSize.height() % scale == 0) {
				return scale;
			}
			scale >>= 1;
		}

		return 1;
	}
}
