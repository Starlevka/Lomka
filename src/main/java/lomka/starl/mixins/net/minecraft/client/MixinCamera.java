package lomka.starl.mixins.net.minecraft.client;

import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.FogType;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = Camera.class, priority = 500)
public abstract class MixinCamera {

    @Shadow private Vec3 position;
    @Shadow @Final private Quaternionf rotation;
    @Shadow protected abstract void setPosition(double d, double e, double f);
    @Shadow @Final private Vector3f forwards;
    @Shadow private Entity entity;
    //? if >=1.21.11 {
    @Shadow private Level level;
    //?} else {
    /*@Shadow private BlockGetter level;
    *///?}
    @Shadow @Final private BlockPos.MutableBlockPos blockPosition;
    @Shadow private boolean initialized;
    @Shadow @Final private Vector3f left;
    @Shadow @Final private Vector3f up;

    @Unique private final Vector3f lomka$moveVector = new Vector3f();

    @Unique private double lomka$npFwdX, lomka$npFwdY, lomka$npFwdZ;
    @Unique private double lomka$npLftX, lomka$npLftY, lomka$npLftZ;
    @Unique private double lomka$npUpX,  lomka$npUpY,  lomka$npUpZ;

    @Unique private final BlockPos.MutableBlockPos lomka$fluidPos = new BlockPos.MutableBlockPos();

    @Unique private final double[] lomka$dx = new double[5];
    @Unique private final double[] lomka$dy = new double[5];
    @Unique private final double[] lomka$dz = new double[5];

    @Unique private FogType lomka$cachedWaterResult  = FogType.NONE;
    @Unique private int     lomka$lastWaterPosX      = Integer.MIN_VALUE;
    @Unique private int     lomka$lastWaterPosY      = Integer.MIN_VALUE;
    @Unique private int     lomka$lastWaterPosZ      = Integer.MIN_VALUE;
    @Unique private double  lomka$lastWaterPosYFloat = Double.NaN;

    @Unique private FogType lomka$cachedLavaSnowResult = FogType.NONE;
    @Unique private double  lomka$lastLSPosX           = Double.NaN;
    @Unique private double  lomka$lastLSPosY           = Double.NaN;
    @Unique private double  lomka$lastLSPosZ           = Double.NaN;
    @Unique private float   lomka$lastRotW             = Float.NaN;
    @Unique private float   lomka$lastRotX             = Float.NaN;
    @Unique private float   lomka$lastRotY             = Float.NaN;
    @Unique private float   lomka$lastRotZ             = Float.NaN;

    //? if >=1.21 {
    /**
     * @author Starlev
     * @reason Use JOML vector rotation to set the camera position in one go,
     * avoiding manual per-component math and temporary object allocations.
     */
    @Overwrite
    public void move(float f, float f1, float f2) {
        this.lomka$moveVector.set(f2, f1, -f).rotate(this.rotation);
        this.setPosition(
                this.position.x + (double) this.lomka$moveVector.x,
                this.position.y + (double) this.lomka$moveVector.y,
                this.position.z + (double) this.lomka$moveVector.z
        );
    }
    //?}

    //? if >=1.21 {
    /**
     * @author Starlev
     * @reason Use Mth.square/Mth.sqrt instead of distanceToSqr, and shadow the
     * level field directly instead of going through Minecraft.getInstance().level.
     * FIX vs previous revision: forward vector must be rescaled by the CURRENT f
     * each iteration, since f can shrink mid-loop (vanilla recomputes vec31 every
     * iteration); precomputing it once before the loop used the original,
     * un-shrunk f for every ray after the first hit.
     */
    @Overwrite
    public float getMaxZoom(float f) {
        double fux = this.forwards.x();
        double fuy = this.forwards.y();
        double fuz = this.forwards.z();

        for (int i = 0; i < 8; ++i) {
            float f2 = (float) ((i & 1) * 2 - 1);
            float f3 = (float) ((i >> 1 & 1) * 2 - 1);
            float f4 = (float) ((i >> 2 & 1) * 2 - 1);

            double startX = this.position.x + (double) (f2 * 0.1F);
            double startY = this.position.y + (double) (f3 * 0.1F);
            double startZ = this.position.z + (double) (f4 * 0.1F);
            double negF   = (double) (-f);

            Vec3 vec3  = new Vec3(startX, startY, startZ);
            Vec3 vec31 = new Vec3(startX + fux * negF, startY + fuy * negF, startZ + fuz * negF);

            BlockHitResult blockhitresult = this.level.clip(
                    new ClipContext(vec3, vec31, ClipContext.Block.VISUAL, ClipContext.Fluid.NONE, this.entity));

            if (blockhitresult.getType() != HitResult.Type.MISS) {
                float f5 = (float) blockhitresult.getLocation().distanceToSqr(this.position);
                if (f5 < Mth.square(f)) {
                    f = Mth.sqrt(f5);
                }
            }
        }

        return f;
    }
    //?}

