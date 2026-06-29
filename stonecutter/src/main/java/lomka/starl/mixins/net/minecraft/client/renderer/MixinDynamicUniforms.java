package lomka.starl.mixins.net.minecraft.client.renderer;

import com.mojang.blaze3d.buffers.GpuBufferSlice;
import net.minecraft.client.renderer.DynamicUniformStorage;
import net.minecraft.client.renderer.DynamicUniforms;
import org.joml.Matrix4f;
import org.joml.Matrix4fc;
import org.joml.Vector3f;
import org.joml.Vector3fc;
import org.joml.Vector4f;
import org.joml.Vector4fc;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.Overwrite;

@Mixin(DynamicUniforms.class)
public abstract class MixinDynamicUniforms {

    @Shadow @Final private DynamicUniformStorage<DynamicUniforms.Transform> transforms;
    @Shadow @Final private DynamicUniformStorage<DynamicUniforms.ChunkSectionInfo> chunkSections;

    @Unique private static final Vector4f lomka$WHITE = new Vector4f(1.0F, 1.0F, 1.0F, 1.0F);
    @Unique private static final Vector3f lomka$ZERO = new Vector3f();

    @Unique private final Matrix4f lomka$lastModelView = new Matrix4f();
    @Unique private final Matrix4f lomka$lastTexMatrix = new Matrix4f();
    @Unique private final Vector4f lomka$lastColor = new Vector4f();
    @Unique private final Vector3f lomka$lastOffset = new Vector3f();
    @Unique private DynamicUniforms.Transform lomka$lastTransform;

    /**
     * @author Starlev
     * @reason Optimize the hot path by avoiding dynamic allocations of uniform components when the state remains unchanged, and eliminating Mixin CallbackInfo overhead.
     */
    @Overwrite
    public GpuBufferSlice writeTransform(
        //? if >=26.2 {
        /*Matrix4f modelView, Vector4f color, Vector3f offset, Matrix4f texMatrix*/
        //?} else {
        Matrix4fc modelView, Vector4fc color, Vector3fc offset, Matrix4fc texMatrix
        //?}
    ) {
        if (this.lomka$lastTransform != null &&
            this.lomka$lastModelView.equals(modelView) &&
            this.lomka$lastColor.equals(color) &&
            this.lomka$lastOffset.equals(offset) &&
            this.lomka$lastTexMatrix.equals(texMatrix)) {

            return this.transforms.writeUniform(this.lomka$lastTransform);
        }

        this.lomka$lastModelView.set(modelView);
        this.lomka$lastColor.set(color);
        this.lomka$lastOffset.set(offset);
        this.lomka$lastTexMatrix.set(texMatrix);

        Vector4fc finalColor = (color.x() == 1.0F && color.y() == 1.0F && color.z() == 1.0F && color.w() == 1.0F) ? lomka$WHITE : new Vector4f(color);
        Vector3fc finalOffset = (offset.x() == 0.0F && offset.y() == 0.0F && offset.z() == 0.0F) ? lomka$ZERO : new Vector3f(offset);

        DynamicUniforms.Transform newTransform = new DynamicUniforms.Transform(new Matrix4f(modelView), finalColor, finalOffset, new Matrix4f(texMatrix));
        this.lomka$lastTransform = newTransform;

        return this.transforms.writeUniform(newTransform);
    }

    @Overwrite
    public void reset() {
        this.transforms.endFrame();
        this.chunkSections.endFrame();
        this.lomka$lastTransform = null;
    }
}