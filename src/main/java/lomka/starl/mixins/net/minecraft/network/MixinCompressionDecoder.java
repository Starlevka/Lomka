package lomka.starl.mixins.net.minecraft.network;

import io.netty.buffer.ByteBuf;
import java.nio.ByteBuffer;
import java.util.zip.Inflater;
import net.minecraft.network.CompressionDecoder;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(CompressionDecoder.class)
public abstract class MixinCompressionDecoder {

    @Shadow @Final private Inflater inflater;

    /**
     * @author Starlev
     * @reason Use internalNioBuffer (Netty's cached-wrapper accessor, already used by
     * inflate() three lines below in this same class) instead of nioBuffer, which
     * allocates a fresh ByteBuffer view via duplicate() on every compressed packet.
     */
    @Overwrite
    private void setupInflaterInput(ByteBuf bytebuf) {
        ByteBuffer bytebuffer;
        int readable = bytebuf.readableBytes();

        if (bytebuf.nioBufferCount() > 0) {
            bytebuffer = bytebuf.internalNioBuffer(bytebuf.readerIndex(), readable);
            bytebuf.skipBytes(readable);
        } else {
            bytebuffer = ByteBuffer.allocateDirect(readable);
            bytebuf.readBytes(bytebuffer);
            bytebuffer.flip();
        }

        this.inflater.setInput(bytebuffer);
    }
}