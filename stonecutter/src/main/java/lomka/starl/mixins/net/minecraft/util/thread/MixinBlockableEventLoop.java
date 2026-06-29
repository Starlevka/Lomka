package lomka.starl.mixins.net.minecraft.util.thread;

import net.minecraft.util.thread.BlockableEventLoop;
import java.util.concurrent.locks.LockSupport;
import java.util.concurrent.Executor;
import java.util.concurrent.CompletableFuture;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

@Mixin(BlockableEventLoop.class)
public abstract class MixinBlockableEventLoop {

    /**
     * @author Starlev
     * @reason Direct executor lambda without wrapping into a vanilla RunnableFuture,
     * avoiding an allocation on every async task submission.
     */
    @Overwrite
    private CompletableFuture<Void> submitAsync(Runnable runnable) {
        return CompletableFuture.runAsync(runnable, (Executor) (Object) this);
    }

    /**
     * @author Starlev
     * @reason Use LockSupport.parkNanos with a fixed 100us budget instead of
     * Thread.yield() spin loop, reducing CPU waste when the event loop is idle.
     */
    @Overwrite
    protected void waitForTasks() {
        LockSupport.parkNanos("waiting for tasks", 100000L);
    }
}