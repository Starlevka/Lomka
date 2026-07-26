package lomka.starl.mixins.net.minecraft.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.ItemInHandRenderer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;

@Mixin(ItemInHandRenderer.class)
public abstract class MixinItemInHandRenderer {

    @Shadow
    @Final
    private ItemModelResolver itemModelResolver;

    @Unique
    private final ItemStackRenderState lomka$rightHandState = new ItemStackRenderState();
    @Unique
    private final ItemStackRenderState lomka$leftHandState = new ItemStackRenderState();

    /**
     * @author Starlev
     * @reason Reuses one ItemStackRenderState per on-screen hand slot instead of allocating
     * a fresh one every call. renderItem() runs every frame for every visible hand, so vanilla's
     * "new ItemStackRenderState()" here is a guaranteed per-frame allocation in a method that runs
     * up to twice a frame. Modeled directly on vanilla's own mapRenderState field in this same
     * class, which already holds a single reusable RenderState instance across calls instead of
     * allocating fresh -- same family of object, same reuse contract.
     * Assumes renderItem() is only ever called from this class's own FIRST_PERSON_LEFT_HAND /
     * FIRST_PERSON_RIGHT_HAND render paths; a third-party caller passing some other
     * ItemDisplayContext would fall through to the right-hand slot rather than crash, but could
     * clobber an in-flight render if it overlaps with a real hand render in the same frame.
     */
    @Overwrite
    public void renderItem(LivingEntity livingentity, ItemStack itemstack, ItemDisplayContext itemdisplaycontext, PoseStack posestack, SubmitNodeCollector submitnodecollector, int i) {
        if (!itemstack.isEmpty()) {
            ItemStackRenderState state = itemdisplaycontext == ItemDisplayContext.FIRST_PERSON_LEFT_HAND
                    ? this.lomka$leftHandState
                    : this.lomka$rightHandState;

            this.itemModelResolver.updateForTopItem(state, itemstack, itemdisplaycontext, livingentity.level(), livingentity, livingentity.getId() + itemdisplaycontext.ordinal());
            state.submit(posestack, submitnodecollector, i, OverlayTexture.NO_OVERLAY, 0);
        }
    }
}