    /**
     * @author Starlev
     * @reason Keeps the 26.1+ near-plane path on the same lightweight custom structure as
     *         the older versions and avoids the heavier vanilla object setup.
     */
    //? if >=26.1 {
    /*@Overwrite
    public Camera.NearPlane getNearPlane(float fov) {
        lomka$computeNearPlane(fov);

        Vec3 vec3  = new Vec3(lomka$npFwdX, lomka$npFwdY, lomka$npFwdZ);
        Vec3 vec31 = new Vec3(lomka$npLftX, lomka$npLftY, lomka$npLftZ);
        Vec3 vec32 = new Vec3(lomka$npUpX,  lomka$npUpY,  lomka$npUpZ);

        return new Camera.NearPlane(vec3, vec31, vec32);
    }*/
    //?} else {
    @Overwrite
    public Camera.NearPlane getNearPlane() {
        lomka$computeNearPlane();

        Vec3 vec3  = new Vec3(lomka$npFwdX, lomka$npFwdY, lomka$npFwdZ);
        Vec3 vec31 = new Vec3(lomka$npLftX, lomka$npLftY, lomka$npLftZ);
        Vec3 vec32 = new Vec3(lomka$npUpX,  lomka$npUpY,  lomka$npUpZ);

        return new Camera.NearPlane(vec3, vec31, vec32);
    }
    //?}

    /*
     * FIX vs previous revision: the division by 2.0D must stay INSIDE Math.tan(...) —
     * the correct formula is tan(radians / 2) * 0.05, not tan(radians) / 2 * 0.05.
     * These are not equivalent: at the default 70 FOV the buggy form was roughly
     * double the correct near-plane half-height, and past ~90 FOV tan(radians)
     * approaches/crosses its asymptote, producing huge or negative values instead
     * of the small, well-behaved values the correct half-angle formula gives.
     */
    //? if >=26.1 {
    /*@Unique
    private void lomka$computeNearPlane(float fov) {
        double d1 = Math.tan((double) (fov * 0.017453292F) / 2.0D) * 0.05000000074505806D;*/
    //?} else {
    @Unique
    private void lomka$computeNearPlane() {
        Minecraft minecraft = Minecraft.getInstance();
        double d1 = Math.tan((double) ((float) (Integer) minecraft.options.fov().get() * 0.017453292F) / 2.0D)
                * 0.05000000074505806D;
    //?}
        double d2 = d1 * (double) Minecraft.getInstance().getWindow().getWidth()
                / (double) Minecraft.getInstance().getWindow().getHeight();

        lomka$npFwdX = this.forwards.x() * 0.05000000074505806D;
        lomka$npFwdY = this.forwards.y() * 0.05000000074505806D;
        lomka$npFwdZ = this.forwards.z() * 0.05000000074505806D;
        lomka$npLftX = this.left.x() * d2;
        lomka$npLftY = this.left.y() * d2;
        lomka$npLftZ = this.left.z() * d2;
        lomka$npUpX  = this.up.x() * d1;
        lomka$npUpY  = this.up.y() * d1;
        lomka$npUpZ  = this.up.z() * d1;
    }

