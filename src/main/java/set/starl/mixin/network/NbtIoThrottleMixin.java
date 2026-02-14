package set.starl.mixin.network;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.concurrent.Semaphore;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.nbt.NbtIo;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(NbtIo.class)
public class NbtIoThrottleMixin {
	@Unique
	private static final int LOMKA$PARALLELISM = Integer.parseInt(System.getProperty("lomka.nbt.io.parallelism", "1"));

	@Unique
	private static final Semaphore LOMKA$GUARD = LOMKA$PARALLELISM > 0 ? new Semaphore(Math.min(64, Math.max(1, LOMKA$PARALLELISM))) : null;

	@Shadow
	private static DataInputStream createDecompressorStream(final InputStream in) throws IOException {
		throw new AssertionError();
	}

	@Shadow
	private static DataOutputStream createCompressorStream(final OutputStream out) throws IOException {
		throw new AssertionError();
	}

	@Shadow
	public static CompoundTag read(final java.io.DataInput input, final NbtAccounter accounter) throws IOException {
		throw new AssertionError();
	}

	@Shadow
	public static void write(final CompoundTag tag, final java.io.DataOutput output) throws IOException {
		throw new AssertionError();
	}

	@Inject(
		method = "readCompressed(Ljava/io/InputStream;Lnet/minecraft/nbt/NbtAccounter;)Lnet/minecraft/nbt/CompoundTag;",
		at = @At("HEAD"),
		cancellable = true
	)
	private static void lomka$readCompressedThrottled(final InputStream in, final NbtAccounter accounter, final CallbackInfoReturnable<CompoundTag> cir) throws IOException {
		Semaphore guard = LOMKA$GUARD;
		if (guard == null) {
			return;
		}

		guard.acquireUninterruptibly();
		try {
			try (DataInputStream dis = createDecompressorStream(in)) {
				cir.setReturnValue(read(dis, accounter));
			}
		} finally {
			guard.release();
		}
	}

	@Inject(
		method = "writeCompressed(Lnet/minecraft/nbt/CompoundTag;Ljava/io/OutputStream;)V",
		at = @At("HEAD"),
		cancellable = true
	)
	private static void lomka$writeCompressedThrottled(final CompoundTag tag, final OutputStream out, final CallbackInfo ci) throws IOException {
		Semaphore guard = LOMKA$GUARD;
		if (guard == null) {
			return;
		}

		guard.acquireUninterruptibly();
		try {
			try (DataOutputStream dos = createCompressorStream(out)) {
				write(tag, (java.io.DataOutput)dos);
			}
		} finally {
			guard.release();
		}
		ci.cancel();
	}
}
