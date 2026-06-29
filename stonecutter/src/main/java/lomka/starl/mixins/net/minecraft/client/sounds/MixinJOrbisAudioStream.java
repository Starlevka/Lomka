package lomka.starl.mixins.net.minecraft.client.sounds;

import com.jcraft.jogg.Packet;
import com.jcraft.jorbis.Block;
import com.jcraft.jorbis.DspState;
import com.jcraft.jorbis.Info;
import it.unimi.dsi.fastutil.floats.FloatConsumer;
import net.minecraft.client.sounds.JOrbisAudioStream;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;

import java.io.IOException;

@Mixin(JOrbisAudioStream.class)
public abstract class MixinJOrbisAudioStream {

    @Shadow private Info info;
    @Shadow private Block block;
    @Shadow private DspState dspState;

    @Shadow private @Nullable Packet readPacket() throws IOException { return null; }
    @Shadow private long getSamplesToWrite(int i) { return 0L; }
    @Shadow private static boolean isError(int i) { return false; }
    @Shadow private static void copyMono(float[] data, int off, long n, FloatConsumer fc) {}
    @Shadow private static void copyStereo(float[] l, int lo, float[] r, int ro, long n, FloatConsumer fc) {}
    @Shadow private static void copyAnyChannels(float[][] data, int ch, int[] offs, long n, FloatConsumer fc) {}

    @Unique private final float[][][] lomka$pcmBuffer = new float[1][][];

    @Unique private int @Nullable [] lomka$pcmIndex;

    /**
     * @author Starlev
     * @reason Eliminates two heap allocations per decoded audio chunk: float[1][][]
     * and int[channels]. Both are output containers that JOrbis unconditionally
     * overwrites before any field is read, making reuse across calls safe.
     */
    @Overwrite(remap = false)
    public boolean readChunk(FloatConsumer floatconsumer) throws IOException {
        int[] aint = this.lomka$pcmIndex;
        if (aint == null) {
            aint = new int[this.info.channels];
            this.lomka$pcmIndex = aint;
        }

        Packet packet = this.readPacket();
        if (packet == null) {
            return false;
        } else if (isError(this.block.synthesis(packet))) {
            throw new IOException("Can't decode audio packet");
        } else {
            this.dspState.synthesis_blockin(this.block);

            float[][][] pcmBuffer = this.lomka$pcmBuffer;
            int i;
            for (; (i = this.dspState.synthesis_pcmout(pcmBuffer, aint)) > 0; this.dspState.synthesis_read(i)) {
                float[][] afloat1 = pcmBuffer[0];
                long j = this.getSamplesToWrite(i);
                switch (this.info.channels) {
                    case 1:
                        copyMono(afloat1[0], aint[0], j, floatconsumer);
                        break;
                    case 2:
                        copyStereo(afloat1[0], aint[0], afloat1[1], aint[1], j, floatconsumer);
                        break;
                    default:
                        copyAnyChannels(afloat1, this.info.channels, aint, j, floatconsumer);
                }
            }
            return true;
        }
    }
}