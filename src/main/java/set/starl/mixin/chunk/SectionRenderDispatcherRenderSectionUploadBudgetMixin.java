package set.starl.mixin.chunk;

import com.mojang.blaze3d.vertex.MeshData;
import java.nio.ByteBuffer;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import net.minecraft.client.renderer.chunk.ChunkSectionLayer;
import net.minecraft.client.renderer.chunk.CompiledSectionMesh;
import net.minecraft.client.renderer.chunk.SectionRenderDispatcher;
import set.starl.Lomka;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(SectionRenderDispatcher.RenderSection.class)
public class SectionRenderDispatcherRenderSectionUploadBudgetMixin {
	@Unique
	private static final ThreadLocal<Long> LOMKA$NEXT_UPLOAD_BYTES = ThreadLocal.withInitial(() -> Long.valueOf(-1L));

	@Inject(method = "upload", at = @At("HEAD"))
	private void lomka$estimateUploadBytes(
		final Map<ChunkSectionLayer, MeshData> renderedLayers,
		final CompiledSectionMesh compiledSectionMesh,
		final CallbackInfoReturnable<CompletableFuture<?>> cir
	) {
		long bytes = 0L;
		for (MeshData mesh : renderedLayers.values()) {
			try {
				ByteBuffer vb = mesh.vertexBuffer();
				if (vb != null) {
					bytes += (long) vb.remaining();
				}
			} catch (Exception ignored) {
			}
			try {
				ByteBuffer ib = mesh.indexBuffer();
				if (ib != null) {
					bytes += (long) ib.remaining();
				}
			} catch (Exception ignored) {
			}
		}
		LOMKA$NEXT_UPLOAD_BYTES.set(Long.valueOf(bytes));
	}

	@Inject(method = "upload", at = @At("RETURN"))
	private void lomka$clearUploadBytes(
		final Map<ChunkSectionLayer, MeshData> renderedLayers,
		final CompiledSectionMesh compiledSectionMesh,
		final CallbackInfoReturnable<CompletableFuture<?>> cir
	) {
		LOMKA$NEXT_UPLOAD_BYTES.set(Long.valueOf(-1L));
	}

	@Redirect(
		method = "upload",
		at = @At(
			value = "INVOKE",
			target = "Ljava/util/concurrent/CompletableFuture;runAsync(Ljava/lang/Runnable;Ljava/util/concurrent/Executor;)Ljava/util/concurrent/CompletableFuture;"
		)
	)
	private CompletableFuture<?> lomka$wrapUploadRunnable(final Runnable runnable, final Executor executor) {
		long bytes = LOMKA$NEXT_UPLOAD_BYTES.get().longValue();
		LOMKA$NEXT_UPLOAD_BYTES.set(Long.valueOf(-1L));
		if (bytes >= 0L) {
			return CompletableFuture.runAsync(new Lomka.UploadSizedRunnable(bytes, runnable), executor);
		}
		return CompletableFuture.runAsync(runnable, executor);
	}

	@Unique
	private static final ThreadLocal<Long> LOMKA$NEXT_INDEX_UPLOAD_BYTES = ThreadLocal.withInitial(() -> Long.valueOf(-1L));

	@Inject(method = "uploadSectionIndexBuffer", at = @At("HEAD"))
	private void lomka$estimateIndexUploadBytes(
		final CompiledSectionMesh compiledSectionMesh,
		final com.mojang.blaze3d.vertex.ByteBufferBuilder.Result indexBuffer,
		final ChunkSectionLayer layer,
		final CallbackInfoReturnable<CompletableFuture<?>> cir
	) {
		long bytes = -1L;
		try {
			ByteBuffer bb = indexBuffer.byteBuffer();
			bytes = bb == null ? -1L : (long) bb.remaining();
		} catch (Exception ignored) {
		}
		LOMKA$NEXT_INDEX_UPLOAD_BYTES.set(Long.valueOf(bytes));
	}

	@Inject(method = "uploadSectionIndexBuffer", at = @At("RETURN"))
	private void lomka$clearIndexUploadBytes(
		final CompiledSectionMesh compiledSectionMesh,
		final com.mojang.blaze3d.vertex.ByteBufferBuilder.Result indexBuffer,
		final ChunkSectionLayer layer,
		final CallbackInfoReturnable<CompletableFuture<?>> cir
	) {
		LOMKA$NEXT_INDEX_UPLOAD_BYTES.set(Long.valueOf(-1L));
	}

	@Redirect(
		method = "uploadSectionIndexBuffer",
		at = @At(
			value = "INVOKE",
			target = "Ljava/util/concurrent/CompletableFuture;runAsync(Ljava/lang/Runnable;Ljava/util/concurrent/Executor;)Ljava/util/concurrent/CompletableFuture;"
		)
	)
	private CompletableFuture<?> lomka$wrapIndexUploadRunnable(final Runnable runnable, final Executor executor) {
		long bytes = LOMKA$NEXT_INDEX_UPLOAD_BYTES.get().longValue();
		LOMKA$NEXT_INDEX_UPLOAD_BYTES.set(Long.valueOf(-1L));
		if (bytes >= 0L) {
			return CompletableFuture.runAsync(new Lomka.UploadSizedRunnable(bytes, runnable), executor);
		}
		return CompletableFuture.runAsync(runnable, executor);
	}
}
