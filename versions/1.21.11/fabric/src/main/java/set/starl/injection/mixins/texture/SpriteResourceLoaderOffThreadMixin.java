package set.starl.injection.mixins.texture;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.Set;
import net.minecraft.client.renderer.texture.SpriteContents;
import net.minecraft.client.renderer.texture.SpriteLoader;
import net.minecraft.client.renderer.texture.atlas.SpriteResourceLoader;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import set.starl.Lomka;

@Mixin(SpriteLoader.class)
public class SpriteResourceLoaderOffThreadMixin {
	@Redirect(
		method = "loadAndStitch",
		at = @At(
			value = "INVOKE",
			target = "Lnet/minecraft/client/renderer/texture/atlas/SpriteResourceLoader;create(Ljava/util/Set;)Lnet/minecraft/client/renderer/texture/atlas/SpriteResourceLoader;"
		)
	)
	private SpriteResourceLoader lomka$wrapSpriteLoader(final Set additionalMetadataSections) {
		SpriteResourceLoader original = SpriteResourceLoader.create(additionalMetadataSections);
		if (!Lomka.TEXTURE_DECODE_OFF_THREAD) {
			return original;
		}
		SpriteResourceLoader wrapped = (spriteLocation, resource) -> {
			if (Boolean.TRUE.equals(Lomka.IN_TEXTURE_DECODE.get())) {
				return original.loadSprite(spriteLocation, resource);
			}
			Lomka.TEXTURE_DECODE_GUARD.acquireUninterruptibly();
			Lomka.IN_TEXTURE_DECODE.set(Boolean.TRUE);
			try {
				CompletableFuture<SpriteContents> future = CompletableFuture.supplyAsync(() -> original.loadSprite(spriteLocation, resource), Lomka.TEXTURE_DECODE_EXECUTOR);
				return future.join();
			} catch (CompletionException e) {
				Throwable cause = e.getCause();
				if (cause instanceof RuntimeException re) {
					throw re;
				}
				if (cause instanceof Error err) {
					throw err;
				}
				throw e;
			} finally {
				Lomka.IN_TEXTURE_DECODE.set(Boolean.FALSE);
				Lomka.TEXTURE_DECODE_GUARD.release();
			}
		};
		return wrapped;
	}
}
