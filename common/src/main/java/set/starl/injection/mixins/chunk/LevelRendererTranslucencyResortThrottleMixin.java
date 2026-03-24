package set.starl.injection.mixins.chunk;

import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import net.minecraft.client.renderer.LevelRenderer;
import set.starl.LomkaCore;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LevelRenderer.class)
public class LevelRendererTranslucencyResortThrottleMixin {
    @Unique
    private static final boolean LOMKA$ENABLE = LomkaCore.CONFIG.render.translucencyResortThrottle;

    @Unique
    private static final long LOMKA$MAX_SKIP_NS = 100_000_000L; // 100ms in nanos

    @Unique
    private static final double LOMKA$MIN_MOVE_SQ = 0.03125D * 0.03125D; // Pre-compute square

    @Unique
    private long lomka$lastRunNs;

    @Unique
    private long lomka$lastBlockLong = Long.MIN_VALUE;

    @Unique
    private double lomka$lastX;

    @Unique
    private double lomka$lastY;

    @Unique
    private double lomka$lastZ;

    @Inject(method = "scheduleTranslucentSectionResort(Lnet/minecraft/world/phys/Vec3;)V", at = @At("HEAD"), cancellable = true)
    private void lomka$throttleTranslucentResort(final Vec3 cameraPos, final CallbackInfo ci) {
        if (!LOMKA$ENABLE || LOMKA$MAX_SKIP_NS <= 0L) {
            return;
        }

        // Use System.nanoTime() — monotonic, no wall-clock jumps, faster than Util.getMillis()
        long now = System.nanoTime();
        if (now - this.lomka$lastRunNs >= LOMKA$MAX_SKIP_NS) {
            return;
        }

        // Fast block position — avoid Mth.floor overhead
        int bx = (int)Math.floor(cameraPos.x);
        int by = (int)Math.floor(cameraPos.y);
        int bz = (int)Math.floor(cameraPos.z);
        long blockLong = BlockPos.asLong(bx, by, bz);
        if (blockLong != this.lomka$lastBlockLong) {
            return;
        }

        double dx = cameraPos.x - this.lomka$lastX;
        double dy = cameraPos.y - this.lomka$lastY;
        double dz = cameraPos.z - this.lomka$lastZ;
        if (dx * dx + dy * dy + dz * dz < LOMKA$MIN_MOVE_SQ) {
            ci.cancel();
        }
    }

    @Inject(method = "scheduleTranslucentSectionResort(Lnet/minecraft/world/phys/Vec3;)V", at = @At("RETURN"))
    private void lomka$rememberResortState(final Vec3 cameraPos, final CallbackInfo ci) {
        this.lomka$lastRunNs = System.nanoTime();
        this.lomka$lastX = cameraPos.x;
        this.lomka$lastY = cameraPos.y;
        this.lomka$lastZ = cameraPos.z;
        int bx = (int)Math.floor(cameraPos.x);
        int by = (int)Math.floor(cameraPos.y);
        int bz = (int)Math.floor(cameraPos.z);
        this.lomka$lastBlockLong = BlockPos.asLong(bx, by, bz);
    }
}