package set.starl.mixin.chunk;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.Semaphore;
import java.util.function.Supplier;
import net.minecraft.client.renderer.chunk.SectionRenderDispatcher;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(SectionRenderDispatcher.class)
public class SectionRenderDispatcherPriorityMixin {
	@Unique
	private static final int LOMKA$THREAD_PRIORITY = Integer.parseInt(System.getProperty("lomka.chunks.threadPriority", "3"));

	@Unique
	private static final int LOMKA$MAX_CONCURRENT_TASKS = Integer.parseInt(System.getProperty(
		"lomka.chunks.maxConcurrentTasks",
		String.valueOf(Math.max(1, Math.min(8, Runtime.getRuntime().availableProcessors() / 4)))
	));

	@Unique
	private static final Semaphore LOMKA$TASK_GUARD = LOMKA$MAX_CONCURRENT_TASKS > 0
		? new Semaphore(Math.min(256, LOMKA$MAX_CONCURRENT_TASKS))
		: null;

	@Unique
	private static final ThreadLocal<Boolean> LOMKA$PRIORITY_SET = ThreadLocal.withInitial(() -> Boolean.FALSE);

	@Redirect(
		method = "runTask",
		at = @At(
			value = "INVOKE",
			target = "Ljava/util/concurrent/CompletableFuture;supplyAsync(Ljava/util/function/Supplier;Ljava/util/concurrent/Executor;)Ljava/util/concurrent/CompletableFuture;"
		)
	)
	private CompletableFuture<?> lomka$wrapSupplyAsync(final Supplier<?> supplier, final Executor executor) {
		return CompletableFuture.supplyAsync(() -> {
			lomka$ensureThreadPriority();
			Semaphore guard = LOMKA$TASK_GUARD;
			if (guard == null) {
				return supplier.get();
			}
			boolean acquired = false;
			try {
				guard.acquire();
				acquired = true;
				return supplier.get();
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
				return supplier.get();
			} finally {
				if (acquired) {
					guard.release();
				}
			}
		}, executor);
	}

	@Unique
	private static void lomka$ensureThreadPriority() {
		if (LOMKA$THREAD_PRIORITY <= 0) {
			return;
		}
		if (LOMKA$PRIORITY_SET.get().booleanValue()) {
			return;
		}
		LOMKA$PRIORITY_SET.set(Boolean.TRUE);
		int p = LOMKA$THREAD_PRIORITY;
		if (p < Thread.MIN_PRIORITY) {
			p = Thread.MIN_PRIORITY;
		} else if (p > Thread.MAX_PRIORITY) {
			p = Thread.MAX_PRIORITY;
		}
		try {
			Thread.currentThread().setPriority(p);
		} catch (Exception ignored) {
		}
	}
}
