package lomka.starl.mixins.net.minecraft.client.multiplayer;

import net.minecraft.client.multiplayer.ClientCommonPacketListenerImpl;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

@Mixin(ClientCommonPacketListenerImpl.class)
public abstract class MixinClientCommonPacketListenerImpl {

    /**
     * @author Starlev
     * @reason Removes the deferred packet processing loop entirely.
     *         Deferred packets (keep-alive, resource pack status, etc.) are
     *         sent immediately via sendWhen() when their condition is already
     *         met at queue time, so the per-tick iteration overhead is unnecessary.
     */
    @Overwrite
    protected void sendDeferredPackets() {
    }
}