package set.starl.mixin.chunk;

import it.unimi.dsi.fastutil.longs.Long2LongLinkedOpenHashMap;
import net.minecraft.client.Camera;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.ViewArea;
import net.minecraft.client.renderer.chunk.RenderRegionCache;
import net.minecraft.client.renderer.chunk.SectionRenderDispatcher;
import net.minecraft.client.renderer.chunk.TranslucencyPointOfView;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Position;
import net.minecraft.core.SectionPos;
import net.minecraft.util.Mth;
import net.minecraft.util.Util;
import set.starl.Lomka;
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
	private static final int LOMKA$BASE_MAX_ASYNC_PER_FRAME = Integer.parseInt(System.getProperty(
		"lomka.chunks.maxAsyncCompilesPerFrame",
		String.valueOf(Math.max(1, Math.min(4, Runtime.getRuntime().availableProcessors() / 2)))
	));

	@Unique
	private static final int LOMKA$BASE_MAX_SYNC_PER_FRAME = Integer.parseInt(System.getProperty(
		"lomka.chunks.maxSyncCompilesPerFrame",
		String.valueOf(Math.max(1, Math.min(2, Runtime.getRuntime().availableProcessors() / 4)))
	));

	@Unique
	private static final boolean LOMKA$ADAPTIVE_BUDGETS = !"false".equalsIgnoreCase(System.getProperty("lomka.chunks.budgets.adaptive", "true"));

	@Unique
	private static final boolean LOMKA$DIRTY_DEDUP_ENABLE = !"false".equalsIgnoreCase(System.getProperty("lomka.chunks.dirtyDedup", "true"));

	@Unique
	private static final int LOMKA$DIRTY_DEDUP_MAX_ENTRIES = Integer.parseInt(System.getProperty("lomka.chunks.dirtyDedup.maxEntries", "65536"));

	@Shadow
	private int ticks;

	@Shadow
	private ViewArea viewArea;

	@Shadow
	private BlockPos lastTranslucentSortBlockPos;

	@Unique
	private int lomka$asyncScheduled;

	@Unique
	private int lomka$syncCompiled;

	@Unique
	private boolean lomka$skipNextSetNotDirty;

	@Unique
	private int lomka$maxAsyncThisFrame;

	@Unique
	private int lomka$maxSyncThisFrame;

	@Unique
	private long lomka$renderLevelStartNanos;

	@Unique
	private Long2LongLinkedOpenHashMap lomka$dirtyStampBySection;

	@Unique
	private final BlockPos.MutableBlockPos lomka$sectionCenterTmp = new BlockPos.MutableBlockPos();

	@Unique
	private final TranslucencyPointOfView lomka$translucencyPointOfView = new TranslucencyPointOfView();

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
		this.lomka$maxAsyncThisFrame = lomka$effectiveBudget(LOMKA$BASE_MAX_ASYNC_PER_FRAME);
		this.lomka$maxSyncThisFrame = lomka$effectiveBudget(LOMKA$BASE_MAX_SYNC_PER_FRAME);
	}

	@Inject(method = "renderLevel", at = @At("HEAD"))
	private void lomka$beginFrameBudget(
		final com.mojang.blaze3d.resource.GraphicsResourceAllocator resourceAllocator,
		final net.minecraft.client.DeltaTracker deltaTracker,
		final boolean renderOutline,
		final Camera camera,
		final org.joml.Matrix4f modelViewMatrix,
		final org.joml.Matrix4f projectionMatrix,
		final org.joml.Matrix4f projectionMatrixForCulling,
		final com.mojang.blaze3d.buffers.GpuBufferSlice terrainFog,
		final org.joml.Vector4f fogColor,
		final boolean shouldRenderSky,
		final CallbackInfo ci
	) {
		this.lomka$renderLevelStartNanos = Util.getNanos();
	}

	@Inject(method = "renderLevel", at = @At("RETURN"))
	private void lomka$endFrameBudget(
		final com.mojang.blaze3d.resource.GraphicsResourceAllocator resourceAllocator,
		final net.minecraft.client.DeltaTracker deltaTracker,
		final boolean renderOutline,
		final Camera camera,
		final org.joml.Matrix4f modelViewMatrix,
		final org.joml.Matrix4f projectionMatrix,
		final org.joml.Matrix4f projectionMatrixForCulling,
		final com.mojang.blaze3d.buffers.GpuBufferSlice terrainFog,
		final org.joml.Vector4f fogColor,
		final boolean shouldRenderSky,
		final CallbackInfo ci
	) {
		long dt = Util.getNanos() - this.lomka$renderLevelStartNanos;
		Lomka.recordRenderLevelNanos(dt);
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
			target = "Lnet/minecraft/core/SectionPos;center()Lnet/minecraft/core/BlockPos;"
		)
	)
	private BlockPos lomka$reuseSectionCenterBlockPos(final SectionPos sectionPos) {
		return this.lomka$sectionCenterTmp.set(
			(sectionPos.x() << 4) + 8,
			(sectionPos.y() << 4) + 8,
			(sectionPos.z() << 4) + 8
		);
	}

	@Redirect(
		method = "scheduleTranslucentSectionResort",
		at = @At(
			value = "INVOKE",
			target = "Lnet/minecraft/core/BlockPos;containing(Lnet/minecraft/core/Position;)Lnet/minecraft/core/BlockPos;"
		)
	)
	private BlockPos lomka$reuseLastTranslucentSortBlockPos(final Position pos) {
		BlockPos last = this.lastTranslucentSortBlockPos;
		int x = Mth.floor(pos.x());
		int y = Mth.floor(pos.y());
		int z = Mth.floor(pos.z());
		if (last != null && last.getX() == x && last.getY() == y && last.getZ() == z) {
			return last;
		}
		return new BlockPos(x, y, z);
	}

	@Redirect(
		method = "scheduleTranslucentSectionResort",
		at = @At(value = "NEW", target = "net/minecraft/client/renderer/chunk/TranslucencyPointOfView")
	)
	private TranslucencyPointOfView lomka$reuseTranslucencyPointOfView() {
		return this.lomka$translucencyPointOfView;
	}

	@Redirect(
		method = "compileSections",
		at = @At(
			value = "INVOKE",
			target = "Lnet/minecraft/client/renderer/chunk/SectionRenderDispatcher$RenderSection;rebuildSectionAsync(Lnet/minecraft/client/renderer/chunk/RenderRegionCache;)V"
		)
	)
	private void lomka$maybeRebuildAsync(final SectionRenderDispatcher.RenderSection section, final RenderRegionCache cache) {
		int maxAsync = this.lomka$maxAsyncThisFrame;
		if (maxAsync <= 0) {
			this.lomka$skipNextSetNotDirty = false;
			section.rebuildSectionAsync(cache);
			return;
		}
		if (this.lomka$asyncScheduled < maxAsync) {
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
		int maxSync = this.lomka$maxSyncThisFrame;
		if (maxSync <= 0 || this.lomka$syncCompiled < maxSync) {
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

	@Unique
	private static int lomka$effectiveBudget(final int base) {
		if (base <= 0) {
			return base;
		}
		if (!LOMKA$ADAPTIVE_BUDGETS) {
			return base;
		}
		float scale = Lomka.getRenderBudgetScale();
		int eff = (int)((float)base * scale);
		return Math.max(1, eff);
	}
}
