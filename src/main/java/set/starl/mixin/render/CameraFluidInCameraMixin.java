package set.starl.mixin.render;

import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.FogType;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Camera.class)
public class CameraFluidInCameraMixin {
	@Unique
	private static final boolean LOMKA$ENABLE = !"false".equalsIgnoreCase(System.getProperty("lomka.camera.fluidFastPath", "true"));

	@Unique
	private static final ThreadLocal<BlockPos.MutableBlockPos> LOMKA$POS = ThreadLocal.withInitial(BlockPos.MutableBlockPos::new);

	@Unique
	private static int LOMKA$LAST_FOV = Integer.MIN_VALUE;

	@Unique
	private static int LOMKA$LAST_W = Integer.MIN_VALUE;

	@Unique
	private static int LOMKA$LAST_H = Integer.MIN_VALUE;

	@Unique
	private static double LOMKA$PLANE_HEIGHT = 0.0;

	@Unique
	private static double LOMKA$PLANE_WIDTH = 0.0;

	@Unique
	private long lomka$lastSigA = Long.MIN_VALUE;

	@Unique
	private long lomka$lastSigB = Long.MIN_VALUE;

	@Unique
	private FogType lomka$lastFog = FogType.NONE;

	@Shadow
	private boolean initialized;

	@Shadow
	private Level level;

	@Shadow
	private Entity entity;

	@Shadow
	private Vec3 position;

	@Shadow
	private BlockPos.MutableBlockPos blockPosition;

	@Shadow
	private Vector3f forwards;

	@Shadow
	private Vector3f up;

	@Shadow
	private Vector3f left;

	@Inject(method = "entity", at = @At("HEAD"), cancellable = true)
	private void lomka$entityFallback(final CallbackInfoReturnable<Entity> cir) {
		if (this.entity != null) {
			return;
		}
		Minecraft minecraft = Minecraft.getInstance();
		Entity fallback = minecraft.getCameraEntity();
		if (fallback != null) {
			cir.setReturnValue(fallback);
			return;
		}
		if (minecraft.player != null) {
			cir.setReturnValue(minecraft.player);
		}
	}

	@Inject(method = "getFluidInCamera", at = @At("HEAD"), cancellable = true)
	private void lomka$getFluidInCameraFast(final CallbackInfoReturnable<FogType> cir) {
		if (!LOMKA$ENABLE) {
			return;
		}
		if (!this.initialized) {
			cir.setReturnValue(FogType.NONE);
			return;
		}

		long sigA = lomka$sigA(this.position, this.blockPosition);
		long sigB = lomka$sigB(this.forwards, this.left, this.up);
		if (sigA == this.lomka$lastSigA && sigB == this.lomka$lastSigB) {
			cir.setReturnValue(this.lomka$lastFog);
			return;
		}

		FluidState fluidState1 = this.level.getFluidState(this.blockPosition);
		if (fluidState1.is(FluidTags.WATER) && this.position.y < (double)((float)this.blockPosition.getY() + fluidState1.getHeight(this.level, this.blockPosition))) {
			lomka$cacheAndReturn(FogType.WATER, sigA, sigB, cir);
			return;
		}

		Minecraft minecraft = Minecraft.getInstance();
		int w = minecraft.getWindow().getWidth();
		int h = minecraft.getWindow().getHeight();
		int fov = (Integer)minecraft.options.fov().get();
		if (fov != LOMKA$LAST_FOV || w != LOMKA$LAST_W || h != LOMKA$LAST_H) {
			LOMKA$LAST_FOV = fov;
			LOMKA$LAST_W = w;
			LOMKA$LAST_H = h;
			double aspectRatio = (double)w / (double)h;
			LOMKA$PLANE_HEIGHT = Math.tan((double)((float)fov * ((float)Math.PI / 180F)) / (double)2.0F) * (double)0.05F;
			LOMKA$PLANE_WIDTH = LOMKA$PLANE_HEIGHT * aspectRatio;
		}

		double px = this.position.x;
		double py = this.position.y;
		double pz = this.position.z;

		double fx = (double)this.forwards.x() * 0.05;
		double fy = (double)this.forwards.y() * 0.05;
		double fz = (double)this.forwards.z() * 0.05;

		double lx = (double)this.left.x() * LOMKA$PLANE_WIDTH;
		double ly = (double)this.left.y() * LOMKA$PLANE_WIDTH;
		double lz = (double)this.left.z() * LOMKA$PLANE_WIDTH;

		double ux = (double)this.up.x() * LOMKA$PLANE_HEIGHT;
		double uy = (double)this.up.y() * LOMKA$PLANE_HEIGHT;
		double uz = (double)this.up.z() * LOMKA$PLANE_HEIGHT;

		BlockPos.MutableBlockPos checkPos = LOMKA$POS.get();

		if (lomka$testFluidPoint(px + fx, py + fy, pz + fz, checkPos)) {
			lomka$cacheAndReturn(FogType.LAVA, sigA, sigB, cir);
			return;
		}

		if (lomka$testFluidPoint(px + fx - lx + ux, py + fy - ly + uy, pz + fz - lz + uz, checkPos)) {
			lomka$cacheAndReturn(FogType.LAVA, sigA, sigB, cir);
			return;
		}

		if (lomka$testFluidPoint(px + fx + lx + ux, py + fy + ly + uy, pz + fz + lz + uz, checkPos)) {
			lomka$cacheAndReturn(FogType.LAVA, sigA, sigB, cir);
			return;
		}

		if (lomka$testFluidPoint(px + fx - lx - ux, py + fy - ly - uy, pz + fz - lz - uz, checkPos)) {
			lomka$cacheAndReturn(FogType.LAVA, sigA, sigB, cir);
			return;
		}

		if (lomka$testFluidPoint(px + fx + lx - ux, py + fy + ly - uy, pz + fz + lz - uz, checkPos)) {
			lomka$cacheAndReturn(FogType.LAVA, sigA, sigB, cir);
			return;
		}

		lomka$cacheAndReturn(FogType.NONE, sigA, sigB, cir);
	}

