package set.starl.mixin.chunk;

import set.starl.Lomka;
import net.minecraft.client.renderer.chunk.SectionRenderDispatcher;
import net.minecraft.util.Util;
import java.util.Queue;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(SectionRenderDispatcher.class)
public class SectionRenderDispatcherUploadThrottleMixin {
	@Unique
	private static final int LOMKA$BASE_MAX_UPLOADS_PER_CALL = Integer.parseInt(System.getProperty(
		"lomka.chunks.maxUploadsPerFrame",
		String.valueOf(Math.max(1, Math.min(4, Runtime.getRuntime().availableProcessors() / 2)))
	));

	@Unique
	private static final long LOMKA$BASE_MAX_UPLOAD_BYTES_PER_CALL = Long.parseLong(System.getProperty(
		"lomka.chunks.upload.maxBytesPerFrame",
		String.valueOf(Runtime.getRuntime().availableProcessors() <= 4 ? 4_194_304L : 8_388_608L)
	));

	@Unique
	private static final long LOMKA$BASE_MAX_UPLOAD_NANOS_PER_CALL = Long.parseLong(System.getProperty("lomka.chunks.upload.maxNanosPerFrame", "2000000"));

	@Unique
	private static final int LOMKA$BASE_MAX_CLOSE_PER_CALL = Integer.parseInt(System.getProperty("lomka.chunks.close.maxPerFrame", "8"));

	@Unique
	private static final boolean LOMKA$ADAPTIVE_BUDGETS = !"false".equalsIgnoreCase(System.getProperty("lomka.chunks.upload.budgets.adaptive", "true"));

	@Shadow
	private Queue toUpload;

	@Shadow
	private Queue toClose;

	@Inject(method = "uploadAllPendingUploads", at = @At("HEAD"), cancellable = true)
	private void lomka$uploadLimited(final CallbackInfo ci) {
		float scale = LOMKA$ADAPTIVE_BUDGETS ? Lomka.getRenderBudgetScale() : 1.0f;
		int max = lomka$scaleIntBudget(LOMKA$BASE_MAX_UPLOADS_PER_CALL, scale);
		long maxBytes = lomka$scaleLongBudget(LOMKA$BASE_MAX_UPLOAD_BYTES_PER_CALL, scale);
		long maxNanos = lomka$scaleLongBudget(LOMKA$BASE_MAX_UPLOAD_NANOS_PER_CALL, scale);
		if (max <= 0 && maxBytes <= 0L && maxNanos <= 0L) {
			return;
		}

		Queue uploadQueue = this.toUpload;
		Queue closeQueue = this.toClose;

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
			Runnable upload = (Runnable)uploadQueue.poll();
			if (upload == null) {
				break;
			}
			long cost = 0L;
			if (upload instanceof Lomka.UploadSizedRunnable sized) {
				cost = sized.bytes;
				if (maxBytes > 0L && cost > 0L && bytes + cost > maxBytes && ran > 0) {
					uploadQueue.add(upload);
					break;
				}
			}

			upload.run();
			bytes += cost;
			ran++;
		}

		int maxClose = lomka$scaleIntBudget(LOMKA$BASE_MAX_CLOSE_PER_CALL, scale);
		if (maxClose <= 0) {
			maxClose = max > 0 ? max : 4;
		}
		for (int i = 0; i < maxClose; i++) {
			net.minecraft.client.renderer.chunk.SectionMesh mesh = (net.minecraft.client.renderer.chunk.SectionMesh)closeQueue.poll();
			if (mesh == null) {
				break;
			}
			try {
				mesh.close();
			} catch (Exception ignored) {
			}
		}

		ci.cancel();
	}

	@Unique
	private static int lomka$scaleIntBudget(final int base, final float scale) {
		if (base <= 0) {
			return base;
		}
		int v = (int)((float)base * scale);
		return Math.max(1, v);
	}

	@Unique
	private static long lomka$scaleLongBudget(final long base, final float scale) {
		if (base <= 0L) {
			return base;
		}
		long v = (long)((double)base * (double)scale);
		return Math.max(1L, v);
	}
}
