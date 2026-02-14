package set.starl.mixin.cache;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Util;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Coerce;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import set.starl.Lomka;

@Mixin(Minecraft.class)
public class MinecraftResourceReloadSaltMixin {
	@Unique
	private static final long LOMKA$DEBOUNCE_MS = Long.parseLong(System.getProperty("lomka.resources.reload.debounceMs", "200"));

	@Unique
	private static final ThreadLocal<Boolean> LOMKA$BYPASS = ThreadLocal.withInitial(() -> Boolean.FALSE);

	@Unique
	private long lomka$reloadDebounceDeadlineMs;

	@Unique
	private boolean lomka$reloadDebounceScheduled;

	@Unique
	private @Nullable CompletableFuture<Void> lomka$reloadDebounceFuture;

	@Inject(
		method = "reloadResourcePacks()Ljava/util/concurrent/CompletableFuture;",
		at = @At("HEAD"),
		cancellable = true
	)
	private void lomka$debounceReload(final CallbackInfoReturnable<CompletableFuture> cir) {
		if (LOMKA$BYPASS.get().booleanValue()) {
			return;
		}
		long debounceMs = LOMKA$DEBOUNCE_MS;
		if (debounceMs <= 0L) {
			return;
		}

		long now = Util.getMillis();
		long deadline = now + debounceMs;
		if (deadline > this.lomka$reloadDebounceDeadlineMs) {
			this.lomka$reloadDebounceDeadlineMs = deadline;
		}

		CompletableFuture<Void> future = this.lomka$reloadDebounceFuture;
		if (future == null || future.isDone()) {
			future = new CompletableFuture<>();
			this.lomka$reloadDebounceFuture = future;
		}

		if (!this.lomka$reloadDebounceScheduled) {
			this.lomka$reloadDebounceScheduled = true;
			this.lomka$scheduleDebouncedReload();
		}

		cir.setReturnValue(future);
	}

	@Unique
	private void lomka$scheduleDebouncedReload() {
		long now = Util.getMillis();
		long delay = this.lomka$reloadDebounceDeadlineMs - now;
		if (delay < 0L) {
			delay = 0L;
		}
		Executor executor = (Executor)(Object)this;
		CompletableFuture.delayedExecutor(delay, TimeUnit.MILLISECONDS, executor).execute(this::lomka$runDebouncedReload);
	}

	@Unique
	private void lomka$runDebouncedReload() {
		long now = Util.getMillis();
		long remain = this.lomka$reloadDebounceDeadlineMs - now;
		if (remain > 1L) {
			this.lomka$scheduleDebouncedReload();
			return;
		}

		this.lomka$reloadDebounceScheduled = false;
		CompletableFuture<Void> out = this.lomka$reloadDebounceFuture;
		if (out == null || out.isDone()) {
			return;
		}

		CompletableFuture<Void> inner;
		LOMKA$BYPASS.set(Boolean.TRUE);
		try {
			inner = ((Minecraft)(Object)this).reloadResourcePacks();
		} finally {
			LOMKA$BYPASS.set(Boolean.FALSE);
		}

		inner.whenComplete((ok, t) -> {
			if (t != null) {
				out.completeExceptionally(t);
			} else {
				out.complete(null);
			}
		});
	}

	@Inject(
		method = "reloadResourcePacks(ZLnet/minecraft/client/Minecraft$GameLoadCookie;)Ljava/util/concurrent/CompletableFuture;",
		at = @At(value = "INVOKE", target = "Lnet/minecraft/server/packs/repository/PackRepository;reload()V", shift = At.Shift.BEFORE)
	)
	private void lomka$bumpResourceReloadSalt(final boolean isRecovery, final @Coerce @Nullable Object loadCookie, final CallbackInfoReturnable<CompletableFuture> cir) {
		Lomka.nextResourceReloadSalt();
	}
}
