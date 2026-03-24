package set.starl.injection.mixins.texture;

import com.mojang.blaze3d.platform.NativeImage;
import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import net.minecraft.client.renderer.texture.atlas.sources.LazyLoadedImage;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import set.starl.LomkaCore;

@Mixin(LazyLoadedImage.class)
public class LazyLoadedImageOffThreadMixin {
	@Redirect(
		method = "get",
		at = @At(
			value = "INVOKE",
			target = "Lcom/mojang/blaze3d/platform/NativeImage;read(Ljava/io/InputStream;)Lcom/mojang/blaze3d/platform/NativeImage;"
		)
	)
    private NativeImage lomka$readOffThread(final InputStream inputStream) throws IOException {
        final InputStream buffered = inputStream instanceof BufferedInputStream
            ? inputStream : new BufferedInputStream(inputStream, 8192);
        if (!LomkaCore.TEXTURE_DECODE_OFF_THREAD) {
            return NativeImage.read(buffered);
        }
        if (Boolean.TRUE.equals(LomkaCore.IN_TEXTURE_DECODE.get())) {
            return NativeImage.read(buffered);
        }
        LomkaCore.TEXTURE_DECODE_GUARD.acquireUninterruptibly();
        LomkaCore.IN_TEXTURE_DECODE.set(Boolean.TRUE);

        NativeImage result;
        try {
            CompletableFuture<NativeImage> future = CompletableFuture.supplyAsync(() -> {
                try {
                    return NativeImage.read(buffered);
                } catch (IOException e) {
                    throw new CompletionException(e);
                }
            }, LomkaCore.TEXTURE_DECODE_EXECUTOR);

            try {
                result = future.join();
            } catch (CompletionException e) {
                Throwable cause = e.getCause();
                if (cause instanceof IOException io) {
                    throw io;
                }
                if (cause instanceof RuntimeException re) {
                    throw re;
                }
                throw e;
            }
        } finally {
            LomkaCore.IN_TEXTURE_DECODE.set(Boolean.FALSE);
            LomkaCore.TEXTURE_DECODE_GUARD.release();
        }

        return result;
    }
}