	@Unique
	private void lomka$cacheAndReturn(final FogType fog, final long sigA, final long sigB, final CallbackInfoReturnable<FogType> cir) {
		this.lomka$lastSigA = sigA;
		this.lomka$lastSigB = sigB;
		this.lomka$lastFog = fog;
		cir.setReturnValue(fog);
	}

	@Unique
	private static long lomka$sigA(final Vec3 pos, final BlockPos.MutableBlockPos blockPos) {
		long a = Double.doubleToLongBits(pos.x);
		a ^= Long.rotateLeft(Double.doubleToLongBits(pos.y), 21);
		a ^= Long.rotateLeft(Double.doubleToLongBits(pos.z), 42);
		a ^= BlockPos.asLong(blockPos.getX(), blockPos.getY(), blockPos.getZ());
		return a;
	}

	@Unique
	private static long lomka$sigB(final Vector3f forwards, final Vector3f left, final Vector3f up) {
		long b = 1469598103934665603L;
		b = b * 1099511628211L + (long)Float.floatToIntBits(forwards.x());
		b = b * 1099511628211L + (long)Float.floatToIntBits(forwards.y());
		b = b * 1099511628211L + (long)Float.floatToIntBits(forwards.z());
		b = b * 1099511628211L + (long)Float.floatToIntBits(left.x());
		b = b * 1099511628211L + (long)Float.floatToIntBits(left.y());
		b = b * 1099511628211L + (long)Float.floatToIntBits(left.z());
		b = b * 1099511628211L + (long)Float.floatToIntBits(up.x());
		b = b * 1099511628211L + (long)Float.floatToIntBits(up.y());
		b = b * 1099511628211L + (long)Float.floatToIntBits(up.z());
		return b;
	}

	@Unique
	private boolean lomka$testFluidPoint(final double x, final double y, final double z, final BlockPos.MutableBlockPos checkPos) {
		int bx = Mth.floor(x);
		int by = Mth.floor(y);
		int bz = Mth.floor(z);
		checkPos.set(bx, by, bz);

		FluidState fluidState = this.level.getFluidState(checkPos);
		if (fluidState.is(FluidTags.LAVA)) {
			return y <= (double)(fluidState.getHeight(this.level, checkPos) + (float)by);
		}

		BlockState state = this.level.getBlockState(checkPos);
		return state.is(Blocks.POWDER_SNOW);
	}
}

