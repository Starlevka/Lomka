package lomka.starl.mixins.net.minecraft.util.thread;

import net.minecraft.util.thread.BlockableEventLoop;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(BlockableEventLoop.class)
public abstract class MixinBlockableEventLoop {

    private static final CompletableFuture<Void> COMPLETED_FUTURE =
            CompletableFuture.completedFuture(null);

    @Shadow
    protected abstract boolean scheduleExecutables();

    /**
     * @author Starlev
     * @reason Uses leaner runAsync(runnable, this) instead of vanilla's supplyAsync(supplier, this),
     *         skipping an extra captured-lambda allocation on the async submission path.
     */
    @Overwrite
    private CompletableFuture<Void> submitAsync(Runnable runnable) {
        return CompletableFuture.runAsync(runnable, (Executor) (Object) this);
    }

    /**
     * @author Starlev
     * @reason Inline path runs the task directly and returns a shared already-completed future
     *         instead of allocating a fresh one each call; the async branch reuses submitAsync.
     */
    @Overwrite
    public CompletableFuture<Void> submit(Runnable runnable) {
        if (this.scheduleExecutables()) {
            return this.submitAsync(runnable);
        }
        runnable.run();
        return COMPLETED_FUTURE;
    }
}