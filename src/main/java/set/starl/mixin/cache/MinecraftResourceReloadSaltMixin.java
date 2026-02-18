package set.starl.mixin.cache;

import java.util.concurrent.CompletableFuture;
import net.minecraft.client.Minecraft;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Coerce;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import set.starl.Lomka;

@Mixin(Minecraft.class)
public class MinecraftResourceReloadSaltMixin {
	@Inject(
		method = "reloadResourcePacks(ZLnet/minecraft/client/Minecraft$GameLoadCookie;)Ljava/util/concurrent/CompletableFuture;",
		at = @At(value = "INVOKE", target = "Lnet/minecraft/server/packs/repository/PackRepository;reload()V", shift = At.Shift.BEFORE)
	)
	private void lomka$bumpResourceReloadSalt(final boolean isRecovery, final @Coerce @Nullable Object loadCookie, final CallbackInfoReturnable<CompletableFuture> cir) {
		Lomka.nextResourceReloadSalt();
	}
}
