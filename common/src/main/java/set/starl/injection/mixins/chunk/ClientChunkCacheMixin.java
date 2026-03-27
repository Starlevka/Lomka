package set.starl.injection.mixins.chunk;

import net.minecraft.client.multiplayer.ClientChunkCache;
import org.slf4j.Logger;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import set.starl.LomkaCore;

@Mixin(ClientChunkCache.class)
public class ClientChunkCacheMixin {
    @Unique
    private static final boolean LOMKA$SILENCE = LomkaCore.CONFIG.chunks.enable && LomkaCore.CONFIG.chunks.silenceIgnoreOutOfRangeLog;

	@Redirect(
		method = "replaceBiomes(IILnet/minecraft/network/FriendlyByteBuf;)V",
		at = @At(value = "INVOKE", target = "Lorg/slf4j/Logger;warn(Ljava/lang/String;Ljava/lang/Object;Ljava/lang/Object;)V", remap = false),
		require = 0
	)
	private void lomka$silenceIgnoreOutOfRangeLogBiomes(Logger logger, String message, Object p0, Object p1) {
		if (LOMKA$SILENCE && "Ignoring chunk since it's not in the view range: {}, {}".equals(message)) {
			return;
		}
		logger.warn(message, p0, p1);
	}

	@Redirect(
		method = "replaceWithPacketData(IILnet/minecraft/network/FriendlyByteBuf;Ljava/util/Map;Ljava/util/function/Consumer;)Lnet/minecraft/world/level/chunk/LevelChunk;",
		at = @At(value = "INVOKE", target = "Lorg/slf4j/Logger;warn(Ljava/lang/String;Ljava/lang/Object;Ljava/lang/Object;)V", remap = false),
		require = 0
	)
	private void lomka$silenceIgnoreOutOfRangeLogPacket(Logger logger, String message, Object p0, Object p1) {
		if (LOMKA$SILENCE && "Ignoring chunk since it's not in the view range: {}, {}".equals(message)) {
			return;
		}
		logger.warn(message, p0, p1);
	}
}