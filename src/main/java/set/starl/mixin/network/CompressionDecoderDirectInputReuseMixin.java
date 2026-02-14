package set.starl.mixin.network;

import java.nio.ByteBuffer;
import net.minecraft.network.CompressionDecoder;
import set.starl.Lomka;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(CompressionDecoder.class)
public class CompressionDecoderDirectInputReuseMixin {
	@Unique
	private static final boolean LOMKA$ENABLE = Lomka.JAVA_22_25_OPTIMIZATIONS
		&& !"false".equalsIgnoreCase(System.getProperty("lomka.network.compressionDecoder.directInputReuse", "true"));

	@Unique
	private static final int LOMKA$MAX_KEEP_BYTES = Integer.parseInt(System.getProperty("lomka.network.compressionDecoder.directInputReuse.maxKeepBytes", "4194304"));

	@Unique
	private static final ThreadLocal<ByteBuffer> LOMKA$DIRECT_INPUT = new ThreadLocal<>();

	@Redirect(
		method = "setupInflaterInput(Lio/netty/buffer/ByteBuf;)V",
		at = @At(value = "INVOKE", target = "Ljava/nio/ByteBuffer;allocateDirect(I)Ljava/nio/ByteBuffer;")
	)
	private ByteBuffer lomka$reuseAllocateDirect(final int capacity) {
		if (!LOMKA$ENABLE || capacity <= 0) {
			return ByteBuffer.allocateDirect(capacity);
		}

		if (LOMKA$MAX_KEEP_BYTES > 0 && capacity > LOMKA$MAX_KEEP_BYTES) {
			return ByteBuffer.allocateDirect(capacity);
		}

		ByteBuffer buf = LOMKA$DIRECT_INPUT.get();
		if (buf == null || buf.capacity() < capacity) {
			int newCap = lomka$nextPow2AtLeast(capacity);
			if (LOMKA$MAX_KEEP_BYTES > 0 && newCap > LOMKA$MAX_KEEP_BYTES) {
				newCap = capacity;
			}
			buf = ByteBuffer.allocateDirect(newCap);
			LOMKA$DIRECT_INPUT.set(buf);
		}

		buf.clear();
		buf.limit(capacity);
		return buf;
	}

	@Unique
	private static int lomka$nextPow2AtLeast(final int x) {
		int v = x - 1;
		v |= v >>> 1;
		v |= v >>> 2;
		v |= v >>> 4;
		v |= v >>> 8;
		v |= v >>> 16;
		return v + 1;
	}
}
