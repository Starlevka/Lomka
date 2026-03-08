package set.starl.injection.mixins.chunk;

import net.minecraft.core.BlockPos;
import net.minecraft.util.Util;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import net.minecraft.client.renderer.LevelRenderer;
import set.starl.Lomka;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LevelRenderer.class)
public class LevelRendererTranslucencyResortThrottleMixin {
	@Unique
	private static final boolean LOMKA$ENABLE = Lomka.CONFIG.render.translucencyResortThrottle;

	@Unique
	private static final long LOMKA$MAX_SKIP_MS = 100L;

	@Unique
	private static final double LOMKA$MIN_MOVE = 0.03125;

	@Unique
	private long lomka$lastRunMs;

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
		if (!LOMKA$ENABLE || LOMKA$MAX_SKIP_MS <= 0L) {
			return;
		}

		long now = Util.getMillis();
		if (now - this.lomka$lastRunMs >= LOMKA$MAX_SKIP_MS) {
			return;
		}

		int bx = Mth.floor(cameraPos.x);
		int by = Mth.floor(cameraPos.y);
		int bz = Mth.floor(cameraPos.z);
		long blockLong = BlockPos.asLong(bx, by, bz);
		if (blockLong != this.lomka$lastBlockLong) {
			// Crosses chunk/block boundary, need resort
			return;
		}

		double dx = cameraPos.x - this.lomka$lastX;
		double dy = cameraPos.y - this.lomka$lastY;
		double dz = cameraPos.z - this.lomka$lastZ;
		double minMoveSq = LOMKA$MIN_MOVE * LOMKA$MIN_MOVE;
		if (dx * dx + dy * dy + dz * dz < minMoveSq) {
			// Sub-block movement and haven't passed skip threshold yet
			ci.cancel();
		}
	}

	@Inject(method = "scheduleTranslucentSectionResort(Lnet/minecraft/world/phys/Vec3;)V", at = @At("RETURN"))
	private void lomka$rememberResortState(final Vec3 cameraPos, final CallbackInfo ci) {
		this.lomka$lastRunMs = Util.getMillis();
		this.lomka$lastX = cameraPos.x;
		this.lomka$lastY = cameraPos.y;
		this.lomka$lastZ = cameraPos.z;
		int bx = Mth.floor(cameraPos.x);
		int by = Mth.floor(cameraPos.y);
		int bz = Mth.floor(cameraPos.z);
		this.lomka$lastBlockLong = BlockPos.asLong(bx, by, bz);
	}
}
