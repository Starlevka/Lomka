package lomka.starl.mixins.net.minecraft.client.renderer;

import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
//? if <26.1 {
import net.minecraft.client.renderer.state.CameraRenderState;
import net.minecraft.client.renderer.state.LevelRenderState;
//?}
import org.joml.Quaternionf;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(GameRenderer.class)
public abstract class MixinGameRenderer {

    @Shadow @Final private Minecraft minecraft;
    @Shadow @Final private Camera mainCamera;

    //? if <26.1 {
    @Shadow @Final private LevelRenderState levelRenderState;
    //?}

    //? if <26.1 {
    /**
     * @author Starlev
     * @reason Direct field assignment instead of per-frame Copies/extractCamera
     * overhead. Avoids the builder/copier pattern used in vanilla for camera state sync.
     */
    @Overwrite
    private void extractCamera(float f) {
        CameraRenderState state = this.levelRenderState.cameraRenderState;
        state.initialized = this.mainCamera.isInitialized();
        state.pos = this.mainCamera.position();
        //? if >=1.21.11 {
        state.blockPos = this.mainCamera.blockPosition();
        state.entityPos = this.mainCamera.entity().getPosition(f);
        //?} else {
        /*state.blockPos = this.mainCamera.getBlockPosition();
        state.entityPos = this.mainCamera.getEntity().getPosition(f);*/
        //?}

        if (state.orientation == null) {
            state.orientation = new Quaternionf(this.mainCamera.rotation());
        } else {
            state.orientation.set(this.mainCamera.rotation());
        }
    }
    //?}
}