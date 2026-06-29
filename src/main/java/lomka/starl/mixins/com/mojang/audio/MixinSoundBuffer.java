package lomka.starl.mixins.com.mojang.blaze3d.audio;

import com.mojang.blaze3d.audio.OpenAlUtil;
import com.mojang.blaze3d.audio.SoundBuffer;
import org.jspecify.annotations.Nullable;
import org.lwjgl.openal.AL10;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

import java.nio.ByteBuffer;
import java.util.OptionalInt;
import javax.sound.sampled.AudioFormat;

@Mixin(SoundBuffer.class)
public abstract class MixinSoundBuffer {

    @Shadow private @Nullable ByteBuffer data;
    @Shadow private AudioFormat format;
    @Shadow private boolean hasAlBuffer;
    @Shadow private int alBuffer;

    /**
     * @author Starlev
     * @reason Avoid allocating an int[1] array when generating OpenAL buffers.
     *         LWJGL 3 provides alGenBuffers() which returns the ID directly.
     */
    @Overwrite
    public OptionalInt getAlBuffer() {
        if (!this.hasAlBuffer) {
            if (this.data == null) {
                return OptionalInt.empty();
            }

            int i = OpenAlUtil.audioFormatToOpenAl(this.format);

            int bufferId = AL10.alGenBuffers();
            if (OpenAlUtil.checkALError("Creating buffer")) {
                return OptionalInt.empty();
            }

            AL10.alBufferData(bufferId, i, this.data, (int) this.format.getSampleRate());
            if (OpenAlUtil.checkALError("Assigning buffer data")) {
                return OptionalInt.empty();
            }

            this.alBuffer = bufferId;
            this.hasAlBuffer = true;
            this.data = null;
        }

        return OptionalInt.of(this.alBuffer);
    }

    /**
     * @author Starlev
     * @reason Avoid allocating an int[1] array when deleting OpenAL buffers.
     */
    @Overwrite
    public void discardAlBuffer() {
        if (this.hasAlBuffer) {
            AL10.alDeleteBuffers(this.alBuffer);
            if (OpenAlUtil.checkALError("Deleting stream buffers")) {
                return;
            }
        }

        this.hasAlBuffer = false;
    }
}