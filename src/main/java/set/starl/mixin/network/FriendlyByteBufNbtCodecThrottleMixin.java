package set.starl.mixin.network;

import io.netty.buffer.ByteBuf;
import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.concurrent.Semaphore;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.Tag;
import net.minecraft.network.FriendlyByteBuf;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(FriendlyByteBuf.class)
public class FriendlyByteBufNbtCodecThrottleMixin {
	@Unique
	private static final int LOMKA$PARALLELISM = Integer.parseInt(System.getProperty("lomka.nbt.codec.parallelism", "1"));

	@Unique
	private static final Semaphore LOMKA$GUARD = LOMKA$PARALLELISM > 0 ? new Semaphore(Math.min(64, Math.max(1, LOMKA$PARALLELISM))) : null;

	@Redirect(
		method = "readNbt(Lio/netty/buffer/ByteBuf;Lnet/minecraft/nbt/NbtAccounter;)Lnet/minecraft/nbt/Tag;",
		at = @At(
			value = "INVOKE",
			target = "Lnet/minecraft/nbt/NbtIo;readAnyTag(Ljava/io/DataInput;Lnet/minecraft/nbt/NbtAccounter;)Lnet/minecraft/nbt/Tag;"
		)
	)
	private static Tag lomka$readAnyTagThrottled(final DataInput input, final NbtAccounter accounter) throws IOException {
		Semaphore guard = LOMKA$GUARD;
		if (guard == null) {
			return NbtIo.readAnyTag(input, accounter);
		}

		guard.acquireUninterruptibly();
		try {
			return NbtIo.readAnyTag(input, accounter);
		} finally {
			guard.release();
		}
	}

	@Redirect(
		method = "writeNbt(Lio/netty/buffer/ByteBuf;Lnet/minecraft/nbt/Tag;)V",
		at = @At(
			value = "INVOKE",
			target = "Lnet/minecraft/nbt/NbtIo;writeAnyTag(Lnet/minecraft/nbt/Tag;Ljava/io/DataOutput;)V"
		)
	)
	private static void lomka$writeAnyTagThrottled(final Tag tag, final DataOutput output) throws IOException {
		Semaphore guard = LOMKA$GUARD;
		if (guard == null) {
			NbtIo.writeAnyTag(tag, output);
			return;
		}

		guard.acquireUninterruptibly();
		try {
			NbtIo.writeAnyTag(tag, output);
		} finally {
			guard.release();
		}
	}
}
