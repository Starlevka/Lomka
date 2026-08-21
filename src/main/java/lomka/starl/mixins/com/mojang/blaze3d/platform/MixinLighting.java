package lomka.starl.mixins.com.mojang.blaze3d.platform;

import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.platform.Lighting;
import com.mojang.blaze3d.systems.RenderSystem;
//? if >=26.1 {
/*import net.minecraft.world.level.CardinalLighting;
*///?} else if >=1.21.11 {
import net.minecraft.world.level.dimension.DimensionType;
//?} else {
//?}
//? if <26.2 {
import org.joml.Vector3f;
//?} else {
import org.joml.Vector3fc;
//?}
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Lighting.class)
public abstract class MixinLighting {

    @Shadow @Final private GpuBuffer buffer;
    //? if >=1.21.11 {
    @Shadow @Final private long paddedSize;
    //?} else {
    /*@Shadow @Final private int paddedSize;*/
    //?}
    @Shadow @Final public static int UBO_SIZE;

    //? if <26.2 {
    @Shadow @Final private static Vector3f DIFFUSE_LIGHT_0;
    @Shadow @Final private static Vector3f DIFFUSE_LIGHT_1;
    @Shadow @Final private static Vector3f NETHER_DIFFUSE_LIGHT_0;
    @Shadow @Final private static Vector3f NETHER_DIFFUSE_LIGHT_1;
    //?} else {
    /*@Shadow @Final private static Vector3fc DIFFUSE_LIGHT_0;
    @Shadow @Final private static Vector3fc DIFFUSE_LIGHT_1;
    @Shadow @Final private static Vector3fc NETHER_DIFFUSE_LIGHT_0;
    @Shadow @Final private static Vector3fc NETHER_DIFFUSE_LIGHT_1;*/
    //?}

    //? if <26.2 {
    @Shadow
    private void updateBuffer(Lighting.Entry lighting_entry, Vector3f vector3f, Vector3f vector3f1) {}
    //?} else {
    /*@Shadow
    private void updateBuffer(Lighting.Entry lighting_entry, Vector3fc vector3f, Vector3fc vector3f1) {}*/
    //?}

    @Unique
    private GpuBufferSlice[] lomka$entrySlices;

    //? if >=26.1 {
    /*@Unique
    private CardinalLighting.Type lomka$currentLightType;*/
    //?} else if >=1.21.11 {
    @Unique
    private DimensionType.CardinalLightType lomka$currentLightType;
    //?} else {
    /*@Unique
    private Boolean lomka$currentNether;*/
    //?}

    /**
     * Precomputes one GpuBufferSlice per Lighting.Entry at construction so setupFor()
     * never has to allocate or slice the shared buffer on the hot render path.
     */
    @Inject(method = "<init>", at = @At("RETURN"))
    private void lomka$initSlices(CallbackInfo ci) {
        Lighting.Entry[] entries = Lighting.Entry.values();
        this.lomka$entrySlices = new GpuBufferSlice[entries.length];
        for (int i = 0; i < entries.length; i++) {
            //? if >=1.21.11 {
            this.lomka$entrySlices[i] = this.buffer.slice((long) i * this.paddedSize, Lighting.UBO_SIZE);
            //?} else {
            /*this.lomka$entrySlices[i] = this.buffer.slice(i * this.paddedSize, this.paddedSize);
            *///?}
        }
    }

    /**
     * @author Starlev
     * @reason Eliminates GpuBufferSlice allocations on hot render paths by using precomputed buffer slice instances.
     */
    @Overwrite
    public void setupFor(Lighting.Entry lighting_entry) {
        RenderSystem.setShaderLights(this.lomka$entrySlices[lighting_entry.ordinal()]);
    }

    /**
     * @author Starlev
     * @reason Skips redundant GPU UBO writes when the dimension cardinal light type has not changed.
     */
    //? if >=26.1 {
    /*@Overwrite
    public void updateLevel(CardinalLighting.Type type) {
        if (this.lomka$currentLightType == type) {
            return;
        }
        this.lomka$currentLightType = type;
        if (type == CardinalLighting.Type.NETHER) {
            this.updateBuffer(Lighting.Entry.LEVEL, NETHER_DIFFUSE_LIGHT_0, NETHER_DIFFUSE_LIGHT_1);
        } else {
            this.updateBuffer(Lighting.Entry.LEVEL, DIFFUSE_LIGHT_0, DIFFUSE_LIGHT_1);
        }
    }
    *///?} else if >=1.21.11 {
    
    /** 
     * @author Starlev
     * @reason Skips redundant GPU UBO writes when the nether flag has not changed.
     */
    @Overwrite
    public void updateLevel(DimensionType.CardinalLightType type) {
        if (this.lomka$currentLightType == type) {
            return;
        }
        this.lomka$currentLightType = type;
        if (type == DimensionType.CardinalLightType.NETHER) {
            this.updateBuffer(Lighting.Entry.LEVEL, NETHER_DIFFUSE_LIGHT_0, NETHER_DIFFUSE_LIGHT_1);
        } else {
            this.updateBuffer(Lighting.Entry.LEVEL, DIFFUSE_LIGHT_0, DIFFUSE_LIGHT_1);
        }
    }
    //?} else {
    /*@Overwrite
    public void updateLevel(boolean flag) {
        Boolean current = this.lomka$currentNether;
        if (current != null && current == flag) {
            return;
        }
        this.lomka$currentNether = flag;
        if (flag) {
            this.updateBuffer(Lighting.Entry.LEVEL, NETHER_DIFFUSE_LIGHT_0, NETHER_DIFFUSE_LIGHT_1);
        } else {
            this.updateBuffer(Lighting.Entry.LEVEL, DIFFUSE_LIGHT_0, DIFFUSE_LIGHT_1);
        }
    }
    *///?}
}