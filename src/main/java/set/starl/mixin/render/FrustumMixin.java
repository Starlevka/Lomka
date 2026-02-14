package set.starl.mixin.render;

import net.minecraft.client.renderer.culling.Frustum;
import org.joml.FrustumIntersection;
import org.joml.Vector4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Frustum.class)
public class FrustumMixin {
	@Unique
	private static final int LOMKA$CAMERA_CUBE_MODE = lomka$cameraCubeMode();

	@Shadow
	@Final
	private FrustumIntersection intersection;

	@Shadow
	private Vector4f viewVector;

	@Shadow
	private double camX;

	@Shadow
	private double camY;

	@Shadow
	private double camZ;

	@Inject(method = "offsetToFullyIncludeCameraCube", at = @At("HEAD"), cancellable = true)
	private void lomka$aggressiveCameraCube(final int cubeSize, final CallbackInfoReturnable<Frustum> cir) {
		int mode = LOMKA$CAMERA_CUBE_MODE;
		if (mode == 0) {
			return;
		}

		if (cubeSize <= 0) {
			cir.setReturnValue((Frustum)(Object)this);
			return;
		}

		if (mode == 1) {
			cir.setReturnValue((Frustum)(Object)this);
			return;
		}

		double camX1 = lomka$fastFloor(this.camX / (double)cubeSize) * (double)cubeSize;
		double camY1 = lomka$fastFloor(this.camY / (double)cubeSize) * (double)cubeSize;
		double camZ1 = lomka$fastFloor(this.camZ / (double)cubeSize) * (double)cubeSize;
		double camX2 = lomka$fastCeil(this.camX / (double)cubeSize) * (double)cubeSize;
		double camY2 = lomka$fastCeil(this.camY / (double)cubeSize) * (double)cubeSize;
		double camZ2 = lomka$fastCeil(this.camZ / (double)cubeSize) * (double)cubeSize;

		while (true) {
			int r = this.intersection.intersectAab(
				(float)(camX1 - this.camX),
				(float)(camY1 - this.camY),
				(float)(camZ1 - this.camZ),
				(float)(camX2 - this.camX),
				(float)(camY2 - this.camY),
				(float)(camZ2 - this.camZ)
			);
			if (r != 0) {
				break;
			}
			this.camX -= (double)(this.viewVector.x() * 4.0F);
			this.camY -= (double)(this.viewVector.y() * 4.0F);
			this.camZ -= (double)(this.viewVector.z() * 4.0F);
		}

		cir.setReturnValue((Frustum)(Object)this);
	}

	@Redirect(method = "offsetToFullyIncludeCameraCube", at = @At(value = "INVOKE", target = "Ljava/lang/Math;floor(D)D"))
	private double lomka$fastFloor(final double v) {
		long i = (long)v;
		return v < (double)i ? (double)(i - 1L) : (double)i;
	}

	@Redirect(method = "offsetToFullyIncludeCameraCube", at = @At(value = "INVOKE", target = "Ljava/lang/Math;ceil(D)D"))
	private double lomka$fastCeil(final double v) {
		long i = (long)v;
		return v > (double)i ? (double)(i + 1L) : (double)i;
	}

	@Unique
	private static int lomka$cameraCubeMode() {
		String mode = System.getProperty("lomka.culling.frustum.cameraCubeMode", "vanilla");
		if (mode == null) {
			return 0;
		}
		mode = mode.toLowerCase();
		if ("full".equals(mode) || "vanilla".equals(mode)) {
			return 0;
		}
		if ("none".equals(mode) || "off".equals(mode) || "false".equals(mode) || "0".equals(mode)) {
			return 1;
		}
		return 2;
	}
}
