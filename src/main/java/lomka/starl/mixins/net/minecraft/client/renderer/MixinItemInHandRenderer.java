package lomka.starl.mixins.net.minecraft.client.renderer;

import net.minecraft.client.renderer.ItemInHandRenderer;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(ItemInHandRenderer.class)
public abstract class MixinItemInHandRenderer {

    @Unique
    private final ItemStackRenderState lomka$scratchRenderState = new ItemStackRenderState();

    /**
     * @author Starlev
     * @reason renderItem() is the shared per-entity hand-item renderer (ItemInHandLayer
     * calls it for every visible LivingEntity holding an item, not just the local
     * player's first-person hands), so its per-call allocation scales with visible
     * mob/player count rather than being bounded to 1-2 calls per frame like the rest
     * of this class. The resolver method is named updateForTopItem — not createFor... —
     * implying the API already expects to overwrite an existing instance rather than
     * build a fresh one each time. ItemInHandRenderer is a render-thread singleton
     * (one per game session), so a plain instance field is sufficient — no ThreadLocal
     * needed. Constructor visibility is confirmed public: vanilla itself instantiates
     * ItemStackRenderState from a different package (net.minecraft.client.renderer vs
     * net.minecraft.client.renderer.item), which is only possible if it's public.
     * ASSUMES updateForTopItem() fully resets prior render-layer state before
     * repopulating (no stale data bleeding between different entities' items) and
     * that renderItem() is never re-entered for a second item before the first call's
     * submit() has fully consumed the render state — verify against
     * ItemModelResolver/ItemStackRenderState source before shipping.
     */
    @Redirect(
        method = "renderItem(Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/item/ItemDisplayContext;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;I)V",
        at = @At(value = "NEW", target = "net/minecraft/client/renderer/item/ItemStackRenderState")
    )
    private ItemStackRenderState lomka$reuseRenderState() {
        return this.lomka$scratchRenderState;
    }
}