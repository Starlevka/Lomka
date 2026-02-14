package set.starl.mixin.chunk;

import it.unimi.dsi.fastutil.longs.Long2LongLinkedOpenHashMap;
import net.minecraft.client.Camera;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.ViewArea;
import net.minecraft.client.renderer.chunk.RenderRegionCache;
import net.minecraft.client.renderer.chunk.SectionRenderDispatcher;
import net.minecraft.core.SectionPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = LevelRenderer.class, priority = 900)
public class LevelRendererChunkCompileThrottleMixin {
	@Unique
	private static final int LOMKA$MAX_ASYNC_PER_FRAME = Integer.parseInt(System.getProperty(
		"lomka.chunks.maxAsyncCompilesPerFrame",
		String.valueOf(Math.max(1, Math.min(4, Runtime.getRuntime().availableProcessors() / 2)))
	));

	@Unique
	private static final int LOMKA$MAX_SYNC_PER_FRAME = Integer.parseInt(System.getProperty(
		"lomka.chunks.maxSyncCompilesPerFrame",
		String.valueOf(Math.max(1, Math.min(2, Runtime.getRuntime().availableProcessors() / 4)))
	));

	@Unique
	private static final boolean LOMKA$DIRTY_DEDUP_ENABLE = !"false".equalsIgnoreCase(System.getProperty("lomka.chunks.dirtyDedup", "true"));

	@Unique
	private static final int LOMKA$DIRTY_DEDUP_MAX_ENTRIES = Integer.parseInt(System.getProperty("lomka.chunks.dirtyDedup.maxEntries", "65536"));

	@Shadow
	private int ticks;

	@Shadow
	private ViewArea viewArea;

	@Unique
	private int lomka$asyncScheduled;

	@Unique
	private int lomka$syncCompiled;

	@Unique
	private boolean lomka$skipNextSetNotDirty;

	@Unique
	private Long2LongLinkedOpenHashMap lomka$dirtyStampBySection;

	@Unique
	private Long2LongLinkedOpenHashMap lomka$dirtyStampBySection() {
		Long2LongLinkedOpenHashMap map = this.lomka$dirtyStampBySection;
		if (map == null) {
			map = new Long2LongLinkedOpenHashMap();
			map.defaultReturnValue(Long.MIN_VALUE);
			this.lomka$dirtyStampBySection = map;
		}
		return map;
	}

	@Inject(method = "compileSections", at = @At("HEAD"))
	private void lomka$resetAsyncCounter(final Camera camera, final CallbackInfo ci) {
		this.lomka$asyncScheduled = 0;
		this.lomka$syncCompiled = 0;
		this.lomka$skipNextSetNotDirty = false;
	}

	@Inject(method = "setSectionDirty(IIIZ)V", at = @At("HEAD"), cancellable = true)
	private void lomka$coalesceSetDirty(final int sectionX, final int sectionY, final int sectionZ, final boolean playerChanged, final CallbackInfo ci) {
		ViewArea viewArea = this.viewArea;
		if (viewArea == null) {
			return;
		}
		if (!LOMKA$DIRTY_DEDUP_ENABLE) {
			return;
		}

		Long2LongLinkedOpenHashMap stamps = this.lomka$dirtyStampBySection();
		int maxEntries = LOMKA$DIRTY_DEDUP_MAX_ENTRIES;
		if (maxEntries > 0 && stamps.size() >= maxEntries) {
			while (stamps.size() >= maxEntries) {
				stamps.removeFirstLong();
			}
		}

		long key = SectionPos.asLong(sectionX, sectionY, sectionZ);
		long stamp = stamps.get(key);
		int lastTick = (int)(stamp >>> 1);
		boolean lastPlayerChanged = (stamp & 1L) != 0L;
		if (lastTick == this.ticks) {
			if (!playerChanged || lastPlayerChanged) {
				ci.cancel();
				return;
			}
			stamps.remove(key);
			stamps.put(key, ((long)this.ticks << 1) | 1L);
			viewArea.setDirty(sectionX, sectionY, sectionZ, true);
			ci.cancel();
			return;
		}

		stamps.remove(key);
		stamps.put(key, ((long)this.ticks << 1) | (playerChanged ? 1L : 0L));
		viewArea.setDirty(sectionX, sectionY, sectionZ, playerChanged);
		ci.cancel();
	}

	@Redirect(
		method = "compileSections",
		at = @At(
			value = "INVOKE",
			target = "Lnet/minecraft/client/renderer/chunk/SectionRenderDispatcher$RenderSection;rebuildSectionAsync(Lnet/minecraft/client/renderer/chunk/RenderRegionCache;)V"
		)
	)
	private void lomka$maybeRebuildAsync(final SectionRenderDispatcher.RenderSection section, final RenderRegionCache cache) {
		if (LOMKA$MAX_ASYNC_PER_FRAME <= 0) {
			this.lomka$skipNextSetNotDirty = false;
			section.rebuildSectionAsync(cache);
			return;
		}
		if (this.lomka$asyncScheduled < LOMKA$MAX_ASYNC_PER_FRAME) {
			this.lomka$asyncScheduled++;
			this.lomka$skipNextSetNotDirty = false;
			section.rebuildSectionAsync(cache);
			return;
		}
		this.lomka$skipNextSetNotDirty = true;
	}

	@Redirect(
		method = "compileSections",
		at = @At(
			value = "INVOKE",
			target = "Lnet/minecraft/client/renderer/chunk/SectionRenderDispatcher;rebuildSectionSync(Lnet/minecraft/client/renderer/chunk/SectionRenderDispatcher$RenderSection;Lnet/minecraft/client/renderer/chunk/RenderRegionCache;)V"
		)
	)
	private void lomka$maybeRebuildSync(final SectionRenderDispatcher dispatcher, final SectionRenderDispatcher.RenderSection section, final RenderRegionCache cache) {
		if (LOMKA$MAX_SYNC_PER_FRAME <= 0 || this.lomka$syncCompiled < LOMKA$MAX_SYNC_PER_FRAME) {
			this.lomka$syncCompiled++;
			this.lomka$skipNextSetNotDirty = false;
			dispatcher.rebuildSectionSync(section, cache);
			return;
		}
		this.lomka$maybeRebuildAsync(section, cache);
	}

	@Redirect(
		method = "compileSections",
		at = @At(
			value = "INVOKE",
			target = "Lnet/minecraft/client/renderer/chunk/SectionRenderDispatcher$RenderSection;setNotDirty()V"
		)
	)
	private void lomka$maybeSetNotDirty(final SectionRenderDispatcher.RenderSection section) {
		if (this.lomka$skipNextSetNotDirty) {
			this.lomka$skipNextSetNotDirty = false;
			return;
		}
		section.setNotDirty();
	}
}