    /**
     * @author Starlev
     * @reason Cache fluid-in-camera results to skip block lookups when the camera
     *         has not moved. Also reuses pre-allocated arrays and MutableBlockPos to
     *         eliminate per-frame allocation of Vec3/Vec3i objects. FIX vs previous
     *         revision: dx[1] (top-left corner, forward+up+left) used up.y (uy) in the
     *         X-component instead of up.x (ux) — at pitch~0, up.x~0 while up.y~1, so the
     *         bug injected an error of roughly the full near-plane half-height into the
     *         X-coordinate of that corner for nearly any camera orientation. Uses the
     *         shadowed level field instead of Minecraft.getInstance().level.
     */
    @Overwrite
    public FogType getFluidInCamera() {
        if (!this.initialized) {
            return FogType.NONE;
        }

        //? if >=1.21.11 {
        Level level = this.level;
        //?} else {
        /*BlockGetter level = this.level;
        *///?}

        int bx = this.blockPosition.getX();
        int by = this.blockPosition.getY();
        int bz = this.blockPosition.getZ();

        if (bx == lomka$lastWaterPosX
         && by == lomka$lastWaterPosY
         && bz == lomka$lastWaterPosZ
         && Double.compare(this.position.y, lomka$lastWaterPosYFloat) == 0) {
            if (lomka$cachedWaterResult == FogType.WATER) {
                return FogType.WATER;
            }
        } else {
            FluidState fluidstate = level.getFluidState(this.blockPosition);
            lomka$lastWaterPosX      = bx;
            lomka$lastWaterPosY      = by;
            lomka$lastWaterPosZ      = bz;
            lomka$lastWaterPosYFloat = this.position.y;

            if (fluidstate.is(FluidTags.WATER)
                    && this.position.y < (double) ((float) by + fluidstate.getHeight(level, this.blockPosition))) {
                lomka$cachedWaterResult = FogType.WATER;
                return FogType.WATER;
            } else {
                lomka$cachedWaterResult = FogType.NONE;
                lomka$lastLSPosX        = Double.NaN;
            }
        }

        float rw  = this.rotation.w();
        float rx  = this.rotation.x();
        float ry  = this.rotation.y();
        float rz  = this.rotation.z();
        double px = this.position.x;
        double py = this.position.y;
        double pz = this.position.z;

        boolean lsHit = (Double.compare(px, lomka$lastLSPosX) == 0
                      && Double.compare(py, lomka$lastLSPosY) == 0
                      && Double.compare(pz, lomka$lastLSPosZ) == 0
                      &&  Float.compare(rw, lomka$lastRotW) == 0
                      &&  Float.compare(rx, lomka$lastRotX) == 0
                      &&  Float.compare(ry, lomka$lastRotY) == 0
                      &&  Float.compare(rz, lomka$lastRotZ) == 0);

        if (lsHit) {
            return lomka$cachedLavaSnowResult;
        }

        //? if >=26.1 {
        /*lomka$computeNearPlane((float) (Integer) Minecraft.getInstance().options.fov().get());*/
        //?} else {
        lomka$computeNearPlane();
        //?}

        double fx = lomka$npFwdX, fy = lomka$npFwdY, fz = lomka$npFwdZ;
        double lx = lomka$npLftX, ly = lomka$npLftY, lz = lomka$npLftZ;
        double ux = lomka$npUpX,  uy = lomka$npUpY,  uz = lomka$npUpZ;

        double[] dx = this.lomka$dx;
        double[] dy = this.lomka$dy;
        double[] dz = this.lomka$dz;

        dx[0] = fx;        dy[0] = fy;        dz[0] = fz;
        dx[1] = fx+ux+lx;  dy[1] = fy+uy+ly;  dz[1] = fz+uz+lz;
        dx[2] = fx+ux-lx;  dy[2] = fy+uy-ly;  dz[2] = fz+uz-lz;
        dx[3] = fx-ux+lx;  dy[3] = fy-uy+ly;  dz[3] = fz-uz+lz;
        dx[4] = fx-ux-lx;  dy[4] = fy-uy-ly;  dz[4] = fz-uz-lz;

        BlockPos.MutableBlockPos checkPos = this.lomka$fluidPos;
        FogType result = FogType.NONE;

        outer:
        for (int i = 0; i < 5; i++) {
            double cx = px + dx[i];
            double cy = py + dy[i];
            double cz = pz + dz[i];
            checkPos.set(cx, cy, cz);

            FluidState fs = level.getFluidState(checkPos);
            if (fs.is(FluidTags.LAVA)) {
                if (cy <= (double) (fs.getHeight(level, checkPos) + (float) checkPos.getY())) {
                    result = FogType.LAVA;
                    break outer;
                }
            } else if (level.getBlockState(checkPos).is(Blocks.POWDER_SNOW)) {
                result = FogType.POWDER_SNOW;
                break outer;
            }
        }

        lomka$cachedLavaSnowResult = result;
        lomka$lastLSPosX = px;
        lomka$lastLSPosY = py;
        lomka$lastLSPosZ = pz;
        lomka$lastRotW   = rw;
        lomka$lastRotX   = rx;
        lomka$lastRotY   = ry;
        lomka$lastRotZ   = rz;

        return result;
    }

    /**
     * Invalidates the cached water/LS fog inputs on camera reset so a freshly re-created
     * camera never reuses stale positions for underwater/biome fog lookup.
     */
    @Inject(method = "reset()V", at = @At("TAIL"))
    private void onReset(CallbackInfo ci) {
        this.lomka$lastWaterPosX        = Integer.MIN_VALUE;
        this.lomka$lastWaterPosY        = Integer.MIN_VALUE;
        this.lomka$lastWaterPosZ        = Integer.MIN_VALUE;
        this.lomka$lastWaterPosYFloat   = Double.NaN;
        this.lomka$cachedWaterResult    = FogType.NONE;
        this.lomka$lastLSPosX           = Double.NaN;
        this.lomka$cachedLavaSnowResult = FogType.NONE;
    }
}