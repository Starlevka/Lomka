package set.starl.mixin.chunk;

import set.starl.Lomka;
import net.minecraft.client.renderer.chunk.SectionRenderDispatcher;
import net.minecraft.util.Util;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(SectionRenderDispatcher.class)
public class SectionRenderDispatcherUploadThrottleMixin {
	@Unique
	private static final int LOMKA$MAX_UPLOADS_PER_CALL = Integer.parseInt(System.getProperty(
		"lomka.chunks.maxUploadsPerFrame",
		String.valueOf(Math.max(1, Math.min(4, Runtime.getRuntime().availableProcessors() / 2)))
	));

	@Unique
	private static final long LOMKA$MAX_UPLOAD_BYTES_PER_CALL = Long.parseLong(System.getProperty(
		"lomka.chunks.upload.maxBytesPerFrame",
		String.valueOf(Runtime.getRuntime().availableProcessors() <= 4 ? 4_194_304L : 8_388_608L)
	));

	@Unique
	private static final long LOMKA$MAX_UPLOAD_NANOS_PER_CALL = Long.parseLong(System.getProperty("lomka.chunks.upload.maxNanosPerFrame", "2000000"));

	@Unique
	private static final int LOMKA$MAX_CLOSE_PER_CALL = Integer.parseInt(System.getProperty("lomka.chunks.close.maxPerFrame", "8"));

	@Shadow
	private java.util.Queue toUpload;

	@Shadow
	private java.util.Queue toClose;

	@Inject(method = "uploadAllPendingUploads", at = @At("HEAD"), cancellable = true)
	private void lomka$uploadLimited(final CallbackInfo ci) {
		int max = LOMKA$MAX_UPLOADS_PER_CALL;
		long maxBytes = LOMKA$MAX_UPLOAD_BYTES_PER_CALL;
		long maxNanos = LOMKA$MAX_UPLOAD_NANOS_PER_CALL;
		if (max <= 0 && maxBytes <= 0L && maxNanos <= 0L) {
			return;
		}

		Object peekUpload = this.toUpload.peek();
		if (peekUpload != null && !(peekUpload instanceof Runnable)) {
			return;
		}
		Object peekClose = this.toClose.peek();
		if (peekClose != null && !(peekClose instanceof net.minecraft.client.renderer.chunk.SectionMesh)) {
			return;
		}

		long start = Util.getNanos();
		long bytes = 0L;
		int ran = 0;

		while (true) {
			if (max > 0 && ran >= max) {
				break;
			}
			if (maxNanos > 0L && Util.getNanos() - start >= maxNanos) {
				break;
			}
			Object polled = this.toUpload.poll();
			if (polled == null) {
				break;
			}
			if (!(polled instanceof Runnable upload)) {
				break;
			}
			long cost = 0L;
			if (upload instanceof Lomka.UploadSizedRunnable sized) {
				cost = sized.bytes;
				if (maxBytes > 0L && cost > 0L && bytes + cost > maxBytes && ran > 0) {
					this.toUpload.add(upload);
					break;
				}
			}

			upload.run();
			bytes += cost;
			ran++;
		}

		int maxClose = LOMKA$MAX_CLOSE_PER_CALL;
		if (maxClose <= 0) {
			maxClose = max > 0 ? max : 4;
		}
		for (int i = 0; i < maxClose; i++) {
			net.minecraft.client.renderer.chunk.SectionMesh mesh = (net.minecraft.client.renderer.chunk.SectionMesh)this.toClose.poll();
			if (mesh == null) {
				break;
			}
			mesh.close();
		}

		ci.cancel();
	}
}
